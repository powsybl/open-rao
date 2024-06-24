/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Map;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class NetworkActionArrayDeserializer {
    private NetworkActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", NETWORK_ACTIONS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            NetworkActionAdder networkActionAdder = crac.newNetworkAction();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case ID:
                        networkActionAdder.withId(jsonParser.nextTextValue());
                        break;
                    case NAME:
                        networkActionAdder.withName(jsonParser.nextTextValue());
                        break;
                    case OPERATOR:
                        networkActionAdder.withOperator(jsonParser.nextTextValue());
                        break;
                    case ON_INSTANT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnInstantArrayDeserializer.deserialize(jsonParser, networkActionAdder);
                        break;
                    case FREE_TO_USE_USAGE_RULES:
                        deserializeFreeToUseUsageRules(jsonParser, version, networkActionAdder);
                        break;
                    case ON_CONTINGENCY_STATE_USAGE_RULES:
                        jsonParser.nextToken();
                        OnStateArrayDeserializer.deserialize(jsonParser, networkActionAdder);
                        break;
                    case ON_STATE_USAGE_RULES:
                        deserializeOnStateUsageRules(jsonParser, version, networkActionAdder);
                        break;
                    case ON_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnConstraintArrayDeserializer.deserialize(jsonParser, networkActionAdder, version);
                        break;
                    case ON_FLOW_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        deserializeOlderOnConstraintUsageRules(jsonParser, ON_FLOW_CONSTRAINT_USAGE_RULES, version, networkActionAdder);
                        break;
                    case ON_ANGLE_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        deserializeOlderOnConstraintUsageRules(jsonParser, ON_ANGLE_CONSTRAINT_USAGE_RULES, version, networkActionAdder);
                        break;
                    case ON_VOLTAGE_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        deserializeOlderOnConstraintUsageRules(jsonParser, ON_VOLTAGE_CONSTRAINT_USAGE_RULES, version, networkActionAdder);
                        break;
                    case ON_FLOW_CONSTRAINT_IN_COUNTRY_USAGE_RULES:
                        jsonParser.nextToken();
                        OnFlowConstraintInCountryArrayDeserializer.deserialize(jsonParser, networkActionAdder, version);
                        break;
                    case TOPOLOGICAL_ACTIONS:
                        jsonParser.nextToken();
                        TopologicalActionArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId);
                        break;
                    case PST_SETPOINTS:
                        jsonParser.nextToken();
                        PstSetpointArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId);
                        break;
                    case INJECTION_SETPOINTS:
                        jsonParser.nextToken();
                        InjectionSetpointArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId, version);
                        break;
                    case SWITCH_PAIRS:
                        jsonParser.nextToken();
                        SwitchPairArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId);
                        break;
                    case EXTENSIONS:
                        throw new OpenRaoException("Extensions are deprecated since CRAC version 1.7");
                    case SPEED:
                        jsonParser.nextToken();
                        networkActionAdder.withSpeed(jsonParser.getIntValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in NetworkAction: " + jsonParser.getCurrentName());
                }
            }
            networkActionAdder.add();
        }
    }

    private static void deserializeOnStateUsageRules(JsonParser jsonParser, String version, NetworkActionAdder networkActionAdder) throws IOException {
        if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 5) {
            throw new OpenRaoException("OnState has been renamed to OnContingencyState since CRAC version 1.6");
        } else {
            jsonParser.nextToken();
            OnStateArrayDeserializer.deserialize(jsonParser, networkActionAdder);
        }
    }

    private static void deserializeFreeToUseUsageRules(JsonParser jsonParser, String version, NetworkActionAdder networkActionAdder) throws IOException {
        if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 5) {
            throw new OpenRaoException("FreeToUse has been renamed to OnInstant since CRAC version 1.6");
        } else {
            jsonParser.nextToken();
            OnInstantArrayDeserializer.deserialize(jsonParser, networkActionAdder);
        }
    }

    private static void deserializeOlderOnConstraintUsageRules(JsonParser jsonParser, String keyword, String version, NetworkActionAdder networkActionAdder) throws IOException {
        if (getPrimaryVersionNumber(version) < 2 || getPrimaryVersionNumber(version) == 2 && getSubVersionNumber(version) < 4) {
            OnConstraintArrayDeserializer.deserialize(jsonParser, networkActionAdder, version);
        } else {
            throw new OpenRaoException("Unsupported field %s in CRAC version >= 2.4".formatted(keyword));
        }
    }
}
