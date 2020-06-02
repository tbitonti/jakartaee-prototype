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

import java.io.DataInput;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.TransformerState;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.SignatureRule;
import org.eclipse.transformer.action.SignatureRule.SignatureType;
import org.eclipse.transformer.util.ByteData;
import org.eclipse.transformer.util.FileUtils;
import org.slf4j.Logger;

import aQute.bnd.classfile.AnnotationDefaultAttribute;
import aQute.bnd.classfile.AnnotationInfo;
import aQute.bnd.classfile.AnnotationsAttribute;
import aQute.bnd.classfile.Attribute;
import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.CodeAttribute;
import aQute.bnd.classfile.CodeAttribute.ExceptionHandler;
import aQute.bnd.classfile.ConstantPool;
import aQute.bnd.classfile.ConstantPool.ClassInfo;
import aQute.bnd.classfile.ConstantPool.MethodTypeInfo;
import aQute.bnd.classfile.ConstantPool.NameAndTypeInfo;
import aQute.bnd.classfile.ConstantPool.StringInfo;
import aQute.bnd.classfile.ConstantValueAttribute;
import aQute.bnd.classfile.ElementValueInfo;
import aQute.bnd.classfile.ElementValueInfo.EnumConst;
import aQute.bnd.classfile.ElementValueInfo.ResultConst;
import aQute.bnd.classfile.EnclosingMethodAttribute;
import aQute.bnd.classfile.ExceptionsAttribute;
import aQute.bnd.classfile.FieldInfo;
import aQute.bnd.classfile.InnerClassesAttribute;
import aQute.bnd.classfile.InnerClassesAttribute.InnerClass;
import aQute.bnd.classfile.LocalVariableTableAttribute;
import aQute.bnd.classfile.LocalVariableTableAttribute.LocalVariable;
import aQute.bnd.classfile.LocalVariableTypeTableAttribute;
import aQute.bnd.classfile.LocalVariableTypeTableAttribute.LocalVariableType;
import aQute.bnd.classfile.MemberInfo;
import aQute.bnd.classfile.MethodInfo;
import aQute.bnd.classfile.ModuleAttribute;
import aQute.bnd.classfile.ModulePackagesAttribute;
import aQute.bnd.classfile.NestHostAttribute;
import aQute.bnd.classfile.NestMembersAttribute;
import aQute.bnd.classfile.ParameterAnnotationInfo;
import aQute.bnd.classfile.ParameterAnnotationsAttribute;
import aQute.bnd.classfile.RuntimeInvisibleAnnotationsAttribute;
import aQute.bnd.classfile.RuntimeInvisibleParameterAnnotationsAttribute;
import aQute.bnd.classfile.RuntimeInvisibleTypeAnnotationsAttribute;
import aQute.bnd.classfile.RuntimeVisibleAnnotationsAttribute;
import aQute.bnd.classfile.RuntimeVisibleParameterAnnotationsAttribute;
import aQute.bnd.classfile.RuntimeVisibleTypeAnnotationsAttribute;
import aQute.bnd.classfile.SignatureAttribute;
import aQute.bnd.classfile.StackMapTableAttribute;
import aQute.bnd.classfile.StackMapTableAttribute.AppendFrame;
import aQute.bnd.classfile.StackMapTableAttribute.FullFrame;
import aQute.bnd.classfile.StackMapTableAttribute.ObjectVariableInfo;
import aQute.bnd.classfile.StackMapTableAttribute.SameLocals1StackItemFrame;
import aQute.bnd.classfile.StackMapTableAttribute.SameLocals1StackItemFrameExtended;
import aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame;
import aQute.bnd.classfile.StackMapTableAttribute.VerificationTypeInfo;
import aQute.bnd.classfile.TypeAnnotationInfo;
import aQute.bnd.classfile.TypeAnnotationsAttribute;
import aQute.bnd.classfile.builder.ClassFileBuilder;
import aQute.bnd.classfile.builder.MutableConstantPool;
import aQute.lib.io.ByteBufferDataInput;
import aQute.lib.io.ByteBufferDataOutput;

/**
 * Transform class bytes.
 */
public class ClassActionImpl extends ActionImpl {

	public static String resourceNameToClassName(String resourceName) {
		String className = resourceName.substring( resourceName.length() - ".class".length() );
		className = className.replace('/',  '.');
		return className;
	}

	public static String classNameToResourceName(String className) {
		String resourceName = className.replace('.',  '/');
		resourceName = resourceName + ".class";
		return resourceName;
	}

	public static String classNameToBinaryTypeName(String className) {
		return className.replace('.',  '/');
	}

	// public static for unit testing

