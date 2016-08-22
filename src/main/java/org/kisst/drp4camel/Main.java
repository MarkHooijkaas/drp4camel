package org.kisst.drp4camel;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.kisst.drp4camel.drp.DrpComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Main {
	static Logger LOG = LoggerFactory.getLogger(Main.class);
	public static void main(String[] args) throws Exception {
		configureLogging("config/logback.xml");
		new Main().run();
	}
	void run() throws Exception {
		SimpleRegistry registry = new SimpleRegistry();
		JacksonDataFormat df = new JacksonDataFormat();
		//df.disableFeature(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		//df.disableFeature(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
		df.enableFeature(SerializationFeature.INDENT_OUTPUT);
		registry.put("jackson", df);

		final CamelContext context = new DefaultCamelContext(registry);
		context.addComponent("drp", new DrpComponent());
		//context.setTracing(true);
		context.start();


		final RouteLoader loader=new RouteLoader(context, new File("config/routes/dynamic"));
		registry.put("loader", loader);

		RouteLoader.loadRoutes(context, new File("config/routes/admin"));

		loader.loadRoutes();

		sleepForEver(context);
	}

	void sleepForEver(CamelContext context) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try { context.stop(); }
				catch (Exception e) { throw new RuntimeException(e); }
			}
		});
		while (true) {
			try {
				Thread.sleep(Long.MAX_VALUE);
			}
			catch (InterruptedException e) { break; }
		}
	}

	private static void configureLogging(String file){
		// assume SLF4J is bound to logback in the current environment
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			// Call context.reset() to clear any previous configuration, e.g. default
			// configuration. For multi-step configuration, omit calling context.reset().
			context.reset();
			configurator.doConfigure(file);
		} catch (JoranException je) {
			// StatusPrinter will handle this
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);

	}
}