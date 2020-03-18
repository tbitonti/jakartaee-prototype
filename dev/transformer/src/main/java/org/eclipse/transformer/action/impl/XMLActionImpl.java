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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.util.ByteData;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLActionImpl extends ActionImpl {
	public static final String CLASS_NAME = XMLActionImpl.class.getSimpleName();

	public XMLActionImpl(
		LoggerImpl logger,
		InputBufferImpl buffer,
		SelectionRuleImpl selectionRule,
		SignatureRuleImpl signatureRule) {

		super(logger, buffer, selectionRule, signatureRule);
	}

	//

	public String getName() {
		return "XML Action";
	}

	@Override
	public ActionType getActionType() {
		return ActionType.XML;
	}

	@Override
	public String getAcceptExtension() {
		return ".xml";
	}

	//

	@Override
	public ByteData apply(String inputName, byte[] inputBytes, int inputCount) throws TransformException {
		clearChanges();

		setResourceNames(inputName, inputName);

		InputStream inputStream = new ByteArrayInputStream(inputBytes, 0, inputCount);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputCount);

		transform(inputName, inputStream, outputStream);

		if ( !hasNonResourceNameChanges() ) {
			return null;

		} else {
			byte[] outputBytes = outputStream.toByteArray();
			return new ByteData(inputName, outputBytes, 0, outputBytes.length);
		}
	}

	//

	private static final SAXParserFactory parserFactory;

	static {
		parserFactory = SAXParserFactory.newInstance();
		parserFactory.setNamespaceAware(true);
	}

	public static SAXParserFactory getParserFactory() {
		return parserFactory;
	}

	//

	private static Charset utf8;

	static {
		utf8 = Charset.forName("UTF-8");
	}

	public static Charset getUTF8() {
		return utf8;
	}

	//

	public void transform(String inputName, InputStream input, OutputStream output) throws TransformException {
		InputSource inputSource = new InputSource(input);

		XMLContentHandler handler = new XMLContentHandler(inputName, inputSource, output);

		SAXParser parser;
		try {
			parser = getParserFactory().newSAXParser();
			// 'newSAXParser' throws ParserConfigurationException, SAXException
		} catch ( Exception e ) {
			throw new TransformException("Failed to obtain parser for [ " + inputName + " ]", e);
		}

		try {
			parser.parse(input, handler); // throws SAXException, IOException
		} catch ( Exception e ) {
			throw new TransformException("Failed to parse [ " + inputName + " ]", e);
		}
	}

	//

	public class XMLContentHandler extends DefaultHandler {
		public XMLContentHandler(String inputName, InputSource inputSource, OutputStream outputStream) {
			this.inputName = inputName;
			this.charset = Charset.forName( inputSource.getEncoding() );
			this.publicId = inputSource.getPublicId();
			this.systemId = inputSource.getSystemId();

			this.outputStream = outputStream;

			this.lineBuilder = new StringBuilder();
		}

		//

		private final String inputName;
		
		private final String publicId;
		private final String systemId;
		private Charset charset;

		private final OutputStream outputStream;

		public String getInputName() {
			return inputName;
		}

		public Charset getCharset() {
			return charset;
		}

		public String getPublicId() {
			return publicId;
		}

		public String getSystemId() {
			return systemId;
		}

		//

		public OutputStream getOutputStream() {
			return outputStream;
		}

		public void write(String text) throws SAXException {
			write( text, getCharset() );
		}
		
		public void writeUTF8(String text) throws SAXException {
			write( text, getUTF8() );
		}

		public void write(String text, Charset useCharset) throws SAXException {
			try {
				outputStream.write( text.getBytes(useCharset) );
			} catch ( IOException e ) {
				throw new SAXException("Failed to write [ " + text + " ]", e);
			}
		}

		//

		private final StringBuilder lineBuilder;

		protected void appendLine() {
			lineBuilder.append('\n');
		}

		protected void append(char c) {
			lineBuilder.append(c);
		}

		protected void append(char[] buffer, int start, int length) {
			for ( int trav = start; trav < start + length; trav++ ) {
				lineBuilder.append( buffer[trav] );
			}
		}

		protected void appendLine(char c) {
			lineBuilder.append(c);
			lineBuilder.append('\n');
		}

		protected void append(String text) {
			lineBuilder.append(text);
		}
		
		protected void appendLine(String text) {
			lineBuilder.append(text);
			lineBuilder.append('\n');
		}

		protected void emit() throws SAXException {
			String nextLine = lineBuilder.toString();
			lineBuilder.setLength(0);

			write(nextLine); // throws SAXException
		}

		protected void emitLineUTF8(String text) throws SAXException {
			String nextLine = lineBuilder.toString();
			lineBuilder.setLength(0);

			writeUTF8(nextLine); // throws SAXException
		}

		//

		@Override
		public void startDocument() throws SAXException {
			String charsetName = getCharset().name();
			emitLineUTF8("<?xml version = \"1.0\" encoding = \""+ charsetName + "\"?>\n");
		}

//		@Override
//		public void endDocument() throws SAXException {
//			super.endDocument();
//		}
//
//		@Override
//		public void setDocumentLocator(Locator locator) {
//			super.setDocumentLocator(locator);
//		}

		@Override
		public void processingInstruction(String target, String data) throws SAXException {
			append("<?");
			append(target);
			if ( (data != null) && data.length() > 0) {
				append(' ');
				append(data);
			}
			append("?>");
		}

		//

//		@Override
//		public void startPrefixMapping(String prefix, String uri) throws SAXException {
//			super.startPrefixMapping(prefix, uri);
//		}
//
//		@Override
//		public void endPrefixMapping(String prefix) throws SAXException {
//			super.endPrefixMapping(prefix);
//		}

		//

		@Override
		public void startElement(String qualifiedName, String arg1, String arg2, Attributes attributes) throws SAXException {
		      append('<');
		      append(qualifiedName);

		      if ( attributes != null ) {
		         int numberAttributes = attributes.getLength();
		         for (int loopIndex = 0; loopIndex < numberAttributes; loopIndex++) {
		            append(' ');
		            append( attributes.getQName(loopIndex) );
		            append("=\"");
		            append( attributes.getValue(loopIndex) );
		            append('"');
		         }
		      }

		      appendLine('>');

		      emit();
		}

		@Override
		public void endElement(String qualifiedName, String arg1, String arg2) throws SAXException {
		      append("</");
		      append(qualifiedName);
		      append('>');
		}

		@Override
		public void characters(char[] chars, int start, int length) throws SAXException {
		      String initialText = new String(chars, start, length);
	
		      String finalText = XMLActionImpl.this.replaceEmbeddedPackages(initialText);
		      if ( finalText == null ) {
		    	  finalText = initialText;
		    	  XMLActionImpl.this.addReplacement();
		      }

		      append(finalText);
		}

		@Override
		public void ignorableWhitespace(char[] whitespace, int start, int length) throws SAXException {
			append(whitespace, start, length);
		}

//		@Override
//		public void skippedEntity(String name) throws SAXException {
//			super.skippedEntity(name);
//		}
	}
}