	/**
	 * Adjust an input path according to the changes made to the name of
	 * the class stored at that path.
	 *
	 * The input path is expected to match the input class name using usual java
	 * class resource placement.
	 *
	 * If the input path does does not match the input class name, the class is
	 * incorrectly placed.  Find an approximate location for the class based on
	 * whether the input path begins with "WEB-INF/classes/" or "META-INF/versions/".
	 * Otherwise, use the usual path for the output class name using java class
	 * resource placement.
	 *
	 * @param logger Logger for messaging
	 * @param inputPath The initial path to the class.
	 * @param inputClassName The initial class name.
	 * @param outputClassName The final class name.
	 *
	 * @return An output path for the class, placing the output class name in the same
	 *     relationship to the output path as the input class name has relative to the
	 *     input path.
	 */
	public static String relocateClass(
		Logger logger,
		String inputPath, String inputClassName, String outputClassName) {

		String directInputPath = classNameToResourceName(inputClassName);
		String directOutputPath = classNameToResourceName(outputClassName);

		// Expected cases:
		// The class was located at the usual relative location,
		// or the class was located under a sub-path.

		if ( directInputPath.equals(inputPath) ) {
			return directOutputPath;
		} else if ( inputPath.endsWith(directInputPath) ) {
			return inputPath.substring( 0, inputPath.length() - directInputPath.length() ) + directOutputPath;
		}

		// Unexpected cases:
		// The class was not properly named.  Do our best to place the class in the
		// same relative location. 

		String relocationCase;
		String outputPath;

		if ( inputPath.startsWith("WEB-INF/classes/") ) {
			relocationCase = "WEB-INF/classes";
			outputPath = "WEB-INF/classes/" + directOutputPath;
		} else if ( inputPath.startsWith("META-INF/versions/") ) {
			int nextSlash = inputPath.indexOf('/', "META-INF/versions/".length());
			if ( nextSlash == -1 ) {
				relocationCase = "META-INF/versions with no version number";
				outputPath = "META-INF/versions/" + directOutputPath;
			} else {
				relocationCase = "META-INF/versions";
				outputPath = inputPath.substring(0, nextSlash + 1) + directOutputPath;
			}
		} else {
			relocationCase = "Unknown location";
			outputPath = directOutputPath;
		}

		logger.error(
			"Approximate relocation of class; case {}:" +
			 " initial class name [ {} ]; final class name [ {} ];" +
			 " initial resource location [ {} ]; final resource location [ {} ].",
			 relocationCase,
			 inputClassName, outputClassName,
			 inputPath, outputPath);

		return outputPath;
	}

	//

	protected static final int DUMP_WIDTH = 16;

	protected void debugDump(byte[] bytes, int offset, int length) {
		if ( !getLogger().isDebugEnabled() ) {
			return;
		}

		StringBuilder outputBuilder = new StringBuilder();

		while ( length > 0 ) {
			int nextWidth = ( (length > DUMP_WIDTH) ? DUMP_WIDTH : length );
			String nextLine = debugDumpLine(outputBuilder, bytes, offset, nextWidth);
			debug(nextLine);

			offset += nextWidth;
			length -= nextWidth;
		}
	}

	private String debugDumpLine(StringBuilder builder, byte[] bytes, int offset, int width) {
		for ( int byteNo = 0; byteNo < width; byteNo++ ) {
			byte nextByte = bytes[ offset + byteNo ];
			builder.append( String.format("%02x ", nextByte) );
		}
		String line = builder.toString();
		builder.setLength(0);
		return line;
	}

	public ClassActionImpl(
		Logger logger, boolean isTerse, boolean isVerbose,
		InputBufferImpl buffer,
		SelectionRuleImpl selectionRule,
		SignatureRuleImpl signatureRule) {

		super(logger, isTerse, isVerbose, buffer, selectionRule, signatureRule);
	}

	//

	public String getName() {
		return "Class";
	}

	@Override
	public ActionType getActionType() {
		return ActionType.CLASS;
	}

	//

	@Override
	protected ClassChangesImpl newChanges() {
		return new ClassChangesImpl();
	}

	@Override
	public ClassChangesImpl getLastActiveChanges(TransformerState state) {
		return (ClassChangesImpl) super.getLastActiveChanges(state);
	}

	@Override
	public ClassChangesImpl getActiveChanges(TransformerState state) {
		return (ClassChangesImpl) super.getActiveChanges(state);
	}

	protected void setClassNames(
		TransformerState state,
		String inputClassName, String outputClassName) {

		ClassChangesImpl useChanges = getActiveChanges(state);
		useChanges.setInputClassName(inputClassName);
		useChanges.setOutputClassName(outputClassName);
	}

	protected void setSuperClassNames(
		TransformerState state,
		String inputSuperName, String outputSuperName) {

		ClassChangesImpl useChanges = getActiveChanges(state);
		useChanges.setInputSuperName(inputSuperName);
		useChanges.setOutputSuperName(outputSuperName);
	}	

	protected void addModifiedInterface(TransformerState state) {
		getActiveChanges(state).addModifiedInterface();
	}

	protected void addModifiedField(TransformerState state) {
		getActiveChanges(state).addModifiedField();
	}

	protected void addModifiedMethod(TransformerState state) {
		getActiveChanges(state).addModifiedMethod();
	}

	protected void addModifiedAttribute(TransformerState state) {
		getActiveChanges(state).addModifiedAttribute();
	}

	protected void setModifiedConstants(TransformerState state, int modifiedConstants) {
		getActiveChanges(state).setModifiedConstants(modifiedConstants);
	}

	//

	@Override
	public String getAcceptExtension() {
		return ".class";
	}

	//

	@Override
	public ByteData apply(
		TransformerState state,
		String inputName, byte[] inputBytes, int inputLength)

