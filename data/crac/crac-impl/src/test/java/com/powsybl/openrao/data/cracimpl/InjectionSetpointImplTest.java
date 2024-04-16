/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.DanglingLineAction;
import com.powsybl.action.DanglingLineActionBuilder;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
class InjectionSetpointImplTest {
    // TODO: one by type
    @Test
    void basicMethods() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction injectionSetpoint = crac.newNetworkAction()
            .withId("injectionSetpoint")
            .newGeneratorAction()
                .withNetworkElement("element")
                .withActivePowerValue(10.0)
                .add()
            .add();
        assertEquals(1, injectionSetpoint.getNetworkElements().size());
        assertEquals("element", injectionSetpoint.getNetworkElements().iterator().next().getId());
        assertTrue(injectionSetpoint.canBeApplied(Mockito.mock(Network.class)));
    }

    @Test
    void hasImpactOnNetworkForGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction generatorSetpoint = crac.newNetworkAction()
            .withId("generatorSetpoint")
            .newGeneratorAction()
            .withNetworkElement("FFR1AA1 _generator")
            .withActivePowerValue(100)
            .add()
            .add();
        assertTrue(generatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetworkForGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction generatorSetpoint = crac.newNetworkAction()
            .withId("generatorSetpoint")
            .newGeneratorAction()
            .withNetworkElement("FFR1AA1 _generator")
            .withActivePowerValue(2000)
            .add()
            .add();
        assertFalse(generatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void applyOnGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction generatorSetpoint = crac.newNetworkAction()
            .withId("generatorSetpoint")
            .newGeneratorAction()
            .withNetworkElement("FFR1AA1 _generator")
            .withActivePowerValue(100.0)
            .add()
            .add();
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), 1e-3);
        generatorSetpoint.apply(network);
        assertEquals(100., network.getGenerator("FFR1AA1 _generator").getTargetP(), 1e-3);
    }

    @Test
    void hasImpactOnNetworkForLoad() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction loadSetpoint = crac.newNetworkAction()
            .withId("loadSetpoint")
            .newLoadAction()
            .withNetworkElement("FFR1AA1 _load")
            .withActivePowerValue(100)
            .add()
            .add();
        assertTrue(loadSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetworkForLoad() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction loadSetpoint = crac.newNetworkAction()
            .withId("loadSetpoint")
            .newLoadAction()
            .withNetworkElement("FFR1AA1 _load")
            .withActivePowerValue(1000)
            .add()
            .add();
        assertFalse(loadSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void applyOnLoad() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction loadSetpoint = crac.newNetworkAction()
            .withId("loadSetpoint")
            .newLoadAction()
            .withNetworkElement("FFR1AA1 _load")
            .withActivePowerValue(100)
            .add()
            .add();
        assertEquals(1000., network.getLoad("FFR1AA1 _load").getP0(), 1e-3);
        loadSetpoint.apply(network);
        assertEquals(100., network.getLoad("FFR1AA1 _load").getP0(), 1e-3);
    }

    @Test
    void hasImpactOnNetworkForDanglingLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction danglingLineSetpoint = crac.newNetworkAction()
            .withId("danglingLineSetpoint")
            .newDanglingLineAction()
            .withNetworkElement("DL1")
            .withActivePowerValue(100)
            .add()
            .add();
        assertTrue(danglingLineSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetworkForDanglingLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction danglingLineSetpoint = crac.newNetworkAction()
            .withId("danglingLineSetpoint")
            .newDanglingLineAction()
            .withNetworkElement("DL1")
            .withActivePowerValue(0)
            .add()
            .add();
        assertFalse(danglingLineSetpoint.hasImpactOnNetwork(network));

    }

    @Test
    void applyOnDanglingLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction danglingLineSetpoint = crac.newNetworkAction()
            .withId("danglingLineSetpoint")
            .newDanglingLineAction()
            .withNetworkElement("DL1")
            .withActivePowerValue(100)
            .add()
            .add();
        assertEquals(0., network.getDanglingLine("DL1").getP0(), 1e-3);
        danglingLineSetpoint.apply(network);
        assertEquals(100., network.getDanglingLine("DL1").getP0(), 1e-3);
    }

    @Test
    void hasImpactOnNetworkForShuntCompensator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("SC1")
            .withSectionCount(0)
            .add()
            .add();
        assertTrue(shuntCompensatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetworkForShuntCompensator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("SC1")
            .withSectionCount(1)
            .add()
            .add();
        assertFalse(shuntCompensatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void applyOnShuntCompensator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("SC1")
            .withSectionCount(2)
            .add()
            .add();
        assertEquals(1., network.getShuntCompensator("SC1").getSectionCount(), 1e-3);
        shuntCompensatorSetpoint.apply(network);
        assertEquals(2., network.getShuntCompensator("SC1").getSectionCount(), 1e-3);
    }

    @Test
    void canNotBeAppliedOnShuntCompensator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("SC1")
            .withSectionCount(3)
            .add()
            .add();
        assertEquals(2, network.getShuntCompensator("SC1").getMaximumSectionCount());
        assertFalse(shuntCompensatorSetpoint.canBeApplied(network)); // max is 2 while setpoint is 3
    }

    @Test
    void canBeAppliedOnShuntCompensator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("SC1")
            .withSectionCount(1)
            .add()
            .add();
        assertEquals(2, network.getShuntCompensator("SC1").getMaximumSectionCount());
        assertTrue(shuntCompensatorSetpoint.canBeApplied(network)); // max is 2 while setpoint is 1
    }

    @Test
    void canMaxBeAppliedOnShuntCompensator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("SC1")
            .withSectionCount(2)
            .add()
            .add();
        assertEquals(2, network.getShuntCompensator("SC1").getMaximumSectionCount());
        assertTrue(shuntCompensatorSetpoint.canBeApplied(network)); // max is 2 while setpoint is 2
    }

    @Test
    void applyThrow() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction dummy = crac.newNetworkAction()
            .withId("dummy")
            .newLoadAction()
            .withNetworkElement("wrong_name")
            .withActivePowerValue(100)
            .add()
            .add();
        PowsyblException e = assertThrows(PowsyblException.class, () -> dummy.apply(network));
        assertEquals("Load 'wrong_name' not found", e.getMessage()); // TODO: not very explicit message
    }

    @Test
    void hasImpactOnNetworkThrow() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction dummy = crac.newNetworkAction()
            .withId("dummy")
            .newGeneratorAction()
            .withNetworkElement("wrong_name")
            .withActivePowerValue(100)
            .add()
            .add();
        assertThrows(NullPointerException.class, () -> dummy.hasImpactOnNetwork(network));
    }

    @Test
    void equals() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction dummy = crac.newNetworkAction()
            .withId("dummy")
            .newDanglingLineAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withActivePowerValue(10.)
            .add()
            .add();
        assertEquals(1, dummy.getElementaryActions().size());

        NetworkAction dummy2 = crac.newNetworkAction()
            .withId("dummy2")
            .newDanglingLineAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  2")
            .withActivePowerValue(12.)
            .add()
            .newDanglingLineAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  2")
            .withActivePowerValue(12.)
            .add()
            .add();
        assertEquals(1, dummy2.getElementaryActions().size());

        NetworkAction dummy3 = crac.newNetworkAction()
            .withId("dummy3")
            .newDanglingLineAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  3")
            .withActivePowerValue(10.)
            .add()
            .newDanglingLineAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  3")
            .withActivePowerValue(12.)
            .add()
            .add();
        assertEquals(2, dummy3.getElementaryActions().size());

        NetworkAction dummy4 = crac.newNetworkAction()
            .withId("dummy4")
            .newDanglingLineAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  4")
            .withActivePowerValue(10.)
            .add()
            .newDanglingLineAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  5")
            .withActivePowerValue(10.)
            .add()
            .add();
        assertEquals(2, dummy4.getElementaryActions().size());

        DanglingLineAction danglingLineAction = new DanglingLineActionBuilder().withId("id").withDanglingLineId("DL1").withActivePowerValue(10).withRelativeValue(false).build();
        DanglingLineAction sameDanglingLineAction = new DanglingLineActionBuilder().withId("id").withDanglingLineId("DL1").withActivePowerValue(10).withRelativeValue(false).build();
        assertEquals(danglingLineAction, sameDanglingLineAction);
        NetworkAction dummy5 = new NetworkActionImpl("id", "name", "operator", null,
            new HashSet<>(List.of(danglingLineAction, sameDanglingLineAction)), 0, Set.of());
        assertEquals(1, dummy5.getElementaryActions().size());
    }
}
