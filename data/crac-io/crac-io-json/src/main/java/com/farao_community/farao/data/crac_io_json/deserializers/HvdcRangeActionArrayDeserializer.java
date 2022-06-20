/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeActionAdder;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
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
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class HvdcRangeActionArrayDeserializer {
    private HvdcRangeActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new FaraoException(String.format("Cannot deserialize %s before %s", HVDC_RANGE_ACTIONS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            HvdcRangeActionAdder adder = crac.newHvdcRangeAction();
            List<Extension<RangeAction>> extensions = new ArrayList<>();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case ID:
                        adder.withId(jsonParser.nextTextValue());
                        break;
                    case NAME:
                        adder.withName(jsonParser.nextTextValue());
                        break;
                    case OPERATOR:
                        adder.withOperator(jsonParser.nextTextValue());
                        break;
                    case FREE_TO_USE_USAGE_RULES:
                        jsonParser.nextToken();
                        FreeToUseArrayDeserializer.deserialize(jsonParser, adder);
                        break;
                    case ON_STATE_USAGE_RULES:
                        jsonParser.nextToken();
                        OnStateArrayDeserializer.deserialize(jsonParser, adder);
                        break;
                    case ON_FLOW_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnFlowConstraintArrayDeserializer.deserialize(jsonParser, adder);
                        break;
                    case ON_FLOW_CONSTRAINT_IN_COUNTRY_USAGE_RULES:
                        jsonParser.nextToken();
                        OnFlowConstraintInCountryArrayDeserializer.deserialize(jsonParser, adder);
                        break;
                    case NETWORK_ELEMENT_ID:
                        String networkElementId = jsonParser.nextTextValue();
                        if (networkElementsNamesPerId.containsKey(networkElementId)) {
                            adder.withNetworkElement(networkElementId, networkElementsNamesPerId.get(networkElementId));
                        } else {
                            adder.withNetworkElement(networkElementId);
                        }
                        break;
                    case GROUP_ID:
                        adder.withGroupId(jsonParser.nextTextValue());
                        break;
                    case INITIAL_SETPOINT:
                        jsonParser.nextToken();
                        adder.withInitialSetpoint(jsonParser.getDoubleValue());
                        break;
                    case RANGES:
                        jsonParser.nextToken();
                        StandardRangeArrayDeserializer.deserialize(jsonParser, adder);
                        break;
                    case EXTENSIONS:
                        jsonParser.nextToken();
                        extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                        break;
                    case SPEED:
                        jsonParser.nextToken();
                        adder.withSpeed(jsonParser.getIntValue());
                        break;
                    default:
                        throw new FaraoException("Unexpected field in HvdcRangeAction: " + jsonParser.getCurrentName());
                }
            }
            if (getPrimaryVersionNumber(version) <= 1 && getSubVersionNumber(version) < 3) {
                // initial setpoint was not exported then, set default value to 0 to avoid errors
                adder.withInitialSetpoint(0);
            }
            RangeAction hvdcRangeAction = adder.add();
            if (!extensions.isEmpty()) {
                ExtensionsHandler.getExtensionsSerializers().addExtensions(hvdcRangeAction, extensions);
            }
        }
    }
}
