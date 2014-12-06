package com.redhat.winddrop.rest;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import com.redhat.winddrop.data.FileRepository;
import com.redhat.winddrop.util.FileUtil;

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
	
	@Resource(mappedName = "java:/ConnectionFactory")
	private ConnectionFactory connectionFactory;

	@Resource(mappedName = "java:/queue/WindupExecutionQueueMDB")
	private Queue queue;
	
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

				// store the file somewhere else
				hashValue = FileUtil.storeFile(IOUtils.toByteArray(inputPart.getBody(InputStream.class, null)), fileName, fileRepository);

				LOG.info("Adding the artefact to the windup execution queue " + hashValue +" - "+fileName);
				if(fileName.endsWith(".war")||fileName.endsWith(".ear")) {
					// put the file in a queue to be processed by windup)
					queueWindupExecution(hashValue);
				} else {
					LOG.info(fileName + " is not an EAR or WAR binary deployment and will be ignored by Windup.");
				}

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
	 * @param hashValue
	 * @throws JMSException
	 */
	private void queueWindupExecution(String hashValue) throws JMSException {

		Connection connection = connectionFactory.createConnection();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		session.createProducer(queue).send(session.createTextMessage(hashValue));
	}

	/**
	 * @param header  Header sample { Content-Type=[image/png], Content-Disposition=[form-data;
     * name="file"; filename="filename.extension"] }
	 * 
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


}
