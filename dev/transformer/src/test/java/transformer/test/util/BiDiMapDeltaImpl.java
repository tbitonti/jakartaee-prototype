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
import java.util.List;
import java.util.Set;

public class BiDiMapDeltaImpl<Held, Holder> implements Delta {
    public static final String CLASS_NAME = BiDiMapDeltaImpl.class.getSimpleName();

    public static BiDiMapDeltaImpl<String, String> stringDelta(String holderTag, String heldTag) {
    	return new BiDiMapDeltaImpl<String, String>(
    		String.class, holderTag, String.class, heldTag,
    		DO_RECORD_ADDED, DO_RECORD_REMOVED, !DO_RECORD_STILL);
    }

    //

    public BiDiMapDeltaImpl(
    	Class<Holder> holderClass, String holderTag, Class<Held> heldClass, String heldTag,
        boolean recordAdded, boolean recordRemoved, boolean recordStill) {

        this.hashText =
            	getClass().getSimpleName() +
            	"<" + holderClass.getSimpleName() + "," + heldClass.getSimpleName() + ">" +
            	"@" + Integer.toHexString(hashCode()) +
            	"(" + holderTag + " : " + heldTag + ")";

    	//

        this.holderClass = holderClass;
        this.holderTag = holderTag;

        this.heldClass = heldClass;
        this.heldTag = heldTag;

        //

        this.addedMap = ( recordAdded ? new BiDiMapImpl<Holder, Held>(holderClass, holderTag, heldClass, heldTag) : null );
        this.removedMap = ( recordRemoved ? new BiDiMapImpl<Holder, Held>(holderClass, holderTag, heldClass, heldTag) : null );
        this.stillMap = ( recordStill ? new BiDiMapImpl<Holder, Held>(holderClass, holderTag, heldClass, heldTag) : null );
    }

    //

    protected final Class<Holder> holderClass;
    protected final String holderTag;

    public String getHolderTag() {
        return holderTag;
    }

    protected final Class<Held> heldClass;
    protected final String heldTag;

    public String getHeldTag() {
        return heldTag;
    }

    //

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    protected final BiDiMapImpl<Holder, Held> addedMap;

    public BiDiMapImpl<Holder, Held> getAddedMap() {
        return addedMap;
    }

    public boolean isNullAdded() {
        return ( (addedMap == null) || addedMap.isEmpty() );
    }

    protected final BiDiMapImpl<Holder, Held> removedMap;

    public BiDiMapImpl<Holder, Held> getRemovedMap() {
        return removedMap;
    }

    public boolean isNullRemoved() {
        return ( (removedMap == null) || removedMap.isEmpty() );
    }

    protected final BiDiMapImpl<Holder, Held> stillMap;

    public BiDiMapImpl<Holder, Held> getStillMap() {
        return stillMap;
    }

    public boolean isNullStill() {
        return ( (stillMap == null) || stillMap.isEmpty() );
    }

    @Override
    public boolean isNull() {
        return ( isNullAdded() && isNullRemoved() );
    }

    public boolean isNull(boolean ignoreRemoved) {
        return ( isNullAdded() && (ignoreRemoved || isNullRemoved()) ); 
    }

    public void describe(String prefix, List<String> nonNull) {
        if ( !isNullAdded() ) {
            nonNull.add(prefix + " Added [ " + getAddedMap().getHolders().size() + " ]");
        }

        if ( !isNullRemoved() ) {
            nonNull.add(prefix + " Removed [ " + getRemovedMap().getHolders().size() + " ]");
        }
    }

    //

