/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.action.LoadAction;

import java.io.IOException;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class LoadActionSerializer extends AbstractJsonSerializer<LoadAction> {
    @Override
    public void serialize(LoadAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(ACTION_ID, value.getId());
        gen.writeStringField(NETWORK_ELEMENT_ID, value.getLoadId());
        gen.writeNumberField(ACTIVE_POWER_VALUE, value.getActivePowerValue().getAsDouble());
        gen.writeEndObject();
    }
}
