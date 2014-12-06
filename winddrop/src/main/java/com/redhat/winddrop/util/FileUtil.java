package com.redhat.winddrop.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Deque;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import com.redhat.winddrop.data.FileRepository;

/**
 * This class provides utility methods to zip a directory.
 * 
 * @author mzottner
 * 
 *         http://stackoverflow.com/questions/1399126/java-util-zip-recreating-
 *         directory-structure
 */
public class FileUtil {

	protected final static Logger LOG = Logger.getLogger(FileUtil.class.getSimpleName());

	public final static String FILE_SEPARATOR = System.getProperty("file.separator");

	public final static String WINDDROP_BASE_DIR = FILE_SEPARATOR + "tmp" + FILE_SEPARATOR + "winddrop" + FILE_SEPARATOR;

	public final static String WINDDROP_STORAGE_DIR = WINDDROP_BASE_DIR + "storage" + FILE_SEPARATOR;

	public final static String WINDDROP_TMP_DIR = WINDDROP_BASE_DIR + "tmp" + FILE_SEPARATOR;

	/**
	 * @return Current formatted date.
	 */
	public static String getCurrentFormattedDate() {
		return FastDateFormat.getInstance("yyyy-MM-dd_HH:mm:ss").format(System.currentTimeMillis());
	}

	/**
	 * Creates the required directories.
	 */
	public static void checkAndCreateRequiredDirectories() {
		File dir = null;
		for (String path : new String[] { WINDDROP_BASE_DIR, WINDDROP_STORAGE_DIR, WINDDROP_TMP_DIR }) {
			dir = new File(path);
			if (!dir.exists()) {
				dir.mkdir();
			}
		}
	}

	/**
	 * Stores the file somewhere else.
	 * 
	 * @param content
	 *            the bytes of the file
	 * @param uploadedFileName
	 *            the filename of the uploaded file
	 * @throws Exception
	 */
	public static String storeFile(byte[] content, String uploadedFileName, FileRepository fr) throws Exception {

		final String hashValue = DigestUtils.sha256Hex(uploadedFileName + System.currentTimeMillis());
		final String time = getCurrentFormattedDate();
		String fileDirectory = System.getenv("OPENSHIFT_DATA_DIR");
		if (StringUtils.isEmpty(fileDirectory)) {
			fileDirectory = WINDDROP_STORAGE_DIR;
		}
		final String storageFileName = fileDirectory + uploadedFileName + "-" + hashValue + "-" + time + ".uploaded";

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
			} else {
				throw new Exception("File " + storageFileName + " already exists");
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Caught an exception while trying to store file " + storageFileName + " (" + e.getMessage() + ")");
			throw e;
		} finally {
			if (success) {
				fr.storeFile(uploadedFileName, storageFileName, hashValue);
			} else {
				new File(storageFileName).delete();
			}

			if (fop != null) {
				fop.close();
			}
		}
		return hashValue;
	}

	public static String storeFile(File content, String uploadedFileName, FileRepository fr) throws Exception {
		FileInputStream fis = new FileInputStream(content);
		try {
			return storeFile(IOUtils.toByteArray(fis), uploadedFileName, fr);
		} finally {
			fis.close();
		}
	}

	public static void zip(File directory, File zipfile) throws IOException {
		URI base = directory.toURI();
		Deque<File> queue = new LinkedList<File>();
		queue.push(directory);
		OutputStream out = new FileOutputStream(zipfile);
		Closeable res = out;
		ZipOutputStream zout = null;
		try {
			zout = new ZipOutputStream(out);
			res = zout;
			while (!queue.isEmpty()) {
				directory = queue.pop();
				for (File kid : directory.listFiles()) {
					String name = base.relativize(kid.toURI()).getPath();
					if (kid.isDirectory()) {
						queue.push(kid);
						name = name.endsWith("/") ? name : name + "/";
						zout.putNextEntry(new ZipEntry(name));
					} else {
						zout.putNextEntry(new ZipEntry(name));
						copy(kid, zout);
						zout.closeEntry();
					}
				}
			}
		} finally {
			for (Closeable c : new Closeable[] {res,out,zout}) {
				try {
					c.close();
				} catch (Throwable t) {
					LOG.severe(t.getMessage());
				}
			}
		}
	}

	public static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		while (true) {
			int readCount = in.read(buffer);
			if (readCount < 0) {
				break;
			}
			out.write(buffer, 0, readCount);
		}
	}

	public static void copy(File file, OutputStream out) throws IOException {
		InputStream in = new FileInputStream(file);
		try {
			copy(in, out);
		} finally {
			in.close();
		}
	}

	public static void copy(InputStream in, File file) throws IOException {
		OutputStream out = new FileOutputStream(file);
		try {
			copy(in, out);
		} finally {
			out.close();
		}
	}

	public static void copy(File in, File outFile) throws IOException {
		//FileUtils.copyFile(in, outFile);
		OutputStream out = new FileOutputStream(outFile);
		try {
			copy(in, out);
		} finally {
			out.close();
		}
	}

}
