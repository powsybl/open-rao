/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.usage_rule.OnAngleConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class OnAngleConstraintImplTest {
    private static final Instant PREVENTIVE_INSTANT = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant OUTAGE_INSTANT = new InstantImpl("outage", InstantKind.OUTAGE, PREVENTIVE_INSTANT);
    private static final Instant AUTO_INSTANT = new InstantImpl("auto", InstantKind.AUTO, OUTAGE_INSTANT);
    private static final Instant CURATIVE_INSTANT = new InstantImpl("curative", InstantKind.CURATIVE, AUTO_INSTANT);

    AngleCnec angleCnec;
    State preventiveState;
    State curativeState;

    @BeforeEach
    public void setUp() {
        angleCnec = Mockito.mock(AngleCnec.class);
        preventiveState = Mockito.mock(State.class);
        Mockito.when(preventiveState.getInstant()).thenReturn(PREVENTIVE_INSTANT);
        Mockito.when(preventiveState.isPreventive()).thenReturn(true);
        curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(CURATIVE_INSTANT);
        Mockito.when(curativeState.isPreventive()).thenReturn(false);
    }

    @Test
    void testConstructor() {
        OnAngleConstraint onAngleConstraint = new OnAngleConstraintImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, angleCnec);

        assertEquals(PREVENTIVE_INSTANT, onAngleConstraint.getInstant());
        assertSame(angleCnec, onAngleConstraint.getAngleCnec());
        assertEquals(UsageMethod.AVAILABLE, onAngleConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onAngleConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onAngleConstraint.getUsageMethod(curativeState));
    }

    @Test
    void testEquals() {
        OnAngleConstraint onAngleConstraint1 = new OnAngleConstraintImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, angleCnec);
        assertEquals(onAngleConstraint1, onAngleConstraint1);
        assertEquals(onAngleConstraint1.hashCode(), onAngleConstraint1.hashCode());

        assertNotNull(onAngleConstraint1);
        assertNotEquals(onAngleConstraint1, Mockito.mock(OnInstantImpl.class));

        OnAngleConstraint onAngleConstraint2 = new OnAngleConstraintImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, angleCnec);
        assertEquals(onAngleConstraint1, onAngleConstraint2);
        assertEquals(onAngleConstraint1.hashCode(), onAngleConstraint2.hashCode());

        onAngleConstraint2 = new OnAngleConstraintImpl(UsageMethod.AVAILABLE, CURATIVE_INSTANT, angleCnec);
        assertNotEquals(onAngleConstraint1, onAngleConstraint2);
        assertNotEquals(onAngleConstraint1.hashCode(), onAngleConstraint2.hashCode());

        onAngleConstraint2 = new OnAngleConstraintImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, Mockito.mock(AngleCnec.class));
        assertNotEquals(onAngleConstraint1, onAngleConstraint2);
        assertNotEquals(onAngleConstraint1.hashCode(), onAngleConstraint2.hashCode());
    }

    @Test
    void testGetUsageMethod() {
        State curativeState2 = Mockito.mock(State.class);
        Mockito.when(curativeState2.getInstant()).thenReturn(CURATIVE_INSTANT);
        Mockito.when(curativeState2.isPreventive()).thenReturn(false);

        OnAngleConstraint onAngleConstraint = new OnAngleConstraintImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, angleCnec);
        assertEquals(UsageMethod.AVAILABLE, onAngleConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onAngleConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onAngleConstraint.getUsageMethod(curativeState2));

        Mockito.when(angleCnec.getState()).thenReturn(curativeState);
        onAngleConstraint = new OnAngleConstraintImpl(UsageMethod.AVAILABLE, CURATIVE_INSTANT, angleCnec);
        assertEquals(UsageMethod.UNDEFINED, onAngleConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.AVAILABLE, onAngleConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onAngleConstraint.getUsageMethod(curativeState2));
    }
}
