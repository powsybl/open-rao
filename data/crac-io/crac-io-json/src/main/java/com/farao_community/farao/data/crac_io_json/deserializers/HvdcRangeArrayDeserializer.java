/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeActionAdder;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 *  @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class HvdcRangeArrayDeserializer {
    private HvdcRangeArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, HvdcRangeActionAdder ownerAdder) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            HvdcRangeAdder adder = ownerAdder.newHvdcRange();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case MIN:
                        adder.withMin(jsonParser.nextIntValue(Integer.MIN_VALUE));
                        break;
                    case MAX:
                        adder.withMax(jsonParser.nextIntValue(Integer.MAX_VALUE));
                        break;
                    case RANGE_TYPE:
                        int debug = 0;
                    default:
                        throw new FaraoException("Unexpected field in HvdcRange: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
