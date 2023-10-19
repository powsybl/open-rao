/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.optimization_perimeters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class CurativeOptimizationPerimeterTest extends AbstractOptimizationPerimeterTest {

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void curativePerimeterTest() {
        raoParameters.addExtension(LoopFlowParametersExtension.class, new LoopFlowParametersExtension());
        Mockito.when(prePerimeterResult.getSetpoint(cRA)).thenReturn(500.);
        OptimizationPerimeter optPerimeter = CurativeOptimizationPerimeter.build(cState1, crac, network, raoParameters, prePerimeterResult);

        assertEquals(cState1, optPerimeter.getMainOptimizationState());
        assertEquals(Set.of(cState1), optPerimeter.getRangeActionOptimizationStates());
        assertEquals(Set.of(cState1), optPerimeter.getMonitoredStates());

        assertEquals(Set.of(cCnec1), optPerimeter.getFlowCnecs());
        assertTrue(optPerimeter.getOptimizedFlowCnecs().isEmpty());
        assertEquals(Set.of(cCnec1), optPerimeter.getMonitoredFlowCnecs());
        assertTrue(optPerimeter.getLoopFlowCnecs().isEmpty());

        assertEquals(Set.of(cNA), optPerimeter.getNetworkActions());

        assertEquals(1, optPerimeter.getRangeActionsPerState().size());
        assertTrue(optPerimeter.getRangeActionsPerState().containsKey(cState1));
        assertEquals(1, optPerimeter.getRangeActionsPerState().get(cState1).size());
        assertTrue(optPerimeter.getRangeActionsPerState().get(cState1).contains(cRA));
    }

    @Test
    void curativePerimeterbuildOnPreventiveStateTest() {
        FaraoException exception = assertThrows(FaraoException.class, () -> CurativeOptimizationPerimeter.build(pState, crac, network, raoParameters, prePerimeterResult));
        assertEquals("", exception.getMessage());
    }
}
