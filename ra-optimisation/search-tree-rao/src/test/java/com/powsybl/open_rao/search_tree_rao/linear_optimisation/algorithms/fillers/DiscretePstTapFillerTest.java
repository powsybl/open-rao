/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.State;
import com.powsybl.open_rao.data.crac_api.range_action.PstRangeAction;
import com.powsybl.open_rao.data.crac_api.range_action.RangeAction;
import com.powsybl.open_rao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.powsybl.open_rao.rao_api.parameters.RaoParameters;
import com.powsybl.open_rao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem.OpenRaoMPConstraint;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem.OpenRaoMPVariable;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemBuilder;
import com.powsybl.open_rao.search_tree_rao.result.api.RangeActionActivationResult;
import com.powsybl.open_rao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.powsybl.open_rao.search_tree_rao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.open_rao.search_tree_rao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class DiscretePstTapFillerTest extends AbstractFillerTest {

    @Test
    void testFillAndUpdateMethods() {

        // prepare data
        init();
        State state = crac.getPreventiveState();
        PstRangeAction pstRangeAction = crac.getPstRangeAction(RANGE_ACTION_ID);
        Map<Integer, Double> tapToAngle = pstRangeAction.getTapToAngleConversionMap();
        double initialAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();
        RangeActionSetpointResult initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(pstRangeAction, initialAlpha));
        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);

        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        rangeActions.put(state, Set.of(pstRangeAction));
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);

        RangeActionsOptimizationParameters rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(new RaoParameters());

        CoreProblemFiller coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            new RangeActionActivationResultImpl(initialRangeActionSetpointResult),
            rangeActionParameters,
            Unit.MEGAWATT,
            false);

        Map<State, Set<PstRangeAction>> pstRangeActions = new HashMap<>();
        pstRangeActions.put(state, Set.of(pstRangeAction));
        DiscretePstTapFiller discretePstTapFiller = new DiscretePstTapFiller(
            network,
            state,
            pstRangeActions,
            initialRangeActionSetpointResult);

        LinearProblem linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(discretePstTapFiller)
            .withSolver(mpSolver)
            .build();

        // fill linear problem
        linearProblem.fill(flowResult, sensitivityResult);

        // check that all constraints and variables exists
        OpenRaoMPVariable setpointV = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);
        OpenRaoMPVariable variationUpV = linearProblem.getPstTapVariationVariable(pstRangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable variationDownV = linearProblem.getPstTapVariationVariable(pstRangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable binaryUpV = linearProblem.getPstTapVariationBinary(pstRangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable binaryDownV = linearProblem.getPstTapVariationBinary(pstRangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPConstraint tapToAngleConversionC = linearProblem.getTapToAngleConversionConstraint(pstRangeAction, state);
        OpenRaoMPConstraint upOrDownC = linearProblem.getUpOrDownPstVariationConstraint(pstRangeAction, state);
        OpenRaoMPConstraint upVariationC = linearProblem.getIsVariationInDirectionConstraint(pstRangeAction, state, LinearProblem.VariationReferenceExtension.PREVIOUS_ITERATION, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPConstraint downVariationC = linearProblem.getIsVariationInDirectionConstraint(pstRangeAction, state, LinearProblem.VariationReferenceExtension.PREVIOUS_ITERATION, LinearProblem.VariationDirectionExtension.DOWNWARD);

        assertNotNull(setpointV);
        assertNotNull(variationUpV);
        assertNotNull(variationDownV);
        assertNotNull(binaryUpV);
        assertNotNull(binaryDownV);
        assertNotNull(tapToAngleConversionC);
        assertNotNull(upOrDownC);
        assertNotNull(upVariationC);
        assertNotNull(downVariationC);

        // check variable bounds
        assertEquals(0, variationUpV.lb(), 1e-6);
        assertEquals(0, variationDownV.lb(), 1e-6);
        assertEquals(0, binaryUpV.lb(), 1e-6);
        assertEquals(1, binaryUpV.ub(), 1e-6);
        assertEquals(0, binaryDownV.lb(), 1e-6);
        assertEquals(1, binaryDownV.ub(), 1e-6);

        // check tap to angle conversion constraints
        assertEquals(initialAlpha, tapToAngleConversionC.lb(), 1e-6);
        assertEquals(initialAlpha, tapToAngleConversionC.ub(), 1e-6);
        assertEquals(1, tapToAngleConversionC.getCoefficient(setpointV), 1e-6);
        assertEquals(-(tapToAngle.get(15) - tapToAngle.get(0)) / 15, tapToAngleConversionC.getCoefficient(variationUpV), 1e-6);
        assertEquals(-(tapToAngle.get(-15) - tapToAngle.get(0)) / 15, tapToAngleConversionC.getCoefficient(variationDownV), 1e-6);

        // check other constraints
        assertEquals(1, upOrDownC.ub(), 1e-6);
        assertEquals(1, upOrDownC.getCoefficient(binaryUpV), 1e-6);
        assertEquals(1, upOrDownC.getCoefficient(binaryDownV), 1e-6);

        assertEquals(0, upVariationC.ub(), 1e-6);
        assertEquals(1, upVariationC.getCoefficient(variationUpV), 1e-6);
        assertEquals(-15, upVariationC.getCoefficient(binaryUpV), 1e-6);

        assertEquals(0, downVariationC.ub(), 1e-6);
        assertEquals(1, downVariationC.getCoefficient(variationDownV), 1e-6);
        assertEquals(-15, downVariationC.getCoefficient(binaryDownV), 1e-6);

        // update linear problem, with a new PST tap equal to -4
        double alphaBeforeUpdate = tapToAngle.get(-4);
        RangeActionActivationResult rangeActionActivationResultBeforeUpdate = new RangeActionActivationResultImpl(new RangeActionSetpointResultImpl(Map.of(this.pstRangeAction, alphaBeforeUpdate)));
        discretePstTapFiller.updateBetweenSensiIteration(linearProblem, flowResult, sensitivityResult, rangeActionActivationResultBeforeUpdate);

        // check tap to angle conversion constraints
        assertEquals(alphaBeforeUpdate, tapToAngleConversionC.lb(), 1e-6);
        assertEquals(alphaBeforeUpdate, tapToAngleConversionC.ub(), 1e-6);
        assertEquals(1, tapToAngleConversionC.getCoefficient(setpointV), 1e-6);
        assertEquals(-(tapToAngle.get(-3) - tapToAngle.get(-4)), tapToAngleConversionC.getCoefficient(variationUpV), 1e-6);
        assertEquals(-(tapToAngle.get(-5) - tapToAngle.get(-4)), tapToAngleConversionC.getCoefficient(variationDownV), 1e-6);

        // check other constraints
        assertEquals(0, upVariationC.ub(), 1e-6);
        assertEquals(1, upVariationC.getCoefficient(variationUpV), 1e-6);
        assertEquals(-19, upVariationC.getCoefficient(binaryUpV), 1e-6);

        assertEquals(0, downVariationC.ub(), 1e-6);
        assertEquals(1, downVariationC.getCoefficient(variationDownV), 1e-6);
        assertEquals(-11, downVariationC.getCoefficient(binaryDownV), 1e-6);
    }
}
