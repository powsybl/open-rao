/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.openrao.data.cracio.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.networkaction.LoadActionAdder;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;

import java.io.IOException;
import java.util.Map;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
public final class LoadActionArrayDeserializer {
    private LoadActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, NetworkActionAdder ownerAdder, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", LOAD_ACTIONS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            LoadActionAdder adder = ownerAdder.newLoadAction();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case ACTION_ID:
                        adder.withId(jsonParser.nextTextValue());
                        break;
                    case NETWORK_ELEMENT_ID:
                        deserializeNetworkElement(jsonParser.nextTextValue(), networkElementsNamesPerId, adder);
                        break;
                    case ACTIVE_POWER_VALUE:
                        jsonParser.nextToken();
                        adder.withActivePowerValue(jsonParser.getDoubleValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in LoadAction: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
