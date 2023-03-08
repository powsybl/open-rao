/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.MnecParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.RangeActionParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemBuilder;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionActivationResultImpl;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionSetpointResultImpl;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class MnecFillerTest extends AbstractFillerTest {
    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;
    private FlowCnec mnec1;
    private FlowCnec mnec2;
    private FlowCnec mnec3;

    @Before
    public void setUp() {
        init();
        mnec1 = crac.newFlowCnec()
                .withId("MNEC1 - N - preventive")
                .withNetworkElement("DDE2AA1  NNL3AA1  1")
                .newThreshold()
                .withMin(-1000.)
                .withSide(Side.RIGHT)
                .withMax(1000.0)
                .withUnit(Unit.MEGAWATT)
                .add()
                .withNominalVoltage(380.)
                .withOptimized(true)
                .withMonitored(true)
                .withInstant(Instant.PREVENTIVE)
                .add();

        mnec2 = crac.newFlowCnec()
                .withId("MNEC2 - N - preventive")
                .withNetworkElement("NNL2AA1  BBE3AA1  1")
                .newThreshold()
                .withMin(-100.)
                .withSide(Side.LEFT)
                .withMax(100.0)
                .withUnit(Unit.MEGAWATT)
                .add()
                .withNominalVoltage(380.)
                .withOptimized(true)
                .withMonitored(true)
                .withInstant(Instant.PREVENTIVE)
                .add();

        mnec3 = crac.newFlowCnec()
                .withId("MNEC3 - N - preventive")
                .withNetworkElement("NNL2AA1  BBE3AA1  1")
                .newThreshold().withMin(-100.).withSide(Side.LEFT).withMax(100.0).withUnit(Unit.MEGAWATT).add()
                .newThreshold().withMin(-100.).withSide(Side.RIGHT).withMax(100.0).withUnit(Unit.MEGAWATT).add()
                .withNominalVoltage(380.)
                .withOptimized(true)
                .withMonitored(true)
                .withInstant(Instant.PREVENTIVE)
                .add();

        RangeActionSetpointResult initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(Collections.emptyMap());

        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(mnec1, mnec2, mnec3));

        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        rangeActions.put(cnec1.getState(), Collections.emptySet());
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);

        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getRangeActionsOptimizationParameters().setPstPenaltyCost(0.01);
        raoParameters.getRangeActionsOptimizationParameters().setHvdcPenaltyCost(0.01);
        raoParameters.getRangeActionsOptimizationParameters().setInjectionRaPenaltyCost(0.01);
        RangeActionParameters rangeActionParameters = RangeActionParameters.buildFromRaoParameters(raoParameters);

        coreProblemFiller = new CoreProblemFiller(
                optimizationPerimeter,
                initialRangeActionSetpointResult,
                new RangeActionActivationResultImpl(initialRangeActionSetpointResult),
                rangeActionParameters,
                Unit.MEGAWATT);
    }

    private void fillProblemWithFiller(Unit unit) {
        MnecParameters parameters = new MnecParameters(50, 10, 3.5);
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        when(flowResult.getFlow(mnec1, Side.RIGHT, Unit.MEGAWATT)).thenReturn(900.);
        when(flowResult.getFlow(mnec2, Side.LEFT, Unit.MEGAWATT)).thenReturn(-200.);
        when(flowResult.getFlow(mnec3, Side.LEFT, Unit.MEGAWATT)).thenReturn(-200.);
        when(flowResult.getFlow(mnec3, Side.RIGHT, Unit.MEGAWATT)).thenReturn(-200.);
        MnecFiller mnecFiller = new MnecFiller(
                flowResult,
                Set.of(mnec1, mnec2, mnec3),
                unit,
                parameters);
        linearProblem = new LinearProblemBuilder()
                .withProblemFiller(coreProblemFiller)
                .withProblemFiller(mnecFiller)
                .withSolver(mpSolver)
                .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    @Test
    public void testAddMnecViolationVariables() {
        fillProblemWithFiller(Unit.MEGAWATT);
        crac.getFlowCnecs().forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            MPVariable variable = linearProblem.getMnecViolationVariable(cnec, side);
            if (cnec.isMonitored()) {
                assertNotNull(variable);
                assertEquals(0, variable.lb(), DOUBLE_TOLERANCE);
                assertEquals(LinearProblem.infinity(), variable.ub(), DOUBLE_TOLERANCE);
            } else {
                assertNull(variable);
            }
        }));
    }

    @Test
    public void testAddMnecMinFlowConstraints() {
        fillProblemWithFiller(Unit.MEGAWATT);

        crac.getFlowCnecs().stream().filter(cnec -> !cnec.isMonitored()).forEach(cnec -> cnec.getMonitoredSides().forEach(side ->
                assertNull(linearProblem.getMnecFlowConstraint(cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD))));

        MPConstraint ct1Max = linearProblem.getMnecFlowConstraint(mnec1, Side.RIGHT, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(ct1Max);
        assertEquals(-LinearProblem.infinity(), ct1Max.lb(), DOUBLE_TOLERANCE);
        double mnec1MaxFlow = 1000 - 3.5;
        assertEquals(mnec1MaxFlow, ct1Max.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, ct1Max.getCoefficient(linearProblem.getFlowVariable(mnec1, Side.RIGHT)), DOUBLE_TOLERANCE);
        assertEquals(-1, ct1Max.getCoefficient(linearProblem.getMnecViolationVariable(mnec1, Side.RIGHT)), DOUBLE_TOLERANCE);

        MPConstraint ct1Min = linearProblem.getMnecFlowConstraint(mnec1, Side.RIGHT, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(ct1Min);
        double mnec1MinFlow = -1000 + 3.5;
        assertEquals(mnec1MinFlow, ct1Min.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), ct1Min.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, ct1Min.getCoefficient(linearProblem.getFlowVariable(mnec1, Side.RIGHT)), DOUBLE_TOLERANCE);
        assertEquals(1, ct1Min.getCoefficient(linearProblem.getMnecViolationVariable(mnec1, Side.RIGHT)), DOUBLE_TOLERANCE);

        MPConstraint ct2Max = linearProblem.getMnecFlowConstraint(mnec2, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(ct2Max);
        assertEquals(-LinearProblem.infinity(), ct2Max.lb(), DOUBLE_TOLERANCE);
        double mnec2MaxFlow = 100 - 3.5;
        assertEquals(mnec2MaxFlow, ct2Max.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, ct2Max.getCoefficient(linearProblem.getFlowVariable(mnec2, Side.LEFT)), DOUBLE_TOLERANCE);
        assertEquals(-1, ct2Max.getCoefficient(linearProblem.getMnecViolationVariable(mnec2, Side.LEFT)), DOUBLE_TOLERANCE);

        MPConstraint ct2Min = linearProblem.getMnecFlowConstraint(mnec2, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(ct2Min);
        double mnec2MinFlow = -250 + 3.5;
        assertEquals(mnec2MinFlow, ct2Min.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), ct2Min.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, ct2Min.getCoefficient(linearProblem.getFlowVariable(mnec2, Side.LEFT)), DOUBLE_TOLERANCE);
        assertEquals(1, ct2Min.getCoefficient(linearProblem.getMnecViolationVariable(mnec2, Side.LEFT)), DOUBLE_TOLERANCE);
    }

    @Test
    public void testAddMnecPenaltyCostMW() {
        fillProblemWithFiller(Unit.MEGAWATT);
        crac.getFlowCnecs().stream().filter(Cnec::isMonitored).forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            MPVariable mnecViolationVariable = linearProblem.getMnecViolationVariable(cnec, side);
            assertEquals(10.0 / cnec.getMonitoredSides().size(), linearProblem.getObjective().getCoefficient(mnecViolationVariable), DOUBLE_TOLERANCE);
        }));
    }

    @Test
    public void testAddMnecPenaltyCostA() {
        fillProblemWithFiller(Unit.AMPERE);
        crac.getFlowCnecs().stream().filter(Cnec::isMonitored).forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            MPVariable mnecViolationVariable = linearProblem.getMnecViolationVariable(cnec, side);
            assertEquals(10.0 / 0.658179 / cnec.getMonitoredSides().size(), linearProblem.getObjective().getCoefficient(mnecViolationVariable), DOUBLE_TOLERANCE);
        }));
    }
}
