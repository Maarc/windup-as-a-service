package com.redhat.winddrop.rest;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
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

	public final static String TOKEN = "|||||";

	/**
	 * 
	 * @param dataInput
	 *            the uploaded file as MultipartFormDataInput
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
		Map<String, List<InputPart>> formDataMap = dataInput.getFormDataMap();
		if (formDataMap == null || formDataMap.isEmpty()) {
			LOG.log(Level.SEVERE, ">>> uploadFile - formDataMap==null || formDataMap.isEmpty()");
			return Response.status(500).entity("No data submitted").build();
		} else {

			String mail = null;
			String packages = null;
			
			//LOG.log(Level.SEVERE, "formDataMap !!");
			//for (Entry<String, List<InputPart>> entry : formDataMap.entrySet()) {
			//	LOG.log(Level.SEVERE, "formDataMap - - - " + entry.getKey() + " - " + entry.getValue());
			// }

			try {
				mail = formDataMap.get("inputEmail").get(0).getBodyAsString();
				packages = formDataMap.get("inputPackages").get(0).getBodyAsString();
			} catch (Throwable t) {
				// Pokemon design pattern.
			}

			if(mail==null || StringUtils.trimToEmpty(mail.trim()).length()<7) {
				return Response.status(500).entity("Mail missing").build();
			}
			
			if(packages==null || StringUtils.trimToEmpty(packages.trim()).length()<1) {
				return Response.status(500).entity("No package specified").build();
			}
			
			List<InputPart> fileInputParts = formDataMap.get("fileselect");

			if (fileInputParts == null) {
				LOG.log(Level.SEVERE, ">>> uploadFile - fileInputParts==null");
				return Response.status(500).entity("No file submitted").build();
			} else {				
	
				for (InputPart inputPart : fileInputParts) {

					try {
						String fileName = getFileName(inputPart.getHeaders());
						if (fileName.endsWith(".war") || fileName.endsWith(".ear")) {
							// store the file somewhere else
							hashValue = FileUtil.storeFile(IOUtils.toByteArray(inputPart.getBody(InputStream.class, null)), fileName, fileRepository, mail, packages);
							// persists the report request in the file store (will be overridden once the report is available)
							fileRepository.storeReportRequest(fileName, mail, packages,hashValue);
							// put the file in a queue to be processed by windup)
							LOG.info("Adding the artefact to the windup execution queue " + hashValue + " - " + fileName);
							queueWindupExecution(hashValue, mail, packages);
							LOG.info(">>> uploadFile - " + fileName + "sent to the server");
						} else {
							LOG.info(fileName + " is not an EAR or WAR binary deployment and will be ignored by Windup.");
							return Response.status(500).entity("Uploaded file is not an EAR or WAR").build();
						}

					} catch (Exception e) {
						LOG.log(Level.SEVERE, ">>> uploadFile - This should not have happened", e);
						return Response.status(500).entity("Upload failure").build();
					}
				}
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
	private void queueWindupExecution(String hashValue, String email, String packages) throws JMSException {
		Connection connection = connectionFactory.createConnection();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		session.createProducer(queue).send(session.createTextMessage(hashValue + TOKEN + packages + TOKEN + email + TOKEN));
		session.close();
		connection.close();
	}

	/**
	 * @param header
	 *            Header sample { Content-Type=[image/png], Content-Disposition=[form-data; name="file"; filename="filename.extension"] }
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
