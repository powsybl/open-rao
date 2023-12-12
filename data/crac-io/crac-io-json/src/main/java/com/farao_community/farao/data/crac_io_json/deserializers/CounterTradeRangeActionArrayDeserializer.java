/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.range_action.CounterTradeRangeActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.iidm.network.Country;

import java.io.IOException;
import java.util.Map;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public final class CounterTradeRangeActionArrayDeserializer {
    private CounterTradeRangeActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new FaraoException(String.format("Cannot deserialize %s before %s", COUNTER_TRADE_RANGE_ACTIONS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction();

            while (!jsonParser.nextToken().isStructEnd()) {
                addElement(counterTradeRangeActionAdder, jsonParser, version);
            }
            if (getPrimaryVersionNumber(version) <= 1 && getSubVersionNumber(version) < 3) {
                // initial setpoint was not exported then, set default value to 0 to avoid errors
                counterTradeRangeActionAdder.withInitialSetpoint(0);
            }
            counterTradeRangeActionAdder.add();
        }
    }

    private static void addElement(CounterTradeRangeActionAdder counterTradeRangeActionAdder, JsonParser jsonParser, String version) throws IOException {
        if (StandardRangeActionDeserializer.addCommonElement(counterTradeRangeActionAdder, jsonParser, version)) {
            return;
        }
        switch (jsonParser.getCurrentName()) {
            case EXPORTING_COUNTRY:
                counterTradeRangeActionAdder.withExportingCountry(Country.valueOf(jsonParser.nextTextValue()));
                break;
            case IMPORTING_COUNTRY:
                counterTradeRangeActionAdder.withImportingCountry(Country.valueOf(jsonParser.nextTextValue()));
                break;
            default:
                throw new FaraoException("Unexpected field in InjectionRangeAction: " + jsonParser.getCurrentName());
        }
    }
}
