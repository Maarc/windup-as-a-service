package com.redhat.winddrop.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import com.redhat.winddrop.data.FileRepository;

/**
 * REST upload service.
 * 
 * @author mzottner
 * 
 */
@Path("/upload")
@RequestScoped
public class UploadService {

	final protected Logger LOG = Logger.getLogger(UploadService.class.getSimpleName());

	@Inject
	private FileRepository fileRepository;

	private final static String FILE_SEPARATOR = System.getProperty("file.separator");
	
	/**
	 * 
	 * @param dataInput the uploaded file as MultipartFormDataInput
	 * @return regular HTTP Response (200 on success and 500 on failure)
	 */
	@POST
	@Consumes("multipart/form-data")
	public Response uploadFile(final MultipartFormDataInput dataInput) {

		if (dataInput == null) {
			LOG.info(">>> uploadFile - dataInput == null");
			return Response.status(500).entity("File does not exist").build();
		}

		String hashValue = StringUtils.EMPTY;
		List<InputPart> inputParts = dataInput.getFormDataMap().get("file");
		for (InputPart inputPart : inputParts) {

			try {

				String fileName = getFileName(inputPart.getHeaders());

				LOG.info(">>> uploadFile - " + fileName + "sent to the server");

				// convert the uploaded file to inputstream
				InputStream inputStream = inputPart.getBody(InputStream.class, null);

				byte[] bytes = IOUtils.toByteArray(inputStream);

				// stores the file somewhere else
				hashValue = storeFile(bytes, fileName);

			} catch (Exception e) {
				LOG.log(Level.SEVERE, ">>> uploadFile - This should not have happened", e);
				return Response.status(500).entity("Upload failure").build();
			}
		}

		final String message = "Upload successful$$$rest/dl/file/" + hashValue;
		LOG.info(">>> uploadFile - " + message);
		return Response.status(200).entity(message).build();
	}

	/**
	 * @param header  Header sample { Content-Type=[image/png], Content-Disposition=[form-data;
     * name="file"; filename="filename.extension"] }
	 * @return a filename that replaces all double quotes with nothing
	 */
	private String getFileName(MultivaluedMap<String, String> header) {

		for (String filename : header.getFirst("Content-Disposition").split(";")) {
			if ((filename.trim().startsWith("filename"))) {
				return filename.split("=")[1].trim().replaceAll("\"", "");
			}
		}
		throw new IllegalStateException("The uploaded file has no name.");
	}

	/**
	 * Stores the file somewhere else.
	 * 
	 * @param content the bytes of the file
	 * @param uploadedFileName the filename of the uploaded file
	 * @throws Exception
	 */
	protected String storeFile(byte[] content, String uploadedFileName) throws Exception {

        final String hashValue = DigestUtils.sha256Hex(uploadedFileName + System.currentTimeMillis());
		final String time = FastDateFormat.getInstance("yyyy-MM-dd-HH.mm.ss").format(System.currentTimeMillis());
        String fileDirectory = System.getenv("OPENSHIFT_DATA_DIR");
        if(StringUtils.isEmpty(fileDirectory))
        {
        	fileDirectory = FILE_SEPARATOR+"tmp";
        }
        final String storageFileName = fileDirectory + FILE_SEPARATOR  + uploadedFileName + "-" + hashValue + "-" + time + ".uploaded";

		File file;
		FileOutputStream fop = null;
		boolean success = false;
		try {
			file = new File(storageFileName);
			if (file.createNewFile()) {
                fop = new FileOutputStream(file);
                fop.write(content);
                fop.flush();
                success = true;
			}
            else
            {
                throw new Exception("File " + storageFileName + " already exists");
            }
		} catch (Exception e) {
            LOG.log(Level.WARNING, "Caught an exception while trying to store file " + storageFileName + " (" + e.getMessage()+")");
			throw e;
		} finally {
			if (success) {
				fileRepository.storeFile(uploadedFileName, storageFileName, hashValue);
			} else {
				new File(storageFileName).delete();
			}

			if (fop != null) {
				fop.close();
			}
		}

		return hashValue;
	}
}
