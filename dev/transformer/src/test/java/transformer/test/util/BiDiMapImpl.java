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
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BiDiMapImpl<Holder, Held> implements BiDiMap<Holder, Held> {
    public static final String CLASS_NAME = BiDiMapImpl.class.getSimpleName();

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    protected BiDiMapImpl(
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

        this.holderToHeldMap = new HashMap<Holder, Set<Held>>();
        this.heldToHoldersMap = new HashMap<Held, Set<Holder>>();
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
    protected String heldTag;

    @Override
    public Class<Held> getHeldClass() {
    	return heldClass;
    }

    @Override
    public String getHeldTag() {
        return heldTag;
    }

    //

    protected Map<Holder, Set<Held>> holderToHeldMap;
    protected Map<Held, Set<Holder>> heldToHoldersMap;

    @Override
    public boolean isEmpty() {
        return ( holderToHeldMap.isEmpty() );
    }

    @Override
    public boolean holds(Holder holder, Held held) {
    	Set<Held> heldBy = holderToHeldMap.get(holder);
    	return ( (heldBy != null) && heldBy.contains(held) );
    }

    @Override
    public boolean isHolder(Holder holder) {
    	return holderToHeldMap.containsKey(holder);
    }

    @Override
    public Set<Holder> getHolders() {
        return (holderToHeldMap.keySet());
    }

    @Override
    public Set<Held> getHeld(Holder holder) {
        Set<Held> heldBy = holderToHeldMap.get(holder);
        return ( (heldBy == null) ? Collections.emptySet() : heldBy );
    }

    @Override
    public boolean isHeld(Held held) {
    	return heldToHoldersMap.containsKey(held);
    }

    @Override
    public Set<Held> getHeld() {
        return ( heldToHoldersMap.keySet() );
    }

    @Override
    public Set<Holder> getHolders(Held held) {
        Set<Holder> holdersOf = heldToHoldersMap.get(held);
        return ( (holdersOf == null) ? Collections.emptySet() : holdersOf );
    }

    //

    @Override
    public boolean record(Holder holder, Held held) {
        boolean addedHeldToHolder = recordHolderToHeld(holder, held);
        boolean addedHolderToHeld = recordHeldToHolder(holder, held);

        if ( addedHeldToHolder != addedHolderToHeld ) {
        	System.out.println(
        		MessageFormat.format(
        			"[ {0} ] Holder [ {1} ] Held [ {2} ] Added to holder [ {3} ] Added to held [ {4} ]",
                    new Object[] {
                        getHashText(),
                        holder,
                        held,
                        Boolean.valueOf(addedHeldToHolder),
                        Boolean.valueOf(addedHolderToHeld) }) );
        }

        return addedHeldToHolder;
    }

    protected boolean recordHolderToHeld(Holder holder, Held held) {
        Set<Held> heldBy = recordHolder(holder);
        return heldBy.add(held);
    }

    protected boolean recordHeldToHolder(Holder holder, Held held) {
        Set<Holder> holderOf = recordHeld(held);
        return holderOf.add(holder);
    }

    protected Set<Held> recordHolder(Holder holder) {
        Set<Held> heldBy = holderToHeldMap.get(holder);
        if (heldBy == null) {
            heldBy = new HashSet<Held>();
            holderToHeldMap.put(holder, heldBy);
        }
        return heldBy;
    }

    protected Set<Holder> recordHeld(Held held) {
        Set<Holder> holderOf = heldToHoldersMap.get(held);
        if ( holderOf == null ) {
            holderOf = new HashSet<Holder>();
            heldToHoldersMap.put(held, holderOf);
        }
        return holderOf;
    }

    @Override
    public <OtherHolder extends Holder, OtherHeld extends Held>
    	void record(BiDiMap<OtherHolder, OtherHeld> otherMap) {
        for ( OtherHolder holder : otherMap.getHolders() ) {
            Set<OtherHeld> heldBy = otherMap.getHeld(holder);
            for ( OtherHeld held : heldBy ) {
                record(holder, held);
            }
        }
    }

    @Override
    public <OtherHolder extends Holder, OtherHeld extends Held>
    	void record(BiDiMap<OtherHolder, OtherHeld> otherMap,
    			    Set<? extends Holder> restrictedHolders) {

        for ( OtherHolder holder : otherMap.getHolders() ) {
            if ( !restrictedHolders.contains(holder) ) {
                continue;
            }
            Set<OtherHeld> heldBy = otherMap.getHeld(holder);
            for ( OtherHeld held : heldBy ) {
                record(holder, held);
            }
        }
    }

    //

    /**
     * <p>Tell if two maps have the same contents.</p>
     *
     * @param otherMap The map to test against this map.

     * @return True if the maps have the same contents.
     */
    @Override
    public <OtherHolder extends Holder, OtherHeld extends Held> 
        boolean sameAs(BiDiMap<OtherHolder, OtherHeld> otherMap) {

        if ( otherMap == null ) {
            return false; // Null other map.
        } else if ( otherMap == this ) {
            return true; // Identical other map.
        }

        if ( getHolders().size() != otherMap.getHolders().size() ) {
            return false; // Unequal sizes of key sets.
        }

        for ( OtherHolder otherHolder : otherMap.getHolders() ) {
        	Set<OtherHeld> otherHeldBy = otherMap.getHeld(otherHolder);
        	Set<Held> heldBy = getHeld(otherHolder);

            if ( otherHeldBy.size() != heldBy.size() ) {
            	return false;
            } else if ( !otherHeldBy.containsAll(heldBy) ) {
            	return false;
            }
        }

        return true;
    }

    //

    private static final String logPrefix = "log: " + CLASS_NAME + ": ";

    @Override
    public void log(PrintWriter writer) {
        writer.println(logPrefix + "BiDi Map: BEGIN: " + getHashText());

        logHolderMap(writer);
        logHeldMap(writer);

        writer.println(logPrefix + "BiDi Map: END: " + getHashText());
    }

    public void logHolderMap(PrintWriter writer) {
        writer.println(logPrefix + "Holder-to-held Map: BEGIN");

        for (Map.Entry<Holder, Set<Held>> holderEntry : holderToHeldMap.entrySet()) {
            Holder holder = holderEntry.getKey();
            Set<Held> held = holderEntry.getValue();
            writer.println(logPrefix + "  Holder [ " + holder + " ] Held [ " + held + " ]");
        }

        writer.println(logPrefix + "Holder-to-held Map: END");
    }

    public void logHeldMap(PrintWriter writer) {
        writer.println(logPrefix + "Held-to-holder Map: BEGIN");

        for (Map.Entry<Held, Set<Holder>> heldEntry : heldToHoldersMap.entrySet()) {
            Held held = heldEntry.getKey();
            Set<Holder> holders = heldEntry.getValue();
            writer.println(logPrefix + "  Held [ " + held + " ] Holders [ " + holders + " ]");
        }

        writer.println(logPrefix + "Held-to-holder Map: END");
    }
}
