/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.OptimizationStepsExecuted;
import org.junit.jupiter.api.Test;

import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class FailedRaoResultImplTest {
    @Test
    void testBasicReturns() {
        OptimizationState optimizationState = mock(OptimizationState.class);
        State state = mock(State.class);
        PstRangeAction pstRangeAction = mock(PstRangeAction.class);
        RangeAction rangeAction = mock(RangeAction.class);
        NetworkAction networkAction = mock(NetworkAction.class);

        FailedRaoResultImpl failedRaoResultImpl = new FailedRaoResultImpl();

        assertEquals(ComputationStatus.FAILURE, failedRaoResultImpl.getComputationStatus());
        assertEquals(ComputationStatus.FAILURE, failedRaoResultImpl.getComputationStatus(state));

        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getFunctionalCost(optimizationState));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getVirtualCost(optimizationState));
        assertThrows(FaraoException.class, failedRaoResultImpl::getVirtualCostNames);
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getVirtualCost(optimizationState, ""));

        assertThrows(FaraoException.class, () -> failedRaoResultImpl.wasActivatedBeforeState(state, networkAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.isActivatedDuringState(state, networkAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getActivatedNetworkActionsDuringState(state));

        assertThrows(FaraoException.class, () -> failedRaoResultImpl.isActivatedDuringState(state, rangeAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getPreOptimizationTapOnState(state, pstRangeAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedTapOnState(state, pstRangeAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getPreOptimizationSetPointOnState(state, rangeAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedSetPointOnState(state, rangeAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getActivatedRangeActionsDuringState(state));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedTapsOnState(state));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedSetPointsOnState(state));
        assertThrows(FaraoException.class, failedRaoResultImpl::getOptimizationStepsExecuted);
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
    }

    @Test
    void testAngleAndVoltageCnec() {
        OptimizationState optimizationState = mock(OptimizationState.class);
        FailedRaoResultImpl failedRaoResultImpl = new FailedRaoResultImpl();
        AngleCnec angleCnec = mock(AngleCnec.class);
        VoltageCnec voltageCnec = mock(VoltageCnec.class);
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getMargin(optimizationState, angleCnec, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getMargin(optimizationState, voltageCnec, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getVoltage(optimizationState, voltageCnec, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getAngle(optimizationState, angleCnec, MEGAWATT));
    }

    @Test
    void testgetFlowAndMargin() {
        OptimizationState optimizationState = mock(OptimizationState.class);
        FailedRaoResultImpl failedRaoResultImpl = new FailedRaoResultImpl();
        FlowCnec flowCnec = mock(FlowCnec.class);
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getFlow(optimizationState, flowCnec, Side.LEFT, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getCommercialFlow(optimizationState, flowCnec, Side.LEFT, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getLoopFlow(optimizationState, flowCnec, Side.LEFT, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getPtdfZonalSum(optimizationState, flowCnec, Side.LEFT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getFlow(optimizationState, flowCnec, Side.LEFT, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getMargin(optimizationState, flowCnec, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getRelativeMargin(optimizationState, flowCnec, MEGAWATT));
    }
}
