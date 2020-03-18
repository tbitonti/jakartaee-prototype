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

public class ClassChangesImpl extends ChangesImpl {
	@Override
	public void clearChanges() {
		inputClassName = null;
		outputClassName = null;

		inputSuperName = null;
		outputSuperName = null;

		modifiedInterfaces = 0;

		modifiedFields = 0;
		modifiedMethods = 0;
		modifiedAttributes = 0;

		modifiedConstants = 0;
	}

	@Override
	public boolean hasNonResourceNameChanges() {
		return ( ((inputClassName != null) && (outputClassName != null) && !inputClassName.equals(outputClassName)) ||
				 ((inputSuperName != null) && (outputSuperName != null) && !inputSuperName.equals(outputSuperName)) ||

				 (modifiedInterfaces > 0) ||

				 (modifiedFields > 0) ||
				 (modifiedMethods > 0) ||
				 (modifiedAttributes > 0) ||

				 (modifiedConstants > 0) );
	}

	//

	private String inputClassName;
	private String outputClassName;

	public String getInputClassName() {
		return inputClassName;
	}

	public void setInputClassName(String inputClassName) {
		this.inputClassName = inputClassName;
	}

	public String getOutputClassName() {
		return outputClassName;
	}

	public void setOutputClassName(String outputClassName) {
		this.outputClassName = outputClassName;
	}

	//

	private String inputSuperName;
	private String outputSuperName;

	public String getInputSuperName() {
		return inputSuperName;
	}

	public void setInputSuperName(String inputSuperName) {
		this.inputSuperName = inputSuperName;
	}

	public String getOutputSuperName() {
		return outputSuperName;
	}

	public void setOutputSuperName(String outputSuperName) {
		this.outputSuperName = outputSuperName;
	}

	private int modifiedInterfaces;

	public int getModifiedInterfaces() {
		return modifiedInterfaces;
	}

	public void setModifiedInterfaces(int modifiedInterfaces) {
		this.modifiedInterfaces = modifiedInterfaces;
	}

	public void addModifiedInterface() {
		modifiedInterfaces++;
		
	}

	//

	private int modifiedFields;
	private int modifiedMethods;
	private int modifiedAttributes;

	public int getModifiedFields() {
		return modifiedFields;
	}

	public void setModifiedFields(int modifiedFields) {
		this.modifiedFields = modifiedFields;
	}

	public void addModifiedField() {
		modifiedFields++;
	}

	public int getModifiedMethods() {
		return modifiedMethods;
	}

	public void setModifiedMethods(int modifiedMethods) {
		this.modifiedMethods = modifiedMethods;
	}

	public void addModifiedMethod() {
		modifiedMethods++;
	}

	public int getModifiedAttributes() {
		return modifiedAttributes;
	}

	public void setModifiedAttributes(int modifiedAttributes) {
		this.modifiedAttributes = modifiedAttributes;
	}

	public void addModifiedAttribute() {
		modifiedAttributes++;
	}

	//

	private int modifiedConstants;

	public int getModifiedConstants() {
		return modifiedConstants;
	}

	public void setModifiedConstants(int modifiedConstants) {
		this.modifiedConstants = modifiedConstants;
	}

	public void addModifiedConstant() {
		modifiedConstants++;
	}

	//

	@Override
	public void displayChanges(PrintStream printStream, String inputPath, String outputPath) {
		printStream.printf(
			"Input name [ %s ] as [ %s ]\n",
			getInputResourceName(), inputPath );
		
		printStream.printf(
			"Output name [ %s ] as [ %s ]\n",
			getOutputResourceName(), outputPath );

		printStream.printf(
			"Class name [ %s ] [ %s ]\n",
			getInputClassName(), getOutputClassName() );

		String useInputSuperName = getInputSuperName();
		if ( useInputSuperName != null ) {
			printStream.printf(
				"Super class name [ %s ] [ %s ]\n",
				useInputSuperName, getOutputSuperName() );
		}

		printStream.printf( "Modified interfaces [ %s ]\n", getModifiedInterfaces() );
		printStream.printf( "Modified fields     [ %s ]\n", getModifiedFields() );
		printStream.printf( "Modified methods    [ %s ]\n", getModifiedMethods() );
		printStream.printf( "Modified constants  [ %s ]\n", getModifiedConstants() );	
	}
}
