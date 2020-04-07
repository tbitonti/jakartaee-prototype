/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.SignatureRule.SignatureType;
import org.eclipse.transformer.util.ByteData;
import org.eclipse.transformer.util.FileUtils;
import org.eclipse.transformer.util.InputStreamData;
import org.slf4j.Logger;

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
import aQute.lib.io.IO;

public abstract class ActionImpl implements Action {
	public ActionImpl(
		Logger logger,
		InputBufferImpl buffer,
		SelectionRuleImpl selectionRule,
		SignatureRuleImpl signatureRule) {

		this.logger = logger;

		this.buffer = buffer;

		this.selectionRule = selectionRule;
		this.signatureRule = signatureRule;

		this.changes = newChanges();
	}

	//

	public static interface ActionInit<A extends ActionImpl> {
		A apply(
			Logger logger,
			InputBufferImpl buffer,
			SelectionRuleImpl selectionRule,
			SignatureRuleImpl signatureRule);
	}

	public <A extends ActionImpl> A createUsing(ActionInit<A> init) {
		return init.apply( getLogger(), getBuffer(), getSelectionRule(), getSignatureRule() );
	}

	//

	private final Logger logger;

	public Logger getLogger() {
		return logger;
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

	private final InputBufferImpl buffer;

	@Override
	public InputBufferImpl getBuffer() {
		return buffer;
	}

	@Override
	public byte[] getInputBuffer() {
		return getBuffer().getInputBuffer();
	}

	@Override
	public void setInputBuffer(byte[] inputBuffer) {
		getBuffer().setInputBuffer(inputBuffer);
	}

    //

    private final SelectionRuleImpl selectionRule;

	public SelectionRuleImpl getSelectionRule() {
		return selectionRule;
    }

	public boolean select(String resourceName) {
		return getSelectionRule().select(resourceName);
	}

	public boolean selectIncluded(String resourceName) {
		return getSelectionRule().selectIncluded(resourceName);
	}

	public boolean rejectExcluded(String resourceName) {
		return getSelectionRule().rejectExcluded(resourceName);
	}

    //

    protected final SignatureRuleImpl signatureRule;

	public SignatureRuleImpl getSignatureRule() {
		return signatureRule;
    }

    public BundleData getBundleUpdate(String symbolicName) {
		return getSignatureRule().getBundleUpdate(symbolicName);
	}

	public Map<String, String> getPackageRenames() {
		return getSignatureRule().getPackageRenames();
	}

	public Map<String, String> getPackageVersions() {
		return getSignatureRule().getPackageVersions();
	}

	public String replacePackage(String initialName) {
		return getSignatureRule().replacePackage(initialName);
	}

	public String replaceBinaryPackage(String initialName) {
		return getSignatureRule().replaceBinaryPackage(initialName);
	}

	public String replaceEmbeddedPackages(String embeddingText) {
		return getSignatureRule().replacePackages(embeddingText);
	}
	
	public String replaceText(String inputFileName, String text) {
	       return getSignatureRule().replaceText(inputFileName, text);
	}

	public String transformConstantAsBinaryType(String inputConstant) {
		return getSignatureRule().transformConstantAsBinaryType(inputConstant);
	}
	
    public String transformConstantAsBinaryType(String inputConstant, boolean simpleSubstitution) {
        return getSignatureRule().transformConstantAsBinaryType(inputConstant, simpleSubstitution);
    }
    
	public String transformBinaryType(String inputName) {
		return getSignatureRule().transformBinaryType(inputName);
	}

	public String transformConstantAsDescriptor(String inputConstant) {
	    return getSignatureRule().transformConstantAsDescriptor(inputConstant);
	}

	public String transformConstantAsDescriptor(String inputConstant, boolean simpleSubstitution) {
	    return getSignatureRule().transformConstantAsDescriptor(inputConstant, simpleSubstitution);
	}

	public String transformDescriptor(String inputDescriptor) {
		return getSignatureRule().transformDescriptor(inputDescriptor);
	}

	public String transform(String input, SignatureType signatureType) {
		return getSignatureRule().transform(input, signatureType);
	}

	public ClassSignature transform(ClassSignature classSignature) {
		return getSignatureRule().transform(classSignature);
	}

	public FieldSignature transform(FieldSignature fieldSignature) {
		return getSignatureRule().transform(fieldSignature);
	}

	public MethodSignature transform(MethodSignature methodSignature) {
		return getSignatureRule().transform(methodSignature);
	}

	public Result transform(Result type) {
		return getSignatureRule().transform(type);
	}

	public ThrowsSignature transform(ThrowsSignature type) {
		return getSignatureRule().transform(type);
	}

	public ArrayTypeSignature transform(ArrayTypeSignature inputType) {
		return getSignatureRule().transform(inputType);
	}

	public TypeParameter transform(TypeParameter inputTypeParameter) {
		return getSignatureRule().transform(inputTypeParameter);
	}

	public ClassTypeSignature transform(ClassTypeSignature inputType) {
		return getSignatureRule().transform(inputType);
	}

	public SimpleClassTypeSignature transform(SimpleClassTypeSignature inputSignature) {
		return getSignatureRule().transform(inputSignature);
	}

	public TypeArgument transform(TypeArgument inputArgument) {
		return getSignatureRule().transform(inputArgument);
	}

	public JavaTypeSignature transform(JavaTypeSignature type) {
		return getSignatureRule().transform(type);
	}

	public ReferenceTypeSignature transform(ReferenceTypeSignature type) {
		return getSignatureRule().transform(type);
	}

	public String transformDirectString(String initialValue) {
		return getSignatureRule().getDirectString(initialValue);
	}
	
	//

	public abstract String getAcceptExtension();

	@Override
	public boolean accept(String resourceName) {
		return accept(resourceName, null);
	}

	@Override
	public boolean accept(String resourceName, File resourceFile) {
		return resourceName.toLowerCase().endsWith( getAcceptExtension() );
	}

	//

	protected ChangesImpl newChanges() {
		return new ChangesImpl();
	}

	protected final ChangesImpl changes;

	@Override
	public ChangesImpl getChanges() {
		return changes;
	}

	@Override
	public void addReplacement() {
		getChanges().addReplacement();
	}

	@Override
	public void addReplacements(int additions) {
		getChanges().addReplacements(additions);
	}

	//

	@Override
	public boolean hasChanges() {
		return getChanges().hasChanges();
	}

	@Override
	public boolean hasResourceNameChange() {
		return getChanges().hasResourceNameChange();
	}

	@Override
	public boolean hasNonResourceNameChanges() {
		return getChanges().hasNonResourceNameChanges();
	}

	protected void clearChanges() {
		getChanges().clearChanges();
	}

	protected void setResourceNames(String inputResourceName, String outputResourceName) {
		ChangesImpl useChanges = getChanges();
		useChanges.setInputResourceName(inputResourceName);
		useChanges.setOutputResourceName(outputResourceName);
	}

	//

	public boolean useStreams() {
		return false;
	}

	/**
	 * Read bytes from an input stream.  Answer byte data and
	 * a count of bytes read.
	 *
	 * @param inputName The name of the input stream.
	 * @param inputStream A stream to be read.
	 * @param inputCount The count of bytes to read from the stream.
	 *     {@link Action#UNKNOWN_LENGTH} if the count of
	 *     input bytes is not known.
	 *
	 * @return Byte data from the read.
	 * 
	 * @throws TransformException Indicates a read failure.
	 */
	protected ByteData read(String inputName, InputStream inputStream, int inputCount) throws TransformException {
		byte[] readBytes = getInputBuffer();

		ByteData readData;
		try {
			readData = FileUtils.read(inputName, inputStream, readBytes, inputCount); // throws IOException
		} catch ( IOException e ) {
			throw new TransformException("Failed to read raw bytes [ " + inputName + " ] count [ " + inputCount + " ]", e);
		}

		setInputBuffer(readData.data);

		return readData;
	}

	/**
	 * Write data to an output stream.
	 * 
	 * Convert any exception thrown when attempting the write into a {@link TransformException}.
	 * 
	 * @param outputData Data to be written.
	 * @param outputStream Stream to which to write the data.
	 * 
	 * @throws TransformException Thrown in case of a write failure.
	 */
	protected void write(ByteData outputData, OutputStream outputStream) throws TransformException {
		try {
			outputStream.write(outputData.data, outputData.offset, outputData.length); // throws IOException

		} catch ( IOException e ) {
			throw new TransformException(
				"Failed to write [ " + outputData.name + " ]" +
				" at [ " + outputData.offset + " ]" +
				" count [ " + outputData.length + " ]",
				e);
		}
	}

	//

	@Override
	public InputStreamData apply(String inputName, InputStream inputStream)
		throws TransformException {

		return apply(inputName, inputStream, InputStreamData.UNKNOWN_LENGTH); // throws JakartaTransformException
	}

	@Override
	public InputStreamData apply(String inputName, InputStream inputStream, int inputCount)
		throws TransformException {

		String className = getClass().getSimpleName();
		String methodName = "apply";

		debug("[ {}.{} ]: Requested [ {} ] [ {} ]", className, methodName, inputName, inputCount);
		ByteData inputData = read(inputName, inputStream, inputCount); // throws JakartaTransformException
		debug("[ {}.{} ]: Obtained [ {} ] [ {} ] [ {} ]", className, methodName, inputName, inputData.length, inputData.data);

		ByteData outputData;
		try {
			outputData = apply(inputName, inputData.data, inputData.length);
			// throws JakartaTransformException
		} catch ( Throwable th ) {
			error("Transform failure [ {} ]", th, inputName);
			outputData = null;			
		}

		if ( outputData == null ) {
			debug("[ {}.{} ]: Null transform", className, methodName);
			outputData = inputData;
		} else {
			debug( "[ {}.{} ]: Active transform [ {} ] [ {} ] [ {} ]",
				   className, methodName,
				   outputData.name, outputData.length, outputData.data );
		}

		return new InputStreamData(outputData);
	}

	@Override
	public void apply(
		String inputName, InputStream inputStream, long inputCount,
		OutputStream outputStream) throws TransformException {

		int intInputCount = FileUtils.verifyArray(0, inputCount);

		String className = getClass().getSimpleName();
		String methodName = "apply";

		debug("[ {}.{} ]: Requested [ {} ] [ {} ]", className, methodName, inputName, inputCount);
		ByteData inputData = read(inputName, inputStream, intInputCount); // throws JakartaTransformException
		debug("[ {}.{} ]: Obtained [ {} ] [ {} ]", className, methodName, inputName, inputData.length);

		ByteData outputData;
		try {
			outputData = apply(inputName, inputData.data, inputData.length);
			// throws JakartaTransformException
		} catch ( Throwable th ) {
			error("Transform failure [ {} ]", th, inputName);
			outputData = null;
		}

		if ( outputData == null ) {
			debug("[ {}.{} ]: Null transform", className, methodName);
			outputData = inputData;
		} else {
			debug( "[ {}.{} ]: Active transform [ {} ] [ {} ]",
				   className, methodName, outputData.name, outputData.length );
		}

		write(outputData, outputStream); // throws JakartaTransformException		
	}

	@Override
	public abstract ByteData apply(String inputName, byte[] inputBytes, int inputLength) 
		throws TransformException;

    @Override
	public void apply(String inputName, File inputFile, File outputFile)
		throws TransformException {

		long inputLength = inputFile.length();
        debug("Input [ {} ] Length [ {} ]", inputName, inputLength);

		InputStream inputStream = openInputStream(inputFile);
		try {
			OutputStream outputStream = openOutputStream(outputFile);
			try {
				apply(inputName, inputStream, inputLength, outputStream);
			} finally {
				closeOutputStream(outputFile, outputStream);
			}
		} finally {
			closeInputStream(inputFile, inputStream);
		}
	}

	//

    protected InputStream openInputStream(File inputFile)
    	throws TransformException {

    	try {
    		return IO.stream(inputFile);
    	} catch ( IOException e ) {
        	throw new TransformException("Failed to open input [ " + inputFile.getAbsolutePath() + " ]", e);
        }
    }

    protected void closeInputStream(File inputFile, InputStream inputStream)
    	throws TransformException {

    	try {
    		inputStream.close();
    	} catch ( IOException e ) {
        	throw new TransformException("Failed to close input [ " + inputFile.getAbsolutePath() + " ]", e);
        }        		
    }

    private OutputStream openOutputStream(File outputFile)
    	throws TransformException {

    	try {
    		return IO.outputStream(outputFile);
    	} catch ( IOException e ) {
    		throw new TransformException("Failed to open output [ " + outputFile.getAbsolutePath() + " ]", e);
    	}
    }

    private void closeOutputStream(File outputFile, OutputStream outputStream)
    	throws TransformException {

    	try {
    		outputStream.close();
    	} catch ( IOException e ) {
        	throw new TransformException("Failed to close output [ " + outputFile.getAbsolutePath() + " ]", e);
        }
    }
}
