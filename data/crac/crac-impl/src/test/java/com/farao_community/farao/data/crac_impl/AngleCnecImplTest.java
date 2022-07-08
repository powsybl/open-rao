/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.*;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class AngleCnecImplTest {
    private final static double DOUBLE_TOLERANCE = 1e-3;

    private Crac crac;

    @Before
    public void setUp() {
        crac = new CracImplFactory().create("cracId");
    }

    private AngleCnecAdder initPreventiveCnecAdder() {
        return crac.newAngleCnec()
            .withId("angle-cnec")
            .withName("angle-cnec-name")
            .withExportingNetworkElement("exportingNetworkElement")
            .withImportingNetworkElement("importingNetworkElement")
            .withOperator("FR")
            .withInstant(Instant.PREVENTIVE)
            .withOptimized(false);
    }

    @Test
    public void testGetLocation1() {

        Network network = NetworkImportsUtil.import12NodesNetwork();

        AngleCnec cnec1 = crac.newAngleCnec()
            .withId("cnec-1-id")
            .withExportingNetworkElement("BBE1AA1")
            .withImportingNetworkElement("BBE2AA1")
            .withInstant(Instant.PREVENTIVE)
            .newThreshold().withUnit(Unit.DEGREE).withMax(1000.).add()
            .add();

        AngleCnec cnec2 = crac.newAngleCnec()
            .withId("cnec-2-id")
            .withExportingNetworkElement("DDE2AA1")
            .withImportingNetworkElement("NNL3AA1")
            .withInstant(Instant.PREVENTIVE)
            .newThreshold().withUnit(Unit.DEGREE).withMax(1000.).add()
            .add();

        Set<Optional<Country>> countries = cnec1.getLocation(network);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.BE)));

        countries = cnec2.getLocation(network);
        assertEquals(2, countries.size());
        assertTrue(countries.contains(Optional.of(Country.DE)));
        assertTrue(countries.contains(Optional.of(Country.NL)));
    }

    // test threshold on branches whose nominal voltage is the same on both side

    @Test
    public void testAngleCnecWithOneMaxThreshold() {

        AngleCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.DEGREE).withMax(500.).add()
            .add();

        // bounds
        assertEquals(500., cnec.getUpperBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(cnec.getLowerBound(Unit.DEGREE).isPresent());

        // margin
        assertEquals(200., cnec.computeMargin(300, Unit.DEGREE), DOUBLE_TOLERANCE); // bound: 500 MW
        assertEquals(800., cnec.computeMargin(-300, Unit.DEGREE), DOUBLE_TOLERANCE); // bound: 760 A
    }

    @Test
    public void testAngleCnecWithSeveralThresholds() {
        AngleCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.).add()
            .newThreshold().withUnit(Unit.DEGREE).withMin(-200.).add()
            .newThreshold().withUnit(Unit.DEGREE).withMax(500.).add()
            .newThreshold().withUnit(Unit.DEGREE).withMin(-300.).add()
            .newThreshold().withUnit(Unit.DEGREE).withMin(-50.).withMax(150.).add()
            .add();

        assertEquals(100., cnec.getUpperBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-50., cnec.getLowerBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200., cnec.computeMargin(300, Unit.DEGREE), DOUBLE_TOLERANCE);
        assertEquals(-150., cnec.computeMargin(-200, Unit.DEGREE), DOUBLE_TOLERANCE);
    }

    @Test
    public void marginsWithNegativeAndPositiveLimits() {

        AngleCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.DEGREE).withMin(-200.).withMax(500.).add()
            .add();

        assertEquals(-100, cnec.computeMargin(-300, Unit.DEGREE), DOUBLE_TOLERANCE);
        assertEquals(200, cnec.computeMargin(0, Unit.DEGREE), DOUBLE_TOLERANCE);
        assertEquals(100, cnec.computeMargin(400, Unit.DEGREE), DOUBLE_TOLERANCE);
        assertEquals(-300, cnec.computeMargin(800, Unit.DEGREE), DOUBLE_TOLERANCE);
    }

    // other

    @Test
    public void testEqualsAndHashCode() {
        AngleCnec cnec1 = initPreventiveCnecAdder().newThreshold().withUnit(Unit.DEGREE).withMax(1000.).add().add();
        AngleCnec cnec2 = initPreventiveCnecAdder().withId("anotherId").newThreshold().withUnit(Unit.DEGREE).withMin(-1000.).add().add();

        assertEquals(cnec1, cnec1);
        assertNotEquals(cnec1, cnec2);
        assertNotEquals(cnec1, null);
        assertNotEquals(cnec1, 1);

        assertEquals(cnec1.hashCode(), cnec1.hashCode());
        assertNotEquals(cnec1.hashCode(), cnec2.hashCode());
    }
}
