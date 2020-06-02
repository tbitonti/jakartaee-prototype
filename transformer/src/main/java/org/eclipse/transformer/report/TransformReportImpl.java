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

package org.eclipse.transformer.report;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.TransformException;
import org.slf4j.Logger;

public abstract class TransformReportImpl implements TransformReport {

	public TransformReportImpl(Logger logger, boolean isTerse, boolean isVerbose) {
		this.logger = logger;
		this.isTerse = isTerse;
		this.isVerbose = isVerbose;

		this.containerNameStack = null;
		this.currentContainerName = null;
		this.lastContainerName = null;

		this.dataStack = null;
		this.currentData = null;
		this.lastData = null;

		this.resources = null;
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
		initContainerNames();
		initData();
		initResources();
	}

	@Override
	public void complete() throws TransformException {
		// NO-OP
	}

	//

	private List<String> containerNameStack;
	private String lastContainerName;
	private String currentContainerName;

	protected void initContainerNames() {
		containerNameStack = new ArrayList<String>();
		currentContainerName = null;
		lastContainerName = null;
	}
	
	public String getLastContainerName() {
		return lastContainerName;
	}

	public String getCurrentContainerName() {
		return currentContainerName;
	}

	protected void pushContainerName(String containerName) {
		// info("pushContainerName: {} -> {}", currentContainerName, containerName);
		containerNameStack.add( currentContainerName = containerName );
		lastContainerName = null;
	}

	protected String popContainerName() {
		int stackEnd = containerNameStack.size() - 1;
		lastContainerName = containerNameStack.remove(stackEnd);
		currentContainerName = ( (stackEnd == 0) ? null : containerNameStack.get(stackEnd - 1) );
		// info("popContainerName: {} <- {}", currentContainerName, lastContainerName);
		return lastContainerName;
	}

	//

	private List<Object> dataStack;
	private Object lastData;
	private Object currentData;

	protected void initData() {
		dataStack = new ArrayList<Object>();
		currentData = null;
		lastData = null;
	}

	public Object getLastData() {
		return lastData;
	}

	public Object getCurrentData() {
		return currentData;
	}

	protected void pushData(Object data) {
		dataStack.add( currentData = data );
		lastData = null;
	}

	protected Object popData() {
		int stackEnd = dataStack.size() - 1;
		lastData = dataStack.remove(stackEnd);
		currentData = ( (stackEnd == 0) ? null : dataStack.get(stackEnd - 1) );
		return lastData;
	}

	//

	protected static final boolean IS_INITIAL = true;
	protected static final boolean IS_CHANGED = true;
	protected static final Object NULL_DATA = null;

	protected static class ResourceData {
		public final String actionName;
		public final boolean isContainer;
		public final boolean isNull;

		public final boolean isInitial;
		public final String name;

		public final boolean isChanged;
		public final Object extensionData;

		public ResourceData(
			String actionName, boolean isContainer, boolean isNull,
			boolean isInitial, String name,
			boolean isChanged, Object extensionData) {

			this.actionName = actionName;
			this.isContainer = isContainer;
			this.isNull = isNull;

			this.isInitial = isInitial;
			this.name = name;

			this.isChanged = isChanged;
			this.extensionData = extensionData;
		}
	}

	private List<ResourceData> resources;
	
	protected List<ResourceData> getResources() {
		return resources;
	}

	protected void initResources() {
		resources = new ArrayList<ResourceData>();
	}

	//

	@Override
	public void begin(Action action, String initialName) {
		if ( action.isContainer() ) {
			pushContainerName(initialName);
		}

		String actionName = action.getName();
		boolean isContainer = action.isContainer();
		boolean isNull = action.isNull();

		// info("begin {} {} {}", actionName, isContainer, initialName);

		ResourceData initialData = new ResourceData(
			actionName, isContainer, isNull,
			IS_INITIAL, initialName,
			!IS_CHANGED, NULL_DATA); 

		getResources().add(initialData); 
	}

	@Override
	public void processInitialData(Object initialData) {
		pushData(initialData);
	}

	@Override
	public Object processFinalData(Object finalData) {
		return popData();
	}

	@Override
	public void end(Action action, Changes changes) {
		String actionName = action.getName();
		boolean isContainer = action.isContainer();
		boolean isNull = action.isNull();

		String finalName = changes.getOutputResourceName();
		boolean isChanged = changes.hasChanges(); 

		// info("end {} {} {}", actionName, isContainer, finalName);

		ResourceData finalData = new ResourceData(
			actionName, isContainer, isNull,
			!IS_INITIAL, finalName,
			isChanged, NULL_DATA);

		getResources().add(finalData); 
	}
}
