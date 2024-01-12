/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class ContingencyImplTest {

    private Network network;
    private ComputationManager computationManager;

    @BeforeEach
    public void setUp() {
        computationManager = LocalComputationManager.getDefault();
        network = Network.read("TestCase2Nodes.xiidm", getClass().getResourceAsStream("/TestCase2Nodes.xiidm"));
    }

    @Test
    void testDifferentWithDifferentIds() {
        ContingencyImpl contingencyImpl1 = new ContingencyImpl(
            "contingency-1", "contingency",
            Stream.of(new NetworkElementImpl("network-element-1"), new NetworkElementImpl("network-element-2")).collect(Collectors.toSet())
        );

        ContingencyImpl contingencyImpl2 = new ContingencyImpl(
            "contingency-2", "contingency",
            Stream.of(new NetworkElementImpl("network-element-1"), new NetworkElementImpl("network-element-2")).collect(Collectors.toSet())
        );

        assertNotEquals(contingencyImpl1, contingencyImpl2);
    }

    @Test
    void testDifferentWithDifferentObjects() {
        ContingencyImpl contingencyImpl1 = new ContingencyImpl(
            "contingency-1", "contingency-1",
            Stream.of(new NetworkElementImpl("network-element-1"), new NetworkElementImpl("network-element-2")).collect(Collectors.toSet())
        );

        ContingencyImpl contingencyImpl2 = new ContingencyImpl(
            "contingency-1", "contingency-1",
            Stream.of(new NetworkElementImpl("network-element-1"), new NetworkElementImpl("network-element-5")).collect(Collectors.toSet())
        );

        assertNotEquals(contingencyImpl1, contingencyImpl2);
    }

    @Test
    void testEqual() {
        ContingencyImpl contingencyImpl1 = new ContingencyImpl(
            "contingency-1", "contingency-1",
            Stream.of(new NetworkElementImpl("network-element-1"), new NetworkElementImpl("network-element-2")).collect(Collectors.toSet())
        );
        assertEquals(contingencyImpl1, contingencyImpl1);
        assertEquals(contingencyImpl1.hashCode(), contingencyImpl1.hashCode());
        assertNotNull(contingencyImpl1);
        assertNotEquals(1.0, contingencyImpl1);

        ContingencyImpl contingencyImpl2 = new ContingencyImpl(
            "contingency-1", "contingency-1",
            Stream.of(new NetworkElementImpl("network-element-1"), new NetworkElementImpl("network-element-2")).collect(Collectors.toSet())
        );

        assertEquals(contingencyImpl1, contingencyImpl2);
        assertEquals(contingencyImpl1.hashCode(), contingencyImpl2.hashCode());

        ContingencyImpl contingencyImpl3 = new ContingencyImpl(
                "contingency-3", "contingency-1",
                Stream.of(new NetworkElementImpl("network-element-1"), new NetworkElementImpl("network-element-2")).collect(Collectors.toSet())
        );
        assertNotEquals(contingencyImpl1, contingencyImpl3);
        assertTrue(contingencyImpl1.hashCode() < contingencyImpl3.hashCode());
    }

    @Test
    void testApplyFails() {
        ContingencyImpl contingencyImpl = new ContingencyImpl("contingency", "contingency", Collections.singleton(new NetworkElementImpl("None")));
        assertEquals(1, contingencyImpl.getNetworkElements().size());
        assertThrows(OpenRaoException.class, () -> contingencyImpl.apply(network, computationManager));
    }

    @Test
    void testApplyOnBranch() {
        ContingencyImpl contingencyImpl = new ContingencyImpl("contingency", "contingency", Collections.singleton(new NetworkElementImpl("FRANCE_BELGIUM_1")));
        assertEquals(1, contingencyImpl.getNetworkElements().size());
        assertFalse(network.getBranch("FRANCE_BELGIUM_1").getTerminal1().connect());
        contingencyImpl.apply(network, computationManager);
        assertTrue(network.getBranch("FRANCE_BELGIUM_1").getTerminal1().connect());
    }

    @Test
    void testApplyOnGenerator() {
        ContingencyImpl contingencyImpl = new ContingencyImpl("contingency", "contingency", Collections.singleton(new NetworkElementImpl("GENERATOR_FR_2")));
        assertEquals(1, contingencyImpl.getNetworkElements().size());
        assertTrue(network.getGenerator("GENERATOR_FR_2").getTerminal().isConnected());
        contingencyImpl.apply(network, computationManager);
        assertFalse(network.getGenerator("GENERATOR_FR_2").getTerminal().isConnected());
    }

    @Test
    void testApplyOnDanglingLine() {
        Network network = Network.read("TestCase12NodesHvdc.uct", getClass().getResourceAsStream("/TestCase12NodesHvdc.uct"));
        ContingencyImpl contingencyImpl = new ContingencyImpl("contingency", "contingency", Collections.singleton(new NetworkElementImpl("BBE2AA1  XLI_OB1B 1")));
        assertEquals(1, contingencyImpl.getNetworkElements().size());
        assertTrue(network.getDanglingLine("BBE2AA1  XLI_OB1B 1").getTerminal().isConnected());
        contingencyImpl.apply(network, computationManager);
        assertFalse(network.getDanglingLine("BBE2AA1  XLI_OB1B 1").getTerminal().isConnected());
    }
}
