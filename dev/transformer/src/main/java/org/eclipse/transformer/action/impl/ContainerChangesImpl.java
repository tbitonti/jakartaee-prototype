/*
 * Copyright (c) 2016-2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.transformer.action.impl;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.ContainerChanges;

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

	@Override
	public void record(Action action) {
		record( action.getName(), action.hasChanges() );

		action.getChanges().addNestedInto(this);
	}

	@Override
	public void record(Action action, boolean hasChanges) {
		record( action.getName(), hasChanges );
	}

	@Override
	public void record(String name, boolean hasChanges) {
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

	@Override
	public void record() {
		allResources++;
		allUnselected++;
	}

	@Override
	public void addNestedInto(ContainerChanges containerChanges) {
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
	@Override
	public void addNested(ContainerChanges otherChanges) {
		if ( allNestedChanges == null ) {
			allNestedChanges = new ContainerChangesImpl();
		}
		allNestedChanges.add(otherChanges);

		ContainerChanges otherNestedChanges = otherChanges.getNestedChanges();
		if ( otherNestedChanges != null ) {
			allNestedChanges.add(otherNestedChanges);
		}
	}

	@Override
	public void add(ContainerChanges otherChanges) {
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
		"================================================================================\n";
	private static final String SMALL_DASH_LINE =
		"--------------------------------------------------------------------------------\n";

	private static final String DATA_LINE =
		"[ %22s ] [ %6s ] %10s [ %6s ] %8s [ %6s ]\n";

	protected void displayChanges(PrintStream stream) {
		stream.printf( DATA_LINE,
			"All Resources", getAllResources(),
			"Unselected", getAllUnselected(),
			"Selected", getAllSelected() );

		stream.printf( DASH_LINE );
		stream.printf( DATA_LINE,
			"All Actions", getAllSelected(),
			"Unchanged", getAllUnchanged(),
			"Changed", getAllChanged());

		for ( String actionName : getActionNames() ) {
			int useUnchangedByAction = getUnchanged(actionName); 
			int useChangedByAction = getChanged(actionName);
			stream.printf( DATA_LINE,
				actionName, useUnchangedByAction + useChangedByAction,
				"Unchanged", useUnchangedByAction,
				"Changed", useChangedByAction);
		}
	}

    @Override
	public void displayChanges(PrintStream stream, String inputPath, String outputPath) {
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
		// [            All Actions ] [     49 ]   Unchangd [     43 ]  Changed [      6 ]
		// [           Class Action ] [     41 ]  Unchanged [     38 ]  Changed [      3 ]
		// [        Manifest Action ] [      1 ]  Unchanged [      0 ]  Changed [      1 ]
		// [  Service Config Action ] [      7 ]  Unchanged [      5 ]  Changed [      2 ]
		// ================================================================================
    	// [ Nested changes: ]
		// --------------------------------------------------------------------------------
    	// [ ... ]
		// ================================================================================

		stream.printf( DASH_LINE );

		stream.printf( "[ Input  ] [ %s ]\n           [ %s ]\n", getInputResourceName(), inputPath );
		stream.printf( "[ Output ] [ %s ]\n           [ %s ]\n", getOutputResourceName(), outputPath );
		stream.printf( DASH_LINE );

		stream.printf( "[ Immediate changes: ]\n");
		stream.printf( SMALL_DASH_LINE );
		displayChanges(stream);
		stream.printf( DASH_LINE );

		if ( allNestedChanges != null ) {
			stream.printf( "[ Nested changes: ]\n");
			stream.printf( SMALL_DASH_LINE );
			allNestedChanges.displayChanges(stream);
			stream.printf( DASH_LINE );
		}
	}
}
