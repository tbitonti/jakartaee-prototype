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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.TransformException;
import org.slf4j.Logger;

public class ListReport extends TransformReportImpl {

	public ListReport(Logger logger, boolean isTerse, boolean isVerbose) {
		super(logger, isTerse, isVerbose);
	}

	@Override
	public String getTitle() {
		return "List Report";
	}

	@Override
	public void emitCore(PrintWriter writer) throws IOException {
		for ( ResourceData resourceData : getListing() ) {
			writer.println(
				resourceData.initialName + ", " +
				resourceData.finalName + ", " +
				resourceData.actionName + ", " +
				(resourceData.changed ? "Changed" : "Unchanged") );
		}
	}

	//

	protected static class ResourceData {
		public final String initialName;
		public final String finalName;

		public final String actionName;
		public final boolean changed;
		
		public ResourceData(String initialName, String finalName, String actionName, boolean changed) {
			this.initialName = initialName;
			this.finalName = finalName;
			this.actionName = actionName;
			this.changed = changed;
		}
	}

	private List<ResourceData> resourceListing;
	
	protected List<ResourceData> getListing() {
		return resourceListing;
	}

	protected void initListing() {
		resourceListing = new ArrayList<ResourceData>();
	}

	protected void addListing(String useInitialName, String finalName, String actionName, boolean changed) {
		getListing().add( new ResourceData(useInitialName, finalName, actionName, changed) );
	}

	protected void addListing(Action action, Changes changes) {
		addListing(
			getInitialName(),
			changes.getOutputResourceName(),
			action.getName(),
			changes.hasNonResourceNameChanges() );
	}

	//

	@Override
	public void init() throws TransformException {
		super.init();

		initListing();
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

		addListing(action, changes);;
	}
}
