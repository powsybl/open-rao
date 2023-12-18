/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_io_json.serializers;

import com.powsybl.open_rao.data.crac_api.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Objects;

import static com.powsybl.open_rao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class ContingencySerializer extends AbstractJsonSerializer<Contingency> {

    @Override
    public void serialize(Contingency value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeStringField(ID, value.getId());

        if (!Objects.isNull(value.getName()) && !value.getName().equals(value.getId())) {
            gen.writeStringField(NAME, value.getName());
        }

        gen.writeArrayFieldStart(NETWORK_ELEMENTS_IDS);
        for (NetworkElement networkElement : value.getNetworkElements()) {
            gen.writeString(networkElement.getId());
        }
        gen.writeEndArray();

        gen.writeEndObject();
    }
}
