/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresultjson.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresultimpl.VoltageCnecResult;
import com.powsybl.openrao.data.raoresultimpl.ElementaryVoltageCnecResult;
import com.powsybl.openrao.data.raoresultimpl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.powsybl.openrao.data.raoresultjson.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class VoltageCnecResultArrayDeserializer {

    private VoltageCnecResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac, String jsonFileVersion) throws IOException {

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(VOLTAGECNEC_ID)) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", VOLTAGECNEC_RESULTS, VOLTAGECNEC_ID));
            }

            String voltageCnecId = jsonParser.nextTextValue();
            VoltageCnec voltageCnec = crac.getVoltageCnec(voltageCnecId);

            if (voltageCnec == null) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: voltageCnec with id %s does not exist in the Crac", voltageCnecId));
            }
            VoltageCnecResult voltageCnecResult = raoResult.getAndCreateIfAbsentVoltageCnecResult(voltageCnec);
            deserializeVoltageCnecResult(jsonParser, voltageCnecResult, jsonFileVersion, crac);
        }
    }

    private static void deserializeVoltageCnecResult(JsonParser jsonParser, VoltageCnecResult voltageCnecResult, String jsonFileVersion, Crac crac) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            ElementaryVoltageCnecResult eVoltageCnecResult;
            Instant optimizedInstant = deserializeOptimizedInstant(jsonParser.getCurrentName(), jsonFileVersion, crac);
            jsonParser.nextToken();
            eVoltageCnecResult = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(optimizedInstant);
            deserializeElementaryVoltageCnecResult(jsonParser, eVoltageCnecResult);
        }
    }

    private static void deserializeElementaryVoltageCnecResult(JsonParser jsonParser, ElementaryVoltageCnecResult eVoltageCnecResult) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            if (!jsonParser.getCurrentName().equals(KILOVOLT_UNIT)) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", VOLTAGECNEC_RESULTS, jsonParser.getCurrentName()));
            } else {
                jsonParser.nextToken();
                deserializeElementaryVoltageCnecResultForUnit(jsonParser, eVoltageCnecResult, Unit.KILOVOLT);
            }
        }
    }

    private static void deserializeElementaryVoltageCnecResultForUnit(JsonParser jsonParser, ElementaryVoltageCnecResult eVoltageCnecResult, Unit unit) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case VOLTAGE:
                    jsonParser.nextToken();
                    eVoltageCnecResult.setVoltage(jsonParser.getDoubleValue(), unit);
                    break;
                case MARGIN:
                    jsonParser.nextToken();
                    eVoltageCnecResult.setMargin(jsonParser.getDoubleValue(), unit);
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", VOLTAGECNEC_RESULTS, jsonParser.getCurrentName()));
            }
        }
    }
}
