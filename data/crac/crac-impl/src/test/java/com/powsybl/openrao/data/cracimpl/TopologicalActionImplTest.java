/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class TopologicalActionImplTest {
    // separate in 2 types
    private NetworkAction topologyOpen;
    private NetworkAction topologyClose;

    @BeforeEach
    public void setUp() {
        Crac crac = new CracImplFactory().create("cracId");
        topologyOpen = crac.newNetworkAction()
            .withId("topologyOpen")
            .newTerminalsConnectionAction()
                .withNetworkElement("FFR2AA1  DDE3AA1  1")
                .withActionType(ActionType.OPEN)
                .add()
            .add();
        topologyClose = crac.newNetworkAction()
            .withId("topologyClose")
            .newTerminalsConnectionAction()
                .withNetworkElement("FFR2AA1  DDE3AA1  1")
                .withActionType(ActionType.CLOSE)
                .add()
            .add();
    }

    @Test
    void basicMethods() {
        assertEquals(1, topologyOpen.getNetworkElements().size());
        assertEquals("FFR2AA1  DDE3AA1  1", topologyOpen.getNetworkElements().iterator().next().getId());
        assertTrue(topologyOpen.canBeApplied(Mockito.mock(Network.class)));
    }

    @Test
    void hasImpactOnNetworkForLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();

        assertTrue(topologyOpen.hasImpactOnNetwork(network));
        assertFalse(topologyClose.hasImpactOnNetwork(network));
    }

    @Test
    void applyOpenCloseLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        assertTrue(network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal1().isConnected());
        assertTrue(network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal2().isConnected());

        topologyOpen.apply(network);
        assertFalse(network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal1().isConnected());
        assertFalse(network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal2().isConnected());

        topologyClose.apply(network);
        assertTrue(network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal1().isConnected());
        assertTrue(network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal2().isConnected());
    }

    @Test
    void hasImpactOnNetworkForSwitch() {
        Network network = NetworkImportsUtil.import12NodesNetworkWithSwitch();
        String switchNetworkElementId = "NNL3AA11 NNL3AA12 1";

        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction openSwitchTopology = crac.newNetworkAction()
            .withId("openSwitchTopology")
            .newSwitchAction()
                .withNetworkElement(switchNetworkElementId)
                .withActionType(ActionType.OPEN)
                .add()
            .add();

        assertTrue(openSwitchTopology.hasImpactOnNetwork(network));

        NetworkAction closeSwitchTopology = crac.newNetworkAction()
            .withId("closeSwitchTopology")
            .newSwitchAction()
            .withNetworkElement(switchNetworkElementId)
            .withActionType(ActionType.CLOSE)
            .add()
            .add();

        assertFalse(closeSwitchTopology.hasImpactOnNetwork(network));
    }

    @Test
    void switchTopology() {
        Network network = NetworkImportsUtil.import12NodesNetworkWithSwitch();
        String switchNetworkElementId = "NNL3AA11 NNL3AA12 1";
        Crac crac = new CracImplFactory().create("cracId");

        NetworkAction openSwitchTopology = crac.newNetworkAction()
            .withId("openSwitchTopology")
            .newSwitchAction()
            .withNetworkElement(switchNetworkElementId)
            .withActionType(ActionType.OPEN)
            .add()
            .add();

        openSwitchTopology.apply(network);
        assertTrue(network.getSwitch(switchNetworkElementId).isOpen());

        NetworkAction closeSwitchTopology = crac.newNetworkAction()
            .withId("closeSwitchTopology")
            .newSwitchAction()
            .withNetworkElement(switchNetworkElementId)
            .withActionType(ActionType.CLOSE)
            .add()
            .add();

        closeSwitchTopology.apply(network);
        assertFalse(network.getSwitch(switchNetworkElementId).isOpen());
    }

    @Test
    void applyOnUnsupportedElement() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction topologyOnNode = crac.newNetworkAction()
            .withId("topologyOnNode")
            .newTerminalsConnectionAction()
            .withNetworkElement("FFR2AA1")
            .withActionType(ActionType.OPEN)
            .add()
            .add();

        NullPointerException e = assertThrows(NullPointerException.class, () -> topologyOnNode.apply(network));
        assertEquals("Cannot invoke \"com.powsybl.iidm.network.Connectable.getTerminals()\" because \"connectable\" is null", e.getMessage());  // TODO: not very clear message
    }

    @Test
    void equals() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction similarTopologyClose = crac.newNetworkAction()
            .withId("topologyClose")
            .newTerminalsConnectionAction()
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withActionType(ActionType.CLOSE)
            .add()
            .add();
        assertEquals(similarTopologyClose, topologyClose);
        assertNotEquals(topologyClose, topologyOpen);
    }
}
