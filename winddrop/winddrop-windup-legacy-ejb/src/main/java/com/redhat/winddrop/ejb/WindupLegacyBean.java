package com.redhat.winddrop.ejb;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.windup.WindupEnvironment;
import org.jboss.windup.reporting.ReportEngine;


/**
 * This EJB calls windup. It has been put in a separate jar for isolating the classloaders of windup legacy and wundup 2.
 * 
 * @author Marc
 */
@Singleton
@Startup
public class WindupLegacyBean {

	private final static Logger LOG = Logger.getLogger(WindupLegacyBean.class.toString());

	/**
	 * Configures and executes Windup.
	 * 
	 * @param packageSignature
	 * @param windupInputFile
	 * @param windupOutputDirectory
	 * @throws IOException
	 */
	public void executeWindup(String packageSignature, File windupInputFile, File windupOutputDirectory) throws IOException {

		// Map the environment settings from the input arguments.
		WindupEnvironment settings = new WindupEnvironment();
		settings.setPackageSignature(packageSignature);
		// settings.setExcludeSignature("excludePkgs");
		// settings.setTargetPlatform("targetPlatform");
		settings.setFetchRemote("false");

		boolean isSource = false;
		//if (BooleanUtils.toBoolean("source")) {
		//	isSource = true;
		//}

		boolean captureLog = true;
		//if (BooleanUtils.toBoolean("captureLog")) {
		//	captureLog = true;
		//}
		String logLevel = "INFO";
				
		settings.setSource(isSource);
		settings.setCaptureLog(captureLog);
		settings.setLogLevel(logLevel);

		LOG.info("captureLog " + captureLog);
		LOG.info("logLevel " + logLevel);
		LOG.info("isSource " + isSource);

		// Run Windup
		new ReportEngine(settings).process(windupInputFile, windupOutputDirectory);

	}

	
}
