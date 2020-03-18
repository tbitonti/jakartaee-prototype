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

import org.eclipse.transformer.action.BundleData;

// Bundle-Description: WAS WebContainer 8.0 with Servlet 3.0 support
// Bundle-Name: WAS WebContainer
// Bundle-SymbolicName: com.ibm.ws.webcontainer
// Bundle-Version: 1.1.35.cl191220191120-0300
// 
// com.ibm.ws.webcontainer=com.ibm.ws.webcontainer.jakarta,2.0,+" Jakarta",+"; Jakarta Enabled"
// 
// Bundle-SymbolicName: com.ibm.ws.webcontainer.jakarta
// Bundle-Version: 2.0
// Bundle-Name: WAS WebContainer Jakarta
// Bundle-Description: WAS WebContainer 8.0 with Servlet 3.0 support; Jakarta enabled

public class BundleDataImpl implements BundleData {
    public BundleDataImpl(
    	String symbolicName,
    	String version,

    	boolean addName, String name,
    	boolean addDescription, String description) {

    	this.symbolicName = symbolicName;
        this.version = version;

        this.addName = addName;
        this.name = name;

        this.addDescription = addDescription;
        this.description = description;

        // String nameText = ( addName ? ('+' + name) : name );
        // String descriptionText = ( addDescription ? ('+' + description) : description );
        // System.out.println(
        //     "BundleData [ " + symbolicName + " ] [ " + version + " ]:" +
        //     " [ " + nameText + " ] [ " + descriptionText + " ]" );
    }

	// com.ibm.ws.webcontainer=com.ibm.ws.webcontainer.jakarta,2.0,+" Jakarta",+"; Jakarta Enabled"    	

	public BundleDataImpl(String packedData) throws IllegalArgumentException {
    	String[] heads = new String[3];
    	String tail = packedData;

    	for ( int commaNo = 0; commaNo < 3; commaNo++ ) {
    		int comma = tail.indexOf(COMMA_CHAR);

    		String head = null;
    		String nextTail = null;
    		if ( comma != -1 ) {
    			head = tail.substring(0, comma).trim();
    			nextTail = tail.substring(comma + 1).trim();
    		}

    		if ( (head == null) || (nextTail == null) || nextTail.isEmpty() ) {
    			throw formatError(packedData);
    		}

    		heads[commaNo] = head;
    		tail = nextTail;
    	}

    	this.symbolicName = heads[0];
    	this.version = heads[1];
    	
    	String useName = heads[2];
    	String useDescription = tail;

    	if ( this.addName = (useName.charAt(0) == ADDITIVE_CHAR) ) {
    		useName = useName.substring(1).trim();
    	}
    	useName = unquote(useName);
    	if ( useName == null ) {
			throw formatError(packedData);
    	}
    	this.name = useName;

    	if ( this.addDescription = (useDescription.charAt(0) == ADDITIVE_CHAR) ) {
    		useDescription = useDescription.substring(1).trim();
    	}
    	useDescription = unquote(useDescription);
    	if ( useDescription == null ) {
			throw formatError(packedData);
    	}
    	this.description = useDescription;
    }

    private static IllegalArgumentException formatError(String text) {
		return new IllegalArgumentException("Incorrectly formatted bundle identity update [ " + text + " ]");
    }

	private static String unquote(String text) {
		if ( text.isEmpty() ) {
			return null;
		}

		if ( text.charAt(0) != QUOTE_CHAR ) {
			return text;
		}

		int textLen = text.length();
		if ( textLen < 2 ) {
			return null;
		} else if ( text.charAt(textLen - 1) != QUOTE_CHAR ) {
			return null;
		}

		return text.substring(1, textLen - 1);
	}

	//

	private final String symbolicName;
    private final String version;

    @Override
	public String getSymbolicName() {
		return symbolicName;
	}

    @Override
	public String getVersion() {
		return version;
	}

    private final boolean addName;
    private final String name;

    @Override
	public boolean getAddName() {
		return addName;
	}

	@Override
	public String getName() {
		return name;
	}

    private final boolean addDescription;
    private final String description;

	@Override    
	public boolean getAddDescription() {
		return addDescription;
	}

	@Override
	public String getDescription() {
		return description;
	}

	//

	@Override
	public String updateName(String initialName) {
		String nameUpdate = getName();
		return  ( getAddName() ? initialName + nameUpdate : nameUpdate );
	}

	@Override
	public String updateDescription(String initialDescription) {
		String descriptionUpdate = getDescription();
		return  ( getAddDescription() ? initialDescription + descriptionUpdate : descriptionUpdate );
	}
}
