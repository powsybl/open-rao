/*
 * Copyright (c) 2018, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.flowbased_computation.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;
import com.powsybl.open_rao.flowbased_computation.FlowbasedComputationParameters;

import java.io.IOException;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowbasedComputationParametersSerializer extends StdSerializer<FlowbasedComputationParameters> {

    FlowbasedComputationParametersSerializer() {
        super(FlowbasedComputationParameters.class);
    }

    @Override
    public void serialize(FlowbasedComputationParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField("version", FlowbasedComputationParameters.VERSION);

        jsonGenerator.writeFieldName("load-flow-parameters");
        JsonLoadFlowParameters.serialize(parameters.getLoadFlowParameters(), jsonGenerator, serializerProvider);

        JsonUtil.writeExtensions(parameters, jsonGenerator, serializerProvider, JsonFlowbasedComputationParameters.getExtensionSerializers());

        jsonGenerator.writeEndObject();
    }
}
