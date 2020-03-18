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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.assertj.core.api.Assertions;
import org.eclipse.transformer.util.ByteData;
import org.eclipse.transformer.util.FileUtils;
import org.junit.jupiter.api.Test;

public class TestURL {

	public static String classToResource(String className) {
		return className.replace('.',  '/') + ".class";
	}

	public static int firstDifference(byte[] data1, byte[] data2, int length) {
		for ( int offset = 0; offset< length; offset++ ) {
			if ( data1[offset] != data2[offset] ) {
				return offset;
			}
		}
		return -1;
	}

	public static class TransformURLStreamHandler extends URLStreamHandler {
		private final URL baseURL;

		public TransformURLStreamHandler(URL baseURL) {
			this.baseURL = baseURL;
		}

		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return new TransformURLConnection(baseURL); // throws IOException
		}
	}

	public static class TransformURLConnection extends URLConnection {
		public TransformURLConnection(URL baseURL) throws IOException {
			super(baseURL);

			this.baseConnection = this.getURL().openConnection(); // 'openConnection' throws IOException
		}

		//

		private final URLConnection baseConnection;

		public URLConnection getBaseConnection() {
			return baseConnection;
		}

		//

		@Override
		public void connect() throws IOException {
			getBaseConnection().connect(); // 'connect' throws IOException
		}

		public InputStream getInputStream() throws IOException {
			URLConnection useBaseConnection = getBaseConnection();
			String baseName = useBaseConnection.getURL().toString();
			InputStream baseStream = useBaseConnection.getInputStream(); // throws IOException

			ByteData inputData = FileUtils.read(baseName, baseStream);

			return new ByteArrayInputStream(inputData.data, 0, inputData.length);
		}
	}

	@Test
	public void testClassLoaderAPI() {
		String urlTestClassName = TestURL.class.getName();

		ClassLoader urlTestLoader = TestURL.class.getClassLoader();

		String urlTestResourceName = classToResource(urlTestClassName);
		URL urlTestURL = urlTestLoader.getResource(urlTestResourceName);
		String urlTestURLName = urlTestURL.toString();

		System.out.println("Class [ " + urlTestClassName + " ] as resource [ " + urlTestResourceName + " ]");
		System.out.println("In [ " + urlTestLoader + " ] as URL [ " + urlTestURLName + " ]");

		URL xformURL;
		try {
			xformURL = new URL(null, urlTestURLName, new TransformURLStreamHandler(urlTestURL) );
		} catch ( MalformedURLException e ) {
			Assertions.fail("Failed to wrap test URL: " + e);
			return;
		}

		System.out.println("As xForm URL [ " + xformURL + " ]");

		ByteData directData;
		try {
			directData = FileUtils.read(urlTestURLName, urlTestURL.openStream(), -1);
		} catch ( IOException e ) {
			Assertions.fail("Failed to read direct URL [ " + urlTestURLName + " ]: " + e);
			return;
		}

		ByteData indirectData;
		try {
			indirectData = FileUtils.read(urlTestURLName, xformURL.openStream(), -1);
		} catch ( IOException e ) {
			Assertions.fail("Failed to read indirect URL [ " + urlTestURLName + " ]: " + e);
			return;
		}

		if ( (directData != null) && (indirectData != null) ) {
			if ( directData.length != indirectData.length ) {
				Assertions.fail("Length change [ " + urlTestURLName + " ] Direct [ " + directData.length + " ] Indirect [ " + indirectData.length + " ]");
			} else {
				int firstChange = firstDifference(directData.data, indirectData.data, directData.length);
				if ( firstChange != -1 ) {
					Assertions.fail("Data change [ " + urlTestURLName + " ] at [ " + firstChange + " ]");
				} else {
					System.out.println("Direct matches indirect [ " + urlTestURLName + " ] to length [ " + directData.length + " ]");
				}
			}
		}
	}
}