		throws TransformException {

		debug("Read [ {} ] Bytes [ {} ]", inputName, inputLength);
		debugDump(inputBytes, 0, inputLength);

		ClassFile inputClass;
		try {
			DataInput inputClassData = ByteBufferDataInput.wrap(inputBytes, 0, inputLength);
			inputClass = ClassFile.parseClassFile(inputClassData); // throws IOException
		} catch ( IOException e ) {
			error("Failed to parse raw class bytes [ {} ]", e, inputName);
			return null;
		}

		debug("Class [ {} ] as [ {} ] ", inputName, inputClass.this_class);
		debug("  Super [ {} ]", inputClass.super_class);
		if ( inputClass.interfaces != null ) {
		    debug("  Interfaces [ {} ]", inputClass.interfaces.length);
		    for ( String interfaceName : inputClass.interfaces ) {
		        debug("    [ {} ]", interfaceName);
		    }
		}
		if ( inputClass.fields != null ) {
		    debug("  Fields [ {} ]", inputClass.fields.length);
		    for ( FieldInfo field : inputClass.fields ) {
		        debug("    [ {} ] [ {} ]", field.name, field.descriptor);
		    }
		}
		if ( inputClass.methods != null ) {
		    debug("  Methods [ {} ]", inputClass.methods.length);
		    for ( MethodInfo method : inputClass.methods) {
		        debug("    [ {} ] [ {} ]", method.name, method.descriptor);
		    }
		}

		ClassFileBuilder classBuilder = new ClassFileBuilder(inputClass);

		// Transform the class declaration ...

		String inputClassName = classBuilder.this_class();
		String outputClassName = transformBinaryType(inputClassName);

		String outputName;
		if ( outputClassName != null ) {
			classBuilder.this_class(outputClassName);
			outputName = relocateClass( getLogger(), inputName, inputClassName, outputClassName );
			verbose("Class name [ {} ] -> [ {} ]", inputName, outputName);
		} else {
			outputClassName = inputClassName;
			outputName = inputName;
		}

		setClassNames(state, inputClassName, outputClassName);
		setResourceNames(state, inputName, outputName);

		debug("{}", classBuilder);

		String inputSuperName = classBuilder.super_class(); 
		if ( inputSuperName != null ) {
			String outputSuperName = transformBinaryType(inputSuperName);
			if ( outputSuperName != null ) {
				classBuilder.super_class(outputSuperName);
			} else {
				outputSuperName = inputSuperName;
			}

			setSuperClassNames(state, inputSuperName, outputSuperName);

			if ( !outputSuperName.equals("java/lang/Object") ) {
			    debug("  extends {}", outputSuperName);
			}
		}

		List<String> interfaces = classBuilder.interfaces();
		if ( !interfaces.isEmpty() ) {
			ListIterator<String> interfaceNames = interfaces.listIterator();
			while ( interfaceNames.hasNext() ) {
				String interfaceName = transformBinaryType( interfaceNames.next() );
				if ( interfaceName != null ) {
					interfaceNames.set(interfaceName);
					addModifiedInterface(state);
				}
			}

			debug("  implements {}", interfaces);
		}

		// Transform members ...

		ListIterator<FieldInfo> fields = classBuilder.fields().listIterator();
		if ( fields.hasNext() ) {
		    debug("  Fields:");
		}
		while ( fields.hasNext() ) {
			FieldInfo inputField = fields.next();
			FieldInfo outputField = transform( inputField, FieldInfo::new, SignatureType.FIELD );
			if ( outputField != null ) {
				fields.set(outputField);
				addModifiedField(state);
				debug("       {}    -> {}", inputField, outputField);

				verbose("Field {} -> {}", inputField, outputField);
			}
		}

		ListIterator<MethodInfo> methods = classBuilder.methods().listIterator();
		if ( methods.hasNext() ) {
		    debug("  Methods:");
		}
		while ( methods.hasNext() ) {
			MethodInfo inputMethod = methods.next();
			MethodInfo outputMethod = transform( inputMethod, MethodInfo::new, SignatureType.METHOD );
			if ( outputMethod != null ) {
				methods.set(outputMethod);
				addModifiedMethod(state);
				debug( "       {}    -> {}", inputMethod, outputMethod);

				verbose("Method {} -> {}", inputMethod, outputMethod);
			}
		}

		// verbose("  <<class>>");

		// Transform attributes ...

		ListIterator<Attribute> attributes = classBuilder.attributes().listIterator();
		if ( attributes.hasNext() ) {
		    debug("  Attributes:");
		}
		while ( attributes.hasNext() ) {
			Attribute inputAttribute = attributes.next();
			Attribute outputAttribute = transform(inputAttribute, SignatureType.CLASS);
			if ( outputAttribute != null ) {
				attributes.set(outputAttribute);
				addModifiedAttribute(state);
				debug("       {}    -> {}", inputAttribute, outputAttribute);
				verbose("Attribute {} -> {}", inputAttribute, outputAttribute);
			}
		}

		MutableConstantPool constants = classBuilder.constant_pool();
		debug("  Constant pool: {}", constants.size()); 

		int modifiedConstants = transform(constants);
		if ( modifiedConstants > 0 ) {
			setModifiedConstants(state, modifiedConstants);
		}

		if ( !hasNonResourceNameChanges(state) ) {
			verbose("  Class bytes: {} {}", inputName, inputLength);
			return null;
		}

		ClassFile outputClass = classBuilder.build();

		ByteBufferDataOutput outputClassData = new ByteBufferDataOutput( inputLength + FileUtils.PAGE_SIZE );
		try {
			outputClass.write(outputClassData); // throws IOException
		} catch ( IOException e ) {
			throw new TransformException("Failed to write transformed class bytes", e);
		}

		byte[] outputBytes = outputClassData.toByteArray();
		verbose("  Class size: {}: {} -> {}", inputName, inputLength, outputBytes.length);

		return new ByteData(outputName, outputBytes, 0, outputBytes.length);
	}

	//

	private <M extends MemberInfo> M transform(
		M member,
		MemberInfo.Constructor<M> constructor,
		SignatureType signatureType) {

		String inputDescriptor = member.descriptor;
		String outputDescriptor = transformDescriptor(inputDescriptor);
		if ( outputDescriptor != null ) {
		    debug("    {}       {}    -> {}", member.name, member.descriptor, outputDescriptor);
			verbose("Member {}.{} > {}", member.name, member.descriptor, outputDescriptor);
		}

		Attribute[] inputAttributes = member.attributes;
		Attribute[] outputAttributes = transform(inputAttributes, signatureType);

		if ( (outputDescriptor == null) && (outputAttributes == null) ) {
			return null;
		}

		return constructor.init(
			member.access, member.name,
			((outputDescriptor == null) ? inputDescriptor : outputDescriptor),
			((outputAttributes == null) ? inputAttributes : outputAttributes) );
	}

