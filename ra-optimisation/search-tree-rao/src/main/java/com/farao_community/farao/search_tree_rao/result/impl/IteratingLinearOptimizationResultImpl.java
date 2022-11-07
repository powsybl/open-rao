/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizationResultImpl implements LinearOptimizationResult {

    private LinearProblemStatus status;
    private int nbOfIteration;
    private final RangeActionActivationResult rangeActionActivationResult;
    private final FlowResult flowResult;
    private final SensitivityResult sensitivityResult;
    private final ObjectiveFunctionResult objectiveFunctionResult;

    public IteratingLinearOptimizationResultImpl(LinearProblemStatus status,
                                                 int nbOfIteration,
                                                 RangeActionActivationResult rangeActionActivationResult,
                                                 FlowResult flowResult,
                                                 ObjectiveFunctionResult objectiveFunctionResult,
                                                 SensitivityResult sensitivityResult) {
        this.status = status;
        this.nbOfIteration = nbOfIteration;
        this.rangeActionActivationResult = rangeActionActivationResult;
        this.flowResult = flowResult;
        this.objectiveFunctionResult = objectiveFunctionResult;
        this.sensitivityResult = sensitivityResult;
    }

    public void setStatus(LinearProblemStatus status) {
        this.status = status;
    }

    public int getNbOfIteration() {
        return nbOfIteration;
    }

    public void setNbOfIteration(int nbOfIteration) {
        this.nbOfIteration = nbOfIteration;
    }

    public SensitivityResult getSensitivityResult() {
        return sensitivityResult;
    }

    public FlowResult getBranchResult() {
        return flowResult;
    }

    public ObjectiveFunctionResult getObjectiveFunctionResult() {
        return objectiveFunctionResult;
    }

    @Override
    public double getFunctionalCost() {
        return objectiveFunctionResult.getFunctionalCost();
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        return objectiveFunctionResult.getMostLimitingElements(number);
    }

    @Override
    public double getVirtualCost() {
        return objectiveFunctionResult.getVirtualCost();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return objectiveFunctionResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return objectiveFunctionResult.getVirtualCost(virtualCostName);
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        return objectiveFunctionResult.getCostlyElements(virtualCostName, number);
    }

    @Override
    public LinearProblemStatus getStatus() {
        return status;
    }

    @Override
    public double getFlow(FlowCnec branchCnec, Side side, Unit unit) {
        return flowResult.getFlow(branchCnec, side, unit);
    }

    @Override
    public double getCommercialFlow(FlowCnec branchCnec, Side side, Unit unit) {
        return flowResult.getCommercialFlow(branchCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec branchCnec, Side side) {
        return flowResult.getPtdfZonalSum(branchCnec, side);
    }

    @Override
    public Map<FlowCnec, Map<Side, Double>> getPtdfZonalSums() {
        return flowResult.getPtdfZonalSums();
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return rangeActionActivationResult.getRangeActions();
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        return rangeActionActivationResult.getActivatedRangeActions(state);
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        return rangeActionActivationResult.getOptimizedSetpoint(rangeAction, state);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        return rangeActionActivationResult.getOptimizedSetpointsOnState(state);
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        return rangeActionActivationResult.getOptimizedTap(pstRangeAction, state);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return rangeActionActivationResult.getOptimizedTapsOnState(state);
    }

    @Override
    public ComputationStatus getSensitivityStatus() {
        return sensitivityResult.getSensitivityStatus();
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        return sensitivityResult.getSensitivityStatus(state);
    }

    @Override
    public double getSensitivityValue(FlowCnec branchCnec, Side side, RangeAction<?> rangeAction, Unit unit) {
        return sensitivityResult.getSensitivityValue(branchCnec, side, rangeAction, unit);
    }

    @Override
    public double getSensitivityValue(FlowCnec branchCnec, Side side, SensitivityVariableSet linearGlsk, Unit unit) {
        return sensitivityResult.getSensitivityValue(branchCnec, side, linearGlsk, unit);
    }

    @Override
    public RangeActionActivationResult getRangeActionActivationResult() {
        return rangeActionActivationResult;
    }
}
