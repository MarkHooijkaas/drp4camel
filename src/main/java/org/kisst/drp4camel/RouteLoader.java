package org.kisst.drp4camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.kisst.drp4camel.drp.Md5Checksum;
import org.kisst.util.TraceItem;
import org.kisst.util.TraceLog;
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
	private final File dir;
	private final int recurseDepth;

	private final ConcurrentHashMap<File,RouteFileInfo> routeFiles = new ConcurrentHashMap<>();

	public RouteLoader(CamelContext context, File dir) { this(context, dir, 0); }
	public RouteLoader(CamelContext context, File dir, int recurseDepth){
		this.context=context;
		this.dir=dir;
		this.recurseDepth=recurseDepth;
	}

	public List<RouteInfo> list() {
		List<Route> routes = context.getRoutes();
		List<RouteInfo> result = new ArrayList<>(routes.size());
		for (Route r : routes)
			result.add(new RouteInfo(r));
		return result;
	}

	public TraceLog loadRoutes() {
		TraceLog trace= new TraceLog(true);
		return loadSubRoutes(trace, dir,0);
	}

	public TraceLog loadSubRoutes(TraceLog trace, File subdir, int depth) {
		long timestamp=System.currentTimeMillis();
		// make a clone of all known routes, which will be removed, unless they are unchanged
		LinkedHashMap<File, RouteFileInfo> toBeRemoved = new LinkedHashMap<>();
		Set<File> oldFiles = new HashSet<>(routeFiles.keySet());
		for (File f : subdir.listFiles()) {
			if (f.isDirectory()) {
				if (depth<recurseDepth)
					loadSubRoutes(trace, f, depth+1);
			}
			else if (f.isFile() && f.getName().endsWith(".xml")) {
				oldFiles.remove(f);
				RouteFileInfo info=routeFiles.get(f);
				String md5 = Md5Checksum.getMD5Checksum(f);
				if (info!=null && md5.equals(info.md5)) {
					trace.info(new SkippingFileTrace(f));
				}
				else {
					List<String> routes = loadRouteFile(trace,f);
					if (info!=null)
						toBeRemoved.put(f,info);
					info = new RouteFileInfo(timestamp, md5, routes);
					routeFiles.put(f,info);
				}
			}
		}
		removeRoutes(trace,toBeRemoved);
		removeFiles(trace,oldFiles);
		return trace;
	}

	private static int routeCounter=0;
	private List<String> loadRouteFile(TraceLog trace, File f) {
		trace.info(new LoadingFileTrace(f));
		List<String> result=new ArrayList<>();
		try (InputStream is = new FileInputStream(f)) {
			RoutesDefinition routes = context.loadRoutesDefinition(is);
			//System.out.println(r.getDescription().getLang());
			System.out.println("SHORT:"+routes.getShortName());
			System.out.println("LABEL:"+routes.getLabel());

			for (RouteDefinition r:routes.getRoutes()) {
				//System.out.println(r.getShortName());
				System.out.println(r.getGroup());
				System.out.println(r.getLabel());
				//for (String key:r.getProperties().keySet())
				//	System.out.println(key+"\t"+r.getProperties().get(key));

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
				trace.info(new AddingRoute(f,id));
			}
			context.addRouteDefinitions(routes.getRoutes());
			return result;
		}
		catch (Exception e) { throw new RuntimeException(e);}
	}

	private void removeRoutes(TraceLog trace, LinkedHashMap<File, RouteFileInfo> oldRouteFiles) {
		for (File filename: oldRouteFiles.keySet()) {
			RouteFileInfo rf=oldRouteFiles.get(filename);
			for (String id:rf.routes) {
				try {
					if (id.startsWith("@@")) {
						// TODO: check if route is still there
						RouteFileInfo rfnew = this.routeFiles.get(filename);
						if (rfnew!=null && rfnew.routes.contains(id))
							trace.info(new KeepingStaticNamedRoute(filename, id));
						else {
							trace.info(new RemovingStaticNamedRoute(filename,id));
							id = id.substring(2);
							context.removeRoute(id);
						}
					}
					else {
						trace.info(new RemovingDynamicNamedRoute(filename,id));
						context.removeRoute(id);
					}
				}
				catch (Exception e) { throw new RuntimeException(e);} // TODO: remove other routes?
			}
		}

	}

	private void removeFiles(TraceLog trace, Set<File> oldFiles) {
		for (File f: oldFiles) {
			RouteFileInfo rf=routeFiles.get(f);
			for (String id:rf.routes) {
				try {
					if (id.startsWith("@@"))
						id = id.substring(2);
					trace.info(new RemovingDeletedFileRoute(f, id));
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

	public class LoadingFileTrace extends TraceItem.MessageBase1<File> {
		public LoadingFileTrace(File file) { super("Loading route file {}",file); }
	}
	public class SkippingFileTrace extends TraceItem.MessageBase1<File> {
		public SkippingFileTrace(File file) { super("Skipping route file {}",file); }
	}
	public class AddingRoute extends TraceItem.MessageBase2<File,String> {
		public AddingRoute(File file, String routeId) { super("\tAdding route from file {} with id {}",file,routeId); }
	}
	public class RemovingDynamicNamedRoute extends TraceItem.MessageBase2<File,String> {
		public RemovingDynamicNamedRoute(File file, String routeId) { super("\tRemoving dynamically named route from file {} with id {}",file,routeId); }
	}
	public class KeepingStaticNamedRoute extends TraceItem.MessageBase2<File,String> {
		public KeepingStaticNamedRoute (File file, String routeId) { super("\tKeeping static named route from file {} with id {}",file,routeId); }
	}
	public class RemovingStaticNamedRoute extends TraceItem.MessageBase2<File,String> {
		public RemovingStaticNamedRoute (File file, String routeId) { super("\tRemoving static named route from file {} with id {}",file,routeId); }
	}
	public class RemovingDeletedFileRoute extends TraceItem.MessageBase2<File,String> {
		public RemovingDeletedFileRoute (File file, String routeId) { super("\tRemoving route from deleted file {} with id {}",file,routeId); }
	}

}
