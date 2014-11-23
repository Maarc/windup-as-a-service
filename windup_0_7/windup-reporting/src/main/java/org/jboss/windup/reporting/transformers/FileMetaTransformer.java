/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *  
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *  
 *  Contributors:
 *      Brad Davis - bradsdavis@gmail.com - Initial API and implementation
*/
package org.jboss.windup.reporting.transformers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.windup.metadata.type.FileMetadata;


public class FileMetaTransformer extends GenericMetaTransformer<FileMetadata> {
	private static final Log LOG = LogFactory.getLog(FileMetaTransformer.class);

	@Override
	protected String buildSyntax() {
		return "xml";
	}
}
