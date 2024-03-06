/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.anglemonitoring.json;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.monitoring.anglemonitoring.AngleMonitoringResult;
import com.powsybl.openrao.monitoring.monitoringcommon.json.MonitoringCommonDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.monitoring.anglemonitoring.json.JsonAngleMonitoringResultConstants.*;
import static com.powsybl.openrao.monitoring.monitoringcommon.json.JsonCommonMonitoringResultConstants.*;
import static com.powsybl.openrao.monitoring.monitoringcommon.json.MonitoringCommonDeserializer.getState;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class AngleMonitoringResultDeserializer extends JsonDeserializer<AngleMonitoringResult> {
    private static final String UNEXPECTED_FIELD_ERROR = "Unexpected field %s in %s";

    private Crac crac;

    private AngleMonitoringResultDeserializer() {
        // should not be used
    }

    public AngleMonitoringResultDeserializer(Crac crac) {
        this.crac = crac;
    }

    @Override
    public AngleMonitoringResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String firstFieldName = jsonParser.nextFieldName();
        if (!firstFieldName.equals(TYPE) || !jsonParser.nextTextValue().equals(ANGLE_MONITORING_RESULT)) {
            throw new OpenRaoException(String.format("Type of document must be specified at the beginning as %s", ANGLE_MONITORING_RESULT));
        }
        AngleMonitoringResult.Status status = null;
        String secondFieldName = jsonParser.nextFieldName();
        if (!secondFieldName.equals(STATUS)) {
            throw new OpenRaoException("Status must be specified right after type of document.");
        } else {
            status = readStatus(jsonParser);
        }

        Set<AngleMonitoringResult.AngleResult> angleResults = new HashSet<>();
        Map<State, Set<RemedialAction<?>>> appliedCras = new HashMap<>();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            if (jsonParser.getCurrentName().equals(ANGLE_VALUES)) {
                jsonParser.nextToken();
                readAngleValues(jsonParser, angleResults);
            } else if (jsonParser.getCurrentName().equals(APPLIED_CRAS)) {
                jsonParser.nextToken();
                MonitoringCommonDeserializer.readAppliedRas(jsonParser, appliedCras, crac);
            } else {
                throw new OpenRaoException(String.format(UNEXPECTED_FIELD_ERROR, jsonParser.getCurrentName(), ANGLE_MONITORING_RESULT));
            }
        }
        return new AngleMonitoringResult(angleResults, appliedCras, status);
    }

    private AngleMonitoringResult.Status readStatus(JsonParser jsonParser) throws IOException {
        String statusString = jsonParser.nextTextValue();
        try {
            return AngleMonitoringResult.Status.valueOf(statusString);
        } catch (IllegalArgumentException e) {
            throw new OpenRaoException(String.format("Unhandled status : %s", statusString));
        }
    }

    private void readAngleValues(JsonParser jsonParser, Set<AngleMonitoringResult.AngleResult> angleResults) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String contingencyId = null;
            Instant instant = null;
            String cnecId = null;
            Double quantity = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.currentName()) {
                    case INSTANT:
                        instant = crac.getInstant(jsonParser.nextTextValue());
                        break;
                    case CONTINGENCY:
                        contingencyId = jsonParser.nextTextValue();
                        break;
                    case CNEC_ID:
                        cnecId = jsonParser.nextTextValue();
                        break;
                    case QUANTITY:
                        jsonParser.nextToken();
                        quantity = jsonParser.getDoubleValue();
                        break;
                    default:
                        throw new OpenRaoException(String.format(UNEXPECTED_FIELD_ERROR, jsonParser.currentName(), ANGLE_VALUES));
                }
            }
            if (instant == null || cnecId == null || quantity == null) {
                throw new OpenRaoException(String.format("Instant, CNEC ID and quantity must be defined in %s", ANGLE_VALUES));
            }
            AngleCnec angleCnec = crac.getAngleCnec(cnecId);
            if (angleCnec == null) {
                throw new OpenRaoException(String.format("AngleCnec %s does not exist in the CRAC", cnecId));
            }
            State state = getState(instant, contingencyId, crac);
            if (angleResults.stream().anyMatch(angleResult -> angleResult.getAngleCnec().equals(angleCnec) &&
                    angleResult.getState().equals(state))) {
                throw new OpenRaoException(String.format("Angle values for AngleCnec %s, instant %s and contingency %s are defined more than once", cnecId, instant.getId(), contingencyId));
            }
            angleResults.add(new AngleMonitoringResult.AngleResult(angleCnec, quantity));
        }
    }
}
