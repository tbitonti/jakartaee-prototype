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

public class LoggerImpl {
	public static final PrintStream NULL_STREAM = null;

	public static final boolean IS_TERSE = true;
	public static final boolean IS_VERBOSE = true;

	public static LoggerImpl createStandardLogger() {
		return new LoggerImpl(NULL_STREAM, !IS_TERSE, !IS_VERBOSE);
	}

	public LoggerImpl(PrintStream logStream, boolean isTerse, boolean isVerbose) {
		this.logStream = logStream;
		this.isTerse = isTerse;
		this.isVerbose = isVerbose;
	}

	//

	private final PrintStream logStream;
	private final boolean isTerse;
	private final boolean isVerbose;

	public PrintStream getLogStream() {
		return logStream;
	}

	public boolean getIsTerse() {
		return isTerse;
	}

	public boolean getIsVerbose() {
		return isVerbose;
	}

	public void log(String text, Object... parms) {
		if ( (logStream != null) && !isTerse ) {
			if ( parms.length == 0 ) {
				logStream.println(text);
			} else {
				logStream.printf(text, parms);
			}
		}
	}

	public void verbose(String text, Object... parms) {
		if ( (logStream != null) && isVerbose ) {
			if ( parms.length == 0 ) {
				logStream.print(text);
			} else {
				logStream.printf(text, parms);
			}
		}
	}

    public void error(String message, Object... parms) {
   		if ( logStream != null ) {
   			if ( parms.length == 0 ) {
   				logStream.print("ERROR: " + message);
   			} else {
   				logStream.printf("ERROR: " + message, parms);
   			}
   		}
    }

    public void error(String message, Throwable th, Object... parms) {
   		error(message, parms);
   		th.printStackTrace( getLogStream() );
    }
}
