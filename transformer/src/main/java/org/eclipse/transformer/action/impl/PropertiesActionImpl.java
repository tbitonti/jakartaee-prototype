/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.eclipse.transformer.action.impl;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.util.ByteData;
import org.slf4j.Logger;
/**
 *
 * @author jdenise
 */
public class PropertiesActionImpl extends ActionImpl {

    public PropertiesActionImpl(
            Logger logger, boolean isTerse, boolean isVerbose,
            InputBufferImpl buffer,
            SelectionRuleImpl selectionRule,
            SignatureRuleImpl signatureRule) {

        super(logger, isTerse, isVerbose, buffer, selectionRule, signatureRule);
    }

    @Override
    public String getAcceptExtension() {
        return ".properties";
    }

    @Override
    protected ByteData apply(String inputName, byte[] inputBytes, int inputLength) throws TransformException {

        String outputName = transformBinaryType(inputName);
        if (outputName != null) {
            verbose("Properties file %s, relocated to %s", inputName, outputName);
            setResourceNames(inputName, outputName);
            return new ByteData(outputName, inputBytes, 0, inputLength);
        } else {
            setResourceNames(inputName, inputName);
            return new ByteData(inputName, inputBytes, 0, inputLength);
        }
    }

    public String getName() {
        return "Properties file relocate";
    }

    public ActionType getActionType() {
        return ActionType.PROPERTIES;
    }

}
