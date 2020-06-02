/** ******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: (EPL-2.0 OR Apache-2.0)
 ******************************************************************************* */
package transformer.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.TldActionImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.eclipse.transformer.util.InputStreamData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import transformer.test.util.CaptureLoggerImpl;

public class TestTransformTld extends CaptureTest {

    public SelectionRuleImpl createSelectionRule(
            CaptureLoggerImpl useLogger,
            Set<String> useIncludes,
            Set<String> useExcludes) {

        return new SelectionRuleImpl(useLogger, useIncludes, useExcludes);
    }

    public SignatureRuleImpl createSignatureRule(
            CaptureLoggerImpl useLogger,
            Map<String, Map<String, String>> masterXmlUpdates) {

        return new SignatureRuleImpl(
                useLogger,
                null, null,
                null,
                masterXmlUpdates,
                null);
    }

    //
    public static final String TEST_DATA_PATH = "transformer/test/data/tld";

    public static final String JAVAX_TLD_PATH = TEST_DATA_PATH + "/" + "META-INF/test-javax.tld";

    public static final String JAKARTA_SERVLET = "jakarta.servlet";

    public static final String JAVAX_SERVLET = "javax.servlet";

    public static final String ALL_TLD = "*.tld";

    protected Set<String> includes;

    public Set<String> getIncludes() {
        if (includes == null) {
            includes = new HashSet<String>();
            includes.add(JAVAX_TLD_PATH);
        }

        return includes;
    }

    public Set<String> getExcludes() {
        return Collections.emptySet();
    }

    public Map<String, Map<String, String>> masterXmlUpdates;

    public Map<String, Map<String, String>> getMasterXmlUpdates() {
        if (masterXmlUpdates == null) {
            Map<String, Map<String, String>> useTldUpdates = new HashMap<String, Map<String, String>>(2);

            Map<String, String> tldUpdates = new HashMap<String, String>(1);
            tldUpdates.put(JAVAX_SERVLET, JAKARTA_SERVLET);
            useTldUpdates.put(ALL_TLD, tldUpdates);
            masterXmlUpdates = useTldUpdates;
        }

        return masterXmlUpdates;
    }

    public TldActionImpl jakartaServiceAction;

    public TldActionImpl getJakartaTldAction() {
        if (jakartaServiceAction == null) {
            CaptureLoggerImpl useLogger = getCaptureLogger();

            jakartaServiceAction = new TldActionImpl(
                    useLogger, false, false,
                    createBuffer(),
                    createSelectionRule(useLogger, getIncludes(), getExcludes()),
                    createSignatureRule(useLogger, getMasterXmlUpdates()));
        }
        return jakartaServiceAction;
    }

    @Test
    public void testJakartaTransform() throws IOException, TransformException {
        TldActionImpl tldAction = getJakartaTldAction();

        verifyTransform(
                tldAction,
                JAVAX_TLD_PATH);
    }

    protected void verifyTransform(
            TldActionImpl action,
            String inputName) throws IOException, TransformException {

        InputStream inputStream = TestUtils.getResourceStream(inputName);

        InputStreamData transformedData;
        try {
            transformedData = action.apply(inputName, inputStream);
        } finally {
            inputStream.close();
        }

        List<String> transformedLines = TestUtils.loadLines(transformedData.stream);
        for(String l : transformedLines) {
            Assertions.assertFalse(l.contains(JAVAX_SERVLET));
        }
    }

}
