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

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.transformer.TransformerState;
import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.ContainerChanges;
import org.slf4j.Logger;

public class ContainerChangesImpl extends ChangesImpl implements ContainerChanges {

	protected ContainerChangesImpl() {
		super();

		this.changedByAction = new HashMap<String, int[]>();
		this.unchangedByAction = new HashMap<String, int[]>();

		this.allChanged = 0;
		this.allUnchanged = 0;

		this.allSelected = 0;
		this.allUnselected = 0;
		this.allResources = 0;

		this.allNestedChanges = null;
	}

	//

	@Override
	public boolean hasNonResourceNameChanges() {
		return ( allChanged > 0 );
	}

	@Override
	public void clearChanges() {
		changedByAction.clear();
		unchangedByAction.clear();

		allChanged = 0;
		allUnchanged = 0;

		allSelected = 0;
		allUnselected = 0;
		allResources = 0;

		allNestedChanges = null;

		super.clearChanges();
	}

	//

	private final Map<String, int[]> changedByAction;
	private final Map<String, int[]> unchangedByAction;

	private int allUnchanged;
	private int allChanged;

	private int allSelected;
	private int allUnselected;	
	private int allResources;

	//

	@Override
	public Set<String> getActionNames() {
		Set<String> changedNames = changedByAction.keySet();
		Set<String> unchangedNames = unchangedByAction.keySet();

		Set<String> allNames =
			new HashSet<String>( changedNames.size() + unchangedNames.size() );

		allNames.addAll(changedNames);
		allNames.addAll(unchangedNames);

		return allNames;
	}

	//

	@Override
	public Map<String, int[]> getChangedByAction() {
		return Collections.unmodifiableMap( changedByAction );
	}

	@Override
	public Map<String, int[]> getUnchangedByAction() {
		return Collections.unmodifiableMap( unchangedByAction );
	}

	//

	@Override
	public int getAllResources() {
		return allResources;
	}

	@Override
	public int getAllUnselected() {
		return allUnselected;
	}

	@Override
	public int getAllSelected() {
		return allSelected;
	}

	@Override
	public int getAllUnchanged() {
		return allUnchanged;
	}

	@Override
	public int getAllChanged() {
		return allChanged;
	}

	@Override
	public int getChanged(Action action) {
		return getChanged( action.getName() );
	}

	@Override
	public int getChanged(String name) {
		int[] changes = changedByAction.get(name);
		return ( (changes == null) ? 0 : changes[0] );
	}

	@Override
	public int getUnchanged(Action action) {
		return getUnchanged( action.getName() );
	}

	@Override
	public int getUnchanged(String name) {
		int[] changes = unchangedByAction.get(name);
		return ( (changes == null) ? 0 : changes[0] );
	}


	protected static final boolean HAS_CHANGES = true;

	protected void record(TransformerState state, ActionImpl action) {
		ChangesImpl changes = action.getLastActiveChanges(state);
		record( action.getName(), changes.hasChanges() );
		changes.addNestedInto(this);
	}

	protected void record(Action action, boolean hasChanges) {
		record( action.getName(), hasChanges );
	}

	protected void record(String name, boolean hasChanges) {
		allResources++;
		allSelected++;

		Map<String, int[]> target;
		if ( hasChanges ) {
			allChanged++;
			target = changedByAction;
		} else {
			allUnchanged++;
			target = unchangedByAction;
		}

		int[] changes = target.get(name);
		if ( changes == null ) {
			changes = new int[] { 1 };
			target.put(name, changes);
		} else {
			changes[0]++;
		}
	}

	protected void record() {
		allResources++;
		allUnselected++;
	}

	protected void addNestedInto(ContainerChangesImpl containerChanges) {
		containerChanges.addNested(this);
	}

	//

	private ContainerChangesImpl allNestedChanges;

	@Override
	public boolean hasNestedChanges() {
		return ( allNestedChanges != null );
	}

	@Override
	public ContainerChangesImpl getNestedChanges() {
		return allNestedChanges;
	}

	/**
	 * Add other changes as nested changes.
	 *
	 * Both the immediate part of the other changes and the nested part
	 * of the other changes are added.
	 *
	 * @param otherChanges Other container changes to add as nested changes.
	 */
	protected void addNested(ContainerChangesImpl otherChanges) {
		if ( allNestedChanges == null ) {
			allNestedChanges = new ContainerChangesImpl();
		}
		allNestedChanges.add(otherChanges);

		ContainerChangesImpl otherNestedChanges = otherChanges.getNestedChanges();
		if ( otherNestedChanges != null ) {
			allNestedChanges.add(otherNestedChanges);
		}
	}

