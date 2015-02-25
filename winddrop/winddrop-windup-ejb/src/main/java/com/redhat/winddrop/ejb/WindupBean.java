package com.redhat.winddrop.ejb;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.windup.bootstrap.Bootstrap;

/**
 * This EJB calls windup. It has been put in a separate jar for isolating the classloaders of windup legacy and wundup 2.
 * 
 * @author Marc
 */
@Singleton
@Startup
public class WindupBean {

	private final static Logger LOG = Logger.getLogger(WindupBean.class.toString());
	private final static String HOME = "/opt/jboss/jboss-eap-6.3/modules/system/layers/base/org/jboss/windup/2.1.0.Final";

	/**
	 * Configures and executes Windup.
	 * 
	 * @param packageSignature
	 * @param windupInputFile
	 * @param windupOutputDirectory
	 * @throws IOException
	 */
	public void executeWindup(String packageSignature, File windupInputFile, File windupOutputDirectory) {

		LOG.info(">>> executeWindup");

		/*
		Marc@RedBook ~/Downloads/windup-distribution-2.1.0.A/bin $
		./windup --evaluate "windup-migrate-app --input /Users/Marc/Downloads/windup-distribution-2.1.0.A/samples/jee-example-app-1.0.0.ear --output OUTPUT_REPORT --packages com.acme"
Using Windup at /Users/Marc/Downloads/windup-distribution-2.1.0.A
"/Library/Java/JavaVirtualMachines/jdk1.7.0_71.jdk/Contents/Home/bin/java" 
-XX:MaxPermSize=256m -XX:ReservedCodeCacheSize=128m 
"-Dforge.standalone=true" 
"-Dforge.home=/Users/Marc/Downloads/windup-distribution-2.1.0.A"
"-Dwindup.home=/Users/Marc/Downloads/windup-distribution-2.1.0.A"

-cp "/Users/Marc/Downloads/windup-distribution-2.1.0.A/lib/*" org.jboss.windup.bootstrap.Bootstrap
"--evaluate" 
"windup-migrate-app --input /Users/Marc/Downloads/windup-distribution-2.1.0.A/samples/jee-example-app-1.0.0.ear --output OUTPUT_REPORT --packages com.acme"
--immutableAddonDir /Users/Marc/Downloads/windup-distribution-2.1.0.A/addons/
		*/


		
		System.setProperty("forge.standalone", "true");
		System.setProperty("forge.home", HOME);
		System.setProperty("windup.home", HOME);
		
		final String[] args = new String[4];
		args[0] = "--evaluate";
		args[1] = "windup-migrate-app --input "+windupInputFile.getAbsolutePath()+" --output "+windupOutputDirectory.getAbsolutePath()+" --packages com.acme";
		args[2] = "--immutableAddonDir";
		args[3] = "/Users/Marc/Downloads/windup-distribution-2.1.0.A/addons/";		
		
		try {
			Bootstrap.main(args);
		} catch (Throwable t) {
			LOG.logrb(Level.SEVERE, WindupBean.class.getSimpleName(), "executeWindup", "com.redhat.winddrop.ejb", "Error", t);
			t.printStackTrace();
		}

		LOG.info("<<< executeWindup");

		// // Map the environment settings from the input arguments.
		// WindupEnvironment settings = new WindupEnvironment();
		// settings.setPackageSignature(packageSignature);
		// // settings.setExcludeSignature("excludePkgs");
		// // settings.setTargetPlatform("targetPlatform");
		// settings.setFetchRemote("false");
		//
		// boolean isSource = false;
		// //if (BooleanUtils.toBoolean("source")) {
		// // isSource = true;
		// //}
		//
		// boolean captureLog = true;
		// //if (BooleanUtils.toBoolean("captureLog")) {
		// // captureLog = true;
		// //}
		// String logLevel = "INFO";
		//
		// settings.setSource(isSource);
		// settings.setCaptureLog(captureLog);
		// settings.setLogLevel(logLevel);
		//
		// LOG.info("captureLog " + captureLog);
		// LOG.info("logLevel " + logLevel);
		// LOG.info("isSource " + isSource);
		//
		// // Run Windup
		// new ReportEngine(settings).process(windupInputFile, windupOutputDirectory);

	}

}
