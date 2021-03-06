/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the 
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.winddrop.mdb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.windup.WindupEnvironment;
import org.jboss.windup.reporting.ReportEngine;

import com.redhat.winddrop.data.FileRepository;
import com.redhat.winddrop.rest.UploadService;
import com.redhat.winddrop.util.FileUtil;

/**
 * <p>
 * A simple Message Driven Bean processing asynchronously all queued windup executions.
 * </p>
 */
@MessageDriven(name = "WindupExecutionQueueMDB", activationConfig = { 
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/WindupExecutionQueueMDB"),
		@ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class WindupExecutionQueueMDB implements MessageListener {

	private final static Logger LOG = Logger.getLogger(WindupExecutionQueueMDB.class.toString());

	@Inject
	private FileRepository fileRepository;

	/**
	 * @see MessageListener#onMessage(Message)
	 */
	public void onMessage(Message message) {
		String jobDescription = null;
		try {
			if (message instanceof TextMessage) {
				jobDescription = ((TextMessage) message).getText();
				
				StringTokenizer t = new StringTokenizer(jobDescription,UploadService.TOKEN);
				String hashValue = t.nextToken();
				String packages = t.nextToken();
				String email = t.nextToken();				
				LOG.info("Message received for Windup execution: " + jobDescription);
				LOG.info("hashValue: " +hashValue+" - email: "+email+" - packages: "+packages);	
				buildAndUploadReport(hashValue, email, packages);
			} else {
				LOG.warning("Message of wrong type: " + message.getClass().getName());
			}
		} catch (Throwable t) {
			LOG.log(Level.SEVERE, "Error occured in onMessage for hashValue " + jobDescription, t);
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void buildAndUploadReport(String hashValue, String submitter, String packages) {
		LOG.info(">>> buildAndUploadReport (hashValue=" + hashValue + ",packages=" + packages + ")");
		List<File> filesToDelete = new ArrayList<File>();
		try {
			com.redhat.winddrop.model.File storedBinaryFile = fileRepository.findByHashValue(hashValue);

			String windupOutputDirectoryName = FileUtil.WINDDROP_TMP_DIR + hashValue + "_out" + FileUtil.FILE_SEPARATOR;
			File windupOutputDirectory = new File(windupOutputDirectoryName);
			windupOutputDirectory.mkdir();
			filesToDelete.add(windupOutputDirectory);

			String windupInputDirectoryName = FileUtil.WINDDROP_TMP_DIR + hashValue + "_in" + FileUtil.FILE_SEPARATOR;
			File windupInputDirectory = new File(windupInputDirectoryName);
			windupInputDirectory.mkdir();
			filesToDelete.add(windupInputDirectory);

			File windupInputFile = new File(windupInputDirectoryName + storedBinaryFile.getUploadedFileName());

			// Copying the uploaded file and use it as input for windup.
			FileUtil.copy(new File(StringUtils.trim(storedBinaryFile.getStorageFileName())), windupInputFile);
			filesToDelete.add(windupInputFile);

			// Execute windup
			executeWindup(packages, windupInputFile, windupOutputDirectory);

			// Zip the resulting report
			String zippedReportFileName = storedBinaryFile.getUploadedFileName() + FileUtil.REPORT_EXTENSION;
			File zippedReport = new File(windupInputDirectoryName + zippedReportFileName);
			filesToDelete.add(zippedReport);
			LOG.info("["+hashValue+"] Zipping the created report ...");
			FileUtil.zip(windupOutputDirectory, zippedReport);

			// Upload the result
			LOG.info("["+hashValue+"] Storing report ...");
			fileRepository.removeReportRequest(hashValue);
			hashValue = FileUtil.storeFile(zippedReport, zippedReportFileName, fileRepository, submitter, packages, true, true);
			LOG.info("["+hashValue+"] Execution successfull ... wget http://localhost:8080/winddrop/rest/dl/file/" + hashValue);

		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			// Cleanup
			LOG.info("["+hashValue+"] Cleaning temporary files ...");
			for (File file : filesToDelete) {
				if (file.exists()) {
					if (file.isDirectory()) {
						try {
							FileUtils.deleteDirectory(file);
						} catch (IOException e) {

						}
					} else {
						file.delete();
					}
				}
			}
		}
		LOG.info("<<< buildAndUploadReport");
	}

	/**
	 * Configures and executes Windup.
	 * 
	 * @param packageSignature
	 * @param windupInputFile
	 * @param windupOutputDirectory
	 * @throws IOException
	 */
	protected void executeWindup(String packageSignature, File windupInputFile, File windupOutputDirectory) throws IOException {

		// Map the environment settings from the input arguments.
		WindupEnvironment settings = new WindupEnvironment();
		settings.setPackageSignature(packageSignature);
		// settings.setExcludeSignature("excludePkgs");
		// settings.setTargetPlatform("targetPlatform");
		settings.setFetchRemote("false");

		boolean isSource = false;
		if (BooleanUtils.toBoolean("source")) {
			isSource = true;
		}
		settings.setSource(isSource);

		boolean captureLog = false;
		if (BooleanUtils.toBoolean("captureLog")) {
			captureLog = true;
		}

		String logLevel = StringUtils.trim("logLevel");
		settings.setCaptureLog(captureLog);
		settings.setLogLevel(logLevel);

		LOG.info("captureLog " + captureLog);
		LOG.info("logLevel " + logLevel);
		LOG.info("isSource " + isSource);

		// Run Windup
		new ReportEngine(settings).process(windupInputFile, windupOutputDirectory);

	}

}
