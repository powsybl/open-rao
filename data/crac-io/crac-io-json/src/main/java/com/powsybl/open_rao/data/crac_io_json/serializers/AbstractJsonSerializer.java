/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_io_json.serializers;

import com.powsybl.open_rao.data.crac_api.RemedialAction;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;
import java.util.Optional;

import static com.powsybl.open_rao.data.crac_io_json.JsonSerializationConstants.SPEED;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public abstract class AbstractJsonSerializer<T> extends JsonSerializer<T> {

    @Override
    public void serializeWithType(T value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException {

        /*
        Open Rao does not explicitly write the type of each object in its Json format
        The type of each object can be implicitly deduced from its position in the Json format
        A more compact format can therefore be provided than if the type were written.
         */

        serialize(value, jsonGenerator, serializerProvider);
    }

    protected void serializeRemedialActionSpeed(RemedialAction<?> ra, JsonGenerator gen) throws IOException {
        Optional<Integer> speed = ra.getSpeed();
        if (speed.isPresent()) {
            gen.writeNumberField(SPEED, speed.get());
        }
    }
}
