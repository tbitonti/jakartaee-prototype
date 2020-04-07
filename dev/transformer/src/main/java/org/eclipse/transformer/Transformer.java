/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

package org.eclipse.transformer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Option.Builder;
import org.eclipse.transformer.TransformerLoggerFactory.LoggerProperty;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.impl.ActionImpl;
import org.eclipse.transformer.action.impl.ClassActionImpl;
import org.eclipse.transformer.action.impl.CompositeActionImpl;
import org.eclipse.transformer.action.impl.DirectoryActionImpl;
import org.eclipse.transformer.action.impl.EarActionImpl;
import org.eclipse.transformer.action.impl.InputBufferImpl;
import org.eclipse.transformer.action.impl.JarActionImpl;
import org.eclipse.transformer.action.impl.JavaActionImpl;
import org.eclipse.transformer.action.impl.ManifestActionImpl;
import org.eclipse.transformer.action.impl.NullActionImpl;
import org.eclipse.transformer.action.impl.RarActionImpl;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.ServiceLoaderConfigActionImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.eclipse.transformer.action.impl.WarActionImpl;
import org.eclipse.transformer.action.impl.XmlActionImpl;
import org.eclipse.transformer.action.impl.ZipActionImpl;
import org.eclipse.transformer.util.FileUtils;

import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.uri.URIUtil;

public class Transformer {
    //

	// TODO: Make this an enum?

    public static final int SUCCESS_RC = 0;
    public static final int PARSE_ERROR_RC = 1;
    public static final int RULES_ERROR_RC = 2;
    public static final int TRANSFORM_ERROR_RC = 3;
    public static final int FILE_TYPE_ERROR_RC = 4;
    public static final int LOGGER_SETTINGS_ERROR_RC = 5;

    public static String[] RC_DESCRIPTIONS = new String[] {
    	"Success",
    	"Parse Error",
    	"Rules Error",
    	"Transform Error",
    	"File Type Error",
    	"Logger Settings Error"
    };

    //

    public static void main(String[] args) throws Exception {
    	@SuppressWarnings("unused")
		int rc = runWith(System.out, System.err, args);
        // System.exit(rc); // TODO: How should this code be returned?
    }

    public static int runWith(PrintStream sysOut, PrintStream sysErr, String...args) {
        Transformer trans = new Transformer(sysOut, sysErr);
        trans.setArgs(args);

        int rc = trans.run();
        if ( rc == SUCCESS_RC ) {
        	System.out.println("Return Code [ 0 ]: Success");
        } else {
        	System.err.println("Return Code [ " + rc + " ]: Failure [ " + RC_DESCRIPTIONS[rc] + " ]");
        }
        return rc;
    }

    //

    public static class OptionSettings {
        private static final boolean HAS_ARG = true;
        private static final boolean HAS_ARGS = true;
        private static final boolean IS_REQUIRED = true;
        private static final String NO_GROUP = null;

        private OptionSettings (
            String shortTag, String longTag, String description,
            boolean hasArg, boolean hasArgs,
            boolean isRequired, String groupTag) {

            this.shortTag = shortTag;
            this.longTag = longTag;
            this.description = description;

            this.isRequired = isRequired;

            this.hasArg = hasArg;
            this.hasArgs = hasArgs;
            this.groupTag = groupTag;
        }

        private final String shortTag;
        private final String longTag;
        private final String description;

        public String getShortTag() {
            return shortTag;
        }

        public String getLongTag() {
            return longTag;
        }

        public String getDescription() {
            return description;
        }

        //

        // Is this option required.
        // If in a group, is at least one of the group required.

        private final boolean isRequired;

        //

        private final boolean hasArg;
        private final boolean hasArgs;
        private final String groupTag;

        public boolean getHasArg() {
            return hasArg;
        }

        public boolean getHasArgs() {
            return hasArgs;
        }

        public String getGroupTag() {
            return groupTag;
        }

        public boolean getIsRequired() {
            return isRequired;
        }

        //

        public static Options build(OptionSettings[] settings) {
            Options options = new Options();

            Map<String, OptionGroup> groups = new HashMap<String, OptionGroup>();

            for ( OptionSettings optionSettings : settings ) {
                String groupTag = optionSettings.getGroupTag();
                OptionGroup group;
                if ( groupTag != null ) {
                    group = groups.get(groupTag);
                    if ( group == null ) {
                        group = new OptionGroup();
                        if ( optionSettings.getIsRequired() ) {
                            group.setRequired(true);
                        }
                        groups.put(groupTag, group);

                        options.addOptionGroup(group);
                    }

                } else {
                    group = null;
                }

                Builder builder = Option.builder( optionSettings.getShortTag() );
                builder.longOpt( optionSettings.getLongTag() );
                builder.desc( optionSettings.getDescription() );
                if ( optionSettings.getHasArgs() ) {
                	builder.hasArg(false);
                	builder.hasArgs();
                } else if ( optionSettings.getHasArg() ) {
                	builder.hasArg();
                } else {
                	// No arguments are required for this option.
                }
                builder.required( (group == null) && optionSettings.getIsRequired() );

                Option option = builder.build();

                if ( group != null ) {
                    group.addOption(option);
                } else {
                    options.addOption(option);
                }
            }

            return options;
        }
    }

