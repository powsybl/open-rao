/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_commons.objective_function_evaluator.*;

import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class ObjectiveFunctionHelper {

    private ObjectiveFunctionHelper() {
        // Should not be used
    }

    public static ObjectiveFunction.ObjectiveFunctionBuilder addMinMarginObjectiveFunction(
            Set<BranchCnec> cnecs,
            BranchResult prePerimeterBranchResult,
            ObjectiveFunction.ObjectiveFunctionBuilder objectiveFunctionBuilder,
            LinearOptimizerParameters linearOptimizerParameters) {
        MarginEvaluator marginEvaluator;
        if (linearOptimizerParameters.hasRelativeMargins()) {
            marginEvaluator = BranchResult::getRelativeMargin;
        } else {
            marginEvaluator = BranchResult::getMargin;
        }
        if (linearOptimizerParameters.getUnoptimizedCnecParameters() != null) {
            objectiveFunctionBuilder.withFunctionalCostEvaluator(new MinMarginEvaluator(
                    cnecs,
                    linearOptimizerParameters.getUnit(),
                    new MarginEvaluatorWithUnoptimizedCnecs(
                            marginEvaluator,
                            linearOptimizerParameters.getUnoptimizedCnecParameters().getOperatorsNotToOptimize(),
                            prePerimeterBranchResult
                    )
            ));
        } else {
            objectiveFunctionBuilder.withFunctionalCostEvaluator(new MinMarginEvaluator(
                    cnecs,
                    linearOptimizerParameters.getUnit(),
                    marginEvaluator
            ));
        }
        return objectiveFunctionBuilder;
    }
}
