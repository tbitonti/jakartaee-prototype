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

package transformer.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.TransformProperties;
import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.impl.InputBufferImpl;
import org.eclipse.transformer.action.impl.LoggerImpl;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.ServiceLoaderConfigActionImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.eclipse.transformer.util.InputStreamData;
import org.junit.jupiter.api.Test;

public class TestTransformServiceConfig {
	public LoggerImpl createLogger(PrintStream printStream, boolean isTerse, boolean isVerbose) {
		return new LoggerImpl(printStream, isTerse, isVerbose);
	}

	public InputBufferImpl createBuffer() {
		return new InputBufferImpl();
	}

	public SelectionRuleImpl createSelectionRule(
		LoggerImpl logger,
		Set<String> useIncludes,
		Set<String> useExcludes) {

		return new SelectionRuleImpl( logger, useIncludes, useExcludes );
	}

	public SignatureRuleImpl createSignatureRule(
		LoggerImpl logger,
		Map<String, String> usePackageRenames,
		Map<String, String> usePackageVersions,
		Map<String, BundleData> bundleData,
		Map<String, String> directStrings) {

		return new SignatureRuleImpl( logger, usePackageRenames, usePackageVersions, bundleData, directStrings );
	}

	//

	public static final String JAVAX_OTHER_READER_SERVICE_PATH = "transformer/test/data/META-INF/services/javax.other.Reader";
	public static final String[] JAVAX_OTHER_READER_LINES = { "javax.other.ReaderImpl" };
	public static final String JAVAX_SAMPLE_READER_SERVICE_PATH = "transformer/test/data/META-INF/services/javax.sample.Reader";
	public static final String[] JAVAX_SAMPLE_READER_LINES = { "javax.sample.ReaderImpl" };	
	public static final String JAVAX_SAMPLE_WRITER_SERVICE_PATH = "transformer/test/data/META-INF/services/javax.sample.Writer";
	public static final String[] JAVAX_SAMPLE_WRITER_LINES = { "javax.sample.WriterImpl" };	
	
	public static final String JAKARTA_OTHER_READER_SERVICE_PATH = "transformer/test/data/META-INF/services/jakarta.other.Reader";
	public static final String[] JAKARTA_OTHER_READER_LINES = { "jakarta.other.ReaderImpl" };
	public static final String JAKARTA_SAMPLE_READER_SERVICE_PATH = "transformer/test/data/META-INF/services/jakarta.sample.Reader";
	public static final String[] JAKARTA_SAMPLE_READER_LINES = { "jakarta.sample.ReaderImpl" };	
	public static final String JAKARTA_SAMPLE_WRITER_SERVICE_PATH = "transformer/test/data/META-INF/services/jakarta.sample.Writer";
	public static final String[] JAKARTA_SAMPLE_WRITER_LINES = { "jakarta.sample.WriterImpl" };	

	public static final String JAVAX_SAMPLE = "javax.sample";
	public static final String JAKARTA_SAMPLE = "jakarta.sample";
	
	public static final String JAVAX_SERVLET = "javax.servlet";
	public static final String JAVAX_SERVLET_ANNOTATION = "javax.servlet.annotation";
	public static final String JAVAX_SERVLET_DESCRIPTOR = "javax.servlet.descriptor";
	public static final String JAVAX_SERVLET_HTTP = "javax.servlet.http";
	public static final String JAVAX_SERVLET_RESOURCES = "javax.servlet.resources";	

	public static final String JAKARTA_SERVLET_VERSION = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_ANNOTATION_VERSION  = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_DESCRIPTOR_VERSION  = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_HTTP_VERSION  = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_RESOURCES_VERSION  = "[2.6, 6.0)";

	protected Set<String> includes;
	
	public Set<String> getIncludes() {
		if ( includes == null ) {
			includes = new HashSet<String>();
			includes.add(JAVAX_SAMPLE_READER_SERVICE_PATH);
			includes.add(JAVAX_SAMPLE_WRITER_SERVICE_PATH);
		}

		return includes;
	}

