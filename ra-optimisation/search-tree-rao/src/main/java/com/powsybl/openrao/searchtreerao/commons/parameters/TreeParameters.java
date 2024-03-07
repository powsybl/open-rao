/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

/**
 * This class contains internal Open RAO parameters used in the SearchTree algorithm.
 * These parameters are dynamically generated by the SearchTreeRaoProvider depending on the context and on
 * the user's RAO parameters, and then used in SearchTree algorithm.
 * They should not be visible to the user.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public record TreeParameters(StopCriterion stopCriterion, double targetObjectiveValue, int maximumSearchDepth, int leavesInParallel, boolean raRangeShrinking) {

    public enum StopCriterion {
        MIN_OBJECTIVE,
        AT_TARGET_OBJECTIVE_VALUE
    }

    public static TreeParameters buildForPreventivePerimeter(RaoParameters parameters) {
        RangeActionsOptimizationParameters.RaRangeShrinking raRangeShrinking = parameters.getRangeActionsOptimizationParameters().getRaRangeShrinking();
        boolean shouldShrinkRaRange = raRangeShrinking.equals(RangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO) ||
            raRangeShrinking.equals(RangeActionsOptimizationParameters.RaRangeShrinking.ENABLED);
        switch (parameters.getObjectiveFunctionParameters().getPreventiveStopCriterion()) {
            case MIN_OBJECTIVE:
                return new TreeParameters(StopCriterion.MIN_OBJECTIVE,
                    0.0, // value does not matter
                    parameters.getTopoOptimizationParameters().getMaxSearchTreeDepth(),
                    parameters.getMultithreadingParameters().getPreventiveLeavesInParallel(),
                    shouldShrinkRaRange);
            case SECURE:
                return new TreeParameters(StopCriterion.AT_TARGET_OBJECTIVE_VALUE,
                    0.0, // secure
                    parameters.getTopoOptimizationParameters().getMaxSearchTreeDepth(),
                    parameters.getMultithreadingParameters().getPreventiveLeavesInParallel(),
                    shouldShrinkRaRange);
            default:
                throw new OpenRaoException("Unknown preventive stop criterion: " + parameters.getObjectiveFunctionParameters().getPreventiveStopCriterion());
        }
    }

    public static TreeParameters buildForAutomatonPerimeter(RaoParameters parameters) {
        return new TreeParameters(StopCriterion.AT_TARGET_OBJECTIVE_VALUE, 0.0, parameters.getTopoOptimizationParameters().getMaxSearchTreeDepth(), parameters.getMultithreadingParameters().getAutoLeavesInParallel(), false);
    }

    public static TreeParameters buildForCurativePerimeter(RaoParameters parameters, Double preventiveOptimizedCost) {
        StopCriterion stopCriterion;
        double targetObjectiveValue;
        switch (parameters.getObjectiveFunctionParameters().getCurativeStopCriterion()) {
            case MIN_OBJECTIVE:
                stopCriterion = StopCriterion.MIN_OBJECTIVE;
                targetObjectiveValue = 0.0;
                break;
            case SECURE:
                stopCriterion = StopCriterion.AT_TARGET_OBJECTIVE_VALUE;
                targetObjectiveValue = 0.0;
                break;
            case PREVENTIVE_OBJECTIVE:
                stopCriterion = StopCriterion.AT_TARGET_OBJECTIVE_VALUE;
                targetObjectiveValue = preventiveOptimizedCost - parameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement();
                break;
            case PREVENTIVE_OBJECTIVE_AND_SECURE:
                stopCriterion = StopCriterion.AT_TARGET_OBJECTIVE_VALUE;
                targetObjectiveValue = Math.min(preventiveOptimizedCost - parameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement(), 0);
                break;
            default:
                throw new OpenRaoException("Unknown curative stop criterion: " + parameters.getObjectiveFunctionParameters().getCurativeStopCriterion());
        }
        RangeActionsOptimizationParameters.RaRangeShrinking raRangeShrinking = parameters.getRangeActionsOptimizationParameters().getRaRangeShrinking();
        boolean shouldShrinkRaRange = raRangeShrinking.equals(RangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO) ||
            raRangeShrinking.equals(RangeActionsOptimizationParameters.RaRangeShrinking.ENABLED);
        return new TreeParameters(stopCriterion,
            targetObjectiveValue,
            parameters.getTopoOptimizationParameters().getMaxSearchTreeDepth(),
            parameters.getMultithreadingParameters().getCurativeLeavesInParallel(),
            shouldShrinkRaRange);
    }

    public static TreeParameters buildForSecondPreventivePerimeter(RaoParameters parameters) {
        boolean raRangeShrinking = parameters.getRangeActionsOptimizationParameters().getRaRangeShrinking().equals(RangeActionsOptimizationParameters.RaRangeShrinking.ENABLED);
        if (parameters.getObjectiveFunctionParameters().getPreventiveStopCriterion().equals(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE)
            && !parameters.getObjectiveFunctionParameters().getCurativeStopCriterion().equals(ObjectiveFunctionParameters.CurativeStopCriterion.MIN_OBJECTIVE)) {
            return new TreeParameters(StopCriterion.AT_TARGET_OBJECTIVE_VALUE,
                0.0, // secure
                parameters.getTopoOptimizationParameters().getMaxSearchTreeDepth(),
                parameters.getMultithreadingParameters().getPreventiveLeavesInParallel(),
                raRangeShrinking);
        } else {
            return new TreeParameters(StopCriterion.MIN_OBJECTIVE,
                0.0, // value does not matter
                parameters.getTopoOptimizationParameters().getMaxSearchTreeDepth(),
                parameters.getMultithreadingParameters().getPreventiveLeavesInParallel(),
                raRangeShrinking);
        }
    }
}
