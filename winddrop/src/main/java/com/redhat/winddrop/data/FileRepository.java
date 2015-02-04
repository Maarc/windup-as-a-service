/**
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package com.redhat.winddrop.data;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.redhat.winddrop.model.File;
import com.redhat.winddrop.util.FileUtil;

/**
 * DAO for Files.
 * 
 * @author mzottner
 */
@Stateless
public class FileRepository {

	private final static Logger LOG = Logger.getLogger(FileRepository.class.toString());

	@Inject
	private EntityManager em;

	/**
	 * Empty constructor.
	 */
	public FileRepository() {
	}

	/**
	 * Stores a file instance.
	 * 
	 * @param file
	 */
	protected void storeFile(final File file) {
		em.persist(file);
	}

	/**
	 * Removes a file that is no more existing
	 * 
	 * @param file
	 */
	protected void removeFile(final File file) {
		em.remove(file);
	}

	/**
	 * Storage helper.
	 * 
	 * @param uploadedFileName
	 * @param storageFileName
	 * @param hashValue
	 * @param submitter
	 * @param isReport
	 */
	public void storeFile(final String uploadedFileName, final String storageFileName, final String hashValue, final String submitter, final String packages, final boolean isReport) {
		storeFile(new File(uploadedFileName, storageFileName, hashValue, submitter, packages, isReport));
	}

	public void storeFile(final String uploadedFileName, final String storageFileName, final String hashValue, final String submitter, final String packages, final boolean isReport, final boolean isReportCompleted) {
		storeFile(new File(uploadedFileName, storageFileName, hashValue, submitter, packages, isReport, isReportCompleted));
	}

	public void storeReportRequest(final String uploadedFileName, final String submitter, final String packages, final String hashValue) {
		storeFile(new File(uploadedFileName + FileUtil.REPORT_EXTENSION, uploadedFileName + FileUtil.REPORT_EXTENSION, hashValue + "_", submitter, packages, true, false));
	}

	public void removeReportRequest(final String hashValue) {
		removeFile(findByHashValue(hashValue + "_"));
	}

	public void startProcessingReportRequest(File file) {
		file.setIsReportBeingProcessed(true);
		storeFile(file);
	}

	/**
	 * Search for a file according its hashValue.
	 * 
	 * @param hashValue
	 * @return file
	 */
	public File findByHashValue(final String hashValue) {
		final CriteriaBuilder cb = em.getCriteriaBuilder();
		final CriteriaQuery<File> criteria = cb.createQuery(File.class);
		final Root<File> rootFile = criteria.from(File.class);
		criteria.select(rootFile).where(cb.equal(rootFile.get("hashValue"), hashValue));
		File file = em.createQuery(criteria).getSingleResult();
		return file;
	}

	/**
	 * Returns all files.
	 * 
	 * @return list of all files.
	 */
	public List<File> getAllReports() {
		LOG.info(">>> getAllReports()");
		@SuppressWarnings("unchecked")
		final List<File> results = em.createQuery("select f from File f where f.isReport = true  order by f.uploadDate").getResultList();

		// TODO This is a pragmatic solution. Ideally no (in-memory) database should be used and the storage should be only based on the file system.
		// Checks if all results reports still exists ... otherwise clean the database.
		final List<File> cleanResults = new ArrayList<File>();
		for (File file : results) {

			boolean isReportsExistingOrInProgress = false;
			if (file == null) {
				LOG.warning(">>> uploadFile - file == null");
			} else {

				if (file.getIsReport() && !file.getIsReportProcessed()) {
					isReportsExistingOrInProgress = true;
				} else {
					try {
						java.io.File ioFile = new java.io.File(file.getStorageFileName());
						if (ioFile != null && ioFile.exists()) {
							isReportsExistingOrInProgress = true;
						}
					} catch (Throwable t) {
						// Pokemon pattern used intentionally.
					}
				}
			}
			if (isReportsExistingOrInProgress) {
				// The file exists or the report is currently built.
				cleanResults.add(file);
			} else {
				removeFile(file);
			}
		}

		LOG.info("<<< getAllReports() - Stored size: " + results.size() + " - Real size: " + cleanResults.size());
		return cleanResults;
	}

	/**
	 * Returns all files.
	 * 
	 * @return list of all files.
	 */
	protected List<File> getAll() {
		LOG.info(">>> getAll()");
		@SuppressWarnings("unchecked")
		final List<File> results = em.createQuery("select f from File f order by f.uploadDate").getResultList();
		LOG.info("<<< getAll()" + results.size());
		return results;
	}

}