    public void subtract(BiDiMap<Holder, Held> finalMap, BiDiMap<Holder, Held> initialMap) {
        for ( Holder finalHolder : finalMap.getHolders() ) {
            Set<Held> finalHeldBy = finalMap.getHeld(finalHolder);
            Set<Held> initialHeld = initialMap.getHeld(finalHolder);

            if ( initialHeld.isEmpty() ) {
                if ( addedMap != null ) {
                    for ( Held finalHeld : finalHeldBy ) {
                        addedMap.record(finalHolder, finalHeld);
                    }
                }
            } else {
                for ( Held finalHeld : finalHeldBy ) {
                    if ( initialHeld.contains(finalHeld) ) {
                        if ( stillMap != null ) {
                            stillMap.record(finalHolder, finalHeld);
                        }
                    } else {
                        if ( addedMap != null ) {
                            addedMap.record(finalHolder, finalHeld);
                        }
                    }
                }
            }
        }

        for ( Holder initialHolder : initialMap.getHolders() ) {
            Set<Held> initialHeldBy = initialMap.getHeld(initialHolder);
            Set<Held> finalHeldBy = finalMap.getHeld(initialHolder);

            if ( finalHeldBy.isEmpty() ) {
                if ( removedMap != null ) {
                    for ( Held held : initialHeldBy ) {
                        removedMap.record(initialHolder, held);
                    }
                }
            } else {
                for ( Held initialHeld : initialHeldBy ) {
                    if ( finalHeldBy.contains(initialHeld) ) {
                        // if ( stillMap != null ) {
                        //    stillMap.record(holder, held);
                        // }
                    } else {
                        if ( removedMap != null ) {
                            removedMap.record(initialHolder, initialHeld);
                        }
                    }
                }
            }
        }
    }

    //

    private static final String logPrefix = CLASS_NAME + ": " + "log: ";

    @Override
    public void log(PrintWriter writer) {
        boolean nullAdded = isNullAdded();
        boolean nullRemoved = isNullRemoved();
        boolean nullStill = isNullStill();
        
        if ( nullAdded && nullRemoved ) {
            int numStill = ( nullStill ? 0 : getStillMap().getHolders().size() );
            writer.println(logPrefix + "Mapping Delta: Unchanged [ " + numStill + " ]: " + getHashText());

        } else {
            writer.println(logPrefix + "Mapping Delta: BEGIN: " + getHashText());

            if ( !nullAdded ) {
                logAddedAnnotations(writer);
            }
            if ( !nullRemoved ) {
                logRemovedAnnotations(writer);
            }
            if ( !nullStill ) {
                logStillAnnotations(writer);
            }

            writer.println(logPrefix + "Mapping Delta: END: " + getHashText());
        }
    }

    public void logAddedAnnotations(PrintWriter writer) {
        BiDiMapImpl<Holder, Held> useAddedMap = getAddedMap();
        if ( useAddedMap == null ) {
            writer.println(logPrefix + "Added Entries: ** NOT RECORDED **");
            return;
        }

        writer.println(logPrefix + "Added Entries: BEGIN");
        logMap(writer, useAddedMap);
        writer.println(logPrefix + "Added Entries: END");
    }

    public void logRemovedAnnotations(PrintWriter writer) {
        BiDiMapImpl<Holder, Held> useRemovedMap = getRemovedMap();
        if ( useRemovedMap == null ) {
            writer.println(logPrefix + "Removed Entries: ** NOT RECORDED **");
            return;
        }

        writer.println(logPrefix + "Removed Entries: BEGIN");
        logMap(writer, useRemovedMap);
        writer.println(logPrefix + "Removed Entries: END");
    }

    public void logStillAnnotations(PrintWriter writer) {
        BiDiMapImpl<Holder, Held> useStillMap = getStillMap();
        if ( useStillMap == null ) {
            writer.println(logPrefix + "Still Entries: [ ** NOT RECORDED ** ]");
            return;
        }

        writer.println(logPrefix + "Still Entries: BEGIN");
        logMap(writer, useStillMap);
        writer.println(logPrefix + "Still Entries: END");
    }

    protected void logMap(PrintWriter writer, BiDiMapImpl<Holder, Held> map) {
        Set<Holder> useHolderSet = map.getHolders();
        if ( useHolderSet.isEmpty() ) {
            writer.println(logPrefix + "  ** NONE **");
        } else {
            for ( Holder holder : map.getHolders() ) {
                writer.println(logPrefix + "  [ " + holder + " ]");

                for ( Held held : map.getHeld(holder) ) {
                    writer.println(logPrefix + "    [ " + held + " ]");
                }
            }
        }
    }
}
