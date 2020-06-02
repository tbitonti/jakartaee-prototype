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

package org.eclipse.transformer.action.impl;

import java.io.File;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.TransformerState;
import org.eclipse.transformer.action.ActionType;
import org.slf4j.Logger;

public class DirectoryActionImpl extends ContainerActionImpl {

    public DirectoryActionImpl(
        Logger logger, boolean isTerse, boolean isVerbose,
    	InputBufferImpl buffer,
    	SelectionRuleImpl selectionRule, SignatureRuleImpl signatureRule) {

    	super(logger,  isTerse, isVerbose, buffer, selectionRule, signatureRule);
    }

	//

	@Override
	public ActionType getActionType() {
		return ActionType.DIRECTORY;
	}

	@Override
	public String getName() {
		return "Directory";
	}

	//

	/**
	 * The choice of using a stream or using an input stream should never occur
	 * on a directory action.
	 */
	public boolean useStreams() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean accept(String resourceName, File resourceFile) {
		return ( (resourceFile != null) && resourceFile.isDirectory() );
	}

    @Override
	public void apply(
		TransformerState state,
		String rootPath, File inputFile, File outputFile)
		throws TransformException {

    	startRecording(state, rootPath);
    	try {
    		setResourceNames(state, rootPath, rootPath);
    		transform(state, null, inputFile, outputFile);

    	} finally {
    		stopRecording(state, rootPath);
    	}
	}

	protected void transform(
		TransformerState state,
		String pathFromRoot, File inputFile,
		File outputFile)  throws TransformException {

	    // Note the asymmetry between the handling of the root directory, 
	    // which is selected by a composite action, and the handling of sub-directories,
	    // which are handled automatically by the directory action.
	    //
	    // This means that the directory action processes the entire tree
	    // of child directories.
	    //
	    // The alternative would be to put the directory action as a child of itself,
	    // and have sub-directories be accepted using composite action selection.

		String inputName = inputFile.getName();

		// info("DirectoryActionImpl.transform pathFromRoot {} inputName {}", pathFromRoot, inputName);

	    if ( inputFile.isDirectory() ) {
	    	if ( !outputFile.exists() ) {
	    		outputFile.mkdir();
	    	}

	    	for ( File childInputFile : inputFile.listFiles() ) {
	    		File childOutputFile = new File( outputFile, childInputFile.getName() );
	    		String childName = childInputFile.getName();

	    		String childPath;
				if ( pathFromRoot == null ) {
					childPath = childName;
				} else {
					childPath = pathFromRoot + '/' + childName;
				}

				// info("DirectoryActionImpl.transform childPath {}", childPath);

	    		transform(state, childPath, childInputFile, childOutputFile);
	    	}

	    } else {
	    	ActionImpl selectedAction = acceptAction(pathFromRoot, inputFile);
	    	if ( selectedAction == null ) {
	    		recordUnaccepted(state, pathFromRoot);
	    	} else if ( !select(pathFromRoot) ) {
	    		recordUnselected(state, selectedAction, pathFromRoot);
	    	} else {
	    		selectedAction.apply(state, pathFromRoot, inputFile, outputFile);
	    		recordTransform(state, selectedAction, pathFromRoot);
	    	}
	    }
	}
}
