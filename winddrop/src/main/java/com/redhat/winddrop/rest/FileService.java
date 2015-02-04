package com.redhat.winddrop.rest;

import java.util.List;
import java.util.logging.Logger;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.redhat.winddrop.data.FileRepository;
import com.redhat.winddrop.model.File;

/**
 * JAX-RS Example
 * <p/>
 * This class produces a RESTful service to read/write the contents of the Files table.
 * 
 * It is used by the HTLM5 report.
 * 
 */
@Path("/files")
@RequestScoped
@Stateful
public class FileService {

	final protected Logger LOG = Logger.getLogger(FileService.class.getSimpleName());

	@Inject
	private FileRepository fileRepository;

	@GET
	@Produces("text/xml")
	public List<File> listAllFiles() {
		LOG.info(">>> listAllFiles(");
		return fileRepository.getAllReports();
	}

	@GET
	@Path("/json")
	@Produces(MediaType.APPLICATION_JSON)
	public List<File> listAllFilesJSON() {
		LOG.info(">>> listAllFilesJSON(");
		return fileRepository.getAllReports();
	}

}
