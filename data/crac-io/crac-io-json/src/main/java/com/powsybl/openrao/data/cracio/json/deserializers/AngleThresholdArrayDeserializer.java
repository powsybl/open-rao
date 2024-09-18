/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnecAdder;
import com.powsybl.openrao.data.cracapi.threshold.AngleThresholdAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class AngleThresholdArrayDeserializer {

    private AngleThresholdArrayDeserializer() {
    }

    public record AngleThreshold(Unit unit, Double min, Double max) {
    }

    public static Set<AngleThreshold> deserialize(JsonParser jsonParser, AngleCnecAdder ownerAdder) throws IOException {
        Set<AngleThreshold> angleThresholds = new HashSet<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            AngleThresholdAdder angleThresholdAdder = ownerAdder.newThreshold(); // TODO: remove this?
            Unit unit = null;
            Double min = null;
            Double max = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case UNIT:
                        unit = deserializeUnit(jsonParser.nextTextValue());
                        angleThresholdAdder.withUnit(unit);
                        break;
                    case MIN:
                        jsonParser.nextToken();
                        min = jsonParser.getDoubleValue();
                        angleThresholdAdder.withMin(min);
                        break;
                    case MAX:
                        jsonParser.nextToken();
                        max = jsonParser.getDoubleValue();
                        angleThresholdAdder.withMax(max);
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in Threshold: " + jsonParser.getCurrentName());
                }
            }
            angleThresholdAdder.add();
            angleThresholds.add(new AngleThreshold(unit, min, max));
        }
        return angleThresholds;
    }
}