	protected void add(ContainerChangesImpl otherChanges) {
		addChangeMap( this.changedByAction, otherChanges.getChangedByAction() );
		addChangeMap( this.unchangedByAction, otherChanges.getUnchangedByAction() );

		this.allChanged += otherChanges.getAllChanged();
		this.allUnchanged += otherChanges.getAllUnchanged();

		this.allSelected += otherChanges.getAllSelected();
		this.allUnselected += otherChanges.getAllUnselected();
		this.allResources += otherChanges.getAllResources();
	}

	private void addChangeMap(
		Map<String, int[]> thisChangeMap, Map<String, int[]> otherChangeMap) {

		int[] nextChanges = new int[1];
		for ( Map.Entry<String, int[]> mapEntry : otherChangeMap.entrySet() ) {
			int[] thisChanges = thisChangeMap.putIfAbsent( mapEntry.getKey(), nextChanges );
			if ( thisChanges == null ) {
				thisChanges = nextChanges;
				nextChanges = new int[1];
			}
			thisChanges[0] += mapEntry.getValue()[0];
		}
	}

	//

	private static final String DASH_LINE =
		"================================================================================";
	private static final String SMALL_DASH_LINE =
		"--------------------------------------------------------------------------------";

	private static final String DATA_LINE =
		"[ %22s ] [ %6s ] %10s [ %6s ] %8s [ %6s ]%s";

	private String formatData(Object... parms) {
		return String.format(DATA_LINE, parms);
	}

	protected void displayChanges(PrintStream stream) {
		stream.print( formatData(
			"All Resources", getAllResources(),
			"Unselected", getAllUnselected(),
			"Selected", getAllSelected(),
			"\n" ) );

		stream.print(SMALL_DASH_LINE);
		stream.print("\n");
		
		stream.print( formatData(
			"All Actions", getAllSelected(),
			"Unchanged", getAllUnchanged(),
			"Changed", getAllChanged(),
			"\n" ) );

		for ( String actionName : getActionNames() ) {
			int useUnchangedByAction = getUnchanged(actionName); 
			int useChangedByAction = getChanged(actionName);
			stream.print( formatData(
				actionName, useUnchangedByAction + useChangedByAction,
				"Unchanged", useUnchangedByAction,
				"Changed", useChangedByAction,
				"\n" ) );
		}
	}

	protected void displayChanges(Logger logger) {
		logger.info( formatData(
			"All Resources", getAllResources(),
			"Unselected", getAllUnselected(),
			"Selected", getAllSelected(),
			"" ) );

		logger.info( SMALL_DASH_LINE );
		logger.info( formatData(
			"All Actions", getAllSelected(),
			"Unchanged", getAllUnchanged(),
			"Changed", getAllChanged(),
			"" ) );

		for ( String actionName : getActionNames() ) {
			int useUnchangedByAction = getUnchanged(actionName); 
			int useChangedByAction = getChanged(actionName);
			logger.info( formatData(
				actionName, useUnchangedByAction + useChangedByAction,
				"Unchanged", useUnchangedByAction,
				"Changed", useChangedByAction,
				"" ) );
		}
	}

	
    @Override
	public void displayVerbose(PrintStream stream, String inputPath, String outputPath) {
		// ================================================================================
		// [ Input  ] [ test.jar ]
    	//            [ c:\dev\jakarta-repo-pub\jakartaee-prototype\dev\transformer\app\test.jar ]
		// [ Output ] [ output_test.jar ]
    	//            [ c:\dev\jakarta-repo-pub\jakartaee-prototype\dev\transformer\app\testOutput.jar ]
		// ================================================================================  
		// [          All Resources ] [     55 ] Unselected [      6 ] Selected [     49 ]
		// ================================================================================
    	// [ Immediate changes: ]
		// --------------------------------------------------------------------------------
		// [            All Actions ] [     49 ]  Unchanged [     43 ]  Changed [      6 ]
		// [           Class Action ] [     41 ]  Unchanged [     38 ]  Changed [      3 ]
		// [        Manifest Action ] [      1 ]  Unchanged [      0 ]  Changed [      1 ]
		// [  Service Config Action ] [      7 ]  Unchanged [      5 ]  Changed [      2 ]
		// ================================================================================
    	// [ Nested changes: ]
		// --------------------------------------------------------------------------------
    	// [ ... ]
		// ================================================================================

		stream.print(DASH_LINE);
		stream.print("\n");

		stream.printf( "[ Input  ] [ %s ]\n           [ %s ]\n", getInputResourceName(), inputPath );
		stream.printf( "[ Output ] [ %s ]\n           [ %s ]\n", getOutputResourceName(), outputPath );
		stream.print(DASH_LINE);
		stream.print("\n");

		stream.printf( "[ Immediate changes: ]\n");
		stream.print(SMALL_DASH_LINE);
		stream.print("\n");

		displayChanges(stream);
		stream.print(DASH_LINE);
		stream.print("\n");
		
		if ( allNestedChanges != null ) {
			stream.printf("[ Nested changes: ]\n");
			stream.printf(SMALL_DASH_LINE);
			stream.print("\n");
			
			allNestedChanges.displayChanges(stream);
			stream.printf(DASH_LINE);
			stream.print("\n");
		}
	}
    
