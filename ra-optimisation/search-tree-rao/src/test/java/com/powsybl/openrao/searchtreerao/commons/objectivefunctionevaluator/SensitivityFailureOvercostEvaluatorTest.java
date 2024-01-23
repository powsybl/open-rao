/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class SensitivityFailureOvercostEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private FlowResult flowResult;
    private SensitivityFailureOvercostEvaluator evaluator;
    private RangeActionActivationResult rangeActionActivationResult;
    private SensitivityResult sensitivityResult;
    private FlowCnec cnec1;
    private FlowCnec cnec2;

    @BeforeEach
    public void setUp() {
        flowResult = Mockito.mock(FlowResult.class);
        rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        sensitivityResult = Mockito.mock(SensitivityResult.class);
        cnec1 = Mockito.mock(FlowCnec.class);
        cnec2 = Mockito.mock(FlowCnec.class);
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        Mockito.when(cnec1.getState()).thenReturn(state1);
        Mockito.when(cnec2.getState()).thenReturn(state2);
        Mockito.when(sensitivityResult.getSensitivityStatus(state1)).thenReturn(ComputationStatus.DEFAULT);
        Mockito.when(sensitivityResult.getSensitivityStatus(state2)).thenReturn(ComputationStatus.FAILURE);
    }

    @Test
    void testGetName() {
        evaluator = new SensitivityFailureOvercostEvaluator(Set.of(cnec1), 10000);
        assertEquals("sensitivity-failure-cost", evaluator.getName());
    }

    @Test
    void testGetUnit() {
        evaluator = new SensitivityFailureOvercostEvaluator(Set.of(cnec1), 10000);
        assertEquals(Unit.MEGAWATT, evaluator.getUnit());
    }

    @Test
    void testCostWithStateInFailure() {
        evaluator = new SensitivityFailureOvercostEvaluator(Set.of(cnec1, cnec2), 10000);
        assertEquals(10000, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.FAILURE).getLeft(), DOUBLE_TOLERANCE);
        assertEquals(10000, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.DEFAULT).getLeft(), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetCostlyElements() {
        evaluator = new SensitivityFailureOvercostEvaluator(Set.of(cnec1, cnec2), 10000);
        assertEquals(0, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.DEFAULT).getRight().size());
        assertEquals(0, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.DEFAULT, Set.of("")).getRight().size());
    }

    @Test
    void testGetFlowCnecs() {
        evaluator = new SensitivityFailureOvercostEvaluator(Set.of(cnec1, cnec2), 10000);
        assertEquals(0, evaluator.getFlowCnecs().size());
    }
}