	private Attribute[] transform(Attribute[] inputAttributes, SignatureType signatureType) {
		Attribute[] outputAttributes = null;

		for ( int attributeNo = 0; attributeNo < inputAttributes.length; attributeNo++ ) {
			Attribute inputAttribute = inputAttributes[attributeNo];
			Attribute outputAttribute = transform(inputAttribute, signatureType);
			if ( outputAttribute != null ) {
				if ( outputAttributes == null ) {
					outputAttributes = inputAttributes.clone();
				}
				outputAttributes[attributeNo] = outputAttribute;

				debug("       {}    -> {}", inputAttribute, outputAttribute);
				verbose("Attribute {} -> {}", inputAttribute, outputAttribute);
			}
		}

		return outputAttributes;
	}

	private Attribute transform(Attribute attr, SignatureType signatureType) {
		switch ( attr.name() ) {
			case SignatureAttribute.NAME: {
				SignatureAttribute inputAttribute = (SignatureAttribute) attr;
				String outputSignature = transform(inputAttribute.signature, signatureType);
				return ( (outputSignature == null) ? null : new SignatureAttribute(outputSignature) );
			}

			case ExceptionsAttribute.NAME: {
				ExceptionsAttribute inputAttribute = (ExceptionsAttribute) attr;
				String[] inputExceptions = inputAttribute.exceptions;
				String[] outputExceptions = null;
				for ( int exNo = 0; exNo < inputExceptions.length; exNo++ ) {
					String exception = transformBinaryType( inputExceptions[exNo] );
					if ( exception != null) {
						if ( outputExceptions == null ) {
							outputExceptions = inputExceptions.clone();
						}
						outputExceptions[exNo] = exception;
					}
				}

				return ( (outputExceptions == null) ? null : new ExceptionsAttribute(outputExceptions) );
			}

			case CodeAttribute.NAME: {
				CodeAttribute attribute = (CodeAttribute) attr;

				ExceptionHandler[] inputHandlers = attribute.exception_table;
				ExceptionHandler[] outputHandlers = null;

				for ( int handlerNo = 0; handlerNo < inputHandlers.length; handlerNo++ ) {
					ExceptionHandler inputHandler = inputHandlers[handlerNo];
					String inputCatchType = inputHandler.catch_type;
					if ( inputCatchType != null ) {
						String outputCatchType = transformBinaryType(inputCatchType);
						if ( outputCatchType != null ) {
							if ( outputHandlers == null ) {
								outputHandlers = inputHandlers.clone();
							}
							outputHandlers[handlerNo] = new ExceptionHandler(
								inputHandler.start_pc, inputHandler.end_pc, inputHandler.handler_pc,
								outputCatchType);
						}
					}
				}

				// TODO Maybe intercept Class.forName/etc calls at
				// runtime to rename types

				Attribute[] inputAttributes = attribute.attributes;
				Attribute[] outputAttributes = transform(inputAttributes, SignatureType.METHOD);

				if ( (outputHandlers == null) && (outputAttributes == null) ) {
					return null;
				} else {
					return new CodeAttribute(
						attribute.max_stack, attribute.max_locals, attribute.code,
						( (outputHandlers == null) ? inputHandlers : outputHandlers ),
						( (outputAttributes == null) ? inputAttributes : outputAttributes) );
				}
			}

			case EnclosingMethodAttribute.NAME: {
				EnclosingMethodAttribute attribute = (EnclosingMethodAttribute) attr;
				String inputDescriptor = attribute.method_descriptor;
				if ( inputDescriptor == null ) {
					return null;
				}
				String outputDescriptor = transformDescriptor(inputDescriptor);
				if ( outputDescriptor == null ) {
					return null;
				} else {
					return new EnclosingMethodAttribute(
						attribute.class_name, attribute.method_name, outputDescriptor);
				}
			}

			case StackMapTableAttribute.NAME: {
				StackMapTableAttribute inputAttribute = (StackMapTableAttribute) attr;

				StackMapFrame[] inputFrames = inputAttribute.entries;
				StackMapFrame[] outputFrames = null;

				for ( int frameNo = 0; frameNo < inputFrames.length; frameNo++ ) {
					StackMapFrame inputFrame = inputFrames[frameNo];
					switch (inputFrame.type()) {
						case StackMapFrame.SAME_LOCALS_1_STACK_ITEM: {
							SameLocals1StackItemFrame frame = (SameLocals1StackItemFrame) inputFrame;
							VerificationTypeInfo stack = transform(frame.stack);
							if (stack != null) {
								if ( outputFrames == null ) {
									outputFrames = inputFrames.clone();
								}
								outputFrames[frameNo] = new SameLocals1StackItemFrame(frame.tag, stack);
							}
							break;
						}
						case StackMapFrame.SAME_LOCALS_1_STACK_ITEM_EXTENDED: {
							SameLocals1StackItemFrameExtended frame = (SameLocals1StackItemFrameExtended) inputFrame;
							VerificationTypeInfo stack = transform(frame.stack);
							if (stack != null) {
								if ( outputFrames == null ) {
									outputFrames = inputFrames.clone();
								}
								outputFrames[frameNo] = new SameLocals1StackItemFrameExtended(frame.tag, frame.delta, stack);
							}
							break;
						}
						case StackMapFrame.APPEND: {
							AppendFrame frame = (AppendFrame) inputFrame;
							VerificationTypeInfo[] locals = transform(frame.locals);
							if (locals != null) {
								if ( outputFrames == null ) {
									outputFrames = inputFrames.clone();
								}
								outputFrames[frameNo] = new AppendFrame(frame.tag, frame.delta, locals);
							}
							break;
						}
						case StackMapFrame.FULL_FRAME: {
							FullFrame frame = (FullFrame) inputFrame;
							VerificationTypeInfo[] locals = transform(frame.locals);
							VerificationTypeInfo[] stack = transform(frame.stack);
							if ((locals != null) || (stack != null)) {
								if ( outputFrames == null ) {
									outputFrames = inputFrames.clone();
								}
								outputFrames[frameNo] = new FullFrame(
									frame.tag, frame.delta,
									( (locals == null) ? frame.locals : locals ),
									( (stack == null) ? frame.stack : stack ) );
							}
							break;
						}
						default:
							break;
					}
				}
				if ( outputFrames == null ) {
					return null;
				} else {
					return new StackMapTableAttribute(outputFrames);
				}
			}

			case InnerClassesAttribute.NAME: {
				InnerClassesAttribute inputAttribute = (InnerClassesAttribute) attr;

				InnerClass[] inputClasses = inputAttribute.classes;
				InnerClass[] outputClasses = null;

				for ( int classNo = 0; classNo < inputClasses.length; classNo++ ) {
					InnerClass inputClass = inputClasses[classNo];

					String inputInnerClass = inputClass.inner_class;
					String outputInnerClass =
						( (inputInnerClass == null) ? null : transformBinaryType(inputInnerClass) );

					String inputOuterClass = inputClass.outer_class;
					String outputOuterClass =
						( (inputOuterClass == null) ? null : transformBinaryType(inputOuterClass) );

					if ( (outputInnerClass != null) || (outputOuterClass != null) ) {
						if ( outputClasses == null ) {
							outputClasses = inputClasses.clone();
						}
						outputClasses[classNo] = new InnerClass(
							( (outputInnerClass == null) ? inputInnerClass : outputInnerClass ),
							( (outputOuterClass == null) ? inputOuterClass : outputOuterClass ),
							inputClass.inner_name, inputClass.inner_access);
					}
				}

				if ( outputClasses == null ) {
					return null;
				} else {
					return new InnerClassesAttribute(outputClasses);
				}
			}

			case LocalVariableTableAttribute.NAME: {
				LocalVariableTableAttribute inputAttribute = (LocalVariableTableAttribute) attr;

				LocalVariable[] inputVariables = inputAttribute.local_variable_table;
				LocalVariable[] outputVariables = null;

				for ( int varNo = 0; varNo < inputVariables.length; varNo++ ) {
					LocalVariable inputVariable = inputVariables[varNo];
					String outputDescriptor = transformDescriptor(inputVariable.descriptor);
					if ( outputDescriptor != null ) {
						if ( outputVariables == null ) {
							outputVariables = inputVariables.clone();
						}
						outputVariables[varNo] = new LocalVariable(
							inputVariable.start_pc, inputVariable.length,
							inputVariable.name, outputDescriptor,
							inputVariable.index);
					}
				}

				if ( outputVariables == null ) {
					return null;
				} else {
					return new LocalVariableTableAttribute(outputVariables);
				}
			}

			case LocalVariableTypeTableAttribute.NAME: {
				LocalVariableTypeTableAttribute inputAttribute =
					(LocalVariableTypeTableAttribute) attr;

				LocalVariableType[] inputTypes = inputAttribute.local_variable_type_table;
				LocalVariableType[] outputTypes = null;

				for ( int varNo = 0; varNo < inputTypes.length; varNo++ ) {
					LocalVariableType inputType = inputTypes[varNo];
					String outputSignature = transform(inputType.signature, SignatureType.FIELD);

					if ( outputSignature != null ) {
						if ( outputTypes == null ) {
							outputTypes = inputTypes.clone();
						}
						outputTypes[varNo] = new LocalVariableType(
							inputType.start_pc, inputType.length,
							inputType.name, outputSignature,
							inputType.index);
					}
				}

				if ( outputTypes == null ) {
					return null;
				} else {
					return new LocalVariableTypeTableAttribute(outputTypes);
				}
			}

			case RuntimeVisibleAnnotationsAttribute.NAME: {
				RuntimeVisibleAnnotationsAttribute inputAttribute =
					(RuntimeVisibleAnnotationsAttribute) attr;
				RuntimeVisibleAnnotationsAttribute outputAttribute =
					transform(inputAttribute, RuntimeVisibleAnnotationsAttribute::new);
				return outputAttribute;
			}

			case RuntimeInvisibleAnnotationsAttribute.NAME: {
				RuntimeInvisibleAnnotationsAttribute inputAttribute =
					(RuntimeInvisibleAnnotationsAttribute) attr;
				RuntimeInvisibleAnnotationsAttribute outputAttribute =
					transform(inputAttribute, RuntimeInvisibleAnnotationsAttribute::new);
				return outputAttribute;
			}

			case RuntimeVisibleParameterAnnotationsAttribute.NAME: {
				RuntimeVisibleParameterAnnotationsAttribute inputAttribute =
					(RuntimeVisibleParameterAnnotationsAttribute) attr;
				RuntimeVisibleParameterAnnotationsAttribute outputAttribute =
					transform( inputAttribute, RuntimeVisibleParameterAnnotationsAttribute::new );
				return outputAttribute;
			}

			case RuntimeInvisibleParameterAnnotationsAttribute.NAME: {
				RuntimeInvisibleParameterAnnotationsAttribute inputAttribute =
					(RuntimeInvisibleParameterAnnotationsAttribute) attr;
				RuntimeInvisibleParameterAnnotationsAttribute outputAttribute =
					transform( inputAttribute, RuntimeInvisibleParameterAnnotationsAttribute::new);
				return outputAttribute;
			}

			case RuntimeVisibleTypeAnnotationsAttribute.NAME: {
				RuntimeVisibleTypeAnnotationsAttribute inputAttribute =
					(RuntimeVisibleTypeAnnotationsAttribute) attr;
				RuntimeVisibleTypeAnnotationsAttribute outputAttribute =
					transform( inputAttribute, RuntimeVisibleTypeAnnotationsAttribute::new );
				return outputAttribute;
			}

			case RuntimeInvisibleTypeAnnotationsAttribute.NAME: {
				RuntimeInvisibleTypeAnnotationsAttribute inputAttribute =
					(RuntimeInvisibleTypeAnnotationsAttribute) attr;
				RuntimeInvisibleTypeAnnotationsAttribute outputAttribute =
					transform(inputAttribute, RuntimeInvisibleTypeAnnotationsAttribute::new);
				return outputAttribute;
			}

			case AnnotationDefaultAttribute.NAME: {
				AnnotationDefaultAttribute inputAttribute =
						(AnnotationDefaultAttribute) attr;
					Object outputValue = transformElementValue(inputAttribute.value);
					return ( (outputValue == null) ? null : new AnnotationDefaultAttribute(outputValue) );
			}

			case ModuleAttribute.NAME:
			case ModulePackagesAttribute.NAME:
				// TODO Handle module metadata in case some
				// used by some Java EE 8/Jakarta EE 8 artifacts.
				break;

			case NestHostAttribute.NAME:
			case NestMembersAttribute.NAME: {
				// TODO These Java SE 9+ attributes should not be used
				// by Java EE 8/Jakarta EE 8 artifacts, so
				// we ignore them for now.
				break;
			}

			case ConstantValueAttribute.NAME: {
				ConstantValueAttribute inputAttribute = (ConstantValueAttribute) attr;
				Object inputValue = inputAttribute.value;
				Object outputValue = transformConstantValue(inputValue);
				return ( (outputValue == null) ? null : new ConstantValueAttribute(outputValue) );
			}

			default:
				break;
		}

		return null;
	}
	    
