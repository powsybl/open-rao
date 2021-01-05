/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;

import javax.annotation.Nullable;
import java.util.Objects;

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

    private StopCriterion stopCriterion;
    private double targetObjectiveValue;

    private int maximumSearchDepth;
    private double relativeNetworkActionMinimumImpactThreshold;
    private double absoluteNetworkActionMinimumImpactThreshold;
    private int leavesInParallel;

    private TreeParameters() {
    }

    private TreeParameters(SearchTreeRaoParameters searchTreeRaoParameters, StopCriterion stopCriterion, double targetObjectiveValue) {
        this.maximumSearchDepth = searchTreeRaoParameters.getMaximumSearchDepth();
        this.relativeNetworkActionMinimumImpactThreshold = searchTreeRaoParameters.getRelativeNetworkActionMinimumImpactThreshold();
        this.absoluteNetworkActionMinimumImpactThreshold = searchTreeRaoParameters.getAbsoluteNetworkActionMinimumImpactThreshold();
        this.leavesInParallel = searchTreeRaoParameters.getLeavesInParallel();
        this.stopCriterion = stopCriterion;
        this.targetObjectiveValue = targetObjectiveValue;
    }

    public StopCriterion getStopCriterion() {
        return stopCriterion;
    }

    public double getTargetObjectiveValue() {
        return targetObjectiveValue;
    }

    public static TreeParameters buildForPreventivePerimeter(@Nullable SearchTreeRaoParameters searchTreeRaoParameters) {
        SearchTreeRaoParameters parameters = Objects.isNull(searchTreeRaoParameters) ? new SearchTreeRaoParameters() : searchTreeRaoParameters;
        switch (parameters.getPreventiveRaoStopCriterion()) {
            case MIN_OBJECTIVE:
                return new TreeParameters(parameters, StopCriterion.MIN_OBJECTIVE, 0.0);
            case SECURE:
                return new TreeParameters(parameters, StopCriterion.AT_TARGET_OBJECTIVE_VALUE, 0.0);
            default:
                throw new FaraoException("Unknown preventive RAO stop criterion: " + parameters.getPreventiveRaoStopCriterion());
        }
    }

    public static TreeParameters buildForCurativePerimeter(@Nullable SearchTreeRaoParameters searchTreeRaoParameters, Double preventiveOptimizedCost) {
        SearchTreeRaoParameters parameters = Objects.isNull(searchTreeRaoParameters) ? new SearchTreeRaoParameters() : searchTreeRaoParameters;
        switch (parameters.getCurativeRaoStopCriterion()) {
            case MIN_OBJECTIVE:
                return new TreeParameters(parameters, StopCriterion.MIN_OBJECTIVE, 0.0);
            case SECURE:
                return new TreeParameters(parameters, StopCriterion.AT_TARGET_OBJECTIVE_VALUE, 0.0);
            case PREVENTIVE_OBJECTIVE:
                return new TreeParameters(parameters, StopCriterion.AT_TARGET_OBJECTIVE_VALUE,
                        preventiveOptimizedCost - parameters.getCurativeRaoMinObjImprovement());
            case PREVENTIVE_OBJECTIVE_AND_SECURE:
                return new TreeParameters(parameters, StopCriterion.AT_TARGET_OBJECTIVE_VALUE,
                        Math.min(preventiveOptimizedCost - parameters.getCurativeRaoMinObjImprovement(), 0));
            default:
                throw new FaraoException("Unknown curative RAO stop criterion: " + parameters.getCurativeRaoStopCriterion());
        }
    }

    public int getMaximumSearchDepth() {
        return maximumSearchDepth;
    }

    public double getRelativeNetworkActionMinimumImpactThreshold() {
        return relativeNetworkActionMinimumImpactThreshold;
    }

    public double getAbsoluteNetworkActionMinimumImpactThreshold() {
        return absoluteNetworkActionMinimumImpactThreshold;
    }

    public int getLeavesInParallel() {
        return leavesInParallel;
    }
}
