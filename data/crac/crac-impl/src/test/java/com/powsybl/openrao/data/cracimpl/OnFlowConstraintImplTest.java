/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.usagerule.OnFlowConstraint;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnFlowConstraintImplTest {
    private static final Instant PREVENTIVE_INSTANT = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant OUTAGE_INSTANT = new InstantImpl("outage", InstantKind.OUTAGE, PREVENTIVE_INSTANT);
    private static final Instant AUTO_INSTANT = new InstantImpl("auto", InstantKind.AUTO, OUTAGE_INSTANT);
    private static final Instant CURATIVE_INSTANT = new InstantImpl("curative", InstantKind.CURATIVE, AUTO_INSTANT);

    FlowCnec flowCnec;
    State preventiveState;
    State curativeState;

    @BeforeEach
    public void setUp() {
        flowCnec = Mockito.mock(FlowCnec.class);
        preventiveState = Mockito.mock(State.class);
        Mockito.when(preventiveState.getInstant()).thenReturn(PREVENTIVE_INSTANT);
        Mockito.when(preventiveState.isPreventive()).thenReturn(true);
        curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(CURATIVE_INSTANT);
        Mockito.when(curativeState.isPreventive()).thenReturn(false);
    }

    @Test
    void testConstructor() {
        OnFlowConstraint onFlowConstraint = new OnFlowConstraintImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, flowCnec);

        assertEquals(PREVENTIVE_INSTANT, onFlowConstraint.getInstant());
        assertSame(flowCnec, onFlowConstraint.getFlowCnec());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(curativeState));
    }

    @Test
    void testEquals() {
        OnFlowConstraint onFlowConstraint1 = new OnFlowConstraintImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, flowCnec);
        assertEquals(onFlowConstraint1, onFlowConstraint1);
        assertEquals(onFlowConstraint1.hashCode(), onFlowConstraint1.hashCode());

        assertNotNull(onFlowConstraint1);
        assertNotEquals(onFlowConstraint1, Mockito.mock(OnInstantImpl.class));

        OnFlowConstraint onFlowConstraint2 = new OnFlowConstraintImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, flowCnec);
        assertEquals(onFlowConstraint1, onFlowConstraint2);
        assertEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());

        onFlowConstraint2 = new OnFlowConstraintImpl(UsageMethod.AVAILABLE, CURATIVE_INSTANT, flowCnec);
        assertNotEquals(onFlowConstraint1, onFlowConstraint2);
        assertNotEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());

        onFlowConstraint2 = new OnFlowConstraintImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, Mockito.mock(FlowCnec.class));
        assertNotEquals(onFlowConstraint1, onFlowConstraint2);
        assertNotEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());
    }

    @Test
    void testGetUsageMethod() {
        State curativeState2 = Mockito.mock(State.class);
        Mockito.when(curativeState2.getInstant()).thenReturn(CURATIVE_INSTANT);
        Mockito.when(curativeState2.isPreventive()).thenReturn(false);

        OnFlowConstraint onFlowConstraint = new OnFlowConstraintImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, flowCnec);
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(curativeState2));

        Mockito.when(flowCnec.getState()).thenReturn(curativeState);
        onFlowConstraint = new OnFlowConstraintImpl(UsageMethod.AVAILABLE, CURATIVE_INSTANT, flowCnec);
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(curativeState2));
    }
}