    private Object transformConstantValue(Object inputValue) {
        if ( inputValue instanceof String ) {
            String inputString = (String) inputValue;
            String transformCase = "constant";  // dotted package format
            String outputString = transformConstantAsDescriptor(inputString, SignatureRule.ALLOW_SIMPLE_SUBSTITUTION);
            if ( outputString == null ) {
                transformCase = "resource";  // url format (slashes)
                outputString = transformConstantAsBinaryType(inputString, SignatureRule.ALLOW_SIMPLE_SUBSTITUTION);
                if ( outputString == null ) {
                    transformCase = "direct";
                    outputString = transformDirectString(inputString);
                }
            }
            if ( outputString == null ) {
                debug("    String ConstantValue: {} (unchanged)", inputValue);
            } else {
                debug("    String ConstantValue: {}                       -> {} ({})", inputValue, outputString, transformCase);
                verbose("String ConstantValue: {} -> {} ({})", inputValue, outputString, transformCase);                
            }
            return outputString;
        } else {
            debug("    Non-String ConstantValue: {} (unchanged)", inputValue);
            return null;
        }
    }

	private <A extends AnnotationsAttribute> A transform(
		A inputAttribute,
		AnnotationsAttribute.Constructor<A> constructor) {

		AnnotationInfo[] outputAnnotations = transform(inputAttribute.annotations);

		return ( (outputAnnotations == null) ? null : constructor.init(outputAnnotations) );
	}

