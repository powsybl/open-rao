/*
 *  Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.serializers;

import com.powsybl.openrao.data.cracapi.threshold.Threshold;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Optional;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ThresholdSerializer extends AbstractJsonSerializer<Threshold> {
    @Override
    public void serialize(Threshold value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(UNIT, serializeUnit(value.getUnit()));
        Optional<Double> min = value.min();
        if (min.isPresent()) {
            gen.writeNumberField(MIN, min.get());
        }
        Optional<Double> max = value.max();
        if (max.isPresent()) {
            gen.writeNumberField(MAX, max.get());
        }
        gen.writeEndObject();
    }

}
