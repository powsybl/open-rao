/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class NetworkActionCombinationTest {
    private NetworkAction networkAction1;
    private NetworkAction networkAction2;
    private NetworkAction networkAction3;
    private NetworkAction networkAction4;

    @BeforeEach
    public void setUp() {

        Crac crac = CracFactory.findDefault().create("crac");
        crac.addInstant("preventive", InstantKind.PREVENTIVE, null);

        networkAction1 = (NetworkAction) crac.newNetworkAction()
            .withId("topological-action-1")
            .withOperator("operator-1")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("any-network-element").add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstantId("preventive").add()
            .add();

        networkAction2 = (NetworkAction) crac.newNetworkAction()
            .withId("topological-action-2")
            .withOperator("operator-2")
            .newTopologicalAction().withActionType(ActionType.CLOSE).withNetworkElement("any-other-network-element").add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstantId("preventive").add()
            .add();

        networkAction3 = (NetworkAction) crac.newNetworkAction()
            .withId("pst-setpoint")
            .withOperator("operator-2")
            .newPstSetPoint().withSetpoint(10).withNetworkElement("any-other-network-element").add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstantId("preventive").add()
            .add();

        networkAction4 = (NetworkAction) crac.newNetworkAction()
            .withId("no-operator")
            .newPstSetPoint().withSetpoint(10).withNetworkElement("any-other-network-element").add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstantId("preventive").add()
            .add();
    }

    @Test
    void individualCombinationTest() {

        NetworkActionCombination naCombination = new NetworkActionCombination(networkAction1);

        assertEquals(Set.of(networkAction1), naCombination.getNetworkActionSet());
        assertEquals(Set.of("operator-1"), naCombination.getOperators());
        assertEquals("topological-action-1", naCombination.getConcatenatedId());
    }

    @Test
    void multipleCombinationTest() {

        NetworkActionCombination naCombination = new NetworkActionCombination(Set.of(networkAction1, networkAction2, networkAction3, networkAction4));

        assertEquals(Set.of(networkAction1, networkAction2, networkAction3, networkAction4),
            naCombination.getNetworkActionSet());

        assertEquals(Set.of("operator-1", "operator-2"), naCombination.getOperators());

        assertTrue(naCombination.getConcatenatedId().contains("topological-action-1"));
        assertTrue(naCombination.getConcatenatedId().contains("topological-action-2"));
        assertTrue(naCombination.getConcatenatedId().contains("pst-setpoint"));

    }
}
