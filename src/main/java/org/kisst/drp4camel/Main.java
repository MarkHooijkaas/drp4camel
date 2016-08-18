package org.kisst.drp4camel;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
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
		final CamelContext context = new DefaultCamelContext();
		//context.setTracing(true);
		context.start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try { context.stop(); }
				catch (Exception e) { throw new RuntimeException(e); }
			}
		});

		final RouteLoader loader=new RouteLoader(context, new File("config/routes"));
		loader.load();


		sleepForEver();
	}

	void sleepForEver() {
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