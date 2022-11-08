/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThreshold;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.commons.parameters.LoopFlowParameters;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowViolationCostEvaluator implements CostEvaluator {
    private final Set<FlowCnec> loopflowCnecs;
    private final FlowResult initialLoopFLowResult;
    private final double loopFlowViolationCost;
    private final double loopFlowAcceptableAugmentation;
    private List<FlowCnec> sortedElements = new ArrayList<>();

    public LoopFlowViolationCostEvaluator(Set<FlowCnec> loopflowCnecs,
                                          FlowResult initialLoopFlowResult,
                                          LoopFlowParameters loopFlowParameters) {
        this.loopflowCnecs = loopflowCnecs;
        this.initialLoopFLowResult = initialLoopFlowResult;
        this.loopFlowViolationCost = loopFlowParameters.getLoopFlowViolationCost();
        this.loopFlowAcceptableAugmentation = loopFlowParameters.getLoopFlowAcceptableAugmentation();
    }

    @Override
    public String getName() {
        return "loop-flow-cost";
    }

    @Override
    public double computeCost(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus) {
        double cost = loopflowCnecs
            .stream()
            .mapToDouble(cnec -> getLoopFlowExcess(flowResult, cnec) * loopFlowViolationCost)
            .sum();

        if (cost > 0) {
            FaraoLoggerProvider.TECHNICAL_LOGS.info("Some loopflow constraints are not respected.");
        }

        return cost;
    }

    @Override
    public Unit getUnit() {
        return Unit.MEGAWATT;
    }

    @Override
    public List<FlowCnec> getCostlyElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, int numberOfElements) {
        if (sortedElements.isEmpty()) {
            sortedElements = loopflowCnecs.stream()
                .collect(Collectors.toMap(
                    Function.identity(),
                    cnec -> getLoopFlowExcess(flowResult, cnec)
                ))
                .entrySet().stream()
                .filter(entry -> entry.getValue() != 0)
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }
        Collections.reverse(sortedElements);

        return sortedElements.subList(0, Math.min(sortedElements.size(), numberOfElements));
    }

    double getLoopFlowExcess(FlowResult flowResult, FlowCnec cnec) {
        return cnec.getMonitoredSides()
            .stream().map(side -> Math.max(0, Math.abs(flowResult.getLoopFlow(cnec, side, Unit.MEGAWATT)) - getLoopFlowUpperBound(cnec, side)))
            .max(Double::compareTo).orElse(0.0);
    }

    private double getLoopFlowUpperBound(FlowCnec cnec, Side side) {
        double loopFlowThreshold = cnec.getExtension(LoopFlowThreshold.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT);
        double initialLoopFlow = initialLoopFLowResult.getLoopFlow(cnec, side, Unit.MEGAWATT);
        return Math.max(0.0, Math.max(loopFlowThreshold, Math.abs(initialLoopFlow) + loopFlowAcceptableAugmentation));
    }
}
