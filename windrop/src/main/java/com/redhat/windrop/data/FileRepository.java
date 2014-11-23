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
package com.redhat.windrop.data;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.redhat.windrop.model.File;

/**
 * DAO for Files.
 * 
 * @author mzottner
 */
@Stateless
public class FileRepository {

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
	public void storeFile(final File file) {
		em.persist(file);
	}

	/**
	 * Storage helper.
	 * 
	 * @param uploadedFileName
	 * @param storageFileName
	 * @param hashValue
	 */
	public void storeFile(final String uploadedFileName, final String storageFileName, final String hashValue) {
		storeFile(new File(uploadedFileName, storageFileName, hashValue));
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
		return em.createQuery(criteria).getSingleResult();
	}

}