	private AnnotationInfo[] transform(AnnotationInfo[] inputAnnotations) {
		AnnotationInfo[] outputAnnotations = null;

		for ( int annoNo = 0; annoNo < inputAnnotations.length; annoNo++ ) {
			AnnotationInfo inputAnnotation = inputAnnotations[annoNo];
			AnnotationInfo outputAnnotation = transform(inputAnnotation, AnnotationInfo::new );
			if ( outputAnnotation != null ) {
				if ( outputAnnotations == null ) {
					outputAnnotations = inputAnnotations.clone();
				}
				outputAnnotations[annoNo] = outputAnnotation;
			}
		}
		
		return outputAnnotations;
	}

	private <A extends ParameterAnnotationsAttribute> A transform(
		A attribute,
		ParameterAnnotationsAttribute.Constructor<A> constructor) {

		ParameterAnnotationInfo[] outputParmAnnotations =
			transform(attribute.parameter_annotations);

		if ( outputParmAnnotations == null ) {
			return null;
		} else {
			return constructor.init(outputParmAnnotations);
		}
	}

	private ParameterAnnotationInfo[] transform(ParameterAnnotationInfo[] inputParmAnnotations) {
		ParameterAnnotationInfo[] outputParmAnnotations = null;

		for ( int parmNo = 0; parmNo < inputParmAnnotations.length; parmNo++ ) {
			ParameterAnnotationInfo inputParmAnnotation = inputParmAnnotations[parmNo];
			AnnotationInfo[] outputAnnotations = transform(inputParmAnnotation.annotations);
			if ( outputAnnotations != null ) {
				if ( outputParmAnnotations == null ) {
					outputParmAnnotations = inputParmAnnotations.clone();
				}
				outputParmAnnotations[parmNo] = new ParameterAnnotationInfo(
					inputParmAnnotation.parameter, outputAnnotations);
			}
		}

		return outputParmAnnotations;
	}

