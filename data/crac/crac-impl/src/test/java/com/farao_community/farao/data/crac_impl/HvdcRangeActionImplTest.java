/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.range.StandardRangeAdder;
import com.farao_community.farao.data.crac_api.range_action.*;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class HvdcRangeActionImplTest {
    private HvdcRangeActionAdder hvdcRangeActionAdder;
    private Network network;
    private Network networkWithAngleDroop;
    private HvdcLine hvdcLine;
    private HvdcLine hvdcLineWithAngleDroop;

    @BeforeEach
    public void setUp() {
        Crac crac = new CracImplFactory().create("cracId");
        network = NetworkImportsUtil.import16NodesNetworkWithHvdc();
        networkWithAngleDroop =  NetworkImportsUtil.import16NodesNetworkWithAngleDroopHvdcs();
        String networkElementId = "BBE2AA11 FFR3AA11 1";

        hvdcRangeActionAdder = crac.newHvdcRangeAction()
            .withId("hvdc-range-action-id")
            .withName("hvdc-range-action-name")
            .withNetworkElement("BBE2AA11 FFR3AA11 1")
            .withOperator("operator")
            .newOnInstantUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add();

        hvdcLine = network.getHvdcLine(networkElementId);
        hvdcLineWithAngleDroop = networkWithAngleDroop.getHvdcLine(networkElementId);
        hvdcLineWithAngleDroop.getExtension(HvdcAngleDroopActivePowerControl.class).setEnabled(true);
    }

    @Test
    void getInitialSetpoint() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();
        assertEquals(0, hvdcRa.getCurrentSetpoint(network), 1e-6);
    }

    @Test
    void applyPositiveSetpoint() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();
        hvdcRa.apply(network, 5);
        hvdcRa.apply(networkWithAngleDroop, 6);
        assertEquals(5, hvdcRa.getCurrentSetpoint(network), 1e-6);
        assertEquals(6, hvdcRa.getCurrentSetpoint(networkWithAngleDroop), 1e-6);
        assertFalse(hvdcLineWithAngleDroop.getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
    }

    @Test
    void applyNegativeSetpoint() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();
        hvdcRa.apply(network, -3);
        hvdcRa.apply(networkWithAngleDroop, -4);
        assertEquals(-3, hvdcRa.getCurrentSetpoint(network), 1e-6);
        assertEquals(-4, hvdcRa.getCurrentSetpoint(networkWithAngleDroop), 1e-6);
        assertFalse(hvdcLineWithAngleDroop.getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
    }

    @Test
    void getPositiveSetpoint() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();
        hvdcRa.apply(network, 5);
        hvdcLine.setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER);
        assertEquals(5, hvdcRa.getCurrentSetpoint(network), 1e-6);
    }

    @Test
    void getNegativeSetpoint() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();
        hvdcRa.apply(network, 3);
        hvdcLine.setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        assertEquals(-3, hvdcRa.getCurrentSetpoint(network), 1e-6);
    }

    @Test
    void applyOnUnknownHvdc() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .withNetworkElement("unknownNetworkElement").add();
        assertThrows(FaraoException.class, () -> hvdcRa.apply(network, 50));
    }

    @Test
    void hvdcWithoutSpecificRange() {
        assertThrows(FaraoException.class, () -> hvdcRangeActionAdder.add());
    }

    @Test
    void hvdcWithSpecificRange() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();

        assertEquals(-5, hvdcRa.getMinAdmissibleSetpoint(0), 1e-3);
        assertEquals(10, hvdcRa.getMaxAdmissibleSetpoint(0), 1e-3);
    }

    @Test
    void hvdcWithNoMin() {
        StandardRangeAdder<HvdcRangeActionAdder> standardRangeAdder = hvdcRangeActionAdder.newRange().withMax(10);
        assertThrows(FaraoException.class, standardRangeAdder::add);
    }

    @Test
    void hvdcWithNoMax() {
        StandardRangeAdder<HvdcRangeActionAdder> standardRangeAdder = hvdcRangeActionAdder.newRange().withMin(10);
        assertThrows(FaraoException.class, standardRangeAdder::add);
    }

    @Test
    void testGetLocation() {
        HvdcRangeAction hvdcRa = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();
        Set<Optional<Country>> countries = hvdcRa.getLocation(network);
        assertEquals(2, countries.size());
        assertTrue(countries.contains(Optional.of(Country.BE)));
        assertTrue(countries.contains(Optional.of(Country.FR)));
    }

    @Test
    void hvdcEquals() {
        HvdcRangeAction hvdcRa1 = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
                .add();
        HvdcRangeAction hvdcRa2 = hvdcRangeActionAdder.withId("anotherId").newRange().withMin(-5).withMax(10).add()
                .add();

        assertEquals(hvdcRa1.hashCode(), hvdcRa1.hashCode());
        assertEquals(hvdcRa1, hvdcRa1);
        assertNotEquals(hvdcRa1.hashCode(), hvdcRa2.hashCode());
        assertNotEquals(hvdcRa1, hvdcRa2);
    }

}
