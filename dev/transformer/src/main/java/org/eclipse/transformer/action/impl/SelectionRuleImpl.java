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

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.transformer.TransformProperties;
import org.eclipse.transformer.action.SelectionRule;

public class SelectionRuleImpl implements SelectionRule {

	public SelectionRuleImpl(
		LoggerImpl logger,
		Set<String> includes, Set<String> excludes) {

		this.logger = logger;

		this.included = new HashSet<String>(includes);
		this.includedExact = new HashSet<String>();
		this.includedHead = new HashSet<String>();
		this.includedTail = new HashSet<String>();
		this.includedAny = new HashSet<String>();

		TransformProperties.processSelections(
			this.included,
			this.includedExact, this.includedHead, this.includedTail, this.includedAny );

		this.excluded = new HashSet<String>(excludes);
		this.excludedExact = new HashSet<String>();
		this.excludedHead = new HashSet<String>();
		this.excludedTail = new HashSet<String>();
		this.excludedAny = new HashSet<String>();

		TransformProperties.processSelections(
			this.excluded,
			this.excludedExact, this.excludedHead, this.excludedTail, this.excludedAny );
	}

	//

	private final LoggerImpl logger;

	public LoggerImpl getLogger() {
		return logger;
	}

	public PrintStream getLogStream() {
		return getLogger().getLogStream();
	}

	public boolean getIsTerse() {
		return getLogger().getIsTerse();
	}

	public boolean getIsVerbose() {
		return getLogger().getIsVerbose();
	}

	public void log(String text, Object... parms) {
		getLogger().log(text, parms);
	}

	public void verbose(String text, Object... parms) {
		getLogger().verbose(text, parms);
	}

    public void error(String message, Object... parms) {
    	getLogger().error(message, parms);
    }

    public void error(String message, Throwable th, Object... parms) {
    	getLogger().error(message, th, parms);
    }

	//

	private final Set<String> included;
	private final Set<String> includedExact;
	private final Set<String> includedHead;
	private final Set<String> includedTail;
	private final Set<String> includedAny;
	
	private final Set<String> excluded;
	private final Set<String> excludedExact;
	private final Set<String> excludedHead;
	private final Set<String> excludedTail;	
	private final Set<String> excludedAny;	

	@Override
	public boolean select(String resourceName) {
		boolean isIncluded = selectIncluded(resourceName);
		boolean isExcluded = rejectExcluded(resourceName);

		return ( isIncluded && !isExcluded );
	}

	@Override
	public boolean selectIncluded(String resourceName) {
		if ( included.isEmpty() ) {
			verbose("Include [ %s ]: %s\n", resourceName, "No includes");
			return true;

		} else if ( includedExact.contains(resourceName) ) {
			verbose("Include [ %s ]: %s\n", resourceName, "Exact include");
			return true;

		} else {
			for ( String tail : includedHead ) {
				if ( resourceName.endsWith(tail) ) {
					verbose("Include [ %s ]: %s (%s)\n", resourceName, "Match tail", tail);
					return true;
				}
			}
			for ( String head : includedTail ) {
				if ( resourceName.startsWith(head) ) {
					verbose("Include [ %s ]: %s (%s)\n", resourceName, "Match head", head);
					return true;
				}
			}
			for ( String middle : includedAny ) {
				if ( resourceName.contains(middle) ) {
					verbose("Include [ %s ]: %s (%s)\n", resourceName, "Match middle", middle);
					return true;
				}
			}

			verbose("Do not include [ %s ]\n", resourceName);
			return false;
		}
	}

	@Override
	public boolean rejectExcluded(String resourceName ) {
		if ( excluded.isEmpty() ) {
			verbose("Do not exclude[ %s ]: %s\n", resourceName, "No excludes");
			return false;

		} else if ( excludedExact.contains(resourceName) ) {
			verbose("Exclude [ %s ]: %s\n", resourceName, "Exact exclude");
			return true;

		} else {
			for ( String tail : excludedHead ) {
				if ( resourceName.endsWith(tail) ) {
					verbose("Exclude[ %s ]: %s (%s)\n", resourceName, "Match tail", tail);
					return true;
				}
			}
			for ( String head : excludedTail ) {
				if ( resourceName.startsWith(head) ) {
					verbose("Exclude[ %s ]: %s (%s)\n", resourceName, "Match head", head);
					return true;
				}
			}
			for ( String middle : excludedAny ) {
				if ( resourceName.contains(middle) ) {
					verbose("Exclude[ %s ]: %s (%s)\n", resourceName, "Match middle", middle);
					return true;
				}
			}

			verbose("Do not exclude[ %s ]\n", resourceName);
			return false;
		}
	}
}
