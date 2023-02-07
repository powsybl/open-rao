/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.OptimizationStepsExecuted;
import com.farao_community.farao.search_tree_rao.result.api.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class OneStateOnlyRaoResultImpl implements SearchTreeRaoResult {
    public static final String WRONG_STATE = "Trying to access perimeter result for the wrong state.";
    private final State optimizedState;
    private final PrePerimeterResult initialResult;
    private final OptimizationResult postOptimizationResult;
    private final Set<FlowCnec> optimizedFlowCnecs;
    private OptimizationStepsExecuted optimizationStepsExecuted = OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY;

    public OneStateOnlyRaoResultImpl(State optimizedState, PrePerimeterResult initialResult, OptimizationResult postOptimizationResult, Set<FlowCnec> optimizedFlowCnecs) {
        this.optimizedState = optimizedState;
        this.initialResult = initialResult;
        this.postOptimizationResult = postOptimizationResult;
        this.optimizedFlowCnecs = optimizedFlowCnecs;
    }

    private FlowResult getAppropriateResult(OptimizationState optimizationState, FlowCnec flowCnec) {
        if (!optimizedFlowCnecs.contains(flowCnec)) {
            throw new FaraoException("Cnec not optimized in this perimeter.");
        }
        State state = flowCnec.getState();
        if (optimizationState == OptimizationState.INITIAL) {
            return initialResult;
        }
        if (optimizedState.isPreventive()) {
            return postOptimizationResult;
        }
        if (state.isPreventive()) {
            return initialResult;
        }
        if (!optimizedState.isPreventive()) {
            Contingency optimizedContingency = optimizedState.getContingency().orElseThrow(() -> new FaraoException("Should not happen"));
            Contingency contingency = state.getContingency().orElseThrow(() -> new FaraoException("Should not happen"));
            if (optimizedContingency.equals(contingency)
                && state.compareTo(optimizedState) >= 0) {
                return postOptimizationResult;
            }
        }
        return initialResult;
    }

    @Override
    public double getMargin(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        return getAppropriateResult(optimizationState, flowCnec).getMargin(flowCnec, unit);
    }

    @Override
    public double getRelativeMargin(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        return getAppropriateResult(optimizationState, flowCnec).getRelativeMargin(flowCnec, unit);
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (initialResult.getSensitivityStatus() == ComputationStatus.FAILURE || postOptimizationResult.getSensitivityStatus() == ComputationStatus.FAILURE) {
            return ComputationStatus.FAILURE;
        }
        if (initialResult.getSensitivityStatus() == postOptimizationResult.getSensitivityStatus()) {
            return initialResult.getSensitivityStatus();
        }
        // TODO: specify what to return in case on is DEFAULT and the other one is FALLBACK
        return ComputationStatus.DEFAULT;
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return postOptimizationResult.getSensitivityStatus(state);
    }

    public PerimeterResult getPerimeterResult(OptimizationState optimizationState, State state) {
        if (!state.equals(optimizedState)) {
            // TODO : change this when getAppropriateResult will return a PerimeterResult (maybe throw an exception)
            return null;
        }
        return new PerimeterResultImpl(initialResult, postOptimizationResult);
    }

    public PerimeterResult getPostPreventivePerimeterResult() {
        if (!optimizedState.getInstant().equals(Instant.PREVENTIVE)) {
            // TODO : review this also
            throw new FaraoException(WRONG_STATE);
        }
        return new PerimeterResultImpl(initialResult, postOptimizationResult);
    }

    public PrePerimeterResult getInitialResult() {
        return initialResult;
    }

    @Override
    public double getFunctionalCost(OptimizationState optimizationState) {
        if (optimizationState == OptimizationState.INITIAL) {
            return initialResult.getFunctionalCost();
        } else {
            return postOptimizationResult.getFunctionalCost();
        }
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(OptimizationState optimizationState, int number) {
        if (optimizationState == OptimizationState.INITIAL) {
            return initialResult.getMostLimitingElements(number);
        } else {
            return postOptimizationResult.getMostLimitingElements(number);
        }
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState) {
        if (optimizationState == OptimizationState.INITIAL) {
            return initialResult.getVirtualCost();
        } else {
            return postOptimizationResult.getVirtualCost();
        }
    }

    @Override
    public Set<String> getVirtualCostNames() {
        Set<String> virtualCostNames = new HashSet<>();
        if (initialResult.getVirtualCostNames() != null) {
            virtualCostNames.addAll(initialResult.getVirtualCostNames());
        }
        if (postOptimizationResult.getVirtualCostNames() != null) {
            virtualCostNames.addAll(postOptimizationResult.getVirtualCostNames());
        }
        return virtualCostNames;
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState, String virtualCostName) {
        if (optimizationState == OptimizationState.INITIAL) {
            return initialResult.getVirtualCost(virtualCostName);
        } else {
            return postOptimizationResult.getVirtualCost(virtualCostName);
        }
    }

    @Override
    public List<FlowCnec> getCostlyElements(OptimizationState optimizationState, String virtualCostName, int number) {
        if (optimizationState == OptimizationState.INITIAL) {
            return initialResult.getCostlyElements(virtualCostName, number);
        } else {
            return postOptimizationResult.getCostlyElements(virtualCostName, number);
        }
    }

    @Override
    public boolean isActivatedDuringState(State state, RemedialAction<?> remedialAction) {
        if (remedialAction instanceof NetworkAction) {
            return isActivatedDuringState(state, (NetworkAction) remedialAction);
        } else if (remedialAction instanceof RangeAction<?>) {
            return isActivatedDuringState(state, (RangeAction<?>) remedialAction);
        } else {
            throw new FaraoException("Unrecognized remedial action type");
        }
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        if (!state.equals(optimizedState)) {
            throw new FaraoException(WRONG_STATE);
        }
        return false;
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        if (!state.equals(optimizedState)) {
            throw new FaraoException(WRONG_STATE);
        }
        return postOptimizationResult.isActivated(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        if (!state.equals(optimizedState)) {
            throw new FaraoException(WRONG_STATE);
        }
        return postOptimizationResult.getActivatedNetworkActions();
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        if (!state.equals(optimizedState)) {
            throw new FaraoException(WRONG_STATE);
        }
        return postOptimizationResult.getOptimizedSetpoint(rangeAction, state) != initialResult.getSetpoint(rangeAction);
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        if (!state.equals(optimizedState)) {
            throw new FaraoException(WRONG_STATE);
        }
        return initialResult.getTap(pstRangeAction);
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        if (!state.equals(optimizedState)) {
            throw new FaraoException(WRONG_STATE);
        }
        return postOptimizationResult.getOptimizedTap(pstRangeAction, state);
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        if (!state.equals(optimizedState)) {
            throw new FaraoException(WRONG_STATE);
        }
        return initialResult.getSetpoint(rangeAction);
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        if (!state.equals(optimizedState)) {
            throw new FaraoException(WRONG_STATE);
        }
        return postOptimizationResult.getOptimizedSetpoint(rangeAction, state);
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        if (!state.equals(optimizedState)) {
            throw new FaraoException(WRONG_STATE);
        }
        return postOptimizationResult.getRangeActions().stream().filter(rangeAction -> isActivatedDuringState(state, rangeAction)).collect(Collectors.toSet());
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        if (!state.equals(optimizedState)) {
            throw new FaraoException(WRONG_STATE);
        }
        return postOptimizationResult.getOptimizedTapsOnState(state);

    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        if (!state.equals(optimizedState)) {
            throw new FaraoException(WRONG_STATE);
        }
        return postOptimizationResult.getOptimizedSetpointsOnState(state);
    }

    @Override
    public void setOptimizationStepsExecuted(OptimizationStepsExecuted optimizationStepsExecuted) {
        if (this.optimizationStepsExecuted.isOverwritePossible(optimizationStepsExecuted)) {
            this.optimizationStepsExecuted = optimizationStepsExecuted;
        } else {
            throw new FaraoException("The RaoResult object should not be modified outside of its usual routine");
        }
    }

    @Override
    public OptimizationStepsExecuted getOptimizationStepsExecuted() {
        return optimizationStepsExecuted;
    }
}
