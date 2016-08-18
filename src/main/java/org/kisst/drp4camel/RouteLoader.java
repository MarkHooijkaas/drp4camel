package org.kisst.drp4camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.model.RoutesDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

public class RouteLoader {
	private final static Logger LOG = LoggerFactory.getLogger(RouteLoader.class);
	private final CamelContext context;
	private final File dir;

	public RouteLoader(CamelContext context, File dir){
		this.context=context;
		this.dir=dir;
	}

	public void load() { loadRoutes(context, dir);}
	public List<Route> list() { return context.getRoutes();}

	public static void loadRoutes(CamelContext context, File dir) {
		for (File f : dir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(".xml"))
				loadRoute(context, f);
		}
	}
	private static void loadRoute(CamelContext context, File f) {
		LOG.info("Loading routes from {}", f.getName());
		try (InputStream is = new FileInputStream(f)) {
			RoutesDefinition routes = context.loadRoutesDefinition(is);
			context.addRouteDefinitions(routes.getRoutes());
		}
		catch (Exception e) { throw new RuntimeException(e);}
	}
}
