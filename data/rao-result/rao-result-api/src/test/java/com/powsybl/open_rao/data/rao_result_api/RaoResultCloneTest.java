/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.rao_result_api;

import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.State;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_api.cnec.VoltageCnec;
import com.powsybl.open_rao.data.crac_api.network_action.NetworkAction;
import com.powsybl.open_rao.data.crac_api.range_action.HvdcRangeAction;
import com.powsybl.open_rao.data.crac_api.range_action.InjectionRangeAction;
import com.powsybl.open_rao.data.crac_api.range_action.PstRangeAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static com.powsybl.open_rao.commons.Unit.*;
import static com.powsybl.open_rao.data.crac_api.cnec.Side.LEFT;
import static com.powsybl.open_rao.data.crac_api.cnec.Side.RIGHT;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class RaoResultCloneTest {
    private Instant preventiveInstant;
    private Instant outageInstant;
    private Instant autoInstant;
    private Instant curativeInstant;

    @BeforeEach
    void setUp() {
        preventiveInstant = mock(Instant.class);
        outageInstant = mock(Instant.class);
        autoInstant = mock(Instant.class);
        curativeInstant = mock(Instant.class);
    }

    @Test
    void testAbstractRaoResultClone() {
        RaoResult raoResult = mock(RaoResult.class);
        Crac crac = Mockito.mock(Crac.class);

        when(raoResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(raoResult.getVirtualCostNames()).thenReturn(Set.of("loopFlow", "MNEC"));

        // Mocking costs results
        when(raoResult.getFunctionalCost(null)).thenReturn(100.0);
        when(raoResult.getFunctionalCost(preventiveInstant)).thenReturn(80.);
        when(raoResult.getFunctionalCost(autoInstant)).thenReturn(-20.0);
        when(raoResult.getFunctionalCost(curativeInstant)).thenReturn(-50.0);

        when(raoResult.getVirtualCost(null, "loopFlow")).thenReturn(0.0);
        when(raoResult.getVirtualCost(preventiveInstant, "loopFlow")).thenReturn(0.0);
        when(raoResult.getVirtualCost(autoInstant, "loopFlow")).thenReturn(15.0);
        when(raoResult.getVirtualCost(curativeInstant, "loopFlow")).thenReturn(10.0);

        when(raoResult.getVirtualCost(null, "MNEC")).thenReturn(0.0);
        when(raoResult.getVirtualCost(preventiveInstant, "MNEC")).thenReturn(0.0);
        when(raoResult.getVirtualCost(autoInstant, "MNEC")).thenReturn(20.0);
        when(raoResult.getVirtualCost(curativeInstant, "MNEC")).thenReturn(2.0);

        when(raoResult.getVirtualCost(null)).thenReturn(0.0);
        when(raoResult.getVirtualCost(preventiveInstant)).thenReturn(0.0);
        when(raoResult.getVirtualCost(autoInstant)).thenReturn(35.0);
        when(raoResult.getVirtualCost(curativeInstant)).thenReturn(12.0);

        when(raoResult.getCost(null)).thenReturn(100.0);
        when(raoResult.getCost(preventiveInstant)).thenReturn(80.);
        when(raoResult.getCost(autoInstant)).thenReturn(15.0);
        when(raoResult.getCost(curativeInstant)).thenReturn(-38.0);

        // Mocking flowCnec results
        FlowCnec cnecP = mock(FlowCnec.class);
        when(crac.getFlowCnec("cnec4prevId")).thenReturn(cnecP);
        Mockito.when(raoResult.getFlow(null, cnecP, RIGHT, MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(raoResult.getFlow(null, cnecP, LEFT, MEGAWATT)).thenReturn(4110.0);

        Mockito.when(raoResult.getFlow(preventiveInstant, cnecP, RIGHT, AMPERE)).thenReturn(Double.NaN);
        Mockito.when(raoResult.getFlow(preventiveInstant, cnecP, LEFT, AMPERE)).thenReturn(4220.);
        Mockito.when(raoResult.getFlow(autoInstant, cnecP, RIGHT, AMPERE)).thenReturn(Double.NaN);
        Mockito.when(raoResult.getFlow(autoInstant, cnecP, LEFT, AMPERE)).thenReturn(4220.);
        Mockito.when(raoResult.getFlow(curativeInstant, cnecP, RIGHT, AMPERE)).thenReturn(Double.NaN);
        Mockito.when(raoResult.getFlow(curativeInstant, cnecP, LEFT, AMPERE)).thenReturn(4220.);

        Mockito.when(raoResult.getMargin(null, cnecP, MEGAWATT)).thenReturn(4111.);
        Mockito.when(raoResult.getMargin(preventiveInstant, cnecP, AMPERE)).thenReturn(4221.);

        Mockito.when(raoResult.getRelativeMargin(null, cnecP, MEGAWATT)).thenReturn(4112.);
        Mockito.when(raoResult.getRelativeMargin(null, cnecP, AMPERE)).thenReturn(4221.);
        Mockito.when(raoResult.getRelativeMargin(preventiveInstant, cnecP, AMPERE)).thenReturn(4222.);

        Mockito.when(raoResult.getLoopFlow(null, cnecP, RIGHT, MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(raoResult.getLoopFlow(null, cnecP, LEFT, MEGAWATT)).thenReturn(Double.NaN);

        Mockito.when(raoResult.getCommercialFlow(null, cnecP, RIGHT, MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(raoResult.getCommercialFlow(null, cnecP, LEFT, MEGAWATT)).thenReturn(Double.NaN);

        Mockito.when(raoResult.getPtdfZonalSum(preventiveInstant, cnecP, LEFT)).thenReturn(0.4);
        Mockito.when(raoResult.getPtdfZonalSum(preventiveInstant, cnecP, RIGHT)).thenReturn(Double.NaN);

        FlowCnec cnecO = Mockito.mock(FlowCnec.class);
        Mockito.when(cnecO.getId()).thenReturn("cnec1outageId");
        Mockito.when(crac.getFlowCnec("cnec1outageId")).thenReturn(cnecO);
        Mockito.when(raoResult.getFlow(null, cnecO, Side.LEFT, AMPERE)).thenReturn(Double.NaN);
        Mockito.when(raoResult.getFlow(null, cnecO, Side.RIGHT, AMPERE)).thenReturn(1120.5);
        Mockito.when(raoResult.getMargin(null, cnecO, AMPERE)).thenReturn(1121.);
        Mockito.when(raoResult.getRelativeMargin(null, cnecO, AMPERE)).thenReturn(1122.);
        Mockito.when(raoResult.getLoopFlow(null, cnecO, Side.LEFT, AMPERE)).thenReturn(Double.NaN);
        Mockito.when(raoResult.getLoopFlow(null, cnecO, Side.RIGHT, AMPERE)).thenReturn(1123.5);
        Mockito.when(raoResult.getCommercialFlow(null, cnecO, Side.LEFT, AMPERE)).thenReturn(Double.NaN);
        Mockito.when(raoResult.getCommercialFlow(null, cnecO, Side.RIGHT, AMPERE)).thenReturn(1124.5);
        Mockito.when(raoResult.getFlow(preventiveInstant, cnecO, Side.LEFT, MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(raoResult.getFlow(preventiveInstant, cnecO, Side.RIGHT, MEGAWATT)).thenReturn(1210.5);
        Mockito.when(raoResult.getMargin(preventiveInstant, cnecO, MEGAWATT)).thenReturn(1211.);
        Mockito.when(raoResult.getRelativeMargin(preventiveInstant, cnecO, MEGAWATT)).thenReturn(1212.);
        Mockito.when(raoResult.getLoopFlow(preventiveInstant, cnecO, Side.LEFT, MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(raoResult.getLoopFlow(preventiveInstant, cnecO, Side.RIGHT, MEGAWATT)).thenReturn(1213.5);
        Mockito.when(raoResult.getCommercialFlow(preventiveInstant, cnecO, Side.LEFT, MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(raoResult.getCommercialFlow(preventiveInstant, cnecO, Side.RIGHT, MEGAWATT)).thenReturn(1214.5);
        Mockito.when(raoResult.getPtdfZonalSum(preventiveInstant, cnecO, Side.LEFT)).thenReturn(Double.NaN);
        Mockito.when(raoResult.getPtdfZonalSum(preventiveInstant, cnecO, Side.RIGHT)).thenReturn(0.6);

        // Mocking networkAction results
        State pState = mock(State.class);
        State cState1 = mock(State.class);
        State cState2 = mock(State.class);
        NetworkAction naP = mock(NetworkAction.class);
        NetworkAction naA = mock(NetworkAction.class);
        NetworkAction naC = mock(NetworkAction.class);
        NetworkAction naN = mock(NetworkAction.class);

        when(crac.getPreventiveState()).thenReturn(pState);
        when(crac.getState("contingency1Id", curativeInstant)).thenReturn(cState1);
        when(crac.getState("contingency2Id", curativeInstant)).thenReturn(cState2);
        when(crac.getNetworkAction("complexNetworkActionId")).thenReturn(naP);
        when(crac.getNetworkAction("injectionSetpointRaId")).thenReturn(naA);
        when(crac.getNetworkAction("pstSetpointRaId")).thenReturn(naC);
        when(crac.getNetworkAction("switchPairRaId")).thenReturn(naN);

        when(raoResult.isActivatedDuringState(pState, naP)).thenReturn(true);
        when(raoResult.isActivated(pState, naP)).thenReturn(true);
        // Mock other methods for NetworkAction as needed

        // Mocking pstRangeAction results
        PstRangeAction pstP = mock(PstRangeAction.class);
        PstRangeAction pstN = mock(PstRangeAction.class);

        when(crac.getPstRangeAction("pstRange1Id")).thenReturn(pstP);
        when(crac.getPstRangeAction("pstRange2Id")).thenReturn(pstN);

        when(raoResult.isActivatedDuringState(pState, pstP)).thenReturn(true);
        // Mock other methods for PstRangeAction as needed

        // Mocking hvdcRangeAction results
        HvdcRangeAction hvdcC = mock(HvdcRangeAction.class);
        when(crac.getHvdcRangeAction("hvdcRange2Id")).thenReturn(hvdcC);

        when(raoResult.isActivatedDuringState(pState, hvdcC)).thenReturn(false);
        // Mock other methods for HvdcRangeAction as needed

        // Mocking injectionRangeAction results
        InjectionRangeAction injectionC = mock(InjectionRangeAction.class);
        when(crac.getInjectionRangeAction("injectionRange1Id")).thenReturn(injectionC);

        when(raoResult.isActivatedDuringState(pState, injectionC)).thenReturn(false);
        // Mock other methods for InjectionRangeAction as needed

        // Mocking voltageCnec results
        VoltageCnec voltageCnec = mock(VoltageCnec.class);
        when(crac.getVoltageCnec("voltageCnecId")).thenReturn(voltageCnec);

        when(raoResult.getVoltage(eq(curativeInstant), eq(voltageCnec), any())).thenReturn(144.38);
        // Mock other methods for VoltageCnec as needed

        // Mocking computation status map
        when(raoResult.getComputationStatus(pState)).thenReturn(ComputationStatus.DEFAULT);
        when(raoResult.getComputationStatus(cState1)).thenReturn(ComputationStatus.FAILURE);
        when(raoResult.getComputationStatus(cState2)).thenReturn(ComputationStatus.DEFAULT);

        when(raoResult.getComputationStatus(crac.getPreventiveState())).thenReturn(ComputationStatus.DEFAULT);
        when(raoResult.getComputationStatus(crac.getState("contingency1Id", curativeInstant))).thenReturn(ComputationStatus.FAILURE);
        when(raoResult.getComputationStatus(crac.getState("contingency2Id", autoInstant))).thenReturn(ComputationStatus.DEFAULT);

        testRaoResultClone(new RaoResultClone(raoResult), crac);

    }

    void testRaoResultClone(RaoResult raoResultClone, Crac crac) {

        // --------------------------
        // --- test Costs results ---
        // --------------------------
        assertEquals(Set.of("loopFlow", "MNEC"), raoResultClone.getVirtualCostNames());

        assertEquals(100., raoResultClone.getFunctionalCost(null), 0.001);
        assertEquals(0., raoResultClone.getVirtualCost(null, "loopFlow"), 0.001);
        assertEquals(0., raoResultClone.getVirtualCost(null, "MNEC"), 0.001);
        assertEquals(0., raoResultClone.getVirtualCost(null), 0.001);
        assertEquals(100., raoResultClone.getCost(null), 0.001);

        assertEquals(80., raoResultClone.getFunctionalCost(preventiveInstant), 0.001);
        assertEquals(0., raoResultClone.getVirtualCost(preventiveInstant, "loopFlow"), 0.001);
        assertEquals(0., raoResultClone.getVirtualCost(preventiveInstant, "MNEC"), 0.001);
        assertEquals(0., raoResultClone.getVirtualCost(preventiveInstant), 0.001);
        assertEquals(80., raoResultClone.getCost(preventiveInstant), 0.001);

        assertEquals(-20., raoResultClone.getFunctionalCost(autoInstant), 0.001);
        assertEquals(15., raoResultClone.getVirtualCost(autoInstant, "loopFlow"), 0.001);
        assertEquals(20., raoResultClone.getVirtualCost(autoInstant, "MNEC"), 0.001);
        assertEquals(35., raoResultClone.getVirtualCost(autoInstant), 0.001);
        assertEquals(15., raoResultClone.getCost(autoInstant), 0.001);

        assertEquals(-50., raoResultClone.getFunctionalCost(curativeInstant), 0.001);
        assertEquals(10., raoResultClone.getVirtualCost(curativeInstant, "loopFlow"), 0.001);
        assertEquals(2., raoResultClone.getVirtualCost(curativeInstant, "MNEC"), 0.001);
        assertEquals(12., raoResultClone.getVirtualCost(curativeInstant), 0.001);
        assertEquals(-38, raoResultClone.getCost(curativeInstant), 0.001);

        // -----------------------------
        // --- test FlowCnec results ---
        // -----------------------------

        /*
        cnec4prevId: preventive, no loop-flows, optimized
        - contains result in null and in PREVENTIVE. Results in AUTO and CURATIVE are the same as PREVENTIVE because the CNEC is preventive
        - contains result relative margin and PTDF sum but not for loop and commercial flows
         */
        FlowCnec cnecP = crac.getFlowCnec("cnec4prevId");
        assertEquals(4110., raoResultClone.getFlow(null, cnecP, Side.LEFT, MEGAWATT), 0.001);
        assertTrue(Double.isNaN(raoResultClone.getFlow(null, cnecP, Side.RIGHT, MEGAWATT)));
        assertEquals(4111., raoResultClone.getMargin(null, cnecP, MEGAWATT), 0.001);
        assertEquals(4112., raoResultClone.getRelativeMargin(null, cnecP, MEGAWATT), 0.001);
        assertTrue(Double.isNaN(raoResultClone.getLoopFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(raoResultClone.getLoopFlow(null, cnecP, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(raoResultClone.getCommercialFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(raoResultClone.getCommercialFlow(null, cnecP, Side.RIGHT, MEGAWATT)));

        assertEquals(4220., raoResultClone.getFlow(preventiveInstant, cnecP, Side.LEFT, AMPERE), 0.001);
        assertTrue(Double.isNaN(raoResultClone.getFlow(preventiveInstant, cnecP, Side.RIGHT, AMPERE)));
        assertEquals(4221., raoResultClone.getMargin(preventiveInstant, cnecP, AMPERE), 0.001);
        assertEquals(4222., raoResultClone.getRelativeMargin(preventiveInstant, cnecP, AMPERE), 0.001);
        assertTrue(Double.isNaN(raoResultClone.getLoopFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(raoResultClone.getLoopFlow(null, cnecP, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(raoResultClone.getCommercialFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(raoResultClone.getCommercialFlow(null, cnecP, Side.RIGHT, MEGAWATT)));

        assertEquals(0.4, raoResultClone.getPtdfZonalSum(preventiveInstant, cnecP, Side.LEFT), 0.001);
        assertTrue(Double.isNaN(raoResultClone.getPtdfZonalSum(preventiveInstant, cnecP, Side.RIGHT)));

        assertEquals(raoResultClone.getFlow(autoInstant, cnecP, LEFT, AMPERE), raoResultClone.getFlow(preventiveInstant, cnecP, LEFT, AMPERE), 0.001);
        assertEquals(raoResultClone.getFlow(autoInstant, cnecP, RIGHT, AMPERE), raoResultClone.getFlow(preventiveInstant, cnecP, RIGHT, AMPERE), 0.001);
        assertEquals(raoResultClone.getFlow(curativeInstant, cnecP, LEFT, AMPERE), raoResultClone.getFlow(preventiveInstant, cnecP, LEFT, AMPERE), 0.001);
        assertEquals(raoResultClone.getFlow(curativeInstant, cnecP, RIGHT, AMPERE), raoResultClone.getFlow(preventiveInstant, cnecP, RIGHT, AMPERE), 0.001);

        /*
        cnec1outageId: outage, with loop-flows, optimized
        - contains result in null and in PREVENTIVE. Results in AUTO and CURATIVE are the same as PREVENTIVE because the CNEC is preventive
        - contains result for loop-flows, commercial flows, relative margin and PTDF sum
         */

        FlowCnec cnecO = crac.getFlowCnec("cnec1outageId");
        assertTrue(Double.isNaN(raoResultClone.getFlow(null, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1120.5, raoResultClone.getFlow(null, cnecO, Side.RIGHT, AMPERE), 0.001);
        assertEquals(1121., raoResultClone.getMargin(null, cnecO, AMPERE), 0.001);
        assertEquals(1122., raoResultClone.getRelativeMargin(null, cnecO, AMPERE), 0.001);
        assertTrue(Double.isNaN(raoResultClone.getLoopFlow(null, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1123.5, raoResultClone.getLoopFlow(null, cnecO, Side.RIGHT, AMPERE), 0.001);
        assertTrue(Double.isNaN(raoResultClone.getCommercialFlow(null, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1124.5, raoResultClone.getCommercialFlow(null, cnecO, Side.RIGHT, AMPERE), 0.001);

        assertTrue(Double.isNaN(raoResultClone.getFlow(preventiveInstant, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1210.5, raoResultClone.getFlow(preventiveInstant, cnecO, Side.RIGHT, MEGAWATT), 0.001);
        assertEquals(1211., raoResultClone.getMargin(preventiveInstant, cnecO, MEGAWATT), 0.001);
        assertEquals(1212., raoResultClone.getRelativeMargin(preventiveInstant, cnecO, MEGAWATT), 0.001);
        assertTrue(Double.isNaN(raoResultClone.getLoopFlow(preventiveInstant, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1213.5, raoResultClone.getLoopFlow(preventiveInstant, cnecO, Side.RIGHT, MEGAWATT), 0.001);
        assertTrue(Double.isNaN(raoResultClone.getCommercialFlow(preventiveInstant, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1214.5, raoResultClone.getCommercialFlow(preventiveInstant, cnecO, Side.RIGHT, MEGAWATT), 0.001);

        assertTrue(Double.isNaN(raoResultClone.getPtdfZonalSum(preventiveInstant, cnecO, Side.LEFT)));
        assertEquals(0.6, raoResultClone.getPtdfZonalSum(preventiveInstant, cnecO, Side.RIGHT), 0.001);

        // -----------------------------
        // --- NetworkAction results ---
        // -----------------------------

        State pState = crac.getPreventiveState();
        State oState2 = crac.getState("contingency2Id", outageInstant);
        State aState2 = crac.getState("contingency2Id", autoInstant);
        State cState1 = crac.getState("contingency1Id", curativeInstant);
        State cState2 = crac.getState("contingency2Id", curativeInstant);

        /*
        complexNetworkActionId, activated in preventive
        */
        NetworkAction naP = crac.getNetworkAction("complexNetworkActionId");
        assertTrue(raoResultClone.isActivatedDuringState(pState, naP));
        assertTrue(raoResultClone.isActivated(pState, naP));
        assertFalse(raoResultClone.isActivatedDuringState(oState2, naP));
        assertFalse(raoResultClone.isActivatedDuringState(aState2, naP));
        assertFalse(raoResultClone.isActivatedDuringState(cState2, naP));

        /*
        injectionSetpointRaId, activated in auto
        */
        NetworkAction naA = crac.getNetworkAction("injectionSetpointRaId");
        assertFalse(raoResultClone.isActivatedDuringState(pState, naA));
        assertFalse(raoResultClone.isActivated(pState, naA));
        assertFalse(raoResultClone.isActivatedDuringState(oState2, naA));
        assertFalse(raoResultClone.isActivatedDuringState(cState1, naA));
        assertFalse(raoResultClone.isActivatedDuringState(cState2, naA));
        assertFalse(raoResultClone.isActivated(cState1, naA));

        /*
        pstSetpointRaId, activated curative1
        */
        NetworkAction naC = crac.getNetworkAction("pstSetpointRaId");
        assertFalse(raoResultClone.isActivatedDuringState(pState, naC));
        assertFalse(raoResultClone.isActivated(pState, naC));
        assertFalse(raoResultClone.isActivatedDuringState(oState2, naC));
        assertFalse(raoResultClone.isActivatedDuringState(aState2, naC));
        assertFalse(raoResultClone.isActivatedDuringState(cState2, naC));
        assertFalse(raoResultClone.isActivated(cState2, naC));

        /*
        switchPairRaId, never activated
        */
        NetworkAction naN = crac.getNetworkAction("switchPairRaId");
        assertFalse(raoResultClone.isActivatedDuringState(pState, naN));
        assertFalse(raoResultClone.isActivated(pState, naN));
        assertFalse(raoResultClone.isActivatedDuringState(oState2, naN));
        assertFalse(raoResultClone.isActivatedDuringState(aState2, naN));
        assertFalse(raoResultClone.isActivatedDuringState(cState1, naN));
        assertFalse(raoResultClone.isActivatedDuringState(cState2, naN));
        assertFalse(raoResultClone.isActivated(cState1, naN));
        assertFalse(raoResultClone.isActivated(cState2, naN));

        // ------------------------------
        // --- PstRangeAction results ---
        // ------------------------------

        /*
        pstRange1Id, activated in preventive
        */
        PstRangeAction pstP = crac.getPstRangeAction("pstRange1Id");
        assertTrue(raoResultClone.isActivatedDuringState(pState, pstP));
        assertFalse(raoResultClone.isActivatedDuringState(cState1, pstP));
        assertFalse(raoResultClone.isActivatedDuringState(cState2, pstP));
        assertEquals(0., raoResultClone.getPreOptimizationTapOnState(pState, pstP));
        assertEquals(0., raoResultClone.getPreOptimizationSetPointOnState(pState, pstP), 0.001);
        assertEquals(0., raoResultClone.getOptimizedTapOnState(pState, pstP));
        assertEquals(0., raoResultClone.getOptimizedSetPointOnState(pState, pstP), 0.001);
        assertEquals(0., raoResultClone.getPreOptimizationSetPointOnState(cState1, pstP), 0.001);
        assertEquals(0., raoResultClone.getPreOptimizationTapOnState(cState1, pstP));
        assertEquals(0., raoResultClone.getOptimizedTapOnState(cState1, pstP));
        assertEquals(0., raoResultClone.getOptimizedTapOnState(cState2, pstP));

        /*
        pstRange2Id, not activated
        */
        PstRangeAction pstN = crac.getPstRangeAction("pstRange2Id");
        assertFalse(raoResultClone.isActivatedDuringState(pState, pstN));
        assertFalse(raoResultClone.isActivatedDuringState(cState1, pstN));
        assertFalse(raoResultClone.isActivatedDuringState(cState2, pstN));
        assertEquals(0, raoResultClone.getPreOptimizationTapOnState(pState, pstN));
        assertEquals(0, raoResultClone.getOptimizedTapOnState(pState, pstN));
        assertEquals(0, raoResultClone.getOptimizedTapOnState(cState1, pstN));
        assertEquals(0, raoResultClone.getOptimizedTapOnState(cState2, pstN));

        // ---------------------------
        // --- RangeAction results ---
        // ---------------------------

        /*
        hvdcRange2Id, two different activations in the two curative states
        */
        HvdcRangeAction hvdcC = crac.getHvdcRangeAction("hvdcRange2Id");
        assertFalse(raoResultClone.isActivatedDuringState(pState, hvdcC));
        assertEquals(0., raoResultClone.getPreOptimizationSetPointOnState(pState, hvdcC), 0.001);
        assertEquals(0., raoResultClone.getOptimizedSetPointOnState(pState, hvdcC), 0.001);
        assertEquals(0., raoResultClone.getPreOptimizationSetPointOnState(cState1, hvdcC), 0.001);
        assertEquals(0., raoResultClone.getOptimizedSetPointOnState(cState1, hvdcC), 0.001);
        assertEquals(0., raoResultClone.getOptimizedSetPointOnState(cState2, hvdcC), 0.001);

        /*
        InjectionRange1Id, one activation in curative
        */
        InjectionRangeAction injectionC = crac.getInjectionRangeAction("injectionRange1Id");
        assertFalse(raoResultClone.isActivatedDuringState(pState, injectionC));
        assertFalse(raoResultClone.isActivatedDuringState(cState2, injectionC));
        assertEquals(0., raoResultClone.getPreOptimizationSetPointOnState(pState, injectionC), 0.001);
        assertEquals(0., raoResultClone.getPreOptimizationSetPointOnState(cState1, injectionC), 0.001);
        assertEquals(0., raoResultClone.getOptimizedSetPointOnState(cState1, injectionC), 0.001);

        // Test computation status map
        assertEquals(ComputationStatus.DEFAULT, raoResultClone.getComputationStatus(crac.getPreventiveState()));
        assertEquals(ComputationStatus.FAILURE, raoResultClone.getComputationStatus(crac.getState("contingency1Id", curativeInstant)));
        assertEquals(ComputationStatus.DEFAULT, raoResultClone.getComputationStatus(crac.getState("contingency2Id", autoInstant)));
    }
}
