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
import java.util.List;
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
		for (File f : dir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(".xml")) {
				RouteFileInfo info=routeFiles.get(f.getName());
				RouteFileInfo newInfo=new RouteFileInfo(f);
				if (info!=null && newInfo.md5.equals(info.md5)) {
					LOG.info("Skipping loading routes from {} since MD5 is not changed",f);
					continue;
				}
				loadRoute(context, f);
				routeFiles.put(f.getName(),newInfo);
			}
		}
	}

	private static int routeCounter=0;
	private static void loadRoute(CamelContext context, File f) {
		LOG.info("Loading routes from {}", f.getName());
		try (InputStream is = new FileInputStream(f)) {
			RoutesDefinition routes = context.loadRoutesDefinition(is);
			for (RouteDefinition r:routes.getRoutes()) {
				String id = r.getId();
				if (id==null) {
					String name = r.getInputs().get(0).getUri();
					name=name.substring(name.indexOf(':')+1);
					r.setId(name + '-' + (routeCounter++));
				}
			}
			context.addRouteDefinitions(routes.getRoutes());
		}
		catch (Exception e) { throw new RuntimeException(e);}
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
		public final String md5;

		private RouteFileInfo(File f) {
			this.md5 = Md5Checksum.getMD5Checksum(f);
		}
	}
}
