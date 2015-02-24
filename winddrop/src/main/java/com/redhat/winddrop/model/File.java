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
package com.redhat.winddrop.model;

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

import com.redhat.winddrop.util.FileUtil;

/**
 * File model object.
 * 
 * @author mzottner
 */
@Entity
@XmlRootElement
@Table(name = "File", uniqueConstraints = @UniqueConstraint(columnNames = "hash_value"))
public class File implements Serializable {
	
	/** Serial version UID. */
	private static final long serialVersionUID = 201502031729L;

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
	@Column(name = "storage_file_name")
	protected String storageFileName;

	/**
	 * Date of the upload.
	 */
	@NotNull
	@Column(name = "upload_date")
	protected Long uploadDate;

	/**
	 * Email of the submitter.
	 */
	@NotNull
	@Column(name = "submitter")
	protected String submitter;
	
	
	/**
	 * True if this file is a zipped windup report file.
	 */
	@NotNull
	@Column(name = "is_report")
	protected Boolean isReport;
	

	/**
	 * True if the report has been created and exists under "storageFileName".
	 */
	@NotNull
	@Column(name = "is_report_processed")
	protected Boolean isReportProcessed;
	
	/**
	 * Packages being analysed by windup.
	 */
	@NotNull
	@Column(name = "packages")
	protected String packages;

	/**
	 * Empty constructor for JPA.
	 */
	public File() {
	}

	/**
	 * Constructor
	 * 
	 * @param uploadedFileName
	 * @param storageFileName
	 * @param hashValue
	 * @param submitter
	 */
	public File(final String uploadedFileName, final String storageFileName, final String hashValue, final String submitter, final String packages) {
		this(uploadedFileName,storageFileName,hashValue,submitter,packages,false,false);
	}
	
	public File(final String uploadedFileName, final String storageFileName, final String hashValue, final String submitter, final String packages, final boolean isReport) {
		this(uploadedFileName,storageFileName,hashValue,submitter,packages,isReport,false);
	}
	
	public File(final String uploadedFileName, final String storageFileName, final String hashValue, final String submitter, final String packages, final boolean isReport, final boolean isReportProcessed) {
		this.uploadedFileName = uploadedFileName;
		this.storageFileName = storageFileName;
		this.hashValue = hashValue;
		this.uploadDate = System.currentTimeMillis();
		this.submitter = submitter;
		this.packages = packages;
		this.isReport = isReport;
		this.isReportProcessed = isReportProcessed;
	}

	public String getSubmitter() {
		return submitter;
	}

	public Boolean getIsReport() {
		return isReport;
	}

	public void setIsReport(Boolean isReport) {
		this.isReport = isReport;
	}

	public Boolean getIsReportProcessed() {
		return isReportProcessed;
	}

	public void setIsReportProcessed(Boolean isReportProcessed) {
		this.isReportProcessed = isReportProcessed;
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

	public String getPackages() {
		return packages;
	}

	public void setPackages(String packages) {
		this.packages = packages;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
	
	public String getUploadFormattedDate() {
		return FileUtil.formatDate(uploadDate);
	}
	

}