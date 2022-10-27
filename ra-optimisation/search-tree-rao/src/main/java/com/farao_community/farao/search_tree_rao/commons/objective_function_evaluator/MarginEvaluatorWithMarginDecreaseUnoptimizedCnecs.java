/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;

import java.util.*;

/**
 * It enables to evaluate the absolute margin of a FlowCnec
 * For cnecs belonging to operators that do not share RAs, margin is considered infinite
 * when cnecs'margin has increased
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MarginEvaluatorWithMarginDecreaseUnoptimizedCnecs implements MarginEvaluator {
    private final MarginEvaluator marginEvaluator;
    private final Set<String> countriesNotToOptimize;
    private final FlowResult prePerimeterFlowResult;

    public MarginEvaluatorWithMarginDecreaseUnoptimizedCnecs(MarginEvaluator marginEvaluator,
                                                             Set<String> countriesNotToOptimize,
                                                             FlowResult prePerimeterFlowResult) {
        this.marginEvaluator = marginEvaluator;
        this.countriesNotToOptimize = countriesNotToOptimize;
        this.prePerimeterFlowResult = prePerimeterFlowResult;
    }

    @Override
    public double getMargin(FlowResult flowResult, FlowCnec flowCnec, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, Unit unit) {
        return flowCnec.getMonitoredSides().stream()
                .map(side -> getMargin(flowResult, flowCnec, side, rangeActionActivationResult, sensitivityResult, unit))
                .min(Double::compareTo).orElseThrow();
    }

    @Override
    public double getMargin(FlowResult flowResult, FlowCnec flowCnec, Side side, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, Unit unit) {
        double newMargin = marginEvaluator.getMargin(flowResult, flowCnec, side, rangeActionActivationResult, sensitivityResult, unit);
        double prePerimeterMargin = marginEvaluator.getMargin(prePerimeterFlowResult, flowCnec, side, rangeActionActivationResult, sensitivityResult, unit);
        return computeMargin(flowCnec, newMargin, prePerimeterMargin);
    }

    private double computeMargin(FlowCnec flowCnec, double newMargin, double prePerimeterMargin) {
        if (countriesNotToOptimize.contains(flowCnec.getOperator()) && newMargin > prePerimeterMargin - .0001 * Math.abs(prePerimeterMargin)) {
            return Double.MAX_VALUE;
        }
        return newMargin;
    }
}


