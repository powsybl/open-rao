/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class LinearProblemResultTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private LinearProblem linearProblem;
    private LinearProblemResult linearProblemResult;
    private State preventiveState;
    private State aCurativeState;
    private PstRangeAction pst1;
    private PstRangeAction pst2;
    private RangeAction<?> ra3;
    private RangeAction<?> ra4;
    private Map<State, Set<RangeAction<?>>> rangeActionsPerState;

    @BeforeEach
    public void setUp() {
        Instant preventiveInstant = Mockito.mock(Instant.class);
        Mockito.when(preventiveInstant.comesBefore(Mockito.any())).thenReturn(true);
        Mockito.when(preventiveInstant.comesBefore(preventiveInstant)).thenReturn(false);
        Instant curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.isCurative()).thenReturn(true);
        Mockito.when(curativeInstant.getOrder()).thenReturn(3);
        preventiveState = Mockito.mock(State.class);
        Mockito.when(preventiveState.getInstant()).thenReturn(preventiveInstant);
        Mockito.when(preventiveState.isPreventive()).thenReturn(true);
        Mockito.when(preventiveState.getId()).thenReturn("pState");
        aCurativeState = Mockito.mock(State.class);
        Mockito.when(aCurativeState.getInstant()).thenReturn(curativeInstant);
        Mockito.when(aCurativeState.getId()).thenReturn("cState");

        pst1 = Mockito.mock(PstRangeAction.class);
        Mockito.when(pst1.getId()).thenReturn("pst1");
        NetworkElement pst1NE = Mockito.mock(NetworkElement.class);
        Mockito.when(pst1NE.getId()).thenReturn("pst1NE");
        Mockito.when(pst1.getNetworkElements()).thenReturn(Set.of(pst1NE));
        pst2 = Mockito.mock(PstRangeAction.class);
        Mockito.when(pst2.getId()).thenReturn("pst2");
        NetworkElement pst2NE = Mockito.mock(NetworkElement.class);
        Mockito.when(pst2NE.getId()).thenReturn("pst2NE");
        Mockito.when(pst2.getNetworkElements()).thenReturn(Set.of(pst2NE));
        ra3 = Mockito.mock(RangeAction.class);
        Mockito.when(ra3.getId()).thenReturn("ra3");
        NetworkElement ra3NE = Mockito.mock(NetworkElement.class);
        Mockito.when(ra3NE.getId()).thenReturn("ra3NE");
        Mockito.when(ra3.getNetworkElements()).thenReturn(Set.of(ra3NE));
        ra4 = Mockito.mock(RangeAction.class);
        Mockito.when(ra4.getId()).thenReturn("ra4");
        NetworkElement ra4NE = Mockito.mock(NetworkElement.class);
        Mockito.when(ra4NE.getId()).thenReturn("ra4NE");
        Mockito.when(ra4.getNetworkElements()).thenReturn(Set.of(ra4NE));

        linearProblem = Mockito.mock(LinearProblem.class);
        rangeActionsPerState = Map.of(
            preventiveState, Set.of(pst1, pst2, ra4),
            aCurativeState, Set.of(pst1, ra3, ra4));

        // pst1 activated in preventive
        // pst2 not activated
        // ra3 activated in curative
        // ra4 activated in preventive and curative

        Map<State, Map<RangeAction<?>, Double>> setPointPerRangeAction = Map.of(
            preventiveState, Map.of(
                pst1, 2.3,
                pst2, 5.4,
                ra4, -300.),
            aCurativeState, Map.of(
                pst1, 2.3,
                ra3, 200.,
                ra4, 700.));

        Map<State, Map<RangeAction<?>, OpenRaoMPVariable>> setPointVariablePerRangeAction = Map.of(
            preventiveState, Map.of(
                pst1, Mockito.mock(OpenRaoMPVariable.class),
                pst2, Mockito.mock(OpenRaoMPVariable.class),
                ra3, Mockito.mock(OpenRaoMPVariable.class),
                ra4, Mockito.mock(OpenRaoMPVariable.class)),
            aCurativeState, Map.of(
                pst1, Mockito.mock(OpenRaoMPVariable.class),
                pst2, Mockito.mock(OpenRaoMPVariable.class),
                ra3, Mockito.mock(OpenRaoMPVariable.class),
                ra4, Mockito.mock(OpenRaoMPVariable.class)));

        rangeActionsPerState.forEach((state, rangeActions) -> rangeActions.forEach(ra -> {
            OpenRaoMPVariable setPointVariable = setPointVariablePerRangeAction.get(state).get(ra);
            Mockito.when(linearProblem.getRangeActionSetpointVariable(ra, state)).thenReturn(setPointVariable);
            Mockito.when(setPointVariable.solutionValue()).thenReturn(setPointPerRangeAction.get(state).get(ra));
        }));

        Mockito.when(pst1.convertAngleToTap(1.5)).thenReturn(3);
        Mockito.when(pst2.convertAngleToTap(5.4)).thenReturn(10);
    }

    @Test
    void testGetOptimizedSetPointPreventivePerimeter() {
        linearProblemResult = new LinearProblemResult(linearProblem);
        assertEquals(2.3, linearProblemResult.getSetpointOnState(pst1, preventiveState), DOUBLE_TOLERANCE);
        assertEquals(5.4, linearProblemResult.getSetpointOnState(pst2, preventiveState), DOUBLE_TOLERANCE);
        assertEquals(-300., linearProblemResult.getSetpointOnState(ra4, preventiveState), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetOptimizedSetPointCurativePerimeter() {
        linearProblemResult = new LinearProblemResult(linearProblem);
        assertEquals(2.3, linearProblemResult.getSetpointOnState(pst1, aCurativeState), DOUBLE_TOLERANCE);
        assertEquals(200., linearProblemResult.getSetpointOnState(ra3, aCurativeState), DOUBLE_TOLERANCE);
        assertEquals(700., linearProblemResult.getSetpointOnState(ra4, aCurativeState), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetOptimizedSetPointGlobalPerimeter() {
        linearProblemResult = new LinearProblemResult(linearProblem);
        assertEquals(2.3, linearProblemResult.getSetpointOnState(pst1, preventiveState), DOUBLE_TOLERANCE);
        assertEquals(5.4, linearProblemResult.getSetpointOnState(pst2, preventiveState), DOUBLE_TOLERANCE);
        assertEquals(-300., linearProblemResult.getSetpointOnState(ra4, preventiveState), DOUBLE_TOLERANCE);

        assertEquals(2.3, linearProblemResult.getSetpointOnState(pst1, aCurativeState), DOUBLE_TOLERANCE);
        assertEquals(200., linearProblemResult.getSetpointOnState(ra3, aCurativeState), DOUBLE_TOLERANCE);
        assertEquals(700., linearProblemResult.getSetpointOnState(ra4, aCurativeState), DOUBLE_TOLERANCE);
    }

}
