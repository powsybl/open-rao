/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerInput;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.MaxMinMarginParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.MaxMinRelativeMarginParameters;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class ProblemFillerFactory {
    private final LinearProblem linearProblem;
    private final LinearOptimizerInput input;
    private final LinearOptimizerParameters parameters;

    public ProblemFillerFactory(LinearProblem linearProblem, LinearOptimizerInput input, LinearOptimizerParameters parameters) {
        this.linearProblem = linearProblem;
        this.input = input;
        this.parameters = parameters;
    }

    public ProblemFiller createProblemFiller(ProblemFillerType type) {
        switch (type) {
            case CORE:
                return createCoreProblemFiller();
            case MAX_MIN_MARGIN:
                return createMaxMinMarginFiller();
            case MAX_MIN_RELATIVE_MARGIN:
                return createMaxMinRelativeMarginFiller();
            case MNEC:
                return createMnecFiller();
            case MAX_LOOP_FLOW:
                return createMaxLoopFlowFiller();
            case UNOPTIMIZED_CNEC:
                return createUnoptimizedCnecFiller();
            default:
                throw new NotImplementedException(format("Problem filler %s is not handled in the factory", type));
        }
    }

    ProblemFiller createCoreProblemFiller() {
        return new CoreProblemFiller(
            linearProblem,
            input.getNetwork(),
            input.getCnecs(),
            input.getPreperimeterSetpoints(),
            parameters.getPstSensitivityThreshold());
    }

    ProblemFiller createMaxMinMarginFiller() {
        return new MaxMinMarginFiller(
            linearProblem,
            input.getCnecs().stream().filter(Cnec::isOptimized).collect(Collectors.toSet()),
            input.getRangeActions(),
            new MaxMinMarginParameters(parameters.getUnit(), parameters.getPstPenaltyCost()));
    }

    ProblemFiller createMaxMinRelativeMarginFiller() {
        return new MaxMinRelativeMarginFiller(
            linearProblem,
            input.getCnecs().stream()
                .filter(Cnec::isOptimized)
                .collect(Collectors.toMap(Function.identity(), input::getInitialAbsolutePtdfSum)),
            input.getRangeActions(),
            new MaxMinRelativeMarginParameters(
                parameters.getUnit(),
                parameters.getPstPenaltyCost(),
                parameters.getNegativeMarginObjectiveCoefficient(),
                parameters.getPtdfSumLowerBound()));
    }

    ProblemFiller createMnecFiller() {
        return new MnecFiller(
            linearProblem,
            input.getCnecs().stream()
                .filter(Cnec::isMonitored)
                .collect(Collectors.toMap(
                    Function.identity(),
                    mnec -> input.getInitialFlow(mnec, parameters.getUnit()))
                ),
            parameters.getUnit(),
            parameters.getMnecParameters());
    }

    ProblemFiller createUnoptimizedCnecFiller() {
        return new UnoptimizedCnecFiller(
            linearProblem,
            input.getCnecs().stream()
                .filter(BranchCnec::isOptimized)
                .filter(cnec -> parameters.getOperatorsNotToOptimize().contains(cnec.getOperator()))
                .collect(Collectors.toMap(
                    Function.identity(),
                    cnec -> input.getInitialFlow(cnec, MEGAWATT))),
            getLargestCnecThreshold(input.getCnecs()));
    }

    static double getLargestCnecThreshold(Set<BranchCnec> cnecs) {
        double max = 0;
        for (BranchCnec cnec : cnecs) {
            if (cnec.isOptimized()) {
                Optional<Double> minFlow = cnec.getLowerBound(Side.LEFT, MEGAWATT);
                if (minFlow.isPresent() && Math.abs(minFlow.get()) > max) {
                    max = Math.abs(minFlow.get());
                }
                Optional<Double> maxFlow = cnec.getUpperBound(Side.LEFT, MEGAWATT);
                if (maxFlow.isPresent() && Math.abs(maxFlow.get()) > max) {
                    max = Math.abs(maxFlow.get());
                }
            }
        }
        return max;
    }

    ProblemFiller createMaxLoopFlowFiller() {
        return new MaxLoopFlowFiller(
            linearProblem,
            input.getLoopflowCnecs().stream().collect(Collectors.toMap(Function.identity(), input::getInitialLoopflowInMW)),
            parameters.getLoopFlowParameters());
    }
}
