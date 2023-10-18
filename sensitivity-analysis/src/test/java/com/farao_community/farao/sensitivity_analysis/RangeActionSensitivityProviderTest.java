/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFunctionType;
import com.powsybl.sensitivity.SensitivityVariableType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class RangeActionSensitivityProviderTest {

    @Test
    void contingenciesCracPstWithRange() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addHvdcLine(network);

        network.getSubstation("BBE1AA").newVoltageLevel().setId("BBE1AA2").setNominalV(225).setTopologyKind(TopologyKind.NODE_BREAKER).add();
        network.getVoltageLevel("BBE1AA2").getNodeBreakerView().newBusbarSection().setId("BB1").setNode(1).add();

        Crac crac = CommonCracCreation.createWithPreventivePstRange();

        crac.newContingency()
            .withId("contingency-generator")
            .withNetworkElement("BBE1AA1 _generator")
            .add();

        crac.newContingency()
            .withId("contingency-hvdc")
            .withNetworkElement("HVDC1")
            .add();

        crac.newContingency()
            .withId("contingency-busbar-section")
            .withNetworkElement("BB1")
            .add();

        crac.newFlowCnec()
            .withId("generatorContingencyCnec")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .newThreshold()
            .withUnit(Unit.AMPERE)
            .withSide(Side.LEFT)
            .withMin(-10.)
            .withMax(10.)
            .add()
            .withNominalVoltage(380.)
            .withInstantId(Instant.CURATIVE)
            .withContingency("contingency-generator")
            .add();

        crac.newFlowCnec()
            .withId("hvdcContingencyCnec")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .newThreshold()
            .withUnit(Unit.AMPERE)
            .withSide(Side.LEFT)
            .withMin(-10.)
            .withMax(10.)
            .add()
            .withNominalVoltage(380.)
            .withInstantId(Instant.CURATIVE)
            .withContingency("contingency-hvdc")
            .add();

        crac.newFlowCnec()
            .withId("busbarContingencyCnec")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .newThreshold()
            .withUnit(Unit.AMPERE)
            .withSide(Side.LEFT)
            .withMin(-10.)
            .withMax(10.)
            .add()
            .withNominalVoltage(380.)
            .withInstantId(Instant.CURATIVE)
            .withContingency("contingency-busbar-section")
            .add();

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));

        // Common Crac contains 6 CNEC (2 network element) and 1 range action
        List<Contingency> contingencyList = provider.getContingencies(network);
        assertEquals(5, contingencyList.size());
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("Contingency FR1 FR3")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("Contingency FR1 FR2")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("contingency-generator")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("contingency-hvdc")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("contingency-busbar-section")));
    }

    @Test
    void testDisableFactorForBaseCase() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange();
        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));

        // factors with basecase and contingency
        assertEquals(4, provider.getBasecaseFactors(network).size());
        assertEquals(4, provider.getContingencyFactors(network, List.of(new Contingency("Contingency FR1 FR3", new ArrayList<>()))).size());

        provider.disableFactorsForBaseCaseSituation();

        // factors after disabling basecase
        assertEquals(0, provider.getBasecaseFactors(network).size());
        assertEquals(4, provider.getContingencyFactors(network, List.of(new Contingency("Contingency FR1 FR3", new ArrayList<>()))).size());
    }

    @Test
    void testFailureOnContingency() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange();

        crac.newContingency()
            .withId("contingency-fail")
            .withNetworkElement("FFR3AA1")
            .add();

        crac.newFlowCnec()
            .withId("failureCnec")
            .withNetworkElement("BBE1AA1  BBE3AA1  1")
            .newThreshold()
            .withUnit(Unit.AMPERE)
            .withSide(Side.LEFT)
            .withMin(-10.)
            .withMax(10.)
            .add()
            .withInstantId(Instant.CURATIVE)
            .withContingency("contingency-fail")
            .withNominalVoltage(380.)
            .add();

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(new HashSet<>(),
            Set.of(crac.getFlowCnec("failureCnec")), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));
        assertThrows(FaraoException.class, () -> provider.getContingencies(network));
    }

    @Test
    void factorsCracPstWithRange() {
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(Side.LEFT, Side.RIGHT));
        Network network = NetworkImportsUtil.import12NodesNetwork();

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        // Common Crac contains 6 CNEC (2 network elements) and 1 range action
        List<SensitivityFactor> factorList = provider.getBasecaseFactors(network);
        assertEquals(8, factorList.size());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_1
                && factor.getVariableType() == SensitivityVariableType.TRANSFORMER_PHASE).count());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_2
                && factor.getVariableType() == SensitivityVariableType.TRANSFORMER_PHASE).count());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_CURRENT_1
                && factor.getVariableType() == SensitivityVariableType.TRANSFORMER_PHASE).count());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_CURRENT_2
                && factor.getVariableType() == SensitivityVariableType.TRANSFORMER_PHASE).count());
    }

    @Test
    void cracWithoutRangeActionButWithPst() {
        Crac crac = CommonCracCreation.create();
        Network network = NetworkImportsUtil.import12NodesNetwork();

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(crac.getRangeActions(),
            crac.getFlowCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));

        // Common Crac contains 6 CNEC and 1 range action
        List<SensitivityFactor> factorList = provider.getBasecaseFactors(network);
        assertEquals(4, factorList.size());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_1
                && factor.getVariableType() == SensitivityVariableType.TRANSFORMER_PHASE).count());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_CURRENT_1
                && factor.getVariableType() == SensitivityVariableType.TRANSFORMER_PHASE).count());
    }

    @Test
    void cracWithoutRangeActionNorPst() {
        Crac crac = CommonCracCreation.create(Set.of(Side.LEFT, Side.RIGHT));
        Network network = NetworkImportsUtil.import12NodesNoPstNetwork();

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        // Common Crac contains 6 CNEC and 1 range action
        List<SensitivityFactor> factorList = provider.getBasecaseFactors(network);
        assertEquals(8, factorList.size());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_1
                && factor.getVariableType() == SensitivityVariableType.INJECTION_ACTIVE_POWER).count());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_2
                && factor.getVariableType() == SensitivityVariableType.INJECTION_ACTIVE_POWER).count());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_CURRENT_1
                && factor.getVariableType() == SensitivityVariableType.INJECTION_ACTIVE_POWER).count());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_CURRENT_2
                && factor.getVariableType() == SensitivityVariableType.INJECTION_ACTIVE_POWER).count());
    }

    @Test
    void testHvdcSensi() {
        Crac crac = CracFactory.findDefault().create("test-crac");
        FlowCnec flowCnec = crac.newFlowCnec()
            .withId("cnec")
            .withNetworkElement("BBE1AA11 FFR5AA11 1")
            .withInstantId(Instant.PREVENTIVE)
            .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
            .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(Side.RIGHT).add()
            .add();

        Network network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));

        NetworkElement hvdc = Mockito.mock(NetworkElement.class);
        Mockito.when(hvdc.getId()).thenReturn("BBE2AA11 FFR3AA11 1");
        HvdcRangeAction mockHvdcRangeAction = Mockito.mock(HvdcRangeAction.class);
        Mockito.when(mockHvdcRangeAction.getNetworkElement()).thenReturn(hvdc);

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(Set.of(mockHvdcRangeAction), Set.of(flowCnec), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        List<SensitivityFactor> factorList = provider.getBasecaseFactors(network);

        assertEquals(4, factorList.size());

        assertTrue(factorList.stream().anyMatch(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_1
                && factor.getVariableType() == SensitivityVariableType.HVDC_LINE_ACTIVE_POWER
        ));

        assertTrue(factorList.stream().anyMatch(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_2
                && factor.getVariableType() == SensitivityVariableType.HVDC_LINE_ACTIVE_POWER
        ));

        assertTrue(factorList.stream().anyMatch(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_CURRENT_1
                && factor.getVariableType() == SensitivityVariableType.HVDC_LINE_ACTIVE_POWER
        ));

        assertTrue(factorList.stream().anyMatch(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_CURRENT_2
                && factor.getVariableType() == SensitivityVariableType.HVDC_LINE_ACTIVE_POWER
        ));

        assertEquals("BBE2AA11 FFR3AA11 1", factorList.get(0).getVariableId());
        assertEquals("BBE2AA11 FFR3AA11 1", factorList.get(1).getVariableId());
        assertEquals("BBE2AA11 FFR3AA11 1", factorList.get(2).getVariableId());
    }

    @Test
    void testUnhandledElement() {
        Crac crac = CracFactory.findDefault().create("test-crac");
        FlowCnec flowCnec = crac.newFlowCnec()
            .withId("cnec")
            .withNetworkElement("BBE1AA11 FFR5AA11 1")
            .withInstantId(Instant.PREVENTIVE)
            .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
            .add();

        Network network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));

        NetworkElement line = Mockito.mock(NetworkElement.class);
        Mockito.when(line.getId()).thenReturn("BBE1AA11 BBE2AA11 1");
        RangeAction<?> mockHvdcRangeAction = Mockito.mock(RangeAction.class);
        Mockito.when(mockHvdcRangeAction.getNetworkElements()).thenReturn(Set.of(line));

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(Set.of(mockHvdcRangeAction), Set.of(flowCnec), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        assertThrows(FaraoException.class, () -> provider.getBasecaseFactors(network));
    }
}
