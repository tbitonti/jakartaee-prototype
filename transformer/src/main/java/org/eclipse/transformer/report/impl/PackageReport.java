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

import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.TransformException;
import org.slf4j.Logger;

public class PackageReport extends TransformReportImpl {

	public PackageReport(Logger logger, boolean isTerse, boolean isVerbose) {
		super(logger, isTerse, isVerbose);
	}

	@Override
	public String getTitle() {
		return "Package Report";
	}

	//

	@Override
	public void emitCore(PrintWriter writer) throws IOException {
		// NO-OP
	}

	//

	@Override
	public void init() throws TransformException {
		super.init();
	}

	@Override
	public void complete() throws TransformException {
		super.complete();
	}

	//

	@Override
	public void begin(Action action, String inputName) {
		super.begin(action, inputName);
	}

	@Override
	public void processInitialData(Object data) {
		super.processInitialData(data);
	}

	@Override
	public void processFinalData(Object data) {
		super.processFinalData(data);
	}

	@Override
	public void end(Action action, Changes changes) {
		super.end(action, changes);
	}
}
