/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class OnStateImplTest {

    private State initialState;
    private State curativeState1;
    private State curativeState2;

    @BeforeEach
    public void setUp() {
        initialState = new PreventiveState();
        Crac crac = new CracImplFactory().create("cracId");
        Contingency contingency1 = crac.newContingency()
            .withId("contingency1")
            .withNetworkElement("anyNetworkElement")
            .add();
        Contingency contingency2 = crac.newContingency()
            .withId("contingency2")
            .withNetworkElement("anyNetworkElement")
            .add();
        curativeState1 = new PostContingencyState(contingency1, Instant.CURATIVE);
        curativeState2 = new PostContingencyState(contingency2, Instant.CURATIVE);
    }

    @Test
    void testSetterGetter() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, curativeState1);
        assertEquals(curativeState1, rule1.getState());
        assertEquals("contingency1", rule1.getContingency().getId());
        assertEquals(Instant.CURATIVE, rule1.getInstant());
    }

    @Test
    void testEqualsSameObject() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        assertEquals(rule1, rule1);
    }

    @Test
    void testEqualsTrue() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        OnStateImpl rule2 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        assertEquals(rule1, rule2);
    }

    @Test
    void testEqualsFalseNotTheSameObject() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        assertNotEquals(Instant.PREVENTIVE, rule1);
    }

    @Test
    void testEqualsFalseForUsageMethod() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        OnStateImpl rule2 = new OnStateImpl(UsageMethod.FORCED, initialState);
        assertNotEquals(rule1, rule2);
    }

    @Test
    void testEqualsFalseForState() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, curativeState1);
        OnStateImpl rule2 = new OnStateImpl(UsageMethod.AVAILABLE, curativeState2);
        assertNotEquals(rule1, rule2);
    }

    @Test
    void testHashCode() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        OnStateImpl rule2 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    void testHashCodeFalseForUsageMethod() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, initialState);
        OnStateImpl rule2 = new OnStateImpl(UsageMethod.FORCED, initialState);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    void testHashCodeFalseForContingency() {
        OnStateImpl rule1 = new OnStateImpl(UsageMethod.AVAILABLE, curativeState1);
        OnStateImpl rule2 = new OnStateImpl(UsageMethod.AVAILABLE, curativeState2);
        assertNotEquals(rule1.hashCode(), rule2.hashCode());
    }

}
