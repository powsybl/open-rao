/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_impl.AlreadySynchronizedException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeDefinition;
import com.farao_community.farao.data.crac_api.RangeType;
import com.farao_community.farao.data.crac_impl.PstRangeActionImpl;
import com.farao_community.farao.data.crac_impl.PstRangeImpl;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstRangeActionImplTest extends AbstractRangeActionTest {
    private String networkElementId;
    private PstRangeActionImpl pst;
    private Network network;
    private PhaseTapChanger phaseTapChanger;
    private NetworkElement networkElement;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        networkElementId = "BBE2AA1  BBE3AA1  1";
        networkElement = new NetworkElement(networkElementId);
        pst = new PstRangeActionImpl("pst_range_id", networkElement);
        phaseTapChanger = network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger();
    }

    @Test
    public void pstOtherConstructor() {
        PstRangeActionImpl pstRange1 = new PstRangeActionImpl("id", networkElement);
        assertEquals("", pstRange1.getOperator());
        PstRangeActionImpl pstRange2 = new PstRangeActionImpl("id", "name", "operator", networkElement);
        assertEquals("operator", pstRange2.getOperator());
    }

    @Test
    public void pstEquals() {
        PstRangeActionImpl pstRange1 = new PstRangeActionImpl("pst_range_id", networkElement);
        assertEquals(pst.hashCode(), pstRange1.hashCode());
        assertEquals(pst, pstRange1);
        PstRangeActionImpl pstDifferent = new PstRangeActionImpl("pst_range_id_2", new NetworkElement("neOther"));
        assertNotEquals(pst.hashCode(), pstDifferent.hashCode());
        assertNotEquals(pst, pstDifferent);
    }

    @Test
    public void apply() {
        assertEquals(0, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getTapPosition());
        pst.apply(network, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getStep(12).getAlpha());
        assertEquals(12, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getTapPosition());
    }

    @Test
    public void applyOutOfBound() {
        try {
            pst.apply(network, 50);
            fail();
        } catch (FaraoException e) {
            assertEquals("Angle value", e.getMessage().substring(0, 11)); // In order to avoid numeric values in the expected String
        }
    }

    @Test
    public void applyOnUnknownPst() {
        PstRangeActionImpl unknownPstRangeAction = new PstRangeActionImpl(
                "unknown_pstrange_id",
                new NetworkElement("unknown pst", "unknown pst"));
        try {
            unknownPstRangeAction.apply(network, 50);
            fail();
        } catch (FaraoException e) {
            assertEquals("PST unknown pst does not exist in the current network", e.getMessage());
        }
    }

    @Test
    public void applyOnTransformerWithNoPhaseShifter() {
        Network network = Importers.loadNetwork(
            "TestCase12Nodes_no_pst.uct",
            getClass().getResourceAsStream("/TestCase12Nodes_no_pst.uct"));
        String notPstRangeElementId = "BBE2AA1  BBE3AA1  1";
        PstRangeActionImpl notAPstRangeAction = new PstRangeActionImpl("not_pstrange_id", networkElement);
        try {
            notAPstRangeAction.apply(network, 50);
            fail();
        } catch (FaraoException e) {
            assertEquals(String.format("Transformer %s is not a PST but is defined as a PstRange", notPstRangeElementId), e.getMessage());
        }
    }

    @Test
    public void pstWithoutSpecificRange() {
        PstRangeActionImpl pstRangeActionWithoutSpecificRange = new PstRangeActionImpl("id", networkElement);
        pstRangeActionWithoutSpecificRange.synchronize(network);
        assertEquals(phaseTapChanger.getStep(phaseTapChanger.getLowTapPosition()).getAlpha(), pstRangeActionWithoutSpecificRange.getMinValue(network, pstRangeActionWithoutSpecificRange.getCurrentValue(network)), 0);
        assertEquals(phaseTapChanger.getStep(phaseTapChanger.getHighTapPosition()).getAlpha(), pstRangeActionWithoutSpecificRange.getMaxValue(network, pstRangeActionWithoutSpecificRange.getCurrentValue(network)), 0);
    }

    @Test
    public void pstWithAbsoluteStartOneRange() {
        pst.addRange(new PstRangeImpl(3, 13, RangeType.ABSOLUTE, RangeDefinition.STARTS_AT_ONE));
        pst.synchronize(network);
        assertEquals(phaseTapChanger.getStep(phaseTapChanger.getLowTapPosition() + 2).getAlpha(), pst.getMinValue(network, pst.getCurrentValue(network)), 0);
        assertEquals(phaseTapChanger.getStep(phaseTapChanger.getLowTapPosition() + 12).getAlpha(), pst.getMaxValue(network, pst.getCurrentValue(network)), 0);
    }

    @Test
    public void pstWithAbsoluteCenteredZeroRange() {
        pst.addRange(new PstRangeImpl(-3, 3, RangeType.ABSOLUTE, RangeDefinition.CENTERED_ON_ZERO));
        pst.synchronize(network);
        int neutralTap = (phaseTapChanger.getHighTapPosition() + phaseTapChanger.getLowTapPosition()) / 2;
        assertEquals(phaseTapChanger.getStep(neutralTap - 3).getAlpha(), pst.getMinValue(network, pst.getCurrentValue(network)), 0);
        assertEquals(phaseTapChanger.getStep(neutralTap + 3).getAlpha(), pst.getMaxValue(network, pst.getCurrentValue(network)), 0);
    }

    @Test
    public void pstWithRelativeDynamicRange() {
        pst.addRange(new PstRangeImpl(-3, 3, RangeType.RELATIVE_TO_PREVIOUS_INSTANT, RangeDefinition.CENTERED_ON_ZERO));
        pst.synchronize(network);
        int initialTapPosition = phaseTapChanger.getTapPosition();
        assertEquals(phaseTapChanger.getStep(initialTapPosition - 3).getAlpha(), pst.getMinValue(network, pst.getCurrentValue(network)), 0);
        assertEquals(phaseTapChanger.getStep(initialTapPosition + 3).getAlpha(), pst.getMaxValue(network, pst.getCurrentValue(network)), 0);

        int newTapPosition = initialTapPosition + 5;
        phaseTapChanger.setTapPosition(newTapPosition);
        assertEquals(phaseTapChanger.getStep(newTapPosition - 3).getAlpha(), pst.getMinValue(network, pst.getCurrentValue(network)), 0);
        assertEquals(phaseTapChanger.getStep(newTapPosition + 3).getAlpha(), pst.getMaxValue(network, pst.getCurrentValue(network)), 0);
    }

    @Test
    public void pstWithRelativeFixedRange() {
        pst.addRange(new PstRangeImpl(-3, 3, RangeType.RELATIVE_TO_INITIAL_NETWORK, RangeDefinition.CENTERED_ON_ZERO));
        pst.synchronize(network);
        int initialTapPosition = phaseTapChanger.getTapPosition();
        assertEquals(phaseTapChanger.getStep(initialTapPosition - 3).getAlpha(), pst.getMinValue(network, pst.getCurrentValue(network)), 0);
        assertEquals(phaseTapChanger.getStep(initialTapPosition + 3).getAlpha(), pst.getMaxValue(network, pst.getCurrentValue(network)), 0);

        int newTapPosition = initialTapPosition + 5;
        phaseTapChanger.setTapPosition(newTapPosition);
        assertEquals(phaseTapChanger.getStep(initialTapPosition - 3).getAlpha(), pst.getMinValue(network, pst.getCurrentValue(network)), 0);
        assertEquals(phaseTapChanger.getStep(initialTapPosition + 3).getAlpha(), pst.getMaxValue(network, pst.getCurrentValue(network)), 0);
    }

    @Test
    public void desynchronize() {
        pst.addRange(new PstRangeImpl(-3, 3, RangeType.RELATIVE_TO_INITIAL_NETWORK, RangeDefinition.CENTERED_ON_ZERO));
        pst.synchronize(network);
        int initialTapPosition = phaseTapChanger.getTapPosition();
        assertEquals(phaseTapChanger.getStep(initialTapPosition + 3).getAlpha(), pst.getMaxValue(network, pst.getCurrentValue(network)), 0);
        pst.desynchronize();

        try {
            pst.getCurrentValue(network);
            fail();
        } catch (FaraoException e) {
            assertEquals("PST pst_range_id have not been synchronized so tap cannot be converted to angle", e.getMessage());
        }
    }

    @Test
    public void computeCurrentValueFromCenteredOnZero() throws NoSuchFieldException {
        PstRangeActionImpl pstRangeAction = spy(pst);

        Whitebox.setInternalState(pstRangeAction, "lowTapPosition", -16);
        Whitebox.setInternalState(pstRangeAction, "highTapPosition", 16);

        pstRangeAction.apply(network, 0.0); // tap 0 (CENTERED_ON_ZERO)
        assertEquals(0, pstRangeAction.getCurrentTapPosition(network, RangeDefinition.CENTERED_ON_ZERO), 0);
        assertEquals(17, pstRangeAction.getCurrentTapPosition(network, RangeDefinition.STARTS_AT_ONE), 0);

        pstRangeAction.apply(network, 3.8946); // tap 10 (CENTERED_ON_ZERO)
        assertEquals(10, pstRangeAction.getCurrentTapPosition(network, RangeDefinition.CENTERED_ON_ZERO), 0);
        assertEquals(27, pstRangeAction.getCurrentTapPosition(network, RangeDefinition.STARTS_AT_ONE), 0);
    }

    @Test
    public void computeCurrentValueFromStartsAtOne() throws NoSuchFieldException {
        PstRangeActionImpl pstRangeAction = spy(pst);

        // As the network contains taps CENTERED_ON_ZERO, but we want to test the case where the taps STARTS_AT_ONE,
        // we artifically modify the lowTapPosition and highTapPosition, and we need also to shift the taps in the assertEquals,
        // because the conversion from angle to tap is based on the network (so it gives a tap CENTERED_ON_ZERO)
        int tapShift = 17;
        Whitebox.setInternalState(pstRangeAction, "lowTapPosition", 1);
        Whitebox.setInternalState(pstRangeAction, "highTapPosition", 33);

        pstRangeAction.apply(network, 0.0); // tap 17 (STARTS_AT_ONE)
        assertEquals(0, pstRangeAction.getCurrentTapPosition(network, RangeDefinition.CENTERED_ON_ZERO) + tapShift, 0);
        assertEquals(17, pstRangeAction.getCurrentTapPosition(network, RangeDefinition.STARTS_AT_ONE) + tapShift, 0);

        pstRangeAction.apply(network, -3.8946); // tap 7 (STARTS_AT_ONE)
        assertEquals(-10, pstRangeAction.getCurrentTapPosition(network, RangeDefinition.CENTERED_ON_ZERO) + tapShift, 0);
        assertEquals(7, pstRangeAction.getCurrentTapPosition(network, RangeDefinition.STARTS_AT_ONE) + tapShift, 0);
    }

    @Test
    public void getCurrentValueTest() {
        pst.synchronize(network);
        assertEquals(0, pst.getCurrentValue(network), 0);
    }

    @Test
    public void convertToStartsAtOneFails() throws NoSuchFieldException {
        PstRangeActionImpl pstRangeAction = spy(pst);
        Whitebox.setInternalState(pstRangeAction, "lowTapPosition", -12);
        Whitebox.setInternalState(pstRangeAction, "highTapPosition", 35);
        try {
            pstRangeAction.getCurrentTapPosition(network, RangeDefinition.STARTS_AT_ONE);
        } catch (FaraoException e) {
            assertEquals("Unhandled range definition, between -12 and 35.", e.getMessage());
        }
    }

    @Test
    public void convertToCenteredOnZero() throws NoSuchFieldException {
        PstRangeActionImpl pstRangeAction = spy(pst);
        Whitebox.setInternalState(pstRangeAction, "lowTapPosition", -12);
        Whitebox.setInternalState(pstRangeAction, "highTapPosition", 35);
        try {
            pstRangeAction.getCurrentTapPosition(network, RangeDefinition.CENTERED_ON_ZERO);
        } catch (FaraoException e) {
            assertEquals("Unhandled range definition, between -12 and 35.", e.getMessage());
        }
    }

    @Test
    public void getMinValueWithNoSynchronizationFails() {
        try {
            pst.getCurrentValue(network);
            fail();
        } catch (FaraoException e) {
            assertEquals("PST pst_range_id have not been synchronized so tap cannot be converted to angle", e.getMessage());
        }
    }

    @Test
    public void synchronizetwiceFails() {
        pst.synchronize(network);
        try {
            pst.synchronize(network);
        } catch (AlreadySynchronizedException e) {
            assertEquals("PST pst_range_id has already been synchronized", e.getMessage());
        }
    }

    @Test
    public void handleDecreasingAnglesMinMax() {
        // First test case where deltaU is negative
        pst.addRange(new PstRangeImpl(-10.0, 10.0, RangeType.ABSOLUTE, RangeDefinition.CENTERED_ON_ZERO));
        pst.synchronize(network);
        assertTrue("Failed to compute min and max tap values for PST with negative deltaU",
                pst.getMinValue(network, pst.getCurrentValue(network)) <= pst.getMaxValue(network, pst.getCurrentValue(network)));

        // Then load a new case with a positive delta U and test min and max values
        Network network2 = Importers.loadNetwork("utils/TestCase12NodesWithPositiveDeltaUPST.uct", NetworkImportsUtil.class.getResourceAsStream("/utils/TestCase12NodesWithPositiveDeltaUPST.uct"));
        String networkElementId2 = "BBE2AA1  BBE3AA1  1";
        NetworkElement networkElement2 = new NetworkElement(networkElementId2);
        PstRangeActionImpl pst2 = new PstRangeActionImpl("pst_range_id", networkElement2);
        pst2.addRange(new PstRangeImpl(-10.0, 10.0, RangeType.ABSOLUTE, RangeDefinition.CENTERED_ON_ZERO));
        pst2.synchronize(network2);
        assertTrue("Failed to compute min and max tap values for PST with positive deltaU",
                pst2.getMinValue(network, pst2.getCurrentValue(network2)) <= pst2.getMaxValue(network, pst2.getCurrentValue(network2)));
    }

    @Test
    public void testGetLocation() {
        Set<Optional<Country>> countries = pst.getLocation(network);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.BE)));
    }
}
