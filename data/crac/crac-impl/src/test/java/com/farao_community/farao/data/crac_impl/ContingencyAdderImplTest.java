/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.ContingencyAdder;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class ContingencyAdderImplTest {

    private Crac crac;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("test-crac");
    }

    @Test
    void testAddContingencies() {
        Contingency con1 = crac.newContingency()
                .withId("conId1")
                .withName("conName1")
                .withNetworkElement("neId1", "neName1")
                .add();
        Contingency con2 = crac.newContingency()
                .withNetworkElement("neId2-1")
                .withNetworkElement("neId2-2", "neName2-2")
                .withId("conId2")
                .add();
        assertEquals(2, crac.getContingencies().size());

        // Verify 1st contingency content
        assertEquals("conName1", crac.getContingency("conId1").getName());
        assertEquals(1, crac.getContingency("conId1").getNetworkElements().size());
        assertEquals("neId1", crac.getContingency("conId1").getNetworkElements().iterator().next().getId());
        assertEquals("neName1", crac.getContingency("conId1").getNetworkElements().iterator().next().getName());
        assertEquals(con1.getId(), crac.getContingency("conId1").getId());

        // Verify 2nd contingency content
        assertEquals("conId2", crac.getContingency("conId2").getName());
        assertEquals(2, crac.getContingency("conId2").getNetworkElements().size());
        assertEquals(con2.getId(), crac.getContingency("conId2").getId());
        Iterator<NetworkElement> iter = crac.getContingency("conId2").getNetworkElements().iterator();
        NetworkElement ne1 = iter.next();
        NetworkElement ne2 = iter.next();
        // Order the network elements from the Set
        if (ne2.getId().compareTo(ne1.getId()) < 0) {
            NetworkElement tmp = ne2;
            ne2 = ne1;
            ne1 = tmp;
        }
        assertEquals("neId2-1", ne1.getId());
        assertEquals("neId2-1", ne1.getName());
        assertEquals("neId2-2", ne2.getId());
        assertEquals("neName2-2", ne2.getName());

        // Verify that network elements were created
        assertEquals(3, ((CracImpl) crac).getNetworkElements().size());
        assertNotNull(((CracImpl) crac).getNetworkElement("neId1"));
        assertNotNull(((CracImpl) crac).getNetworkElement("neId2-1"));
        assertNotNull(((CracImpl) crac).getNetworkElement("neId2-2"));
    }

    @Test
    void testAddWithNoIdFail() {
        ContingencyAdder contingencyAdder = crac.newContingency()
            .withName("conName1")
            .withNetworkElement("neId1", "neName1");
        assertThrows(FaraoException.class, contingencyAdder::add);
    }

    @Test
    void testNullParentFail() {
        assertThrows(NullPointerException.class, () -> new ContingencyAdderImpl(null));
    }

    @Test
    void testAddEmptyContingency() {
        crac.newContingency().withId("cont").add();
        assertEquals(1, crac.getContingencies().size());
        assertNotNull(crac.getContingency("cont"));
        assertTrue(crac.getContingency("cont") instanceof ContingencyImpl);
        assertEquals(0, crac.getContingency("cont").getNetworkElements().size());
    }

    @Test
    void testAddExistingSameContingency() {
        Contingency contingency1 = crac.newContingency()
                .withId("conId1")
                .withName("conName1")
                .withNetworkElement("neId1", "neName1")
                .add();
        Contingency contingency2 = crac.newContingency()
                .withId("conId1")
                .withName("conName1")
                .withNetworkElement("neId1", "neName1")
                .add();
        assertSame(contingency1, contingency2);
    }

    @Test
    void testAddExistingDifferentContingency() {
        crac.newContingency()
                .withId("conId1")
                .withName("conName1")
                .withNetworkElement("neId1", "neName1")
                .add();
        ContingencyAdder contingencyAdder = crac.newContingency()
            .withId("conId1")
            .withName("conName1")
            .withNetworkElement("neId2", "neName1");
        assertThrows(FaraoException.class, contingencyAdder::add);
    }
}
