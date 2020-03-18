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

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.eclipse.transformer.action.impl.JavaActionImpl;
import org.eclipse.transformer.action.impl.ManifestActionImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.transformer.Transformer;
import org.eclipse.transformer.Transformer.TransformOptions;

class TestCommandLine {
    
    String currentDirectory = ".";
    final String DATA_DIR = "src/test/data/";

    @BeforeEach
    public void setUp() {
        currentDirectory = System.getProperty("user.dir");
        System.out.println("setUp: Current directory is: [" + currentDirectory + "]");
    }

    @Test
    void testManifestActionAccepted() throws Exception {   
        String inputFileName = DATA_DIR + "MANIFEST.MF";
        String outputFileName = DATA_DIR + "output_MANIFEST.MF";
        verifyAction(ManifestActionImpl.class.getName(), inputFileName, outputFileName);
    }

    @Test
    void testJavaActionAccepted() throws Exception {
        String inputFileName = DATA_DIR + "A.java";
        String outputFileName = DATA_DIR + "output_A.java";
        verifyAction(JavaActionImpl.class.getName(), inputFileName, outputFileName);
    }

    private void verifyAction(
    	String actionClassName,
    	String inputFileName, String outputFileName) throws Exception {

        Transformer t = new Transformer(System.out, System.err);

        String[] args = new String[] { inputFileName, "-o"};

        t.setArgs(args);
        t.setParsedArgs();

        TransformOptions options = t.getTransformOptions();

        // SET INPUT
        assertTrue(options.setInput(), "options.setInput() failed");
        assertEquals(inputFileName, options.getInputFileName(), 
        	"input file name is not correct [" + options.getInputFileName() + "]");

        // SET OUTPUT
        assertTrue(options.setOutput(), "options.setOutput() failed");
        assertEquals(outputFileName, options.getOutputFileName(), 
        	"output file name is not correct [" + options.getOutputFileName() + "]");

        assertTrue(options.setRules(), "options.setRules() failed");
        assertTrue(options.acceptAction(), "options.acceptAction() failed");
        assertEquals(actionClassName, options.acceptedAction.getClass().getName());

        options.transform();
        assertTrue((new File(outputFileName)).exists(), "output file not created");            }
}
