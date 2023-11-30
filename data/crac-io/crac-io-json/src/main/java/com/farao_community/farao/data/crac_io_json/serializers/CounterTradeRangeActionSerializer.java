/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json.serializers;

import com.farao_community.farao.data.crac_api.range_action.CounterTradeRangeAction;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.EXPORTING_COUNTRY;
import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.IMPORTING_COUNTRY;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public class CounterTradeRangeActionSerializer extends AbstractJsonSerializer<CounterTradeRangeAction> {

    @Override
    public void serialize(CounterTradeRangeAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        StandardRangeActionSerializerUtils.serializeCommon(value, gen);
        gen.writeStringField(EXPORTING_COUNTRY, value.getExportingCountry().toString());
        gen.writeStringField(IMPORTING_COUNTRY, value.getImportingCountry().toString());
        serializeRemedialActionSpeed(value, gen);
        gen.writeEndObject();
    }
}
