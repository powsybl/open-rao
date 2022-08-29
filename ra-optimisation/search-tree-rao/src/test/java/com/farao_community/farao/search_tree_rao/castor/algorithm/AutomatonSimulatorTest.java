/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.result.impl.AutomatonPerimeterResultImpl;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class AutomatonSimulatorTest {
    private AutomatonSimulator automatonSimulator;

    private Crac crac;
    private Network network;
    private State autoState;
    private RangeAction<?> ra2;
    private RangeAction<?> ra3;
    private RangeAction<?> ra4;
    private PstRangeAction ara1;
    private RangeAction<?> ara2;
    private RangeAction<?> ara3;
    private RangeAction<?> ara4;
    private RangeAction<?> ara5;
    private RangeAction<?> ara6;
    private NetworkAction na;
    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private PrePerimeterSensitivityAnalysis mockedPreAutoPerimeterSensitivityAnalysis;
    private PrePerimeterResult mockedPrePerimeterResult;

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Before
    public void setup() {
        network = Importers.loadNetwork("TestCase16NodesWith2Hvdc.xiidm", getClass().getResourceAsStream("/network/TestCase16NodesWith2Hvdc.xiidm"));
        crac = CracFactory.findDefault().create("test-crac");
        Contingency contingency1 = crac.newContingency()
            .withId("contingency1")
            .withNetworkElement("contingency1-ne")
            .add();
        cnec1 = crac.newFlowCnec()
            .withId("cnec1")
            .withNetworkElement("cnec-ne")
            .withContingency("contingency1")
            .withInstant(Instant.AUTO)
            .withNominalVoltage(220.)
            .newThreshold().withRule(BranchThresholdRule.ON_RIGHT_SIDE).withMax(1000.).withUnit(Unit.AMPERE).add()
            .add();
        cnec2 = crac.newFlowCnec()
            .withId("cnec2")
            .withNetworkElement("cnec-ne")
            .withContingency("contingency1")
            .withInstant(Instant.AUTO)
            .withNominalVoltage(220.)
            .newThreshold().withRule(BranchThresholdRule.ON_RIGHT_SIDE).withMax(1000.).withUnit(Unit.AMPERE).add()
            .add();
        autoState = crac.getState(contingency1, Instant.AUTO);
        ra2 = crac.newPstRangeAction()
            .withId("ra2")
            .withNetworkElement("ra2-ne")
            .withSpeed(2)
            .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        ra3 = crac.newPstRangeAction()
            .withId("ra3")
            .withNetworkElement("ra3-ne")
            .withSpeed(4)
            .newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec1").add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        ra4 = crac.newPstRangeAction()
            .withId("ra4")
            .withNetworkElement("ra4-ne")
            .withSpeed(4)
            .newOnFlowConstraintUsageRule().withInstant(Instant.PREVENTIVE).withFlowCnec("cnec1").add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();

        // Add 2 aligned range actions
        ara1 = crac.newPstRangeAction()
            .withId("ara1")
            .withGroupId("group1")
            .withNetworkElement("BBE2AA11 BBE3AA11 1")
            .withSpeed(3)
            .newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec1").add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, 0.1, 1, 1.1, 2, 2.1, 3, 3.1, -1, -1.1, -2, -2.1, -3, -3.1))
            .add();
        ara2 = crac.newPstRangeAction()
            .withId("ara2")
            .withGroupId("group1")
            .withNetworkElement("FFR2AA11 FFR4AA11 1")
            .withSpeed(3)
            .newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec1").add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, 0.1, 1, 1.1, 2, 2.1, 3, 3.1, -1, -1.1, -2, -2.1, -3, -3.1))
            .add();

        // Add 2 aligned range actions of different types
        ara3 = crac.newPstRangeAction()
            .withId("ara3")
            .withGroupId("group2")
            .withNetworkElement("ra2-ne")
            .withSpeed(5)
            .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        ara4 = crac.newHvdcRangeAction()
            .withId("ara4")
            .withNetworkElement("ra1-ne")
            .withGroupId("group2")
            .withSpeed(5)
            .newRange().withMax(1).withMin(-1).add()
            .add();

        // Add 2 aligned range actions with different usage methods
        ara5 = crac.newPstRangeAction()
            .withId("ara5")
            .withGroupId("group3")
            .withNetworkElement("ra2-ne")
            .withSpeed(6)
            .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        ara6 = crac.newPstRangeAction()
            .withId("ara6")
            .withGroupId("group3")
            .withNetworkElement("ra3-ne")
            .withSpeed(6)
            .newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec1").add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();

        // Add a network aciton
        na = crac.newNetworkAction()
            .withId("na")
            .newTopologicalAction().withActionType(ActionType.CLOSE).withNetworkElement("DDE3AA11 DDE4AA11 1").add()
            .newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec2").add()
            .add();

        autoState = crac.getState(contingency1, Instant.AUTO);

        RaoParameters raoParameters = new RaoParameters();
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        raoParameters.setSensitivityProvider("OpenLoadFlow");

        mockedPreAutoPerimeterSensitivityAnalysis = mock(PrePerimeterSensitivityAnalysis.class);
        mockedPrePerimeterResult = mock(PrePerimeterResult.class);
        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        automatonSimulator = new AutomatonSimulator(crac, raoParameters, null, null, mockedPrePerimeterResult, null, 0);
    }

    @Test
    public void testGatherCnecs() {
        assertEquals(2, automatonSimulator.gatherFlowCnecsForAutoRangeAction(ra2, autoState, network).size());
        assertEquals(1, automatonSimulator.gatherFlowCnecsForAutoRangeAction(ra3, autoState, network).size());
    }

    @Test
    public void testGatherCnecsError() {
        RangeAction<?> ra = Mockito.mock(RangeAction.class);
        Mockito.when(ra.getUsageMethod(autoState)).thenReturn(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, () -> automatonSimulator.gatherFlowCnecsForAutoRangeAction(ra, autoState, network));
    }

    @Test
    public void testCheckAlignedRangeActions1() {
        // OK
        assertTrue(AutomatonSimulator.checkAlignedRangeActions(autoState, List.of(ara1, ara2), List.of(ara1, ara2)));
        assertTrue(AutomatonSimulator.checkAlignedRangeActions(autoState, List.of(ara2, ara1), List.of(ara1, ara2)));
        // different types
        assertFalse(AutomatonSimulator.checkAlignedRangeActions(autoState, List.of(ara3, ara4), List.of(ara3, ara4)));
        assertFalse(AutomatonSimulator.checkAlignedRangeActions(autoState, List.of(ara4, ara3), List.of(ara3, ara4)));
        // different usage method
        assertFalse(AutomatonSimulator.checkAlignedRangeActions(autoState, List.of(ara5, ara6), List.of(ara5, ara6)));
        assertFalse(AutomatonSimulator.checkAlignedRangeActions(autoState, List.of(ara5, ra4), List.of(ara5, ra3, ra4)));
        // one unavailable RA
        assertFalse(AutomatonSimulator.checkAlignedRangeActions(autoState, List.of(ara1, ara2), List.of(ara1)));
    }

    @Test
    public void testBuildRangeActionsGroupsOrderedBySpeed() {
        PrePerimeterResult rangeActionSensitivity = Mockito.mock(PrePerimeterResult.class);
        List<List<RangeAction<?>>> result = automatonSimulator.buildRangeActionsGroupsOrderedBySpeed(rangeActionSensitivity, autoState, network);
        assertEquals(List.of(List.of(ra2), List.of(ara1, ara2), List.of(ra3)), result);
    }

    @Test
    public void testDisableAcEmulation() {
        HvdcRangeAction hvdcRa = crac.newHvdcRangeAction()
            .withId("hvdc-ra")
            .withNetworkElement("BBE2AA11 FFR3AA11 1")
            .withSpeed(1)
            .newRange().withMax(1).withMin(-1).add()
            .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
            .add();
        HvdcRangeAction hvdcRa2 = crac.newHvdcRangeAction()
            .withId("hvdc-ra2")
            .withNetworkElement("BBE2AA12 FFR3AA12 1")
            .withSpeed(1)
            .newRange().withMax(1).withMin(-1).add()
            .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
            .add();

        PrePerimeterResult prePerimeterResult = mock(PrePerimeterResult.class);
        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        PrePerimeterResult result = automatonSimulator.disableACEmulation(List.of(hvdcRa), network, mockedPreAutoPerimeterSensitivityAnalysis, prePerimeterResult);
        // check that AC emulation was disabled on HVDC
        assertFalse(network.getHvdcLine("BBE2AA11 FFR3AA11 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        // check that other HVDC was not touched
        assertTrue(network.getHvdcLine("BBE2AA12 FFR3AA12 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        // check that sensi computation has been run
        assertEquals(mockedPrePerimeterResult, result);

        // run a second time => no influence + sensi not run
        result = automatonSimulator.disableACEmulation(List.of(hvdcRa), network, mockedPreAutoPerimeterSensitivityAnalysis, prePerimeterResult);
        assertFalse(network.getHvdcLine("BBE2AA11 FFR3AA11 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertTrue(network.getHvdcLine("BBE2AA12 FFR3AA12 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertEquals(prePerimeterResult, result);

        // Test on 2 aligned HVDC RAs
        result = automatonSimulator.disableACEmulation(List.of(hvdcRa, hvdcRa2), network, mockedPreAutoPerimeterSensitivityAnalysis, prePerimeterResult);
        assertFalse(network.getHvdcLine("BBE2AA11 FFR3AA11 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertFalse(network.getHvdcLine("BBE2AA12 FFR3AA12 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertEquals(mockedPrePerimeterResult, result);

        // Test on a HVDC with no HvdcAngleDroopActivePowerControl
        network.getHvdcLine("BBE2AA11 FFR3AA11 1").removeExtension(HvdcAngleDroopActivePowerControl.class);
        result = automatonSimulator.disableACEmulation(List.of(hvdcRa), network, mockedPreAutoPerimeterSensitivityAnalysis, prePerimeterResult);
        assertEquals(prePerimeterResult, result);

        // Test on non-HVDC : nothing should happen
        result = automatonSimulator.disableACEmulation(List.of(ra2), network, mockedPreAutoPerimeterSensitivityAnalysis, prePerimeterResult);
        assertEquals(prePerimeterResult, result);
    }

    @Test
    public void testRoundUpAngleToTapWrtInitialSetpoint() {
        assertEquals(2.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, 1.2, 0.1), DOUBLE_TOLERANCE);
        assertEquals(1.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, 1.1, 0.1), DOUBLE_TOLERANCE);
        assertEquals(1.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, 0.2, 0.1), DOUBLE_TOLERANCE);
        assertEquals(0.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, 0.2, 1.1), DOUBLE_TOLERANCE);
        assertEquals(2.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, 1.2, 1.1), DOUBLE_TOLERANCE);
        assertEquals(2.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, 2.1, 2.1), DOUBLE_TOLERANCE);
        assertEquals(3.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, 3.1, 2.1), DOUBLE_TOLERANCE);
        assertEquals(-3.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, -3.1, 2.1), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeOptimalSetpoint() {
        double optimalSetpoint;
        // limit by min
        optimalSetpoint = automatonSimulator.computeOptimalSetpoint(0.1, 1100., -100., 1, ara1, -3.1, 3.1);
        assertEquals(-3.1, optimalSetpoint, DOUBLE_TOLERANCE);
        // limit by max
        optimalSetpoint = automatonSimulator.computeOptimalSetpoint(0.1, 1100., -100., -1, ara1, -3.1, 3.1);
        assertEquals(3.1, optimalSetpoint, DOUBLE_TOLERANCE);
        // Reduce flow from 1100 to 1000 with one setpoint change, sensi > 0
        optimalSetpoint = automatonSimulator.computeOptimalSetpoint(0.1, 1100., -100., 100, ara1, -3.1, 3.1);
        assertEquals(-1.1, optimalSetpoint, DOUBLE_TOLERANCE);
        // Reduce flow from 1100 to 1000 with two setpoint changes, sensi > 0
        optimalSetpoint = automatonSimulator.computeOptimalSetpoint(0.1, 1100., -100., 50, ara1, -3.1, 3.1);
        assertEquals(-2.1, optimalSetpoint, DOUBLE_TOLERANCE);
        // Increase flow from -1100 to -1000 with two setpoint changes, sensi > 0
        optimalSetpoint = automatonSimulator.computeOptimalSetpoint(0.1, -1100., -100., 50, ara1, -3.1, 3.1);
        assertEquals(2.1, optimalSetpoint, DOUBLE_TOLERANCE);
        // Reduce flow from 1100 to 1000 with two setpoint changes, sensi < 0
        optimalSetpoint = automatonSimulator.computeOptimalSetpoint(0.1, 1100., -100., -50, ara1, -3.1, 3.1);
        assertEquals(2.1, optimalSetpoint, DOUBLE_TOLERANCE);
        // Increase flow from -1100 to -1000 with two setpoint changes, sensi < 0
        optimalSetpoint = automatonSimulator.computeOptimalSetpoint(0.1, -1100., -100., -50, ara1, -3.1, 3.1);
        assertEquals(-2.1, optimalSetpoint, DOUBLE_TOLERANCE);
    }

    @Test
    public void testShiftRangeActionsUntilFlowCnecsSecureCase1() {
        FlowCnec cnec = mock(FlowCnec.class);

        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // suppose threshold is -1000, flow is -1100 then -1010 then -1000
        // getFlow is called once in every iteration
        when(mockedPrePerimeterResult.getFlow(cnec, Unit.MEGAWATT)).thenReturn(-1100., -1010., -1000.);
        // getMargin is called once before loop, once in 1st iteration, twice in second iteration
        when(mockedPrePerimeterResult.getMargin(cnec, Unit.MEGAWATT)).thenReturn(-100., -100., -10., -10., 0.);
        // suppose approx sensi is +50 on both RAs first, then +5 (so +100 then +10 total)
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, ara1, Unit.MEGAWATT)).thenReturn(50., 5.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, ara2, Unit.MEGAWATT)).thenReturn(50., 5.);
        // so PSTs should be shifted to setpoint +1.1 on first iteration, then +2.1 on second

        Pair<PrePerimeterResult, Map<RangeAction<?>, Double>> shiftResult =
            automatonSimulator.shiftRangeActionsUntilFlowCnecsSecure(List.of(ara1, ara2), Set.of(cnec), network, mockedPreAutoPerimeterSensitivityAnalysis, mockedPrePerimeterResult);
        assertEquals(2.1, shiftResult.getRight().get(ara1), DOUBLE_TOLERANCE);
        assertEquals(2.1, shiftResult.getRight().get(ara2), DOUBLE_TOLERANCE);
    }

    @Test
    public void testShiftRangeActionsUntilFlowCnecsSecureCase2() {
        FlowCnec cnec = mock(FlowCnec.class);

        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // same as case 1 but flows & sensi are inverted -> setpoints should be the same
        // getFlow is called once in every iteration
        when(mockedPrePerimeterResult.getFlow(cnec, Unit.MEGAWATT)).thenReturn(1100., 1010., 1000.);
        // getMargin is called once before loop, once in 1st iteration, twice in second iteration
        when(mockedPrePerimeterResult.getMargin(cnec, Unit.MEGAWATT)).thenReturn(-100., -100., -10., -10., 0.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, ara1, Unit.MEGAWATT)).thenReturn(-50., -5.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, ara2, Unit.MEGAWATT)).thenReturn(-50., -5.);

        Pair<PrePerimeterResult, Map<RangeAction<?>, Double>> shiftResult =
            automatonSimulator.shiftRangeActionsUntilFlowCnecsSecure(List.of(ara1, ara2), Set.of(cnec), network, mockedPreAutoPerimeterSensitivityAnalysis, mockedPrePerimeterResult);
        assertEquals(2.1, shiftResult.getRight().get(ara1), DOUBLE_TOLERANCE);
        assertEquals(2.1, shiftResult.getRight().get(ara2), DOUBLE_TOLERANCE);
    }

    @Test
    public void testShiftRangeActionsUntilFlowCnecsSecureCase3() {
        FlowCnec cnec = mock(FlowCnec.class);

        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // same as case 1 but flows are inverted -> setpoints should be inverted
        // getFlow is called once in every iteration
        when(mockedPrePerimeterResult.getFlow(cnec, Unit.MEGAWATT)).thenReturn(1100., 1010., 1000.);
        // getMargin is called once before loop, once in 1st iteration, twice in second iteration
        when(mockedPrePerimeterResult.getMargin(cnec, Unit.MEGAWATT)).thenReturn(-100., -100., -10., -10., 0.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, ara1, Unit.MEGAWATT)).thenReturn(50., 5.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, ara2, Unit.MEGAWATT)).thenReturn(50., 5.);

        Pair<PrePerimeterResult, Map<RangeAction<?>, Double>> shiftResult =
            automatonSimulator.shiftRangeActionsUntilFlowCnecsSecure(List.of(ara1, ara2), Set.of(cnec), network, mockedPreAutoPerimeterSensitivityAnalysis, mockedPrePerimeterResult);
        assertEquals(-2.1, shiftResult.getRight().get(ara1), DOUBLE_TOLERANCE);
        assertEquals(-2.1, shiftResult.getRight().get(ara2), DOUBLE_TOLERANCE);
    }

    @Test
    public void testShiftRangeActionsUntilFlowCnecsSecureCase4() {
        FlowCnec cnec = mock(FlowCnec.class);

        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // same as case 1 but sensi are inverted -> setpoints should be inverted
        // + added a cnec with sensi = 0
        // getFlow is called once in every iteration
        when(mockedPrePerimeterResult.getFlow(cnec, Unit.MEGAWATT)).thenReturn(-1100., -1010., -1000.);
        when(mockedPrePerimeterResult.getMargin(cnec, Unit.MEGAWATT)).thenReturn(-100., -100., -100., -100., -100., -10., -10., 0.);
        // getMargin is called once before loop, twice in 1st iteration when most limiting cnec is different, twice in second iteration, twice in third iteration
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, ara1, Unit.MEGAWATT)).thenReturn(-50., -5.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, ara2, Unit.MEGAWATT)).thenReturn(-50., -5.);

        FlowCnec cnec2 = mock(FlowCnec.class);
        when(mockedPrePerimeterResult.getFlow(cnec2, Unit.MEGAWATT)).thenReturn(2200.);
        when(mockedPrePerimeterResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(-200.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec2, ara1, Unit.MEGAWATT)).thenReturn(0.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec2, ara2, Unit.MEGAWATT)).thenReturn(0.);

        Pair<PrePerimeterResult, Map<RangeAction<?>, Double>> shiftResult =
            automatonSimulator.shiftRangeActionsUntilFlowCnecsSecure(List.of(ara1, ara2), Set.of(cnec, cnec2), network, mockedPreAutoPerimeterSensitivityAnalysis, mockedPrePerimeterResult);
        assertEquals(-2.1, shiftResult.getRight().get(ara1), DOUBLE_TOLERANCE);
        assertEquals(-2.1, shiftResult.getRight().get(ara2), DOUBLE_TOLERANCE);
    }

    @Test
    public void testSimulateRangeAutomatons() {
        State curativeState = mock(State.class);
        when(curativeState.getInstant()).thenReturn(Instant.CURATIVE);
        when(curativeState.getContingency()).thenReturn(Optional.of(crac.getContingency("contingency1")));

        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // only keep ara1 & ara2
        Set<String> toRemove = crac.getRangeActions().stream().map(Identifiable::getId).collect(Collectors.toSet());
        toRemove.remove("ara1");
        toRemove.remove("ara2");
        toRemove.forEach(ra -> crac.removeRemedialAction(ra));

        when(mockedPrePerimeterResult.getFlow(cnec1, Unit.MEGAWATT)).thenReturn(1100.);
        when(mockedPrePerimeterResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(-100.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec1, ara1, Unit.MEGAWATT)).thenReturn(0.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec1, ara2, Unit.MEGAWATT)).thenReturn(0.);

        AutomatonSimulator.RangeAutomatonSimulationResult result = automatonSimulator.simulateRangeAutomatons(autoState, curativeState, network, mockedPreAutoPerimeterSensitivityAnalysis, mockedPrePerimeterResult);

        assertNotNull(result);
        assertNotNull(result.getPerimeterResult());
        assertNotNull(result.getActivatedRangeActions());
        assertTrue(result.getActivatedRangeActions().isEmpty());
        assertEquals(Map.of(ara1, 0.1, ara2, 0.1), result.getRangeActionsWithSetpoint());
    }

    @Test
    public void testSimulateTopologicalAutomatons() {
        // margin < 0 => activate NA
        when(mockedPrePerimeterResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(-100.);
        AutomatonSimulator.TopoAutomatonSimulationResult result = automatonSimulator.simulateTopologicalAutomatons(autoState, network, mockedPreAutoPerimeterSensitivityAnalysis);
        assertNotNull(result);
        assertNotNull(result.getPerimeterResult());
        assertEquals(Set.of(na), result.getActivatedNetworkActions());

        // margin = 0 => activate NA
        when(mockedPrePerimeterResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(0.);
        result = automatonSimulator.simulateTopologicalAutomatons(autoState, network, mockedPreAutoPerimeterSensitivityAnalysis);
        assertNotNull(result);
        assertNotNull(result.getPerimeterResult());
        assertEquals(Set.of(na), result.getActivatedNetworkActions());

        // margin > 0 => do not activate NA
        when(mockedPrePerimeterResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(1.);
        result = automatonSimulator.simulateTopologicalAutomatons(autoState, network, mockedPreAutoPerimeterSensitivityAnalysis);
        assertNotNull(result);
        assertNotNull(result.getPerimeterResult());
        assertEquals(Set.of(), result.getActivatedNetworkActions());
    }

    @Test
    public void testSimulateAutomatonState() {
        State curativeState = mock(State.class);
        when(curativeState.getInstant()).thenReturn(Instant.CURATIVE);
        when(curativeState.getContingency()).thenReturn(Optional.of(crac.getContingency("contingency1")));

        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // only keep ara1, ara2 & na
        Set<String> toRemove = crac.getRemedialActions().stream().map(Identifiable::getId).collect(Collectors.toSet());
        toRemove.remove("ara1");
        toRemove.remove("ara2");
        toRemove.remove("na");
        toRemove.forEach(ra -> crac.removeRemedialAction(ra));

        when(mockedPrePerimeterResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(100.);

        when(mockedPrePerimeterResult.getFlow(cnec1, Unit.MEGAWATT)).thenReturn(1100.);
        when(mockedPrePerimeterResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(-100.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec1, ara1, Unit.MEGAWATT)).thenReturn(0.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec1, ara2, Unit.MEGAWATT)).thenReturn(0.);

        AutomatonPerimeterResultImpl result = automatonSimulator.simulateAutomatonState(autoState, curativeState, network);
        assertNotNull(result);
        assertEquals(Set.of(), result.getActivatedNetworkActions());
        assertEquals(Set.of(), result.getActivatedRangeActions(autoState));
        assertEquals(Map.of(ara1, 0.1, ara2, 0.1), result.getOptimizedSetpointsOnState(autoState));
    }
}
