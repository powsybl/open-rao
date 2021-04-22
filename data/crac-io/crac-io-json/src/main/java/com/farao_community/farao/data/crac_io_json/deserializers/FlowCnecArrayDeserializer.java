/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.ExtensionsHandler;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
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
public final class FlowCnecArrayDeserializer {

    private FlowCnecArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new FaraoException(String.format("Cannot deserialize %s before %s", FLOW_CNECS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            FlowCnecAdder adder = crac.newFlowCnec();
            List<Extension<FlowCnec>> extensions = new ArrayList<>();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case ID:
                        adder.withId(jsonParser.nextTextValue());
                        break;
                    case NAME:
                        adder.withName(jsonParser.nextTextValue());
                        break;
                    case NETWORK_ELEMENT_ID:
                        String networkElementId = jsonParser.nextTextValue();
                        if (networkElementsNamesPerId.containsKey(networkElementId)) {
                            adder.withNetworkElement(networkElementId, networkElementsNamesPerId.get(networkElementId));
                        } else {
                            adder.withNetworkElement(networkElementId);
                        }
                        break;
                    case OPERATOR:
                        adder.withOperator(jsonParser.nextTextValue());
                        break;
                    case INSTANT:
                        adder.withInstant(deserializeInstant(jsonParser.nextTextValue()));
                        break;
                    case CONTINGENCY_ID:
                        adder.withContingency(jsonParser.nextTextValue());
                        break;
                    case OPTIMIZED:
                        adder.withOptimized(jsonParser.nextBooleanValue());
                        break;
                    case MONITORED:
                        adder.withMonitored(jsonParser.nextBooleanValue());
                        break;
                    case FRM:
                        jsonParser.nextToken();
                        adder.withReliabilityMargin(jsonParser.getDoubleValue());
                        break;
                    case THRESHOLDS:
                        jsonParser.nextToken();
                        BranchThresholdArrayDeserializer.deserialize(jsonParser, adder);
                        break;
                    case EXTENSIONS:
                        jsonParser.nextToken();
                        extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                        break;
                    default:
                        throw new FaraoException("Unexpected field in FlowCnec: " + jsonParser.getCurrentName());
                }
            }
            FlowCnec cnec = adder.add();
            if (!extensions.isEmpty()) {
                ExtensionsHandler.getExtensionsSerializers().addExtensions(cnec, extensions);
            }
        }
    }
}
