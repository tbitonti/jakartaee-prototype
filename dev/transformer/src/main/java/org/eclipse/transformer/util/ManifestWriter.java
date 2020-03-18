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

package org.eclipse.transformer.util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

public class ManifestWriter {

	public static void main(String[] parms) {
		if ( parms.length != 2 ) {
			System.out.println(ManifestWriter.class.getName() + " inputPath outputPath");
			return;
		}

		String inputPath = parms[0];
		String outputPath = parms[1];

		System.out.println("Input path [ " + inputPath + " ]");
		System.out.println("Output path [ " + outputPath + " ]");

		File inputFile = new File(inputPath);
		String inputAbsPath = inputFile.getAbsolutePath();
		if ( !inputFile.exists() ) {
			System.out.println("Input [ " + inputAbsPath + " ] does not exist");
			return;
		} else {
			System.out.println("Input full path [ " + inputAbsPath + " ]");
		}

		File outputFile = new File(outputPath);
		String outputAbsPath = outputFile.getAbsolutePath();
		if ( outputFile.exists() ) {
			System.out.println("Input [ " + outputAbsPath + " ] already exists");
			return;
		} else {
			System.out.println("Input full path [ " + outputAbsPath + " ]");
		}

		System.out.println("Reading manifest ...");

		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(inputFile);
		} catch ( IOException e ) {
			System.out.println("Failed to open [ " + inputAbsPath + " ]: " + e.getMessage());
			e.printStackTrace();
			
			return;
		}
		
		Manifest manifest = new Manifest();
		try {
			try {
				manifest.read(inputStream);
			} catch ( IOException e2 ) {
				System.out.println("Failed to read as manifest [ " + inputAbsPath + " ]: " + e2.getMessage());
				e2.printStackTrace();
				return;
			}
		} finally {
			try {
				inputStream.close();
			} catch ( IOException e1 ) {
				System.out.println("Failed to close [ " + inputAbsPath + " ]: " + e1.getMessage());
				e1.printStackTrace();
			}
		}

		System.out.println("Reading manifest ... done");
		
		System.out.println("Writing manifest ...");

		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(outputFile);
		} catch ( IOException e ) {
			System.out.println("Failed to open [ " + outputAbsPath + " ]: " + e.getMessage());
			e.printStackTrace();
			return;
		}

		try {
			try {
				write(manifest, outputStream);
			} catch ( IOException e2 ) {
				System.out.println("Failed to write as manifest [ " + outputAbsPath + " ]: " + e2.getMessage());
				e2.printStackTrace();
				return;
			}
		} finally {
			try {
				outputStream.close();
			} catch ( IOException e1 ) {
				System.out.println("Failed to close [ " + outputAbsPath + " ]: " + e1.getMessage());
				e1.printStackTrace();
			}
		}

		System.out.println("Writing manifest ... done");
		
		System.out.println("Successfully rewrite manifest:");
		System.out.println("Input path [ " + inputPath + " ]");
		System.out.println("Output path [ " + outputPath + " ]");
	}

	public static List<String> sort(Set<String> initialElements) {
		List<String> finalElements = new ArrayList<String>(initialElements);
		Collections.sort(finalElements);
		return finalElements;
	}

    public static void write(Manifest mf, OutputStream out) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);

        StringBuilder builder = new StringBuilder();

        write( mf.getMainAttributes(), IS_MAIN, dos, builder );

        for ( String mfKey : sort( mf.getEntries().keySet() ) ) {
        	Attributes mfValue = mf.getAttributes(mfKey);

        	builder.append("Name: ");
            builder.append(mfKey);
            builder.append("\r\n");

            make72Safe(builder);
            dos.writeBytes(builder.toString());
            builder.setLength(0);

            write(mfValue, !IS_MAIN, dos, builder);
        }

        dos.flush();
    }

    private static Map<String, Name> getNames(Attributes attributes) {
    	Map<String, Name> names = new HashMap<String, Name>( attributes.size() );
    	for ( Object name : attributes.keySet() ) {
    		Name typedName = (Name) name;
    		names.put( typedName.toString(), typedName );
    	}
    	return names;
    }

    private static void write(
    	Attributes attributes, boolean isMain,
    	DataOutputStream out, StringBuilder builder) throws IOException {

        String vername;
        String version;
        if ( isMain ) {
        	vername = Name.MANIFEST_VERSION.toString();
        	version = attributes.getValue(vername);
        	if ( version == null ) {
        		vername = Name.SIGNATURE_VERSION.toString();
        		version = attributes.getValue(vername);
        	}
        	if ( version != null ) {
        		writeAttribute(vername, version, out, builder); // throws IOException
        	}
        } else {
        	vername = null;
        	version = null;
        }

    	Map<String, Name> names = getNames(attributes);

    	for ( String name : sort( names.keySet() ) ) {
    		if ( isMain && (vername != null) && name.equals(vername) ) {
    			continue;
    		}
            String value = (String) attributes.get( names.get(name) );
            writeAttribute(name, value, out, builder);
        }

        out.writeBytes("\r\n");
    }

    private static final boolean IS_MAIN = true;

    private static void writeAttribute(
    	String name, String value,
    	DataOutputStream out, StringBuilder builder) throws IOException {

		builder.append(name);
		builder.append(": ");
		builder.append(value);
		builder.append("\r\n");

        make72Safe(builder);

        out.writeBytes( builder.toString() ); // 'writeBytes' throws IOException
        builder.setLength(0);
    }

    
    private static void make72Safe(StringBuilder builder) {
        int length = builder.length();
        if ( length > 72 ) {
            int index = 70;
            while ( index < length - 2 ) {
                builder.insert(index, "\r\n ");
                index += 72;
                length += 3;
            }
        }
    }
}
