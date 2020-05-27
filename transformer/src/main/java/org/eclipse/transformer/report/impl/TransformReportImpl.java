/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: (EPL-2.0 OR Apache-2.0)
 ********************************************************************************/

package org.eclipse.transformer.report.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.report.TransformReport;
import org.eclipse.transformer.TransformException;
import org.slf4j.Logger;

public abstract class TransformReportImpl implements TransformReport {

	public TransformReportImpl(Logger logger, boolean isTerse, boolean isVerbose) {
		this.logger = logger;
		this.isTerse = isTerse;
		this.isVerbose = isVerbose;
	}

	//

	private final Logger logger;
	private final boolean isTerse;
	private final boolean isVerbose;

	public Logger getLogger() {
		return logger;
	}

	public boolean getIsTerse() {
		return isTerse;
	}
	
	public boolean getIsVerbose() {
		return isVerbose;
	}

	public void trace(String message, Object... parms) {
		getLogger().trace(message, parms);
	}
	
	public void debug(String message, Object... parms) {
		getLogger().debug(message, parms);
	}

	public void info(String message, Object... parms) {
		getLogger().info(message, parms);
	}

	public void terse(String message, Object...parms) {
		if ( getIsTerse() ) {
			info(message, parms);
		}
	}

	public void verbose(String message, Object...parms) {
		if ( getIsVerbose() ) {
			info(message, parms);
		}
	}
	
	public void warn(String message, Object... parms) {
		getLogger().warn(message, parms);
	}

	public void error(String message, Object... parms) {
		getLogger().error(message, parms);
	}

	public void error(String message, Throwable th, Object... parms) {
		Logger useLogger = getLogger();
		if ( !useLogger.isErrorEnabled() ) {
			return;
		}

		if ( parms.length != 0 ) {
			message = message.replace("{}", "%s");
			message = String.format(message, parms);
		}

		useLogger.error(message, th);
	}

	//

	@Override
	public abstract String getTitle();

	//

	private String reportPath; 

	@Override
	public void setOutput(String reportPath) throws TransformException {
		this.reportPath = reportPath;
	}

	@Override
	public String getOutput() {
		return reportPath;
	}

	@Override
	public void emit() throws IOException {
		File reportFile = new File( getOutput() );

		try ( PrintWriter reportWriter = new PrintWriter(reportFile) ) {
			emit(reportWriter); // throws IOException
		}
	}

	@Override
	public void emit(PrintWriter writer) throws IOException {
		emitHeader(writer); // throws IOException
		emitCore(writer); // throws IOException
		emitTrailer(writer); // throws IOException
	}

	//

	public static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy.MM.dd HH:mm:ss z";
	public static final String DASHES = "================================================================================";

	public void emitHeader(PrintWriter writer) throws IOException {
		Date date = new Date();
		DateFormat format = new SimpleDateFormat(DEFAULT_TIMESTAMP_FORMAT);

		writer.println( getTitle() + " [ " + format.format(date) + " ]" );
		writer.println(DASHES); 
	}

	public abstract void emitCore(PrintWriter writer) throws IOException;

	public void emitTrailer(PrintWriter writer) throws IOException {
		writer.println(DASHES); 
	}

	//

	@Override
	public void init() throws TransformException {
		// NO-OP
	}

	@Override
	public void complete() throws TransformException {
		// NO-OP
	}

	//

	private String initialName;
	
	protected void setInitialName(String initialName) {
		this.initialName = initialName;
	}

	public String getInitialName() {
		return initialName;
	}

	//

	@Override
	public void begin(Action action, String useInitialName) {
		setInitialName(useInitialName);
	}

	@Override
	public void processInitialData(Object data) {
		// NO-OP
	}

	@Override
	public void processFinalData(Object data) {
		// NO-OP
	}

	@Override
	public void end(Action action, Changes changes) {
		// NO-OP
	}
}
