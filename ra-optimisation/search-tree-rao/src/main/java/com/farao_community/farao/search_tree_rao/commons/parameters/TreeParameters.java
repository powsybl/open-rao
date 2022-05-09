/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;

import javax.annotation.Nullable;
import java.util.*;

/**
 * This class contains internal FARAO parameters used in the SearchTree algorithm.
 * These parameters are dynamically generated by the SearchTreeRaoProvider depending on the context and on
 * the user's RAO parameters, and then used in SearchTree algorithm.
 * They should not be visible to the user.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class TreeParameters {

    public enum StopCriterion {
        MIN_OBJECTIVE,
        AT_TARGET_OBJECTIVE_VALUE
    }

    private final StopCriterion stopCriterion;
    private final double targetObjectiveValue;
    private final int maximumSearchDepth;
    private final int leavesInParallel;

    public TreeParameters(StopCriterion stopCriterion,
                           double targetObjectiveValue,
                           int maximumSearchDepth,
                           int leavesInParallel) {
        this.stopCriterion = stopCriterion;
        this.targetObjectiveValue = targetObjectiveValue;
        this.maximumSearchDepth = maximumSearchDepth;
        this.leavesInParallel = leavesInParallel;
    }

    public StopCriterion getStopCriterion() {
        return stopCriterion;
    }

    public double getTargetObjectiveValue() {
        return targetObjectiveValue;
    }

    public int getMaximumSearchDepth() {
        return maximumSearchDepth;
    }

    public int getLeavesInParallel() {
        return leavesInParallel;
    }

    public static TreeParameters buildForPreventivePerimeter(@Nullable SearchTreeRaoParameters searchTreeRaoParameters) {

        SearchTreeRaoParameters parameters = Objects.isNull(searchTreeRaoParameters) ? new SearchTreeRaoParameters() : searchTreeRaoParameters;
        switch (parameters.getPreventiveRaoStopCriterion()) {
            case MIN_OBJECTIVE:
                return new TreeParameters(StopCriterion.MIN_OBJECTIVE,
                    0.0, // value does not matter
                    parameters.getMaximumSearchDepth(),
                    parameters.getPreventiveLeavesInParallel());
            case SECURE:
                return new TreeParameters(StopCriterion.AT_TARGET_OBJECTIVE_VALUE,
                    0.0, // secure
                    parameters.getMaximumSearchDepth(),
                    parameters.getPreventiveLeavesInParallel());
            default:
                throw new FaraoException("Unknown preventive RAO stop criterion: " + parameters.getPreventiveRaoStopCriterion());
        }
    }

    public static TreeParameters buildForCurativePerimeter(@Nullable SearchTreeRaoParameters searchTreeRaoParameters, Double preventiveOptimizedCost) {
        SearchTreeRaoParameters parameters = Objects.isNull(searchTreeRaoParameters) ? new SearchTreeRaoParameters() : searchTreeRaoParameters;
        StopCriterion stopCriterion;
        double targetObjectiveValue;
        switch (parameters.getCurativeRaoStopCriterion()) {
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
                targetObjectiveValue = preventiveOptimizedCost - parameters.getCurativeRaoMinObjImprovement();
                break;
            case PREVENTIVE_OBJECTIVE_AND_SECURE:
                stopCriterion = StopCriterion.AT_TARGET_OBJECTIVE_VALUE;
                targetObjectiveValue = Math.min(preventiveOptimizedCost - parameters.getCurativeRaoMinObjImprovement(), 0);
                break;
            default:
                throw new FaraoException("Unknown curative RAO stop criterion: " + parameters.getCurativeRaoStopCriterion());
        }
        return new TreeParameters(stopCriterion,
            targetObjectiveValue,
            parameters.getMaximumSearchDepth(),
            parameters.getCurativeLeavesInParallel());
    }

    public static TreeParameters buildForSecondPreventivePerimeter(@Nullable SearchTreeRaoParameters searchTreeRaoParameters) {
        SearchTreeRaoParameters parameters = Objects.isNull(searchTreeRaoParameters) ? new SearchTreeRaoParameters() : searchTreeRaoParameters;
        if (parameters.getPreventiveRaoStopCriterion().equals(SearchTreeRaoParameters.PreventiveRaoStopCriterion.SECURE)
            && !parameters.getCurativeRaoStopCriterion().equals(SearchTreeRaoParameters.CurativeRaoStopCriterion.MIN_OBJECTIVE)) {
            return new TreeParameters(StopCriterion.AT_TARGET_OBJECTIVE_VALUE,
                0.0, // secure
                parameters.getMaximumSearchDepth(),
                parameters.getPreventiveLeavesInParallel());
        } else {
            return new TreeParameters(StopCriterion.MIN_OBJECTIVE,
                0.0, // value does not matter
                parameters.getMaximumSearchDepth(),
                parameters.getPreventiveLeavesInParallel());
        }
    }
}
