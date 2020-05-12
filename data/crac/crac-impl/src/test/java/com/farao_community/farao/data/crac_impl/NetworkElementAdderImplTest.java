package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElementAdder;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkElementAdderImplTest {

    private SimpleCrac crac;

    @Before
    public void setUp() {
        crac = new SimpleCrac("test-crac");
    }

    @Test
    public void testCracAddNetworkElement() {
        crac.newNetworkElement()
                .setId("neID")
                .setName("neName")
                .add();
        crac.newNetworkElement()
                .setId("neID2")
                .add();
        assertEquals(2, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neID"));
        assertEquals("neName", crac.getNetworkElement("neID").getName());
        assertNotNull(crac.getNetworkElement("neID2"));
        assertEquals("neID2", crac.getNetworkElement("neID2").getName());
    }

    @Test(expected = FaraoException.class)
    public void testCracAddNetworkElementNoIdFail() {
        crac.newNetworkElement()
                .setName("neName")
                .add();
    }

    @Test(expected = NullPointerException.class)
    public void testNullParentFail() {
        NetworkElementAdder tmp = new NetworkElementAdderImpl(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullIdFail() {
        crac.newNetworkElement().setId(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullNameFail() {
        crac.newNetworkElement().setName(null);
    }
}
