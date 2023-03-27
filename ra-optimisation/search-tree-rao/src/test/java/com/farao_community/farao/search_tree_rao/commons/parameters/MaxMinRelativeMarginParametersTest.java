/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.rao_api.parameters.ObjectiveFunctionParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.parameters.extensions.RelativeMarginsParametersExtension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class MaxMinRelativeMarginParametersTest {

    @Test
    void buildFromRaoParametersTestWithRelativeMargin() {
        RaoParameters raoParameters = new RaoParameters();

        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        raoParameters.addExtension(RelativeMarginsParametersExtension.class, new RelativeMarginsParametersExtension());
        raoParameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfSumLowerBound(0.01);

        MaxMinRelativeMarginParameters mmrmp = MaxMinRelativeMarginParameters.buildFromRaoParameters(raoParameters);

        assertNotNull(mmrmp);
        assertEquals(0.01, mmrmp.getPtdfSumLowerBound(), 1e-6);
    }

    @Test
    void buildFromRaoParametersTestWithoutRelativeMargin() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE);
        MaxMinRelativeMarginParameters mmrmp = MaxMinRelativeMarginParameters.buildFromRaoParameters(raoParameters);
        assertNull(mmrmp);
    }
}
