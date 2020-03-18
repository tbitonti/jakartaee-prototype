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

/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package transformer.test.util;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Set;

public class BiDiMapImpl_Empty<Holder, Held> implements BiDiMap<Holder, Held> {
    public static final String CLASS_NAME = BiDiMapImpl_Empty.class.getSimpleName();

    //

    protected BiDiMapImpl_Empty(
    	Class<Holder> holderClass, String holderTag,
    	Class<Held> heldClass, String heldTag) {

        super();

        this.hashText =
        	getClass().getSimpleName() +
            "<" + holderClass.getSimpleName() + "," + heldClass.getSimpleName() + ">" +
            "@" + Integer.toHexString(hashCode()) +
            "(" + holderTag + " : " + heldTag + ")";

        this.holderClass = holderClass;
        this.holderTag = holderTag;

        this.heldClass = heldClass;
        this.heldTag = heldTag;
    }

    //

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    protected final Class<Holder> holderClass;
    protected final String holderTag;

    @Override
    public Class<Holder> getHolderClass() {
        return holderClass;
    }

    @Override
    public String getHolderTag() {
        return holderTag;
    }

    protected final Class<Held> heldClass;
    protected final String heldTag;

    @Override
    public Class<Held> getHeldClass() {
        return heldClass;
    }

    @Override
    public String getHeldTag() {
        return heldTag;
    }

    //

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean holds(Holder holder, Held held) {
        return false;
    }

    //

    @Override
    public boolean isHolder(Holder holder) {
        return false;
    }

    @Override
    public Set<Holder> getHolders() {
        return Collections.emptySet();
    }

    @Override
    public Set<Held> getHeld(Holder holder) {
        return Collections.emptySet();
    }

    @Override
    public boolean isHeld(Held held) {
        return false;
    }

    @Override
    public Set<Held> getHeld() {
        return Collections.emptySet();
    }

    @Override
    public Set<Holder> getHolders(Held held) {
        return Collections.emptySet();
    }

    //

    public boolean record(Holder holder, Held held) {
        throw new UnsupportedOperationException();
    }

	@Override
	public <OtherHolder extends Holder, OtherHeld extends Held>
		void record(BiDiMap<OtherHolder, OtherHeld> otherMap) {
        throw new UnsupportedOperationException();
	}

	@Override
	public <OtherHolder extends Holder, OtherHeld extends Held>
	    void record(BiDiMap<OtherHolder, OtherHeld> otherMap,
		        	Set<? extends Holder> restrictedHolders) {
        throw new UnsupportedOperationException();
	}

	@Override
	public <OtherHolder extends Holder, OtherHeld extends Held> boolean sameAs(
			BiDiMap<OtherHolder, OtherHeld> otherMap) {
		return ( otherMap.isEmpty() );
	}

    //

    private static final String logPrefix = CLASS_NAME + ": " + "log" + ": ";

    @Override
    public void log(PrintWriter writer) {
        writer.println(logPrefix + "BiDi Map (Empty): BEGIN: " + getHashText());

        writer.println(logPrefix + "Holder-to-held Map: [ NULL ]");
        writer.println(logPrefix + "Held-to-holder Map: [ NULL ]");

        writer.println(logPrefix + "BiDi Map (Empty): END: " + getHashText());
    }
}