	private <A extends TypeAnnotationsAttribute> A transform(
		A inputAttribute,
		TypeAnnotationsAttribute.Constructor<A> constructor) {

		TypeAnnotationInfo[] outputAnnotations = transform(inputAttribute.type_annotations);

		if ( outputAnnotations == null ) {
			return null;
		} else {
			return constructor.init(outputAnnotations);
		}
	}

	private TypeAnnotationInfo[] transform(TypeAnnotationInfo[] inputAnnotations) {
		TypeAnnotationInfo[] outputAnnotations = null;

		for ( int annoNo = 0; annoNo < inputAnnotations.length; annoNo++ ) {
			TypeAnnotationInfo inputAnnotation = inputAnnotations[annoNo];
			TypeAnnotationInfo outputAnnotation = transform(
				inputAnnotation,
				(type, values) -> new TypeAnnotationInfo(
					inputAnnotation.target_type, inputAnnotation.target_info,
					inputAnnotation.target_index, inputAnnotation.type_path,
					type, values) );

			if ( outputAnnotation != null ) {
				if ( outputAnnotations == null ) {
					outputAnnotations = inputAnnotations.clone();
				}
				outputAnnotations[annoNo] = outputAnnotation;
			}
		}

		return outputAnnotations;
	}

	private <A extends AnnotationInfo> A transform(
		A inputAnnotation,
		AnnotationInfo.Constructor<A> constructor) {

		String inputType = inputAnnotation.type;
		String outputType = transformDescriptor(inputType);

		ElementValueInfo[] inputValues = inputAnnotation.values;
		ElementValueInfo[] outputValues = transform(inputValues);

		if ( (outputType == null) && (outputValues == null) ) {
			return null;
		} else {
			return constructor.init(
				( (outputType == null) ? inputType : outputType ),
				( (outputValues == null) ? inputValues : outputValues ) );
		}
	}

	private ElementValueInfo[] transform(ElementValueInfo[] inputElementValues) {
		ElementValueInfo[] outputElementValues = null;

		for ( int valueNo = 0; valueNo < inputElementValues.length; valueNo++ ) {
			ElementValueInfo inputElementValue = inputElementValues[valueNo];
			Object outputValue = transformElementValue(inputElementValue.value);

			if ( outputValue != null ) {
				if ( outputElementValues == null ) {
					outputElementValues = inputElementValues.clone();
				}
				outputElementValues[valueNo] =
					new ElementValueInfo(inputElementValue.name, outputValue);
			}
		}

		return outputElementValues;
	}

	private Object transformElementValue(Object inputValue) {
		if ( inputValue instanceof EnumConst ) {
			EnumConst enumValue = (EnumConst) inputValue;
			String inputType= enumValue.type;
			String outputType = transformDescriptor(inputType);
			if ( outputType == null ) {
				return null;
			} else {
				return new EnumConst(outputType, enumValue.name);
			}

		} else if ( inputValue instanceof ResultConst ) {
			ResultConst resultValue = (ResultConst) inputValue;
			String inputDescriptor = resultValue.descriptor;
			String outputDescriptor = transformDescriptor(inputDescriptor);
			if ( outputDescriptor == null ) {
				return null;
			} else {
				return new ResultConst(outputDescriptor);
			}

		} else if ( inputValue instanceof AnnotationInfo ) {
			AnnotationInfo annotationValue = (AnnotationInfo) inputValue;
			return transform(annotationValue, AnnotationInfo::new);

		} else if (inputValue instanceof Object[]) {
			Object[] inputElementValues = ((Object[]) inputValue);
			Object[] outputElementValues = null;

			for ( int valueNo = 0; valueNo < inputElementValues.length; valueNo++ ) {
				Object outputElementValue = transformElementValue(inputElementValues[valueNo]);
				if ( outputElementValue != null ) {
					if ( outputElementValues == null ) {
						outputElementValues = inputElementValues.clone();
					}
					outputElementValues[valueNo] = outputElementValue;
				}
			}

			return outputElementValues;

		} else {
			return null;
		}
	}

	//
	
	private VerificationTypeInfo[] transform(VerificationTypeInfo[] inputVtis) {
		VerificationTypeInfo[] outputVtis = null;

		for ( int vtiNo = 0; vtiNo < inputVtis.length; vtiNo++ ) {
			VerificationTypeInfo inputVti =  inputVtis[vtiNo];
			VerificationTypeInfo outputVti = transform(inputVti);
			if ( outputVti != null ) {
				if ( outputVtis == null ) {
					outputVtis = inputVtis.clone();
				}
				outputVtis[vtiNo] = outputVti;
			}
		}

		return outputVtis;
	}

	private VerificationTypeInfo transform(VerificationTypeInfo vti) {
		if ( !(vti instanceof ObjectVariableInfo) ) {
			return null;
		}
		ObjectVariableInfo inputOvi = (ObjectVariableInfo) vti;

		String inputType = inputOvi.type;
		if ( inputType == null ) {
			return null;
		}

		String outputType = transformBinaryType(inputType);
		if ( outputType == null ) {
			return null;
		} else {
			return new ObjectVariableInfo(inputOvi.tag, outputType);
		}
	}

	//

