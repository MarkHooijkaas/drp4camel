package org.kisst.drp4camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.kisst.drp4camel.drp.Md5Checksum;
import org.kisst.drp4j.NonPuller;
import org.kisst.drp4j.ResourcePuller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RouteLoader {
	private final static Logger LOG = LoggerFactory.getLogger(RouteLoader.class);
	private final CamelContext context;
	private final ResourcePuller puller;
	private final File dir;
	private final ConcurrentHashMap<String,RouteFileInfo> routeFiles = new ConcurrentHashMap<>();


	public RouteLoader(CamelContext context, File dir){
		this(context, new NonPuller(dir));
	}

	public RouteLoader(CamelContext context, ResourcePuller puller){
		this.context=context;
		this.puller=puller;
		this.dir=puller.getLocalDirectory();
	}

	public void pullRoutes() {
		puller.pull();
		loadRoutes(dir);
	}

	public void loadRoutes() {
		loadRoutes(dir);
	}


	public List<RouteInfo> list() {
		List<Route> routes = context.getRoutes();
		List<RouteInfo> result = new ArrayList<>(routes.size());
		for (Route r : routes)
			result.add(new RouteInfo(r));
		return result;
	}

	public void loadRoutes(File dir) {
		long timestamp=System.currentTimeMillis();
		// make a clone of all known routes, which will be removed, unless they are unchanged
		LinkedHashMap<String, RouteFileInfo> toBeRemoved = new LinkedHashMap<String, RouteFileInfo>();
		Set<String> oldFiles = new HashSet<>(routeFiles.keySet());
		for (File f : dir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(".xml")) {
				oldFiles.remove(f.getName());
				RouteFileInfo info=routeFiles.get(f.getName());
				String md5 = Md5Checksum.getMD5Checksum(f);
				if (info!=null && md5.equals(info.md5)) {
					LOG.info("Skipping loading routes from {} since MD5 is not changed",f);
				}
				else {
					List<String> routes = loadRouteFile(f);
					if (info!=null)
						toBeRemoved.put(f.getName(),info);
					info = new RouteFileInfo(timestamp, md5, routes);
					routeFiles.put(f.getName(),info);
				}
			}
		}
		removeRoutes(toBeRemoved);
		removeFiles(oldFiles);
	}

	private static int routeCounter=0;
	private List<String> loadRouteFile(File f) {
		LOG.info("Loading routes from {}", f.getName());
		List<String> result=new ArrayList<>();
		try (InputStream is = new FileInputStream(f)) {
			RoutesDefinition routes = context.loadRoutesDefinition(is);
			for (RouteDefinition r:routes.getRoutes()) {
				String id = r.getId();
				if (id==null) {
					String name = r.getInputs().get(0).getUri();
					name=f.getName()+"/"+name.substring(name.indexOf(':')+1);
					id=name + '-' + (routeCounter++);
					r.setId(id);
				}
				else
					id="@@"+id;
				result.add(id);
			}
			context.addRouteDefinitions(routes.getRoutes());
			return result;
		}
		catch (Exception e) { throw new RuntimeException(e);}
	}

	private void removeRoutes(LinkedHashMap<String, RouteFileInfo> oldRouteFiles) {
		for (String filename: oldRouteFiles.keySet()) {
			RouteFileInfo rf=oldRouteFiles.get(filename);
			for (String id:rf.routes) {
				try {
					if (id.startsWith("@@")) {
						// TODO: check if route is still there
						RouteFileInfo rfnew = this.routeFiles.get(filename);
						if (rfnew!=null && rfnew.routes.contains(id))
							LOG.info("keeping statically named route {} in file {}", id, filename);
						else {
							LOG.info("removing statically named route {} in file {}", id, filename);
							id = id.substring(2);
							context.removeRoute(id);
						}
					}
					else {
						LOG.info("removing dynamically named route {} in file {}", id, filename);
						context.removeRoute(id);
					}
				}
				catch (Exception e) { throw new RuntimeException(e);} // TODO: remove other routes?
			}
		}

	}

	private void removeFiles(Set<String> oldFiles) {
		for (String filename: oldFiles) {
			RouteFileInfo rf=routeFiles.get(filename);
			for (String id:rf.routes) {
				try {
					if (id.startsWith("@@"))
						id = id.substring(2);
					LOG.info("removing route {} in deleted file {}", id, filename);
					context.removeRoute(id);
				}
				catch (Exception e) { throw new RuntimeException(e);} // TODO: remove other routes?
			}
		}
	}


	/** Class to be easily used by Jackson
	 */
	public static class RouteInfo {
		public final String id;
		public final String description;
		public final String consumer;
		public final String endpoint;

		public RouteInfo(Route r) {
			this.id=r.getId();
			this.description=r.getDescription();
			this.consumer=r.getConsumer().getClass().getSimpleName();
			this.endpoint=r.getEndpoint().toString();
		}
	}

	public static class RouteFileInfo {
		public final long timestamp;
		public final String md5;
		public final List<String> routes;

		public RouteFileInfo(long timestamp, String md5, List<String> routes) {
			this.timestamp=timestamp;
			this.md5=md5;
			this.routes=routes;
		}
	}
}
