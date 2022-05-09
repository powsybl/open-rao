/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class GlobalRemedialActionLimitationParametersTest {

    @Test
    public void buildFromRaoParametersTestOk() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.addExtension(SearchTreeRaoParameters.class, new SearchTreeRaoParameters());

        raoParameters.getExtension(SearchTreeRaoParameters.class).setMaxCurativeRa(3);
        raoParameters.getExtension(SearchTreeRaoParameters.class).setMaxCurativeTso(1);
        raoParameters.getExtension(SearchTreeRaoParameters.class).setMaxCurativePstPerTso(Map.of("BE", 2, "FR", 1));
        raoParameters.getExtension(SearchTreeRaoParameters.class).setMaxCurativeTopoPerTso(Map.of("DE", 0));
        raoParameters.getExtension(SearchTreeRaoParameters.class).setMaxCurativeRaPerTso(Map.of("ES", 3, "PT", 1));

        GlobalRemedialActionLimitationParameters gralp = GlobalRemedialActionLimitationParameters.buildFromRaoParameters(raoParameters);

        assertEquals(Integer.valueOf(3), gralp.getMaxCurativeRa());
        assertEquals(Integer.valueOf(1), gralp.getMaxCurativeTso());
        assertEquals(Map.of("BE", 2, "FR", 1), gralp.getMaxCurativePstPerTso());
        assertEquals(Map.of("DE", 0), gralp.getMaxCurativeTopoPerTso());
        assertEquals(Map.of("ES", 3, "PT", 1), gralp.getMaxCurativeRaPerTso());
    }

    @Test (expected = FaraoException.class)
    public void buildFromRaoParametersWithMissingSearchTreeRaoParametersTest() {
        RaoParameters raoParameters = new RaoParameters();
        GlobalRemedialActionLimitationParameters.buildFromRaoParameters(raoParameters);
    }
}
