/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_io_json.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class NetworkActionArrayDeserializer {
    private NetworkActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new FaraoException(String.format("Cannot deserialize %s before %s", NETWORK_ACTIONS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            NetworkActionAdder networkActionAdder = crac.newNetworkAction();
            List<Extension<NetworkAction>> extensions = new ArrayList<>();
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
                    case FREE_TO_USE_USAGE_RULES:
                        jsonParser.nextToken();
                        FreeToUseArrayDeserializer.deserialize(jsonParser, networkActionAdder);
                        break;
                    case ON_STATE_USAGE_RULES:
                        jsonParser.nextToken();
                        OnStateArrayDeserializer.deserialize(jsonParser, networkActionAdder);
                        break;
                    case ON_FLOW_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnFlowConstraintArrayDeserializer.deserialize(jsonParser, networkActionAdder);
                        break;
                    case ON_ANGLE_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnAngleConstraintArrayDeserializer.deserialize(jsonParser, networkActionAdder);
                        break;
                    case ON_FLOW_CONSTRAINT_IN_COUNTRY_USAGE_RULES:
                        jsonParser.nextToken();
                        OnFlowConstraintInCountryArrayDeserializer.deserialize(jsonParser, networkActionAdder);
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
                        InjectionSetpointArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId);
                        break;
                    case SWITCH_PAIRS:
                        jsonParser.nextToken();
                        SwitchPairArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId);
                        break;
                    case EXTENSIONS:
                        jsonParser.nextToken();
                        extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                        break;
                    default:
                        throw new FaraoException("Unexpected field in NetworkAction: " + jsonParser.getCurrentName());
                }
            }
            NetworkAction networkAction = networkActionAdder.add();
            if (!extensions.isEmpty()) {
                ExtensionsHandler.getExtensionsSerializers().addExtensions(networkAction, extensions);
            }
        }
    }
}
