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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.TransformException;
import org.slf4j.Logger;

public class ListReportImpl extends TransformReportImpl {

	public ListReportImpl(Logger logger, boolean isTerse, boolean isVerbose) {
		super(logger, isTerse, isVerbose);
	}

	@Override
	public String getTitle() {
		return "List Report";
	}

	private static final String SPACES = "                                                            ";

	private static Map<Integer, String> spaces = new HashMap<Integer, String>();

	private static String getSpaces(int count) {
		if ( count * 2 > SPACES.length() ) {
			count = SPACES.length() / 2;
		}

		String countedSpaces;
		synchronized( spaces ) {
			countedSpaces =  spaces.get(count);
			if ( countedSpaces == null ) {
				countedSpaces = SPACES.substring(0, count);
				spaces.put(count, countedSpaces);
			}
		}
		return countedSpaces;
	}

	@Override
	public void emitCore(PrintWriter writer) throws IOException {
		List<String> containerNames = new ArrayList<String>();
		int numContainerNames = 0;
		String containerName = null;
		String containerPrefix = getSpaces(0);

		StringBuilder lineBuilder = new StringBuilder();

		String prefixText;

		String initialName = null;
		String finalName = null;

		for ( ResourceData resourceData : getResources() ) {
			String actionName = resourceData.actionName;
			boolean isContainer = resourceData.isContainer;
			boolean isNull = resourceData.isNull;

			boolean isInitial = resourceData.isInitial;
			String resourceName = resourceData.name;

			char transitionChar;
			String changeText;

			// Cases:
			//   initial container
			//     initial non-container
			//     final non-container
			//   final container

			if ( isInitial ) {
				initialName = resourceName;

				if ( isContainer ) {
					containerName = initialName;
					transitionChar = '>';
					changeText = null;

					prefixText = containerPrefix;

					containerNames.add(initialName);

					numContainerNames++;
					containerPrefix = getSpaces(numContainerNames * 2);

					// Display container data at the beginning and at the end of the action.

				} else {
					continue; // Display non-container data at the end of the action.
				}

			} else {
				finalName = resourceName;

				if ( isContainer ) {
					transitionChar = '<';

					numContainerNames--;
					containerPrefix = getSpaces(numContainerNames * 2);
					prefixText = containerPrefix;

					initialName = containerNames.remove(numContainerNames);

					if ( numContainerNames == 0 ) {
						containerName = null;
					} else {
						containerName = containerNames.get(numContainerNames - 1);
					}

					// Display container data at the beginning and at the end of the action.

				} else {
					if ( initialName == null ) {
						error("Resource end without resource beginning {}", finalName);
						continue;
					}

					transitionChar = ' ';
					prefixText = containerPrefix;

					// Display non-container data at the end of the action.					
				}

				changeText = ( resourceData.isChanged ? "+" : null );
			}

			lineBuilder.append(transitionChar);
			lineBuilder.append(' ');

			lineBuilder.append(prefixText);
			lineBuilder.append(initialName);

			if ( !isInitial ) {
				if ( (finalName != null) && !initialName.equals(finalName) ) {
					lineBuilder.append(", ");
					lineBuilder.append(finalName);
				}
			}

			if ( !isNull ) {
				lineBuilder.append(", ");
				lineBuilder.append(actionName);
			}

			if ( changeText != null ) {
				lineBuilder.append(", ");
				lineBuilder.append(changeText);
			}

			writer.println( lineBuilder.toString() );
			lineBuilder.setLength(0);

			if ( !isInitial ) {
				initialName = null;
				finalName = null;
			}
		}
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
	public Object processFinalData(Object data) {
		return super.processFinalData(data);
	}

	@Override
	public void end(Action action, Changes changes) {
		super.end(action, changes);
	}
}
