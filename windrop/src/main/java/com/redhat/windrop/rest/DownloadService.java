package com.redhat.windrop.rest;

import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.redhat.windrop.data.FileRepository;
import com.redhat.windrop.model.File;

@RequestScoped
@Path("/dl")
public class DownloadService {

	final protected Logger LOG = Logger.getLogger(UploadService.class.getSimpleName());

	@Inject
	private FileRepository fileRepository;

	@GET
	@Path("/file/{hash}")
	public Response getFile(@PathParam("hash") String hashValue) {

		if (hashValue == null) {
			LOG.warning(">>> uploadFile - hashValue == null");
			return Response.status(500).entity("Error 500 - File does not exist").build();
		}

		File file = fileRepository.findByHashValue(hashValue);
		if (file == null) {
			LOG.warning(">>> uploadFile - file == null");
			return Response.status(500).entity("Error 500 - File does not exist").build();
		}

		java.io.File ioFile = null;
		try {
			ioFile = new java.io.File(file.getStorageFileName());
			if (ioFile == null || !ioFile.exists()) {
				LOG.warning(">>> uploadFile - !ioFile.exists");
				return Response.status(500).entity("Error 500 - File does not exist").build();
			}
		} catch (Exception e) {
			return Response.status(500).entity("Error 500 - File does not exist").build();
		}

		return Response.ok(ioFile, MediaType.APPLICATION_OCTET_STREAM).header("content-disposition", "attachment; filename =" + file.getUploadedFileName()).build();

		/* default to xml file */
		// return Response.ok(new
		// FileInputStream("custom.xml")).type("application/xml").build();
	}

	@DELETE
	@Path("/file/{hash}")
	public void removeFile(@PathParam("hash") String id) {
		// ...
	}

}
