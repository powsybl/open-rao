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
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnecAdder;
import com.farao_community.farao.data.crac_api.threshold.ThresholdAdder;
import com.farao_community.farao.data.crac_api.threshold.VoltageThresholdAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class VoltageCnecAdderImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private final String contingency1Id = "condId1";
    private CracImpl crac;
    private Contingency contingency1;
    private VoltageCnec cnec1;
    private VoltageCnec cnec2;
    private Instant preventiveInstant;
    private Instant outageInstant;
    private Instant autoInstant;
    private Instant curativeInstant;

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        contingency1 = crac.newContingency().withId(contingency1Id).add();
    }

    private void createVoltageCnecs() {
        cnec1 = crac.newVoltageCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withInstant(outageInstant)
            .withContingency(contingency1Id)
            .withOperator("cnec1Operator")
            .withNetworkElement("neId1", "neName1")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(1000.0).withMin(-1000.0).add()
            .add();
        cnec2 = crac.newVoltageCnec()
            .withId("cnecId2")
            .withInstant(preventiveInstant)
            .withOperator("cnec2Operator")
            .withNetworkElement("neId2")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(500.0).add()
            .add();
    }

    @Test
    void testCheckCnecs() {
        createVoltageCnecs();
        assertEquals(2, crac.getVoltageCnecs().size());

        // Verify 1st cnec content
        assertEquals(cnec1, crac.getVoltageCnec("cnecId1"));
        assertEquals("cnecName1", cnec1.getName());
        assertEquals(contingency1, cnec1.getState().getContingency().orElseThrow());
        assertEquals(outageInstant, cnec1.getState().getInstant());
        assertEquals("cnec1Operator", cnec1.getOperator());
        assertEquals("neName1", cnec1.getNetworkElement().getName());
        assertEquals(1000.0, cnec1.getUpperBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, cnec1.getLowerBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);

        // Verify 2nd cnec content
        assertEquals(cnec2, crac.getVoltageCnec("cnecId2"));
        assertEquals("cnecId2", cnec2.getName());
        assertEquals(preventiveInstant, cnec2.getState().getInstant());
        assertEquals("cnec2Operator", cnec2.getOperator());
        assertEquals(Optional.empty(), cnec2.getState().getContingency());
        assertEquals("neId2", cnec2.getNetworkElement().getName());
        assertEquals(500.0, cnec2.getUpperBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(cnec2.getLowerBound(Unit.KILOVOLT).isPresent());

    }

    @Test
    void testAdd() {
        createVoltageCnecs();
        // Verify that network elements were created
        crac.newVoltageCnec()
            .withId("cnecId3")
            .withInstant(preventiveInstant)
            .withOperator("cnec2Operator")
            .withNetworkElement("neId2") // same as cnec2
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(500.0).add()
            .add();
        assertEquals(2, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neId1"));
        assertNotNull(crac.getNetworkElement("neId2"));

        // Verify states were created
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
        assertNotNull(crac.getState(contingency1Id, crac.getInstant(OUTAGE_INSTANT_ID)));
    }

    @Test
    void testReliabilityMarginHandling() {
        double maxValue = 100.0;
        double reliabilityMargin = 5.0;
        VoltageCnec cnec = crac.newVoltageCnec().withId("Cnec ID")
            .withInstant(outageInstant)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(maxValue).withMin(-maxValue).add()
            .withReliabilityMargin(reliabilityMargin)
            .add();
        assertEquals(maxValue - reliabilityMargin, cnec.getUpperBound(Unit.KILOVOLT).orElseThrow(FaraoException::new), DOUBLE_TOLERANCE);
        assertEquals(reliabilityMargin - maxValue, cnec.getLowerBound(Unit.KILOVOLT).orElseThrow(FaraoException::new), DOUBLE_TOLERANCE);
    }

    @Test
    void testNotOptimizedMonitored() {
        VoltageCnec cnec = crac.newVoltageCnec().withId("Cnec ID")
            .withInstant(outageInstant)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .withMonitored()
            .add();
        assertFalse(cnec.isOptimized());
        assertTrue(cnec.isMonitored());
    }

    @Test
    void testOptimizedNotMonitored() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec().withId("Cnec ID")
            .withInstant(outageInstant)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .withOptimized();
        FaraoException exception = assertThrows(FaraoException.class, voltageCnecAdder::add);
        assertEquals("Error while adding cnec Cnec ID : Farao does not allow the optimization of VoltageCnecs.", exception.getMessage());
    }

    @Test
    void testNotOptimizedNotMonitored() {
        VoltageCnec cnec = crac.newVoltageCnec().withId("Cnec ID")
            .withInstant(outageInstant)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    void testNotOptimizedNotMonitored2() {
        VoltageCnec cnec = crac.newVoltageCnec().withId("Cnec ID")
            .withInstant(outageInstant)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .withOptimized(false)
            .withMonitored(false)
            .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    void testNullParentFail() {
        assertThrows(NullPointerException.class, () -> new VoltageCnecAdderImpl(null));
    }

    @Test
    void testNoIdFail() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withName("cnecName")
            .withInstant(outageInstant)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        FaraoException exception = assertThrows(FaraoException.class, voltageCnecAdder::add);
        assertEquals("Cannot add a VoltageCnec object with no specified id. Please use withId()", exception.getMessage());
    }

    @Test
    void testNoStateInstantFail() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        FaraoException exception = assertThrows(FaraoException.class, voltageCnecAdder::add);
        assertEquals("Cannot add Cnec without a instant. Please use withInstant() with a non null value", exception.getMessage());
    }

    @Test
    void testNoNetworkElementFail() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(outageInstant)
            .withContingency(contingency1Id)
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        FaraoException exception = assertThrows(FaraoException.class, voltageCnecAdder::add);
        assertEquals("Cannot add Cnec without a network element. Please use withNetworkElement()", exception.getMessage());
    }

    @Test
    void testNoThresholdFail() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(outageInstant)
            .withContingency(contingency1Id)
            .withNetworkElement("neId");
        FaraoException exception = assertThrows(FaraoException.class, voltageCnecAdder::add);
        assertEquals("Cannot add an VoltageCnec without a threshold. Please use newThreshold", exception.getMessage());
    }

    @Test
    void testAddTwiceError() {
        crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(outageInstant)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(outageInstant)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        FaraoException exception = assertThrows(FaraoException.class, voltageCnecAdder::add);
        assertEquals("Cannot add a cnec with an already existing ID - cnecId.", exception.getMessage());
    }

    @Test
    void testAddPreventiveCnecWithContingencyError() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(preventiveInstant)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        FaraoException exception = assertThrows(FaraoException.class, voltageCnecAdder::add);
        assertEquals("You cannot define a contingency for a preventive cnec.", exception.getMessage());
    }

    @Test
    void testAddOutageCnecWithNoContingencyError() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(outageInstant)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        FaraoException exception = assertThrows(FaraoException.class, voltageCnecAdder::add);
        assertEquals("You must define a contingency for a non-preventive cnec.", exception.getMessage());
    }

    @Test
    void testAddAutoCnecWithNoContingencyError() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(autoInstant)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        FaraoException exception = assertThrows(FaraoException.class, voltageCnecAdder::add);
        assertEquals("You must define a contingency for a non-preventive cnec.", exception.getMessage());
    }

    @Test
    void testAddCurativeCnecWithNoContingencyError() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(curativeInstant)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        FaraoException exception = assertThrows(FaraoException.class, voltageCnecAdder::add);
        assertEquals("You must define a contingency for a non-preventive cnec.", exception.getMessage());
    }

    @Test
    void testAddCurativeCnecWithAbsentContingencyError() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(curativeInstant)
            .withContingency("absent-from-crac")
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        FaraoException exception = assertThrows(FaraoException.class, voltageCnecAdder::add);
        assertEquals("Contingency absent-from-crac of Cnec cnecId does not exist in the crac. Use crac.newContingency() first.", exception.getMessage());
    }

    @Test
    void testThresholdWithUnitAmpere() {
        ThresholdAdder<VoltageThresholdAdder> thresholdAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(outageInstant)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold();
        FaraoException exception = assertThrows(FaraoException.class, () -> thresholdAdder.withUnit(Unit.AMPERE));
        assertEquals("A Unit is not suited to measure a VOLTAGE value.", exception.getMessage());
    }
}
