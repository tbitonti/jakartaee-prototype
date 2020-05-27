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

package org.eclipse.transformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.action.impl.ChangesImpl;
import org.eclipse.transformer.report.TransformReport;
import org.slf4j.Logger;

public class TransformerState {
	public TransformerState(Logger logger, boolean isTerse, boolean isVerbose) {
		this.logger = logger;
		this.isTerse = isTerse;
		this.isVerbose = isVerbose;

		this.actions = new ArrayList<Action>();

		this.changes = new ArrayList<ChangesImpl>();
		this.activeChanges = null;
		this.lastActiveChanges = null;

		this.reports = new LinkedHashMap<Transformer.ReportType, TransformReport>();
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

	private final List<Action> actions;

	public void pushAction(Action action) {
		actions.add(action);
	}

	public Action popAction() {
		return ( actions.remove( actions.size() - 1 ) );
	}

	//

	protected final List<ChangesImpl> changes;
	protected ChangesImpl activeChanges;
	protected ChangesImpl lastActiveChanges;

	public ChangesImpl getActiveChanges() {
		return activeChanges;
	}

	public ChangesImpl getLastActiveChanges() {
		return lastActiveChanges;
	}

	public void pushChanges(Supplier<ChangesImpl> changeMaker ) {
		changes.add( activeChanges = changeMaker.get() );

		lastActiveChanges = null;

		// logChanges("pushChanges");
	}

	public void popChanges() {
		int numLastChanges = changes.size() - 1;
		lastActiveChanges = changes.remove( numLastChanges );
		activeChanges = ( (numLastChanges == 0) ? null : changes.get(numLastChanges - 1) );

		// logChanges("popChanges");
	}

//	private void logChanges(String methodName) {
//		info("{} Change stack:", methodName);
//
//		int numChanges = changes.size();
//		for ( int changeNo = 0; changeNo < numChanges; changeNo++ ) {
//			info("{}: Active [ {} ]: {}", methodName, changeNo, changes.get(changeNo));
//		}
//
//		info("{}: Last   [ {} ]: [ {} ]", methodName, numChanges, lastActiveChanges);
//	}

	//

	public void setResourceNames(String inputResourceName, String outputResourceName) {
		ChangesImpl useChanges = getActiveChanges();
		useChanges.setInputResourceName(inputResourceName);
		useChanges.setOutputResourceName(outputResourceName);
	}

	public void addReplacement() {
		getActiveChanges().addReplacement();
	}

	public void addReplacements(int additions) {
		getActiveChanges().addReplacements(additions);
	}

	//

	public boolean hasChanges() {
		return getActiveChanges().hasChanges();
	}

	public boolean hasResourceNameChange() {
		return getActiveChanges().hasResourceNameChange();
	}

	public boolean hasNonResourceNameChanges() {
		return getActiveChanges().hasNonResourceNameChanges();
	}

	//

	private final Map<Transformer.ReportType, TransformReport> reports;

	public void putReport(Transformer.ReportType reportType, TransformReport report) {
		reports.put(reportType, report);
	}

	//

	public void emitReports() throws IOException {
		for ( TransformReport report : reports.values() ) {
			report.emit(); // throws IOException
		}
	}

	//

	public void initReports() throws TransformException {
		for ( TransformReport report : reports.values() ) {
			report.init(); // throws TransformException
		}
	}

	public void completeReports() throws TransformException {
		for ( TransformReport report : reports.values() ) {
			report.complete(); // throws TransformException
		}
	}

	//

	public void begin(Action action, String inputName) {
		for ( TransformReport report : reports.values() ) {
			report.begin(action, inputName);
		}
	}

	public void processInitialData(Object data) {
		for ( TransformReport report : reports.values() ) {
			report.processInitialData(data);
		}
	}

	public void processFinalData(Object data) {
		for ( TransformReport report : reports.values() ) {
			report.processFinalData(data);
		}
	}

	public void end(Action action, Changes changes) {
		for ( TransformReport report : reports.values() ) {
			report.end(action, changes);
		}
	}
}
