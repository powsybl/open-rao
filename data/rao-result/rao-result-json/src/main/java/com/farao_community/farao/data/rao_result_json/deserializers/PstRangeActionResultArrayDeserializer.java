/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.rao_result_impl.PstRangeActionResult;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;
import static com.farao_community.farao.data.rao_result_json.deserializers.DeprecatedRaoResultJsonConstants.PST_NETWORKELEMENT_ID;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class PstRangeActionResultArrayDeserializer {

    private PstRangeActionResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac, String jsonFileVersion) throws IOException {

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(PSTRANGEACTION_ID)) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", PSTRANGEACTION_RESULTS, PSTRANGEACTION_ID));
            }

            String pstRangeActionId = jsonParser.nextTextValue();
            PstRangeAction pstRangeAction = crac.getPstRangeAction(pstRangeActionId);

            if (pstRangeAction == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: cannot deserialize RaoResult: pstRangeAction with id %s does not exist in the Crac", pstRangeActionId));
            }

            PstRangeActionResult pstRangeActionResult = (PstRangeActionResult) raoResult.getAndCreateIfAbsentRangeActionResult(pstRangeAction);
            Integer afterPraTap = null;
            Double afterPraSetpoint = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    case PST_NETWORKELEMENT_ID:
                        readPstNetworkElementId(jsonParser, jsonFileVersion);
                        break;

                    case INITIAL_TAP:
                        jsonParser.nextToken();
                        pstRangeActionResult.setPreOptimTap(jsonParser.getIntValue());
                        break;

                    case INITIAL_SETPOINT:
                        jsonParser.nextToken();
                        pstRangeActionResult.setPreOptimSetPoint(jsonParser.getDoubleValue());
                        break;

                    case AFTER_PRA_TAP:
                        jsonParser.nextToken();
                        afterPraTap = jsonParser.getIntValue();
                        break;

                    case AFTER_PRA_SETPOINT:
                        jsonParser.nextToken();
                        afterPraSetpoint = jsonParser.getDoubleValue();
                        break;

                    case STATES_ACTIVATED:
                        jsonParser.nextToken();
                        deserializeResultsPerStates(jsonParser, pstRangeActionResult, crac);
                        break;

                    default:
                        throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", PSTRANGEACTION_RESULTS, jsonParser.getCurrentName()));
                }
            }
            // Do this at the end: for PSTs with afterPraTap and afterPraSetpoint, initial tap/setpoint should be set to afterPra values
            if (afterPraTap != null && afterPraSetpoint != null) {
                pstRangeActionResult.setPreOptimTap(afterPraTap);
                pstRangeActionResult.setPreOptimSetPoint(afterPraSetpoint);
            }
        }
    }

    private static void readPstNetworkElementId(JsonParser jsonParser, String jsonFileVersion) throws IOException {
        // only used in version <=1.1
        // keep here for retrocompatibility, but information is not used anymore
        if (getPrimaryVersionNumber(jsonFileVersion) > 1 || getSubVersionNumber(jsonFileVersion) > 1) {
            throw new FaraoException(String.format("Cannot deserialize RaoResult: field %s in %s in not supported in file version %s", jsonParser.getCurrentName(), PSTRANGEACTION_RESULTS, jsonFileVersion));
        } else {
            jsonParser.nextTextValue();
        }
    }

    private static void deserializeResultsPerStates(JsonParser jsonParser, PstRangeActionResult pstRangeActionResult, Crac crac) throws IOException {

        Instant instant = null;
        String contingencyId = null;
        Double setpoint = null;
        Integer tap = null;

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    case INSTANT:
                        instant = deserializeInstant(jsonParser.nextTextValue());
                        break;

                    case CONTINGENCY_ID:
                        contingencyId = jsonParser.nextTextValue();
                        break;

                    case TAP:
                        jsonParser.nextToken();
                        tap = jsonParser.getIntValue();
                        break;

                    case SETPOINT:
                        jsonParser.nextToken();
                        setpoint = jsonParser.getDoubleValue();
                        break;

                    default:
                        throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", PSTRANGEACTION_RESULTS, jsonParser.getCurrentName()));
                }
            }

            if (setpoint == null || tap == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: tap and setpoint are required in %s", PSTRANGEACTION_RESULTS));
            }
            pstRangeActionResult.addActivationForState(StateDeserializer.getState(instant, contingencyId, crac, PSTRANGEACTION_RESULTS), tap, setpoint);

        }
    }
}
