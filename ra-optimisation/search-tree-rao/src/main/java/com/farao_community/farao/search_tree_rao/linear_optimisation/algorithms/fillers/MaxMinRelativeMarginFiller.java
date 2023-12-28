/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.rao_api.parameters.extensions.PtdfApproximation;
import com.farao_community.farao.rao_api.parameters.extensions.RelativeMarginsParametersExtension;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPConstraint;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPVariable;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;

import java.util.Optional;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MaxMinRelativeMarginFiller extends MaxMinMarginFiller {
    private final FlowResult initialFlowResult;
    private final PtdfApproximation ptdfApproximationLevel;
    private final Unit unit;
    private final double ptdfSumLowerBound;
    private final double highestThreshold;
    private final double maxPositiveRelativeRam;
    private final double maxNegativeRelativeRam;

    public MaxMinRelativeMarginFiller(Set<FlowCnec> optimizedCnecs,
                                      FlowResult initialFlowResult,
                                      Unit unit,
                                      RelativeMarginsParametersExtension maxMinRelativeMarginParameters) {
        super(optimizedCnecs, unit);
        this.initialFlowResult = initialFlowResult;
        this.ptdfApproximationLevel = maxMinRelativeMarginParameters.getPtdfApproximation();
        this.unit = unit;
        this.ptdfSumLowerBound = maxMinRelativeMarginParameters.getPtdfSumLowerBound();
        this.highestThreshold = RaoUtil.getLargestCnecThreshold(optimizedCnecs, MEGAWATT);
        this.maxPositiveRelativeRam = highestThreshold / ptdfSumLowerBound;
        this.maxNegativeRelativeRam = 5 * maxPositiveRelativeRam;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        super.fill(linearProblem, flowResult, sensitivityResult);
        buildMinimumRelativeMarginSignBinaryVariable(linearProblem);
        updateMinimumNegativeMarginDefinition(linearProblem);
        buildMinimumRelativeMarginVariable(linearProblem);
        buildMinimumRelativeMarginConstraints(linearProblem);
        fillObjectiveWithMinRelMargin(linearProblem);
    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        if (ptdfApproximationLevel.shouldUpdatePtdfWithPstChange()) {
            optimizedCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side ->
                setOrUpdateRelativeMarginCoefficients(linearProblem, flowResult, cnec, side)
            ));
        }
    }

    private void updateMinimumNegativeMarginDefinition(LinearProblem linearProblem) {
        FaraoMPVariable minimumMarginVariable = linearProblem.getMinimumMarginVariable();
        FaraoMPVariable minRelMarginSignBinaryVariable = linearProblem.getMinimumRelativeMarginSignBinaryVariable();
        double maxNegativeRam = 5 * highestThreshold;

        // Minimum Margin is negative or zero
        minimumMarginVariable.setUb(.0);
        // Forcing miminumRelativeMarginSignBinaryVariable to 0 when minimumMarginVariable is negative
        FaraoMPConstraint minimumRelMarginSignDefinition = linearProblem.addMinimumRelMarginSignDefinitionConstraint(-LinearProblem.infinity(), maxNegativeRam);
        minimumRelMarginSignDefinition.setCoefficient(minRelMarginSignBinaryVariable, maxNegativeRam);
        minimumRelMarginSignDefinition.setCoefficient(minimumMarginVariable, -1);
    }

    /**
     * Add a new minimum relative margin variable. Unfortunately, we cannot force it to be positive since it
     * should be able to be negative in unsecured cases (see constraints)
     */
    private void buildMinimumRelativeMarginVariable(LinearProblem linearProblem) {
        if (!optimizedCnecs.isEmpty()) {
            linearProblem.addMinimumRelativeMarginVariable(-LinearProblem.infinity(), LinearProblem.infinity());
        } else {
            // if there is no Cnecs, the minRelativeMarginVariable is forced to zero.
            // otherwise it would be unbounded in the LP
            linearProblem.addMinimumRelativeMarginVariable(0.0, 0.0);
        }
    }

    /**
     * Build the  miminum relative margin sign binary variable, P.
     * P represents the sign of the minimum margin.
     */
    private void buildMinimumRelativeMarginSignBinaryVariable(LinearProblem linearProblem) {
        linearProblem.addMinimumRelativeMarginSignBinaryVariable();
    }

    /**
     * Define the minimum relative margin (like absolute margin but by dividing by sum of PTDFs)
     */
    private void buildMinimumRelativeMarginConstraints(LinearProblem linearProblem) {
        FaraoMPVariable minRelMarginVariable = linearProblem.getMinimumRelativeMarginVariable();
        FaraoMPVariable minRelMarginSignBinaryVariable = linearProblem.getMinimumRelativeMarginSignBinaryVariable();

        // Minimum Relative Margin is positive or null
        minRelMarginVariable.setLb(.0);
        // Forcing minRelMarginVariable to 0 when minimumMarginVariable is negative
        FaraoMPConstraint minimumRelativeMarginSetToZero = linearProblem.addMinimumRelMarginSetToZeroConstraint(-LinearProblem.infinity(), 0);
        minimumRelativeMarginSetToZero.setCoefficient(minRelMarginSignBinaryVariable, -maxPositiveRelativeRam);
        minimumRelativeMarginSetToZero.setCoefficient(minRelMarginVariable, 1);

        optimizedCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side ->
            setOrUpdateRelativeMarginCoefficients(linearProblem, initialFlowResult, cnec, side)
        ));
    }

    private void setOrUpdateRelativeMarginCoefficients(LinearProblem linearProblem, FlowResult flowResult, FlowCnec cnec, Side side) {
        FaraoMPVariable minRelMarginVariable = linearProblem.getMinimumRelativeMarginVariable();
        FaraoMPVariable minRelMarginSignBinaryVariable = linearProblem.getMinimumRelativeMarginSignBinaryVariable();
        FaraoMPVariable flowVariable = linearProblem.getFlowVariable(cnec, side);

        double unitConversionCoefficient = RaoUtil.getFlowUnitMultiplier(cnec, side, unit, MEGAWATT);
        double relMarginCoef = Math.max(flowResult.getPtdfZonalSum(cnec, side), ptdfSumLowerBound);

        Optional<Double> minFlow = cnec.getLowerBound(side, MEGAWATT);
        Optional<Double> maxFlow = cnec.getUpperBound(side, MEGAWATT);

        if (minFlow.isPresent()) {
            FaraoMPConstraint minimumMarginNegative;
            try {
                minimumMarginNegative = linearProblem.getMinimumRelativeMarginConstraint(cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD);
            } catch (FaraoException ignored) {
                minimumMarginNegative = linearProblem.addMinimumRelativeMarginConstraint(-LinearProblem.infinity(), LinearProblem.infinity(), cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD);
            }
            minimumMarginNegative.setUb(-minFlow.get() + unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam);
            minimumMarginNegative.setCoefficient(minRelMarginVariable, unitConversionCoefficient * relMarginCoef);
            minimumMarginNegative.setCoefficient(minRelMarginSignBinaryVariable, unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam);
            minimumMarginNegative.setCoefficient(flowVariable, -1);
        }
        if (maxFlow.isPresent()) {
            FaraoMPConstraint minimumMarginPositive;
            try {
                minimumMarginPositive = linearProblem.getMinimumRelativeMarginConstraint(cnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
            } catch (FaraoException ignored) {
                minimumMarginPositive = linearProblem.addMinimumRelativeMarginConstraint(-LinearProblem.infinity(), LinearProblem.infinity(), cnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
            }
            minimumMarginPositive.setUb(maxFlow.get() + unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam);
            minimumMarginPositive.setCoefficient(minRelMarginVariable, unitConversionCoefficient * relMarginCoef);
            minimumMarginPositive.setCoefficient(minRelMarginSignBinaryVariable, unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam);
            minimumMarginPositive.setCoefficient(flowVariable, 1);
        }
    }

    private void fillObjectiveWithMinRelMargin(LinearProblem linearProblem) {
        FaraoMPVariable minRelMarginVariable = linearProblem.getMinimumRelativeMarginVariable();
        linearProblem.getObjective().setCoefficient(minRelMarginVariable, -1);
    }
}
