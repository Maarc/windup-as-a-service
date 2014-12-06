package com.redhat.winddrop.util;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
public class WindropInit {

	private final static Logger LOG = Logger.getLogger(WindropInit.class.toString());

	@PostConstruct
	public void init() {
		LOG.info("Checking and creating the required directories.");
		FileUtil.checkAndCreateRequiredDirectories();
	}

}
