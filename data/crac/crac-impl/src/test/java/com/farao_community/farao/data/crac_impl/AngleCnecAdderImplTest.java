/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class AngleCnecAdderImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private CracImpl crac;
    private String contingency1Id = "condId1";
    private Contingency contingency1;
    private AngleCnec cnec1;
    private AngleCnec cnec2;

    @Before
    public void setUp() {
        crac = new CracImpl("test-crac");
        contingency1 = crac.newContingency().withId(contingency1Id).add();
    }

    private void createAngleCnecs() {
        cnec1 = crac.newAngleCnec()
                .withId("cnecId1")
                .withName("cnecName1")
                .withInstant(Instant.OUTAGE)
                .withContingency(contingency1Id)
                .withOperator("cnec1Operator")
                .withExportingNetworkElement("eneId1", "eneName1")
                .withImportingNetworkElement("ineId1", "ineName1")
                .newThreshold().withUnit(Unit.DEGREE).withMax(1000.0).withMin(-1000.0).add()
                .add();
        cnec2 = crac.newAngleCnec()
                .withId("cnecId2")
                .withInstant(Instant.PREVENTIVE)
                .withOperator("cnec2Operator")
                .withExportingNetworkElement("eneId2")
                .withImportingNetworkElement("ineId2")
                .newThreshold().withUnit(Unit.DEGREE).withMax(500.0).add()
                .add();
    }

    @Test
    public void testCheckCnecs() {
        createAngleCnecs();
        assertEquals(2, crac.getAngleCnecs().size());

        // Verify 1st cnec content
        assertEquals(cnec1, crac.getAngleCnec("cnecId1"));
        assertEquals("cnecName1", cnec1.getName());
        assertEquals(contingency1, cnec1.getState().getContingency().orElseThrow());
        assertEquals(Instant.OUTAGE, cnec1.getState().getInstant());
        assertEquals("cnec1Operator", cnec1.getOperator());
        assertEquals("eneName1", cnec1.getExportingNetworkElement().getName());
        assertEquals("ineName1", cnec1.getImportingNetworkElement().getName());
        assertEquals(1000.0, cnec1.getUpperBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, cnec1.getLowerBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);

        // Verify 2nd cnec content
        assertEquals(cnec2, crac.getAngleCnec("cnecId2"));
        assertEquals("cnecId2", cnec2.getName());
        assertEquals(Instant.PREVENTIVE, cnec2.getState().getInstant());
        assertEquals("cnec2Operator", cnec2.getOperator());
        assertEquals(Optional.empty(), cnec2.getState().getContingency());
        assertEquals("eneId2", cnec2.getExportingNetworkElement().getName());
        assertEquals("ineId2", cnec2.getImportingNetworkElement().getName());
        assertEquals(500.0, cnec2.getUpperBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(cnec2.getLowerBound(Unit.DEGREE).isPresent());

    }

    @Test
    public void testAdd() {
        createAngleCnecs();
        // Verify that network elements were created
        crac.newAngleCnec()
            .withId("cnecId3")
            .withInstant(Instant.PREVENTIVE)
            .withOperator("cnec2Operator")
            .withExportingNetworkElement("eneId2") // same as cnec2
            .withImportingNetworkElement("ineId2") // same as cnec2
            .newThreshold().withUnit(Unit.DEGREE).withMax(500.0).add()
            .add();
        assertEquals(4, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("eneId1"));
        assertNotNull(crac.getNetworkElement("ineId1"));
        assertNotNull(crac.getNetworkElement("eneId2"));
        assertNotNull(crac.getNetworkElement("ineId2"));

        // Verify states were created
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
        assertNotNull(crac.getState(contingency1Id, Instant.OUTAGE));
    }

    @Test
    public void testReliabilityMarginHandling() {
        double maxValue = 100.0;
        double reliabilityMargin = 5.0;
        AngleCnec cnec = crac.newAngleCnec().withId("Cnec ID")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(maxValue).withMin(-maxValue).add()
            .withReliabilityMargin(reliabilityMargin)
            .add();
        assertEquals(maxValue - reliabilityMargin, cnec.getUpperBound(Unit.DEGREE).orElseThrow(FaraoException::new), DOUBLE_TOLERANCE);
        assertEquals(reliabilityMargin - maxValue, cnec.getLowerBound(Unit.DEGREE).orElseThrow(FaraoException::new), DOUBLE_TOLERANCE);
    }

    @Test
    public void testNotOptimizedMonitored() {
        AngleCnec cnec = crac.newAngleCnec().withId("Cnec ID")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .withMonitored()
            .add();
        assertFalse(cnec.isOptimized());
        assertTrue(cnec.isMonitored());
    }

    @Test(expected = FaraoException.class)
    public void testOptimizedNotMonitored() {
        crac.newAngleCnec().withId("Cnec ID")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .withOptimized()
            .add();
    }

    @Test
    public void testNotOptimizedNotMonitored() {
        AngleCnec cnec = crac.newAngleCnec().withId("Cnec ID")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    public void testNotOptimizedNotMonitored2() {
        AngleCnec cnec = crac.newAngleCnec().withId("Cnec ID")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .withOptimized(false)
            .withMonitored(false)
            .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test(expected = NullPointerException.class)
    public void testNullParentFail() {
        new AngleCnecAdderImpl(null);
    }

    @Test(expected = FaraoException.class)
    public void testNetworkElementNotImportingNotExporting() {
        crac.newAngleCnec()
            .withNetworkElement("neId1", "neName1");
    }

    @Test(expected = FaraoException.class)
    public void testNoIdFail() {
        crac.newAngleCnec()
            .withName("cnecName")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoStateInstantFail() {
        crac.newAngleCnec()
            .withId("cnecId")
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoExportingNetworkElementFail() {
        crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoImportingNetworkElementFail() {
        crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoThresholdFail() {
        crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddTwiceError() {
        crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .add();
        crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddPreventiveCnecWithContingencyError() {
        crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(Instant.PREVENTIVE)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddOutageCnecWithNoContingencyError() {
        crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(Instant.OUTAGE)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddAutoCnecWithNoContingencyError() {
        crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(Instant.AUTO)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddCurativeCnecWithNoContingencyError() {
        crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(Instant.CURATIVE)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddCurativeCnecWithAbsentContingencyError() {
        crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(Instant.CURATIVE)
            .withContingency("absent-from-crac")
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testThresholdWithUnitKiloVolt() {
        crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
    }
}
