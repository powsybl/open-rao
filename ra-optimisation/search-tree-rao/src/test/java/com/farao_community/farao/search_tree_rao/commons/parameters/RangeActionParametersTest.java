/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.rao_api.parameters.RaoParameters;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RangeActionParametersTest {

    @Test
    public void buildFromRaoParametersTest() {
        RaoParameters raoParameters = new RaoParameters();

        raoParameters.setPstSensitivityThreshold(1.1);
        raoParameters.setHvdcSensitivityThreshold(2.2);
        raoParameters.setInjectionRaSensitivityThreshold(3.3);
        raoParameters.setPstPenaltyCost(4.4);
        raoParameters.setHvdcPenaltyCost(5.5);
        raoParameters.setInjectionRaPenaltyCost(6.6);

        RangeActionParameters rap = RangeActionParameters.buildFromRaoParameters(raoParameters);

        assertEquals(1.1, rap.getPstSensitivityThreshold(), 1e-6);
        assertEquals(2.2, rap.getHvdcSensitivityThreshold(), 1e-6);
        assertEquals(3.3, rap.getInjectionSensitivityThreshold(), 1e-6);
        assertEquals(4.4, rap.getPstPenaltyCost(), 1e-6);
        assertEquals(5.5, rap.getHvdcPenaltyCost(), 1e-6);
        assertEquals(6.6, rap.getInjectionPenaltyCost(), 1e-6);
    }
}