	private int transform(MutableConstantPool constants) throws TransformException {
		int modifiedConstants = 0;

		int numConstants = constants.size();
		for ( int constantNo = 1; constantNo < numConstants; constantNo++ ) {
		    debug( String.format(
		    	"Constant [ %3s ] [ %16s ] [ %s ]", 
		        constantNo, constants.tag(constantNo), constants.entry(constantNo) ) );

			switch ( constants.tag(constantNo) ) {
				case ConstantPool.CONSTANT_Class: {
					ClassInfo info = constants.entry(constantNo);
					String inputClassName = constants.entry(info.class_index);
					String outputClassName = transformBinaryType(inputClassName);
					if ( outputClassName != null ) {
						constants.entry( constantNo, new ClassInfo(constants.utf8Info(outputClassName)) );
						modifiedConstants++;
						debug("    Class: {}        -> {}", inputClassName, outputClassName);
						verbose("Class Reference: {} -> {}", inputClassName, outputClassName);
					} else {
					    debug("Skip class {} (unchanged)", inputClassName);
					}
					break;
				}

				case ConstantPool.CONSTANT_NameAndType: {
					NameAndTypeInfo info = constants.entry(constantNo);
					String inputDescriptor = constants.utf8(info.descriptor_index);
					String outputDescriptor = transformDescriptor(inputDescriptor);
					if ( outputDescriptor != null ) {
						constants.entry(constantNo,
							new NameAndTypeInfo( info.name_index, constants.utf8Info(outputDescriptor)) );
						modifiedConstants++;
						debug("    NameAndType: {}              -> {}", inputDescriptor, outputDescriptor);
						verbose("NameAndType: {} -> {}", inputDescriptor, outputDescriptor);						
					} else {
					    debug("Skip name-and-type {} (unchanged)", inputDescriptor);
					}
					break;
				}

				case ConstantPool.CONSTANT_MethodType: {
					MethodTypeInfo info = constants.entry(constantNo);
					String inputDescriptor = constants.utf8(info.descriptor_index);
					String outputDescriptor = transformDescriptor(inputDescriptor);
					if ( outputDescriptor != null ) {
						constants.entry( constantNo, new MethodTypeInfo(constants.utf8Info(outputDescriptor)) );
						modifiedConstants++;
						debug("    MethodType: {} -> {}", inputDescriptor, outputDescriptor);
						verbose("MethodType: {} -> {}", inputDescriptor, outputDescriptor);
					} else {
					    debug("Skip method-type {} (unchanged)", inputDescriptor);
					}
					break;
				}

				case ConstantPool.CONSTANT_Utf8:
					String inputUtf8 = constants.entry(constantNo);

					String transformCase;
					String outputUtf8;

					if ( (inputUtf8 == null) || inputUtf8.isEmpty()  ) {
						transformCase = null; // Unused
						outputUtf8 = null;
					} else {
						transformCase = "constant";  // dotted package format
						outputUtf8 = transformConstantAsDescriptor(inputUtf8, SignatureRule.ALLOW_SIMPLE_SUBSTITUTION);
						if ( outputUtf8 == null ) {
							transformCase = "resource";  // url format (slashes)
							outputUtf8 = transformConstantAsBinaryType(inputUtf8, SignatureRule.ALLOW_SIMPLE_SUBSTITUTION);
	                        if ( outputUtf8 == null ) {
	                        	transformCase = "Direct";
	                        	outputUtf8 = transformDirectString(inputUtf8);
	                        }
						}
					}

					if ( outputUtf8 != null ) {
						constants.entry(constantNo, outputUtf8);
						modifiedConstants++;
						debug("    UTF8: {} -> {} ({})", inputUtf8, outputUtf8, transformCase);						
						verbose("UTF8: {} -> {} ({})", inputUtf8, outputUtf8, transformCase);
					} else {
					    debug("Skip UTF8 {} (unchanged)", inputUtf8);
					}

					break;

				case ConstantPool.CONSTANT_String: {
					StringInfo stringInfo = constants.entry(constantNo);
					String inputString = constants.utf8(stringInfo.string_index);
					transformCase = "constant";   // dotted package format
					String outputString = transformConstantAsDescriptor(inputString, SignatureRule.ALLOW_SIMPLE_SUBSTITUTION);
                    if ( outputString == null ) {
                        transformCase = "String";   // url format (slashes)
                        outputString = transformConstantAsBinaryType(inputString, SignatureRule.ALLOW_SIMPLE_SUBSTITUTION);
                        if ( outputString == null ) {
                        	transformCase = "Direct";
                        	outputString = transformDirectString(inputString);
                        }
                    }
					if ( outputString != null ) {
						constants.entry(constantNo, new StringInfo( constants.utf8Info(outputString) ) );
						modifiedConstants++;
						debug("    String: {} -> {}", inputString, outputString);
						verbose("String: {} -> {}", inputString, outputString);
					} else {
					    debug("Skip string {} (unchanged)", inputString);
					}
					break;
				}

				case ConstantPool.CONSTANT_Fieldref:
				case ConstantPool.CONSTANT_Methodref:
				case ConstantPool.CONSTANT_InterfaceMethodref:
				case ConstantPool.CONSTANT_MethodHandle:
				case ConstantPool.CONSTANT_Dynamic:
				case ConstantPool.CONSTANT_InvokeDynamic:
				case ConstantPool.CONSTANT_Module:
				case ConstantPool.CONSTANT_Package:
				case ConstantPool.CONSTANT_Integer:
				case ConstantPool.CONSTANT_Float:
					debug("Skip other (ignored)");
					break;

				case ConstantPool.CONSTANT_Long:
				case ConstantPool.CONSTANT_Double:
					debug("Skip floating point value +1 (ignored)");
					// For some insane optimization reason, the Long(5) and Double(6)
					// entries take two slots in the constant pool.  See 4.4.5
					constantNo++;
					break;

				default:
					throw new TransformException(
						"Unrecognized constant pool entry [ " + constantNo + " ]:" +
						" [ " + constants.entry(constantNo) + " ]");
			}
		}

		return modifiedConstants;
	}
}
