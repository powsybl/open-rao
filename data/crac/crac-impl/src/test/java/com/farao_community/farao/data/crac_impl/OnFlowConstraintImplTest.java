/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnFlowConstraintImplTest {
    FlowCnec flowCnec;
    State preventiveState;
    State curativeState;
    UsageMethod usageMethod;

    @BeforeEach
    public void setUp() {
        flowCnec = Mockito.mock(FlowCnec.class);
        usageMethod = Mockito.mock(UsageMethod.class);
        preventiveState = Mockito.mock(State.class);
        Mockito.when(preventiveState.getInstant()).thenReturn(Instant.PREVENTIVE);
        Mockito.when(preventiveState.isPreventive()).thenReturn(true);
        curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(Instant.CURATIVE);
        Mockito.when(curativeState.isPreventive()).thenReturn(false);
    }

    @Test
    void testConstructor() {
        OnFlowConstraint onFlowConstraint = new OnFlowConstraintImpl(Instant.PREVENTIVE, flowCnec, usageMethod);

        assertEquals(Instant.PREVENTIVE, onFlowConstraint.getInstant());
        assertSame(flowCnec, onFlowConstraint.getFlowCnec());
        assertEquals(usageMethod, onFlowConstraint.getUsageMethod());
        assertEquals(usageMethod, onFlowConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(curativeState));
    }

    @Test
    void testEquals() {
        OnFlowConstraint onFlowConstraint1 = new OnFlowConstraintImpl(Instant.PREVENTIVE, flowCnec, usageMethod);
        assertEquals(onFlowConstraint1, onFlowConstraint1);
        assertEquals(onFlowConstraint1.hashCode(), onFlowConstraint1.hashCode());

        assertNotNull(onFlowConstraint1);
        assertNotEquals(onFlowConstraint1, Mockito.mock(OnInstantImpl.class));

        OnFlowConstraint onFlowConstraint2 = new OnFlowConstraintImpl(Instant.PREVENTIVE, flowCnec, usageMethod);
        assertEquals(onFlowConstraint1, onFlowConstraint2);
        assertEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());

        onFlowConstraint2 = new OnFlowConstraintImpl(Instant.CURATIVE, flowCnec, usageMethod);
        assertNotEquals(onFlowConstraint1, onFlowConstraint2);
        assertNotEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());

        onFlowConstraint2 = new OnFlowConstraintImpl(Instant.PREVENTIVE, Mockito.mock(FlowCnec.class), usageMethod);
        assertNotEquals(onFlowConstraint1, onFlowConstraint2);
        assertNotEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());
    }

    @Test
    void testGetUsageMethod() {
        State curativeState2 = Mockito.mock(State.class);
        Mockito.when(curativeState2.getInstant()).thenReturn(Instant.CURATIVE);
        Mockito.when(curativeState2.isPreventive()).thenReturn(false);

        OnFlowConstraint onFlowConstraint = new OnFlowConstraintImpl(Instant.PREVENTIVE, flowCnec, usageMethod);
        assertEquals(usageMethod, onFlowConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(curativeState2));

        Mockito.when(flowCnec.getState()).thenReturn(curativeState);
        onFlowConstraint = new OnFlowConstraintImpl(Instant.CURATIVE, flowCnec, usageMethod);
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(preventiveState));
        assertEquals(usageMethod, onFlowConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(curativeState2));
    }
}