    @Override
	public void displayVerbose(Logger logger, String inputPath, String outputPath) {
    	if ( !logger.isInfoEnabled() ) {
    		return;
    	}

		logger.info( DASH_LINE );

		logger.info("[ Input  ] [ {} ]", getInputResourceName());
		logger.info("           [ {} ]", inputPath);
		logger.info("[ Output ] [ {} ]", getOutputResourceName());
		logger.info("           [ {} ]", outputPath );
		logger.info( DASH_LINE );

		logger.info( "[ Immediate changes: ]");
		logger.info( SMALL_DASH_LINE );
		displayChanges(logger);
		logger.info( DASH_LINE );

		if ( allNestedChanges != null ) {
			logger.info( "[ Nested changes: ]");
			logger.info( SMALL_DASH_LINE );
			allNestedChanges.displayChanges(logger);
			logger.info( DASH_LINE );
		}
	}
    
	@Override
	public void displayTerse(PrintStream stream, String inputPath, String outputPath) {
		// [          All Resources ] [     55 ] Unselected [      6 ] Selected [     49 ]
		// [            All Actions ] [     49 ]  Unchanged [     43 ]  Changed [      6 ]

		if ( !inputPath.equals(outputPath) ) {
			stream.printf("Input [ %s ] as [ %s ]: %s\n", inputPath, outputPath, getChangeTag() );
		} else {
			stream.printf("Input [ %s ]: %s\n", inputPath, getChangeTag() );
		}

		stream.print( formatData(
			"All Resources", getAllResources(),
			"Unselected", getAllUnselected(),
			"Selected", getAllSelected(),
			"\n" ) );
		stream.print( formatData(
			"All Actions", getAllSelected(),
			"Unchanged", getAllUnchanged(),
			"Changed", getAllChanged(),
			"\n" ) );
		if ( allNestedChanges != null ) {
			stream.print( formatData(
				"Nested Resources", allNestedChanges.getAllResources(),
				"Unselected", allNestedChanges.getAllUnselected(),
				"Selected", allNestedChanges.getAllSelected(),
				"\n" ) );
			stream.print( formatData(
				"Nested Actions", allNestedChanges.getAllSelected(),
				"Unchanged", allNestedChanges.getAllUnchanged(),
				"Changed", allNestedChanges.getAllChanged(),
				"\n" ) );
		}
	}

	@Override
	public void displayTerse(Logger logger, String inputPath, String outputPath) {
		if ( !logger.isInfoEnabled() ) {
			return;
		}

		if ( !inputPath.equals(outputPath) ) {
			if ( !inputPath.equals(outputPath) ) {
				logger.info("Input [ {} ] as [ {} ]: {}", inputPath, outputPath, getChangeTag() );
			} else {
				logger.info("Input [ {} ]: {}", inputPath, getChangeTag() );
			}
		}

		logger.info( formatData(
			"All Resources", getAllResources(),
			"Unselected", getAllUnselected(),
			"Selected", getAllSelected(),
			"" ) );
		logger.info( formatData(
			"All Actions", getAllSelected(),
			"Unchanged", getAllUnchanged(),
			"Changed", getAllChanged(),
			"" ) );
		if ( allNestedChanges != null ) {
			logger.info( formatData(
				"Nested Resources", allNestedChanges.getAllResources(),
				"Unselected", allNestedChanges.getAllUnselected(),
				"Selected", allNestedChanges.getAllSelected(),
				"" ) );
			logger.info( formatData(
				"Nested Actions", allNestedChanges.getAllSelected(),
				"Unchanged", allNestedChanges.getAllUnchanged(),
				"Changed", allNestedChanges.getAllChanged(),
				"" ) );
		}
	}
}
