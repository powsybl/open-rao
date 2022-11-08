/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.ObjectiveFunctionResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ObjectiveFunctionTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private MinMarginEvaluator minMarginEvaluator;
    private MnecViolationCostEvaluator mnecViolationCostEvaluator;
    private LoopFlowViolationCostEvaluator loopFlowViolationCostEvaluator;
    private FlowResult flowResult;
    private ComputationStatus sensitivityStatus;
    private RangeActionActivationResult rangeActionActivationResult;
    private SensitivityResult sensitivityResult;
    private FlowCnec cnec1;
    private FlowCnec cnec2;

    @Before
    public void setUp() {
        flowResult = Mockito.mock(FlowResult.class);
        sensitivityStatus = Mockito.mock(ComputationStatus.class);
        cnec1 = Mockito.mock(FlowCnec.class);
        cnec2 = Mockito.mock(FlowCnec.class);

        rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        sensitivityResult = Mockito.mock(SensitivityResult.class);

        minMarginEvaluator = Mockito.mock(MinMarginEvaluator.class);
        when(minMarginEvaluator.computeCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus)).thenReturn(-300.);
        when(minMarginEvaluator.getCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, 10)).thenReturn(List.of(cnec1, cnec2));

        mnecViolationCostEvaluator = Mockito.mock(MnecViolationCostEvaluator.class);
        when(mnecViolationCostEvaluator.getName()).thenReturn("mnec-cost");
        when(mnecViolationCostEvaluator.computeCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus)).thenReturn(1000.);
        when(mnecViolationCostEvaluator.getCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, 10)).thenReturn(List.of(cnec1));

        loopFlowViolationCostEvaluator = Mockito.mock(LoopFlowViolationCostEvaluator.class);
        when(loopFlowViolationCostEvaluator.getName()).thenReturn("loop-flow-cost");
        when(loopFlowViolationCostEvaluator.computeCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus)).thenReturn(100.);
        when(loopFlowViolationCostEvaluator.getCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, 10)).thenReturn(List.of(cnec2));
    }

    @Test
    public void testWithFunctionalCostOnly() {
        ObjectiveFunction objectiveFunction = ObjectiveFunction.create()
                .withFunctionalCostEvaluator(minMarginEvaluator)
                .build();

        // functional cost
        assertEquals(-300., objectiveFunction.getFunctionalCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1, cnec2), objectiveFunction.getMostLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, 10));

        // virtual cost
        assertTrue(objectiveFunction.getVirtualCostNames().isEmpty());
        assertEquals(0., objectiveFunction.getVirtualCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(objectiveFunction.getVirtualCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, "mnec-cost")));
        assertTrue(objectiveFunction.getCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, "mnec-cost", 10).isEmpty());

        // ObjectiveFunctionResult
        ObjectiveFunctionResult result = objectiveFunction.evaluate(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus);
        assertEquals(-300., result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(0., result.getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(-300., result.getCost(), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1, cnec2), result.getMostLimitingElements(10));
        assertTrue(result.getVirtualCostNames().isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testWithVirtualCostOnly() {
        ObjectiveFunction.create()
                .withVirtualCostEvaluator(mnecViolationCostEvaluator)
                .build();
    }

    @Test
    public void testWithFunctionalAndVirtualCost() {
        ObjectiveFunction objectiveFunction = ObjectiveFunction.create()
                .withFunctionalCostEvaluator(minMarginEvaluator)
                .withVirtualCostEvaluator(mnecViolationCostEvaluator)
                .withVirtualCostEvaluator(loopFlowViolationCostEvaluator)
                .build();

        // functional cost
        assertEquals(-300., objectiveFunction.getFunctionalCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1, cnec2), objectiveFunction.getMostLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, 10));

        // virtual cost sum
        assertEquals(2, objectiveFunction.getVirtualCostNames().size());
        assertTrue(objectiveFunction.getVirtualCostNames().containsAll(Set.of("mnec-cost", "loop-flow-cost")));
        assertEquals(1100., objectiveFunction.getVirtualCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus), DOUBLE_TOLERANCE);

        // mnec virtual cost
        assertEquals(1000., objectiveFunction.getVirtualCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, "mnec-cost"), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1), objectiveFunction.getCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, "mnec-cost", 10));

        // loopflow virtual cost
        assertEquals(100., objectiveFunction.getVirtualCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, "loop-flow-cost"), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec2), objectiveFunction.getCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, "loop-flow-cost", 10));

        // ObjectiveFunctionResult
        ObjectiveFunctionResult result = objectiveFunction.evaluate(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus);
        assertEquals(-300., result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(1100., result.getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(800., result.getCost(), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1, cnec2), result.getMostLimitingElements(10));
        assertEquals(2, result.getVirtualCostNames().size());
        assertTrue(result.getVirtualCostNames().containsAll(Set.of("mnec-cost", "loop-flow-cost")));
        assertEquals(1000., result.getVirtualCost("mnec-cost"), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1), result.getCostlyElements("mnec-cost", 10));
        assertEquals(100., result.getVirtualCost("loop-flow-cost"), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec2), result.getCostlyElements("loop-flow-cost", 10));
    }
}
