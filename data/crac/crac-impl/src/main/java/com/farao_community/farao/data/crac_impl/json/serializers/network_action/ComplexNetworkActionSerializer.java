/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json.serializers.network_action;

import com.farao_community.farao.data.crac_api.ExtensionsHandler;
import com.farao_community.farao.data.crac_impl.json.JsonSerializationNames;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.NetworkActionImpl;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ComplexNetworkActionSerializer extends NetworkActionSerializer<NetworkActionImpl> {

    @Override
    public void serialize(NetworkActionImpl networkAction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        super.serializeCommon(networkAction, jsonGenerator);
        jsonGenerator.writeFieldName(JsonSerializationNames.ELEMENTARY_NETWORK_ACTIONS);
        jsonGenerator.writeStartArray();
        for (AbstractElementaryNetworkAction abstractElementaryNetworkAction: networkAction.getElementaryNetworkActions()) {
            jsonGenerator.writeObject(abstractElementaryNetworkAction);
        }
        jsonGenerator.writeEndArray();
        JsonUtil.writeExtensions(networkAction, jsonGenerator, serializerProvider, ExtensionsHandler.getExtensionsSerializers());
    }
}
