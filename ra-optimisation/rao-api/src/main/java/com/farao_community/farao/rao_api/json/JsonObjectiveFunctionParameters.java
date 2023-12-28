/*
 *  Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.ObjectiveFunctionParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

import static com.farao_community.farao.rao_api.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class JsonObjectiveFunctionParameters {

    private JsonObjectiveFunctionParameters() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(OBJECTIVE_FUNCTION);
        jsonGenerator.writeObjectField(TYPE, parameters.getObjectiveFunctionParameters().getType());
        jsonGenerator.writeBooleanField(FORBID_COST_INCREASE, parameters.getObjectiveFunctionParameters().getForbidCostIncrease());
        jsonGenerator.writeObjectField(PREVENTIVE_STOP_CRITERION, parameters.getObjectiveFunctionParameters().getPreventiveStopCriterion());
        jsonGenerator.writeObjectField(CURATIVE_STOP_CRITERION, parameters.getObjectiveFunctionParameters().getCurativeStopCriterion());
        jsonGenerator.writeNumberField(CURATIVE_MIN_OBJ_IMPROVEMENT, parameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement());
        jsonGenerator.writeEndObject();
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case TYPE:
                    raoParameters.getObjectiveFunctionParameters().setType(stringToObjectiveFunction(jsonParser.nextTextValue()));
                    break;
                case FORBID_COST_INCREASE:
                    jsonParser.nextToken();
                    raoParameters.getObjectiveFunctionParameters().setForbidCostIncrease(jsonParser.getBooleanValue());
                    break;
                case PREVENTIVE_STOP_CRITERION:
                    raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(stringToPreventiveStopCriterion(jsonParser.nextTextValue()));
                    break;
                case CURATIVE_STOP_CRITERION:
                    raoParameters.getObjectiveFunctionParameters().setCurativeStopCriterion(stringToCurativeStopCriterion(jsonParser.nextTextValue()));
                    break;
                case CURATIVE_MIN_OBJ_IMPROVEMENT:
                    jsonParser.nextToken();
                    raoParameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(jsonParser.getValueAsDouble());
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize objective function parameters: unexpected field in %s (%s)", OBJECTIVE_FUNCTION, jsonParser.getCurrentName()));
            }
        }
    }

    private static ObjectiveFunctionParameters.ObjectiveFunctionType stringToObjectiveFunction(String string) {
        try {
            return ObjectiveFunctionParameters.ObjectiveFunctionType.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown objective function type value: %s", string));
        }
    }

    private static ObjectiveFunctionParameters.PreventiveStopCriterion stringToPreventiveStopCriterion(String string) {
        try {
            return ObjectiveFunctionParameters.PreventiveStopCriterion.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown preventive stop criterion: %s", string));
        }
    }

    private static ObjectiveFunctionParameters.CurativeStopCriterion stringToCurativeStopCriterion(String string) {
        try {
            return ObjectiveFunctionParameters.CurativeStopCriterion.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown curative stop criterion: %s", string));
        }
    }

}
