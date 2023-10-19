/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class HvdcRangeActionAdderImplTest {
    private CracImpl crac;
    private String networkElementId;

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac");
        crac.addInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.addInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.addInstant("auto", InstantKind.AUTO, "outage");
        networkElementId = "BBE2AA11 FFR3AA11 1";
    }

    @Test
    void testAdd() {
        HvdcRangeAction hvdcRangeAction = (HvdcRangeAction) crac.newHvdcRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .withGroupId("groupId1")
            .newRange().withMin(-5).withMax(10).add()
            .newOnInstantUsageRule()
            .withInstantId("preventive")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, hvdcRangeAction.getNetworkElement().getId());
        assertEquals("BE", hvdcRangeAction.getOperator());
        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(1, hvdcRangeAction.getUsageRules().size());
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement(networkElementId));
    }

    @Test
    void testAddAuto() {
        HvdcRangeAction hvdcRangeAction = (HvdcRangeAction) crac.newHvdcRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .withGroupId("groupId1")
            .withSpeed(1)
            .withInitialSetpoint(1)
            .newRange().withMin(-5).withMax(10).add()
            .newOnInstantUsageRule()
            .withInstantId("auto")
            .withUsageMethod(UsageMethod.FORCED)
            .add()
            .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, hvdcRangeAction.getNetworkElement().getId());
        assertEquals("BE", hvdcRangeAction.getOperator());
        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(1, hvdcRangeAction.getUsageRules().size());
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement(networkElementId));
        assertEquals(1, hvdcRangeAction.getSpeed().orElseThrow().intValue());
        assertEquals(1.0, hvdcRangeAction.getInitialSetpoint());
    }

    @Test
    void testAddAutoWithoutSpeed() {
        HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .withGroupId("groupId1")
            .withInitialSetpoint(1)
            .newRange().withMin(-5).withMax(10).add()
            .newOnInstantUsageRule()
            .withInstantId("auto")
            .withUsageMethod(UsageMethod.FORCED)
            .add();
        FaraoException exception = assertThrows(FaraoException.class, hvdcRangeActionAdder::add);
        assertEquals("Cannot create an AUTO standard range action without speed defined", exception.getMessage());
    }

    @Test
    void testAddWithoutGroupId() {
        HvdcRangeAction hvdcRangeAction = (HvdcRangeAction) crac.newHvdcRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .newRange().withMin(-5).withMax(10).add()
            .newOnInstantUsageRule()
            .withInstantId("preventive")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, hvdcRangeAction.getNetworkElement().getId());
        assertEquals("BE", hvdcRangeAction.getOperator());
        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(1, hvdcRangeAction.getUsageRules().size());
    }

    @Test
    void testAddWithoutUsageRule() {
        /*
        This behaviour is considered admissible:
            - without usage rule, the remedial action will never be available

        This test should however return two warnings
         */
        HvdcRangeAction hvdcRangeAction = (HvdcRangeAction) crac.newHvdcRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .newRange().withMin(-5).withMax(10).add()
            .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, hvdcRangeAction.getNetworkElement().getId());
        assertEquals("BE", hvdcRangeAction.getOperator());
        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(0, hvdcRangeAction.getUsageRules().size());
    }

    @Test
    void testAddWithoutOperator() {
        HvdcRangeAction hvdcRangeAction = (HvdcRangeAction) crac.newHvdcRangeAction()
            .withId("id1")
            .withNetworkElement(networkElementId)
            .newRange().withMin(-5).withMax(10).add()
            .newOnInstantUsageRule()
            .withInstantId("preventive")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, hvdcRangeAction.getNetworkElement().getId());
        assertNull(hvdcRangeAction.getOperator());
        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(1, hvdcRangeAction.getUsageRules().size());
    }

    @Test
    void testNoIdFail() {
        HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction()
            .withOperator("BE")
            .withNetworkElement(networkElementId);
        FaraoException exception = assertThrows(FaraoException.class, hvdcRangeActionAdder::add);
        assertEquals("Cannot add a HvdcRangeAction object with no specified id. Please use withId()", exception.getMessage());
    }

    @Test
    void testNoNetworkElementFail() {
        HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction()
            .withId("id1")
            .withOperator("BE");
        FaraoException exception = assertThrows(FaraoException.class, hvdcRangeActionAdder::add);
        assertEquals("Cannot add HvdcRangeAction without a network element. Please use withNetworkElement() with a non null value", exception.getMessage());
    }

    @Test
    void testIdNotUnique() {
        crac.newNetworkAction()
            .withId("sameId")
            .withOperator("BE")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add()
            .add();
        HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction()
            .withId("sameId")
            .withOperator("BE")
            .withNetworkElement("networkElementId");
        FaraoException exception = assertThrows(FaraoException.class, hvdcRangeActionAdder::add);
        assertEquals("Cannot add HvdcRangeAction without a range. Please use newRange()", exception.getMessage());
    }
}