	public Set<String> getExcludes() {
		return Collections.emptySet();
	}

	protected Map<String, String> packageRenames;

	public Map<String, String> getPackageRenames() {
		if ( packageRenames == null ) {
			packageRenames = new HashMap<String, String>();
			packageRenames.put(JAVAX_SAMPLE, JAKARTA_SAMPLE);
		}
		return packageRenames;
	}
	
	public ServiceLoaderConfigActionImpl jakartaServiceAction;
	public ServiceLoaderConfigActionImpl javaxServiceAction;

	public ServiceLoaderConfigActionImpl getJakartaServiceAction() {
		if ( jakartaServiceAction == null ) {
			LoggerImpl logger = createLogger( System.out, !LoggerImpl.IS_TERSE, LoggerImpl.IS_VERBOSE );

			jakartaServiceAction = new ServiceLoaderConfigActionImpl(
					logger,
					createBuffer(),
					createSelectionRule( logger, getIncludes(), getExcludes() ),
					createSignatureRule( logger, getPackageRenames(), null, null, null ) );
		}
		return jakartaServiceAction;
	}

	public ServiceLoaderConfigActionImpl getJavaxServiceAction() {
		if ( javaxServiceAction == null ) {
			Map<String, String> invertedRenames = TransformProperties.invert( getPackageRenames() );
			LoggerImpl logger = createLogger( System.out, !LoggerImpl.IS_TERSE, LoggerImpl.IS_VERBOSE );

			javaxServiceAction = new ServiceLoaderConfigActionImpl(
					logger,
					createBuffer(),
					createSelectionRule( logger, getIncludes(), getExcludes() ),
					createSignatureRule( logger, invertedRenames, null, null, null ) );
		}
		return javaxServiceAction;
	}

	@Test
	public void testJakartaTransform() throws IOException, TransformException {
		ServiceLoaderConfigActionImpl jakartaAction = getJakartaServiceAction();

		verifyTransform(
			jakartaAction,
			JAVAX_OTHER_READER_SERVICE_PATH,
			JAVAX_OTHER_READER_LINES); // Not transformed 
		verifyTransform(
			jakartaAction,
			JAVAX_SAMPLE_READER_SERVICE_PATH,
			JAKARTA_SAMPLE_READER_LINES); // Transformed
		verifyTransform(
			jakartaAction,
			JAVAX_SAMPLE_READER_SERVICE_PATH,
			JAKARTA_SAMPLE_READER_LINES); // Transformed 
	}

	@Test
	public void testJavaxTransform() throws IOException, TransformException {
		ServiceLoaderConfigActionImpl javaxAction = getJavaxServiceAction();

		verifyTransform(
			javaxAction,
			JAKARTA_OTHER_READER_SERVICE_PATH,
			JAKARTA_OTHER_READER_LINES); // Not transformed
		verifyTransform(
			javaxAction,
			JAKARTA_SAMPLE_READER_SERVICE_PATH,
			JAVAX_SAMPLE_READER_LINES); // Transformed
		verifyTransform(
			javaxAction,
			JAKARTA_SAMPLE_READER_SERVICE_PATH,
			JAVAX_SAMPLE_READER_LINES); // Transformed
	}

	protected void verifyTransform(
		ServiceLoaderConfigActionImpl action,
		String inputName,
		String[] expectedLines) throws IOException, TransformException {

		InputStream inputStream = TestUtils.getResourceStream(inputName);

		InputStreamData transformedData;
		try {
			transformedData = action.apply(inputName, inputStream);
		} finally {
			inputStream.close();
		}

		List<String> transformedLines = TestUtils.loadLines(transformedData.stream);
		TestUtils.filter(transformedLines);
		TestUtils.verify(inputName, expectedLines, transformedLines);
	}
	
}
