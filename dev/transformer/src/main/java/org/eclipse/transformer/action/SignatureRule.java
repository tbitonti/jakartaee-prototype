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

package org.eclipse.transformer.action;

import java.util.Map;

import aQute.bnd.signatures.ArrayTypeSignature;
import aQute.bnd.signatures.ClassSignature;
import aQute.bnd.signatures.ClassTypeSignature;
import aQute.bnd.signatures.FieldSignature;
import aQute.bnd.signatures.JavaTypeSignature;
import aQute.bnd.signatures.MethodSignature;
import aQute.bnd.signatures.ReferenceTypeSignature;
import aQute.bnd.signatures.Result;
import aQute.bnd.signatures.SimpleClassTypeSignature;
import aQute.bnd.signatures.ThrowsSignature;
import aQute.bnd.signatures.TypeArgument;
import aQute.bnd.signatures.TypeParameter;

public interface SignatureRule {

	BundleData getBundleUpdate(String symbolicName);

	//

	Map<String, String> getPackageRenames();

	Map<String, String> getPackageVersions();

	//

	public static enum SignatureType {
		CLASS, FIELD, METHOD
	}

	public static final boolean ALLOW_SIMPLE_SUBSTITUTION = true;
	public static final boolean NO_SIMPLE_SUBSTITUTION = false;
	
	/**
	 * Replace a single package according to the package rename rules.
	 * 
	 * Package names must match exactly.
	 *
	 * @param initialName The package name which is to be replaced.
	 *
	 * @return The replacement for the initial package name.  Null if no
	 *     replacement is available.
	 */
	String replacePackage(String initialName);

	/**
	 * Replace a single package according to the package rename rules.
	 * The package name has '/' separators, not '.' separators.
	 *
	 * Package names must match exactly.
	 *
	 * @param initialName The package name which is to be replaced.
	 *
	 * @return The replacement for the initial package name.  Null if no
	 *     replacement is available.
	 */
	String replaceBinaryPackage(String initialName);

	/**
	 * Replace all embedded packages of specified text with replacement
	 * packages.
	 *
	 * @param text String embedding zero, one, or more package names.
	 * @param packageRenames map of names and replacement values
	 * @return The text with all embedded package names replaced.  Null if no
	 *     replacements were performed.
	 */
	String replacePackages(String text);
	String replacePackages(String text, Map<String, String> packageRenames);

	String transformConstantAsBinaryType(String inputConstant);
	String transformConstantAsBinaryType(String inputConstant, boolean allowSimpleSubstitution);

	/**
	 * Modify a fully qualified type name according to the package rename table.
	 * Answer either the transformed type name, or, if the type name was not changed,
	 * a wrapped null.
	 * 
	 * @param inputName A fully qualified type name which is to be transformed.
	 *
	 * @return The transformed type name, or a wrapped null if no changed was made.
	 */
	String transformBinaryType(String inputName);

	String transformConstantAsDescriptor(String inputConstant);
	String transformConstantAsDescriptor(String inputConstant, boolean allowSimpleSubstitution);
	
	String transformDescriptor(String inputDescriptor);
	String transformDescriptor(String inputDescriptor, boolean allowSimpleSubstitution);

	/**
	 * Transform a class, field, or method signature.
	 * 
	 * Answer a wrapped null if the signature is not changed by the transformation
	 * rules.
	 *
	 * @param input The signature value which is to be transformed.
	 * @param signatureType The type of the signature value.
	 *
	 * @return The transformed signature value.  A wrapped null if no change
	 *     was made to the value.
	 */
	String transform(String input, SignatureType signatureType);

	ClassSignature transform(ClassSignature classSignature);

	FieldSignature transform(FieldSignature fieldSignature);

	MethodSignature transform(MethodSignature methodSignature);

	Result transform(Result type);

	ThrowsSignature transform(ThrowsSignature type);

	ArrayTypeSignature transform(ArrayTypeSignature inputType);

	TypeParameter transform(TypeParameter inputTypeParameter);

	ClassTypeSignature transform(ClassTypeSignature inputType);

	SimpleClassTypeSignature transform(SimpleClassTypeSignature inputSignature);

	TypeArgument transform(TypeArgument inputArgument);

	JavaTypeSignature transform(JavaTypeSignature type);

	ReferenceTypeSignature transform(ReferenceTypeSignature type);

	//

	String getDirectString(String initialValue);

}