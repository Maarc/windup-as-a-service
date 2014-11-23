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
package com.redhat.windrop.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * File model object.
 * 
 * @author mzottner
 */
@Entity
@XmlRootElement
@Table(name = "File_html5mobi", uniqueConstraints = @UniqueConstraint(columnNames = "hash_value"))
public class File implements Serializable {

	/** Serial version UID. */
	private static final long serialVersionUID = 201301241729L;

	/**
	 * Hash value for file identification.
	 */
	@Id
	@NotNull
	@Column(name = "hash_value")
	protected String hashValue;

	/**
	 * Name of file upladed in dropspace.
	 */
	@NotNull
	@Column(name = "uploaded_file_name")
	protected String uploadedFileName;

	/**
	 * Name of the file
	 */
	@NotNull
	@Column(name = "storage_file_name")
	protected String storageFileName;

	/**
	 * Date of the upload.
	 */
	@NotNull
	@Column(name = "upload_date")
	protected Long uploadDate;

	/**
	 * Empty constructor for JPA.
	 */
	public File() {
	}

	/**
	 * Constructor.
	 * 
	 * @param uploadedFileName
	 * @param storageFileName
	 * @param hashValue
	 */
	public File(final String uploadedFileName, final String storageFileName, final String hashValue) {
		this.uploadedFileName = uploadedFileName;
		this.storageFileName = storageFileName;
		this.hashValue = hashValue;
		this.uploadDate = System.currentTimeMillis();
	}

	public String getUploadedFileName() {
		return uploadedFileName;
	}

	public String getHashValue() {
		return hashValue;
	}

	public String getStorageFileName() {
		return storageFileName;
	}

	public Long getUploadDate() {
		return uploadDate;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

}