    // Not in use, until option grouping is figured out.

    public static final String INPUT_GROUP = "input";
    public static final String LOGGING_GROUP = "logging";

    // public static final String DEFAULT_SELECTIONS_REFERENCE = "jakarta-selections.properties";
    public static final String DEFAULT_RENAMES_REFERENCE = "jakarta-renames.properties";
    public static final String DEFAULT_VERSIONS_REFERENCE = "jakarta-versions.properties";
    public static final String DEFAULT_BUNDLES_REFERENCE = "jakarta-bundles.properties";
    public static final String DEFAULT_SPECIFIC_XML_FILE_MAP_REFERENCE = "jakarta-xml.properties";

    public static enum AppOption {
        USAGE  ("u", "usage",    "Display usage",
        	!OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
            !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        HELP   ("h", "help",    "Display help",
            !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
            !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

        LOG_TERSE("q", "quiet", "Display quiet output",
        	!OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
        	!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        LOG_VERBOSE("v", "verbose", "Display verbose output",
        	!OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
        	!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        LOG_PROPERTY("lp", "logProperty", "Logging property",
        	!OptionSettings.HAS_ARG, OptionSettings.HAS_ARGS,
        	!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        LOG_PROPERTY_FILE("lpf", "logPropertyFile", "Logging properties file",
        	OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
            !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        LOG_NAME("ln", "logName", "Logger name",
        	OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
        	!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        LOG_LEVEL("ll", "logLevel", "Logging level",
            OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
            !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        LOG_FILE("lf", "logFile", "Logging file",
        	OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
            !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

        RULES_SELECTIONS("ts", "selection", "Transformation selections URL",
        	OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
        	!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        RULES_RENAMES("tr", "renames", "Transformation package renames URL",
        	OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
        	!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        RULES_VERSIONS("tv", "versions", "Transformation package versions URL",
            OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
            !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        RULES_BUNDLES("tb", "bundles", "Transformation bundle updates URL",
            OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
            !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        RULES_DIRECT("td", "direct", "Transformation direct string replacements",
            OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
            !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        
        RULES_SPECIFIC_XML_FILES("tf", "direct", "Map of XML filenames to property files",
                OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
                !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

        INVERT("i", "invert", "Invert transformation rules",
           	!OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
           	!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

        FILE_TYPE("t", "type", "Input file type",
            OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
            !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        OVERWRITE("o", "overwrite", "Overwrite",
            !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
            !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

    	DRYRUN("d", "dryrun", "Dry run",
                !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
                !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP);

        private AppOption(
            String shortTag, String longTag, String description,
            boolean hasArg, boolean hasArgs, boolean isRequired, String groupTag) {

            this.settings = new OptionSettings(
                shortTag, longTag, description,
                hasArg, hasArgs, isRequired, groupTag);
        }

        private final OptionSettings settings;

        public OptionSettings getSettings() {
            return settings;
        }

        public String getShortTag() {
            return getSettings().getShortTag();
        }
        
        public String getLongTag() {
            return getSettings().getLongTag();
        }

        public String getDescription() {
            return getSettings().getDescription();
        }

        public boolean getIsRequired() {
            return getSettings().getIsRequired();
        }

        public boolean getHasArg() {
            return getSettings().getHasArg();
        }

        public String getGroupTag() {
            return getSettings().getGroupTag();
        }

        //

        private static OptionSettings[] getAllSettings() {
            AppOption[] allAppOptions =  AppOption.values();

            OptionSettings[] allSettings = new OptionSettings[ allAppOptions.length ];

            for ( int optionNo = 0; optionNo < allAppOptions.length; optionNo++ ) {
                allSettings[optionNo] = allAppOptions[optionNo].getSettings();
            }

            return allSettings;
        }

        public static Options build() {
            return OptionSettings.build( getAllSettings() );
        }
    }

    //

    public Transformer(PrintStream sysOut, PrintStream sysErr) {
    	this.sysOut = sysOut;
    	this.sysErr = sysErr;

        this.appOptions = AppOption.build();
    }

    //

    private final PrintStream sysOut;

    protected PrintStream getSystemOut() {
    	return sysOut;
    }

    private final PrintStream sysErr;
    
    protected PrintStream getSystemErr() {
    	return sysErr;
    }

    public void systemPrint(PrintStream output, String message, Object... parms) {
    	if ( parms.length != 0 ) {
    		message = String.format(message, parms);
    	}
    	output.println(message);
    }

    public void errorPrint(String message, Object... parms) {
    	systemPrint( getSystemErr(), message, parms );
    }

    public void outputPrint(String message, Object... parms) {
    	systemPrint( getSystemOut(), message, parms );
    }    

    //

    private final Options appOptions;

    public Options getAppOptions() {
        return appOptions;
    }

    private String[] args;
    private CommandLine parsedArgs;

    public void setArgs(String[] args) {
        this.args = args;
    }

    protected String[] getArgs() {
        return args;
    }

    public void setParsedArgs() throws ParseException {
        CommandLineParser parser = new DefaultParser();
        parsedArgs = parser.parse( getAppOptions(), getArgs());
    }

    protected CommandLine getParsedArgs() {
        return parsedArgs;
    }

    protected String getInputFileNameFromCommandLine() {
        String[] useArgs = parsedArgs.getArgs();
        if ( useArgs != null ) {
            if ( useArgs.length > 0 ) {
                return useArgs[0]; // First argument
            } 
        }
        return null;
    }

    protected String getOutputFileNameFromCommandLine() {
        String[] useArgs = parsedArgs.getArgs();
        if ( useArgs != null ) {
            if ( useArgs.length > 1 ) {
                return useArgs[1]; // Second argument
            } 
        }
        return null;
    }

    protected boolean hasOption(AppOption option) {
        return getParsedArgs().hasOption( option.getShortTag() );
    }

    protected String getOptionValue(AppOption option) {
        CommandLine useParsedArgs = getParsedArgs();
        String useShortTag = option.getShortTag();
        if ( useParsedArgs.hasOption(useShortTag) ) {
            return useParsedArgs.getOptionValue(useShortTag);
        } else {
            return null;
        }
    }

    protected String[] getOptionValues(AppOption option) {
        CommandLine useParsedArgs = getParsedArgs();
        String useShortTag = option.getShortTag();
        if ( useParsedArgs.hasOption(useShortTag) ) {
            return useParsedArgs.getOptionValues(useShortTag);
        } else {
            return null;
        }
    }

    //

    private void help(PrintStream helpStream) {
        try ( PrintWriter helpWriter = new PrintWriter(helpStream) ) {
            helpWriter.println();

            HelpFormatter helpFormatter = new HelpFormatter();
            boolean AUTO_USAGE = true;

            helpFormatter.printHelp(
                helpWriter,
                HelpFormatter.DEFAULT_WIDTH + 5,
                Transformer.class.getName() + " input [ output ] [ options ]", // Command line syntax
                "Options:", // Header
                getAppOptions(),
                HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD,
                "", // Footer
                !AUTO_USAGE);

            helpWriter.println("Actions:");
            for ( ActionType actionType : ActionType.values() ) {
            	helpWriter.println("  [ " + actionType.name() + " ]");
            }

            helpWriter.println("Logging Properties:");
            for ( TransformerLoggerFactory.LoggerProperty loggerProperty :
            	      TransformerLoggerFactory.LoggerProperty.values() ) {
            	helpWriter.println("  [ " + loggerProperty.getPropertyName() + " ]");
            }
            
            helpWriter.flush();

        }
    }

    //

    protected UTF8Properties loadProperties(AppOption ruleOption) throws IOException, URISyntaxException {
        String rulesReference = getOptionValue(ruleOption);

        if ( rulesReference == null ) {
        	dual_info("Skipping option [ %s ]", ruleOption);
        	return FileUtils.createProperties();
        } else {
        	return loadExternalProperties(ruleOption, rulesReference);
        }
    }

    protected UTF8Properties loadProperties(AppOption ruleOption, String defaultReference)
    	throws IOException, URISyntaxException {

        String rulesReference = getOptionValue(ruleOption);
        if ( rulesReference != null ) {
        	return loadExternalProperties(ruleOption, rulesReference);
        } else {
        	return loadDefaultProperties(ruleOption, defaultReference);
        }
    }

    protected UTF8Properties loadDefaultProperties(AppOption ruleOption, String defaultReference)
        	throws IOException {

    	dual_info("Using internal [ %s ]: [ %s ]", ruleOption, defaultReference);
    	URL rulesUrl = getClass().getResource(defaultReference);
    	if ( rulesUrl == null ) {
    		dual_info("Default [ %s ] were not found [ %s ]", AppOption.RULES_SELECTIONS, defaultReference);
    		return null;
    	} else {
    		dual_info("Default [ %s ] URL [ %s ]", ruleOption, rulesUrl);
    	}
    	return FileUtils.loadProperties(rulesUrl);
    }

    protected UTF8Properties loadExternalProperties(AppOption ruleOption, String externalReference)
    	throws URISyntaxException, IOException {

    	return loadExternalProperties( ruleOption.toString(), externalReference );
    }

    protected UTF8Properties loadExternalProperties(String referenceName, String externalReference)
        throws URISyntaxException, IOException {

    	dual_info("Using external [ %s ]: [ %s ]", referenceName, externalReference);
    	URI currentDirectoryUri = IO.work.toURI();
    	URL rulesUrl = URIUtil.resolve(currentDirectoryUri, externalReference).toURL();
    	dual_info("External [ %s ] URL [ %s ]", referenceName, rulesUrl);

    	return FileUtils.loadProperties(rulesUrl);
    }

    //

    private Logger logger;

    public Logger getLogger() {
    	return logger;
    }

    public void info(String message, Object... parms) {
    	getLogger().info(message, parms);
    }

    protected void error(String message, Object... parms) {
    	getLogger().error(message, parms);
    }

    protected void error(String message, Throwable th, Object... parms) {
    	Logger useLogger = getLogger();
    	if ( useLogger.isErrorEnabled() ) {
    		message = String.format(message, parms);
    		useLogger.error(message, th);
    	}
    }

    //

	public boolean toSysOut;
	public boolean toSysErr;

	protected void detectLogFile() {
		toSysOut = TransformerLoggerFactory.logToSysOut();
		if ( toSysOut ) {
			outputPrint("Logging is to System.out\n");
		}

		toSysErr = TransformerLoggerFactory.logToSysErr();
		if ( toSysOut ) {
			outputPrint("Logging is to System.err\n");
		}

		outputPrint("Log file [ " + System.getProperty(LoggerProperty.LOG_FILE.getPropertyName()) + " ]");
	}

    public void dual_info(String message, Object... parms) {
    	if ( parms.length != 0 ) {
    		message = String.format(message, parms);
    	}
    	if ( !toSysOut && !toSysErr ) {
    		systemPrint( getSystemOut(), message );
    	}
    	info(message);
    }

    protected void dual_error(String message, Object... parms) {
    	if ( parms.length != 0 ) {
    		message = String.format(message, parms);
    	}
    	if ( !toSysOut && !toSysErr ) {
    		systemPrint( getSystemErr(), message );
    	}
    	info(message);
    }

    protected void dual_error(String message, Throwable th, Object... parms) {
    	if ( parms.length != 0 ) {
    		message = String.format(message, parms);
    	}
    	if ( !toSysOut && !toSysErr ) {
    		PrintStream useOutput = getSystemErr();
    		systemPrint(useOutput, message);
    		th.printStackTrace(useOutput);
    	}
    	getLogger().error(message, th);
    }

    //

    public TransformOptions createTransformOptions() {
        return new TransformOptions();
    }

    public class TransformOptions {
    	public boolean isVerbose;
    	public boolean isTerse;

    	public Set<String> includes;
    	public Set<String> excludes;

    	public boolean invert;
    	public Map<String, String> packageRenames;
    	public Map<String, String> packageVersions;
    	public Map<String, BundleData> bundleUpdates;
    	public Map<String, Map<String,String>> specificXmlFileUpdates;   // file name, (property, value)
    	public Map<String, String> directStrings;

    	public CompositeActionImpl rootAction;
    	public ActionImpl acceptedAction;

    	public String inputName;
        public String inputPath;
        public File inputFile;

        public boolean allowOverwrite;

        public String outputName;
        public String outputPath;
        public File outputFile;

        //

    	public void setLogging() throws TransformException {
    		logger = new TransformerLoggerFactory(Transformer.this).createLogger(); // throws TransformException

            if ( hasOption(AppOption.LOG_TERSE) ) {
            	isTerse = true;
            } else if ( hasOption(AppOption.LOG_VERBOSE) ) {
            	isVerbose = true;
            }
    	}

        protected void info(String message, Object... parms) {
        	getLogger().info(message, parms);
        }

        protected void error(String message, Object... parms) {
        	getLogger().error(message, parms);
        }

        protected void error(String message, Throwable th, Object... parms) {
        	Logger useLogger = getLogger();
        	if ( useLogger.isErrorEnabled() ) {
        		message = String.format(message, parms);
        		useLogger.error(message, th);
        	}
        }

        //

    	public String getInputFileName() {
    	    return inputName;
    	}
    	
        public String getOutputFileName() {
            return outputName;
        }

    	private InputBufferImpl buffer;
    	
    	protected InputBufferImpl getBuffer() {
    		if ( buffer == null ) {
    			buffer = new InputBufferImpl();
    		}
    		return buffer;
    	}

    	public boolean setRules() throws IOException, URISyntaxException, IllegalArgumentException {
    		UTF8Properties selectionProperties = loadProperties(AppOption.RULES_SELECTIONS);
    		UTF8Properties renameProperties = loadProperties(AppOption.RULES_RENAMES, DEFAULT_RENAMES_REFERENCE);
    		UTF8Properties versionProperties = loadProperties(AppOption.RULES_VERSIONS, DEFAULT_VERSIONS_REFERENCE);
    		UTF8Properties updateProperties = loadProperties(AppOption.RULES_BUNDLES, DEFAULT_BUNDLES_REFERENCE);
    		UTF8Properties directProperties = loadProperties(AppOption.RULES_DIRECT);
    		UTF8Properties xmlFileMapProperties = loadProperties(AppOption.RULES_SPECIFIC_XML_FILES, DEFAULT_SPECIFIC_XML_FILE_MAP_REFERENCE);

        	invert = hasOption(AppOption.INVERT);

        	includes = new HashSet<String>();
        	excludes = new HashSet<String>();

        	if ( selectionProperties != null ) {
        		TransformProperties.setSelections(includes, excludes, selectionProperties);
        	} else {
        		dual_info("All resources will be selected");
        	}

        	if ( renameProperties != null ) {
        		Map<String, String> renames = TransformProperties.getPackageRenames(renameProperties);
        		if ( invert ) {
        			renames = TransformProperties.invert(renames);
        		}
        		packageRenames = renames;
        	} else {
        		dual_info("No package renames are available");
        		packageRenames = null;
        	}

        	if ( versionProperties != null ) {
        		packageVersions = TransformProperties.getPackageVersions(versionProperties);
        	} else {
        		dual_info("Package versions will not be updated");
        	}

        	if ( updateProperties != null ) {
        		bundleUpdates = TransformProperties.getBundleUpdates(updateProperties);
        		// throws IllegalArgumentException
        	} else {
        		dual_info("Bundle identities will not be updated");
        	}
        	
        	if ( xmlFileMapProperties != null ) {
        	    Map<String, String> xmlFileMap = TransformProperties.convertPropertiesToMap(xmlFileMapProperties); // throws IllegalArgumentException
        	    Map<String, String> substitutionsMap;
        	    specificXmlFileUpdates = new HashMap<String, Map<String, String>>();
        	    for(String xmlFileName : xmlFileMap.keySet()) {
        	        // Load DEFAULT properties directly - no command line option to specify location of properties file.
        	        // Properties file locations are specified in "xmlFileMap"
        	        UTF8Properties substitutions = loadDefaultProperties(AppOption.RULES_SPECIFIC_XML_FILES, xmlFileMap.get(xmlFileName));
        	        if ( substitutions != null ) {
        	           substitutionsMap = TransformProperties.convertPropertiesToMap(substitutions); // throws IllegalArgumentException
        	           specificXmlFileUpdates.put(xmlFileName, substitutionsMap);
        	        } else {
        	            dual_error("[ %s ] will not be updated", xmlFileName);
        	        }
        	    }
        	} else {
        	    dual_info("No specific-xml-file properties will be updated");
        	}

        	directStrings = TransformProperties.getDirectStrings(directProperties);

        	if ( packageRenames != null ) {
        	    if ( packageVersions != null ) {
        	       return validateRules(packageRenames, packageVersions);
        	    } else {
        	        return true;  // Don't care if Package Versions is null
        	    }
        	} else {
        	    return false;
        	}
    	}
    	
    	protected boolean validateRules(Map<String, String> renamesMap, 
    	                                Map<String, String> versionsMap) {

    	    for ( String entry : versionsMap.keySet() ) {
    	        if ( !renamesMap.containsValue(entry) ) {
    	            dual_error(
    	            	"Version rule key [ " + entry + "]" +
    	                " from [ " + getRuleFileName(AppOption.RULES_VERSIONS, DEFAULT_VERSIONS_REFERENCE) + " ]" +
    	            	" not found in rename rules [ " + getRuleFileName(AppOption.RULES_RENAMES, DEFAULT_RENAMES_REFERENCE) +" ]");
    	            return false;
    	        }
    	    }
    	    return true;
    	}
    	      
        protected String getRuleFileName(AppOption ruleOption, String defaultFileName) {
            String rulesFileName = getOptionValue(ruleOption);
            if ( rulesFileName != null ) {
                return rulesFileName;
            } else {
                return defaultFileName;
            }
        }

    	protected void logRules() {
    		info("Includes:");
    		if ( includes.isEmpty() ) {
    			info("  [ ** NONE ** ]");
    		} else {
    			for ( String include : includes ) {
    				info("  [ " + include + " ]");
    			}
    		}

      		info("Excludes:");
    		if ( excludes.isEmpty() ) {
    			info("  [ ** NONE ** ]");
    		} else {
    			for ( String exclude : excludes ) {
    				info("  [ " + exclude + " ]");
    			}
    		}

    		if ( invert ) {
          		info("Package Renames: [ ** INVERTED ** ]");
    		} else {
          		info("Package Renames:");
    		}

    		if ( packageRenames.isEmpty() ) {
    			info("  [ ** NONE ** ]");
    		} else {
    			for ( Map.Entry<String, String> renameEntry : packageRenames.entrySet() ) {
        			info("  [ " + renameEntry.getKey() + " ]: [ " + renameEntry.getValue() + " ]");
    			}
    		}

    		info("Package Versions:");
    		if ( packageVersions.isEmpty() ) {
    			info("  [ ** NONE ** ]");
    		} else {
    			for ( Map.Entry<String, String> versionEntry : packageVersions.entrySet() ) {
        			info("  [ " + versionEntry.getKey() + " ]: [ " + versionEntry.getValue() + " ]");
    			}
    		}

    		info("Bundle Updates:");
    		if ( bundleUpdates.isEmpty() ) {
    			info("  [ ** NONE ** ]");
    		} else {
    			for ( Map.Entry<String, BundleData> updateEntry : bundleUpdates.entrySet() ) {
    				BundleData updateData = updateEntry.getValue();

    				info("  [ " + updateEntry.getKey() + " ]: [ " + updateData.getSymbolicName() + " ]");

        			info("    [ Version ]: [ " + updateData.getVersion() + " ]");

        			if ( updateData.getAddName() ) {
        				info("    [ Name ]: [ " + BundleData.ADDITIVE_CHAR + updateData.getName() + " ]");
        			} else {
        				info("    [ Name ]: [ " + updateData.getName() + " ]");
        			}

        			if ( updateData.getAddDescription() ) {
        				info("    [ Description ]: [ " + BundleData.ADDITIVE_CHAR + updateData.getDescription() + " ]");
        			} else {
        				info("    [ Description ]: [ " + updateData.getDescription() + " ]");
        			}
    			}
    		}

      		info("Direct strings:");
    		if ( directStrings.isEmpty() ) {
    			info("  [ ** NONE ** ]");
    		} else {
    			for ( Map.Entry<String, String> directEntry : directStrings.entrySet() ) {
    				info( "  [ " + directEntry.getKey() + " ]: [ " + directEntry.getValue() + "]");
    			}
    		}
    	}

    	private SelectionRuleImpl selectionRules;

    	protected SelectionRuleImpl getSelectionRule() {
    		if ( selectionRules == null ) {
    			selectionRules = new SelectionRuleImpl(logger, includes, excludes);
    		}
    		return selectionRules;
    	}

    	private SignatureRuleImpl signatureRules;

    	protected SignatureRuleImpl getSignatureRule() {
    		if ( signatureRules == null ) {
    			signatureRules =  new SignatureRuleImpl(
    				                            logger,
        			                            packageRenames, 
        			                            packageVersions, 
        			                            bundleUpdates,
        			                            specificXmlFileUpdates,
        			                            directStrings);
    		}
    		return signatureRules;
    	}

        public boolean setInput() {
        	String useInputName = getInputFileNameFromCommandLine();
            if ( useInputName == null ) {
                dual_error("No input file was specified");
                return false;
            }

            inputName = FileUtils.normalize(useInputName);
			inputFile = new File(inputName);
            inputPath = inputFile.getAbsolutePath();

            if ( !inputFile.exists() ) {
                dual_error("Input does not exist [ %s ] [ %s ]", inputName, inputPath);
                return false;
            }

            dual_info("Input     [ %s ]", inputName);
            dual_info("          [ %s ]", inputPath);
            return true;
        }

        public static final String OUTPUT_PREFIX = "output_";


//      info("Output file not specified.");
//
//      final String OUTPUT_PREFIX = "output_";
//      String inputFileName = getInputFileName();
//      int indexOfLastSlash = inputFileName.lastIndexOf('/');
//      if (indexOfLastSlash == -1 ) {
//          return OUTPUT_PREFIX + inputFileName; 
//      } else {
//          return inputFileName.substring(0, indexOfLastSlash+1) + OUTPUT_PREFIX + inputFileName.substring(indexOfLastSlash+1);
//      }

        public boolean setOutput() {
        	String useOutputName = getOutputFileNameFromCommandLine();

        	boolean isExplicit;

        	if ( isExplicit = (useOutputName != null) ) {
        		useOutputName = FileUtils.normalize(useOutputName);

        	} else {
        		int indexOfLastSlash = inputName.lastIndexOf('/');
        		if ( indexOfLastSlash == -1 ) {
        			useOutputName = OUTPUT_PREFIX + inputName;
        		} else {
        			String inputPrefix = inputName.substring( 0, indexOfLastSlash + 1 ); 
        			String inputSuffix = inputName.substring( indexOfLastSlash + 1 ); 
        			useOutputName = inputPrefix + OUTPUT_PREFIX + inputSuffix;
        		}
        	}

        	File useOutputFile = new File(useOutputName);
        	String useOutputPath = useOutputFile.getAbsolutePath();

        	boolean putIntoDirectory;

            if ( putIntoDirectory = (inputFile.isFile() && useOutputFile.isDirectory()) ) {
            	useOutputName = useOutputName + '/' + inputName;
            	if ( isVerbose ) {
            		dual_info("Output generated using input name and output directory [ %s ]", useOutputName);
            	}

            	useOutputFile = new File(useOutputName);
            	useOutputPath = useOutputFile.getAbsolutePath();
            }

            String outputCase;
            if ( isExplicit ) {
            	if ( putIntoDirectory ) {
            		outputCase = "Explicit directory";
            	} else {
            		outputCase = "Explicit";
            	}
            } else {
            	if ( putIntoDirectory ) {
            		outputCase = "Directory generated from input";
            	} else {
            		outputCase = "Generated from input";
            	}
            }

            dual_info("Output    [ %s ] (%s)", useOutputName, outputCase);
            dual_info("          [ %s ]", useOutputPath);

            allowOverwrite = hasOption(AppOption.OVERWRITE);
            if ( allowOverwrite) {
            	dual_info("Overwrite of output is enabled");
            }

            if ( useOutputFile.exists() ) {
            	if ( allowOverwrite ) {
                    dual_info("Output exists and will be overwritten [ %s ]", useOutputPath);
            	} else {
            		dual_error("Output already exists [ %s ]", useOutputPath);
            		return false;
            	}
            } else {
            	if ( allowOverwrite ) {
            		if ( isVerbose ) {
            			dual_info("Overwritten specified, but output [ %s ] does not exist", useOutputPath);
            		}
            	}
            }

            outputName = useOutputName;
            outputFile = useOutputFile;
            outputPath = useOutputPath;

            return true;
        }

        public CompositeActionImpl getRootAction() {
        	if ( rootAction == null ) {
        		CompositeActionImpl useRootAction = new CompositeActionImpl(
                     getLogger(), getBuffer(), getSelectionRule(), getSignatureRule() );

        		DirectoryActionImpl directoryAction =
        			useRootAction.addUsing( DirectoryActionImpl::new );

        		ClassActionImpl classAction =
        			useRootAction.addUsing( ClassActionImpl::new );
        		JavaActionImpl javaAction =
        			useRootAction.addUsing( JavaActionImpl::new );
        		ServiceLoaderConfigActionImpl serviceConfigAction =
        			useRootAction.addUsing( ServiceLoaderConfigActionImpl::new );
        		ManifestActionImpl manifestAction =
        			useRootAction.addUsing( ManifestActionImpl::newManifestAction );
        		ManifestActionImpl featureAction =
        			useRootAction.addUsing( ManifestActionImpl::newFeatureAction );

        		JarActionImpl jarAction =
                	useRootAction.addUsing( JarActionImpl::new );
        		WarActionImpl warAction =
                	useRootAction.addUsing( WarActionImpl::new );
        		RarActionImpl rarAction =
                	useRootAction.addUsing( RarActionImpl::new );
        		EarActionImpl earAction =
                	useRootAction.addUsing( EarActionImpl::new );

        		XmlActionImpl xmlAction =
                        useRootAction.addUsing( XmlActionImpl::new );
        		
        		ZipActionImpl zipAction =
        			useRootAction.addUsing( ZipActionImpl::new );

        		NullActionImpl nullAction =
        			useRootAction.addUsing( NullActionImpl::new );

        		// Directory actions know about all actions except for directory actions.

        		directoryAction.addAction(classAction);
        		directoryAction.addAction(javaAction);
        		directoryAction.addAction(serviceConfigAction);
        		directoryAction.addAction(manifestAction);
        		directoryAction.addAction(featureAction);
        		directoryAction.addAction(zipAction);
        		directoryAction.addAction(jarAction);
        		directoryAction.addAction(warAction);
        		directoryAction.addAction(rarAction);
        		directoryAction.addAction(earAction);
        		directoryAction.addAction(xmlAction);
        		directoryAction.addAction(nullAction);

        		jarAction.addAction(classAction);
        		jarAction.addAction(javaAction);
        		jarAction.addAction(serviceConfigAction);
        		jarAction.addAction(manifestAction);
        		jarAction.addAction(featureAction);
        		jarAction.addAction(xmlAction);
        		jarAction.addAction(nullAction);

        		warAction.addAction(classAction);
        		warAction.addAction(javaAction);
        		warAction.addAction(serviceConfigAction);
        		warAction.addAction(manifestAction);
        		warAction.addAction(featureAction);
        		warAction.addAction(jarAction);
        		warAction.addAction(xmlAction);
        		warAction.addAction(nullAction);

        		rarAction.addAction(classAction);
        		rarAction.addAction(javaAction);
        		rarAction.addAction(serviceConfigAction);
        		rarAction.addAction(manifestAction);
        		rarAction.addAction(featureAction);
        		rarAction.addAction(jarAction);
        		rarAction.addAction(xmlAction);
        		rarAction.addAction(nullAction);

        		earAction.addAction(manifestAction);
        		earAction.addAction(jarAction);
        		earAction.addAction(warAction);
        		earAction.addAction(rarAction);
        		earAction.addAction(xmlAction);
        		earAction.addAction(nullAction);

        		zipAction.addAction(classAction);
        		zipAction.addAction(javaAction);
        		zipAction.addAction(serviceConfigAction);
        		zipAction.addAction(manifestAction);
        		zipAction.addAction(featureAction);
        		zipAction.addAction(jarAction);
        		zipAction.addAction(warAction);
        		zipAction.addAction(rarAction);
        		zipAction.addAction(earAction);
        		zipAction.addAction(xmlAction);
        		zipAction.addAction(nullAction);

        		rootAction = useRootAction;
            }

            return rootAction;
        }

        public boolean acceptAction() {
        	String actionName = getOptionValue(AppOption.FILE_TYPE);
        	if ( actionName != null ) {
        		for ( ActionImpl action : getRootAction().getActions() ) {
        			if ( action.getActionType().matches(actionName) ) {
        				dual_info("Forced action [ %s ] [ %s ]", actionName, action.getName());
        				acceptedAction = action;
        				return true;
        			}
        		}
        		dual_error("No match for forced action [ %s ]", actionName);
        		return false;

        	} else {
        		acceptedAction = getRootAction().acceptAction(inputName,  inputFile);
        		if ( acceptedAction == null ) {
        			dual_error("No action selected for input [ %s ]", inputName);
        			return false;
        		} else {
        			dual_info("Action selected for input [ %s ]: %s", inputName, acceptedAction.getName());
        			return true;
        		}
        	}
        }

        public void transform()
        	throws TransformException {

        	acceptedAction.apply(inputName, inputFile, outputFile);

        	if ( isTerse ) {
        		if ( !toSysOut && !toSysErr ) {
        			acceptedAction.getChanges().displayTerse( getSystemOut(), inputPath, outputPath );
        		}
        		acceptedAction.getChanges().displayTerse( getLogger(), inputPath, outputPath );
        	} else if ( isVerbose ) {
        		if ( !toSysOut && !toSysErr ) {
        			acceptedAction.getChanges().displayVerbose( getSystemOut(), inputPath, outputPath );
        		}
        		acceptedAction.getChanges().displayVerbose( getLogger(), inputPath, outputPath );
    		} else {
        		if ( !toSysOut && !toSysErr ) {
        			acceptedAction.getChanges().display( getSystemOut(), inputPath, outputPath );
        		}
        		acceptedAction.getChanges().display( getLogger(), inputPath, outputPath );
    		}
        }
    }

    public int run() {
        try {
            setParsedArgs();
        } catch ( ParseException e ) {
            errorPrint("Exception parsing command line arguments: %s", e);
            help( getSystemOut() );
            return PARSE_ERROR_RC;
        }

        if ( hasOption(AppOption.HELP) || hasOption(AppOption.USAGE) ) {
            help( getSystemOut() );
            // TODO: Split help and usage
            return SUCCESS_RC; // TODO: Is this the correct return value?
        }

        TransformOptions options = createTransformOptions();

        try {
        	options.setLogging();
        } catch ( Exception e ) {
            errorPrint("Logger settings error: %s", e);
            return LOGGER_SETTINGS_ERROR_RC;
        }
        detectLogFile();

        if ( !options.setInput() ) { 
            return TRANSFORM_ERROR_RC;
        }

        if ( !options.setOutput() ) {
            return TRANSFORM_ERROR_RC;
        }

        boolean loadedRules;
        try {
        	loadedRules = options.setRules();
        } catch ( Exception e ) {
            dual_error("Exception loading rules: %s", e);
            return RULES_ERROR_RC;
        }
        if ( !loadedRules ) {
        	dual_error("Transformation rules cannot be used");
        	return RULES_ERROR_RC;
        }
        if ( options.isVerbose ) {
        	options.logRules();
        }

        if ( !options.acceptAction() ) {
        	dual_error("No action selected");
        	return FILE_TYPE_ERROR_RC;
        }

        try {
        	options.transform(); // throws JakartaTransformException
        } catch ( TransformException e ) {
            dual_error("Transform failure: %s", e);
            return TRANSFORM_ERROR_RC;
        } catch ( Throwable th) {
        	dual_error("Unexpected failure: %s", th);
            return TRANSFORM_ERROR_RC;
        }

        return SUCCESS_RC;
    }
}
