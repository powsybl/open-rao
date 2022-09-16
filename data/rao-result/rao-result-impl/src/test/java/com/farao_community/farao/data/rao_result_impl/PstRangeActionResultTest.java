/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import org.junit.Before;
import org.junit.Test;

import static com.farao_community.farao.data.crac_api.Instant.CURATIVE;
import static com.farao_community.farao.data.crac_api.Instant.OUTAGE;
import static junit.framework.TestCase.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PstRangeActionResultTest {

    private Crac crac;

    @Before
    public void setUp() {
        crac = CommonCracCreation.create();

        //define CNECs on Outage state so that the Crac contains outage states
        crac.newFlowCnec()
            .withId("cnec-outage-co1")
            .withNetworkElement("anyNetworkElement")
            .withContingency("Contingency FR1 FR2")
            .withInstant(OUTAGE)
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.MEGAWATT).withMax(1000.).add()
            .add();

        crac.newFlowCnec()
            .withId("cnec-outage-co2")
            .withNetworkElement("anyNetworkElement")
            .withContingency("Contingency FR1 FR3")
            .withInstant(OUTAGE)
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.MEGAWATT).withMax(1000.).add()
            .add();
    }

    @Test
    public void defaultValuesTest() {
        PstRangeActionResult pstRangeActionResult = new PstRangeActionResult();

        assertFalse(pstRangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertFalse(pstRangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", OUTAGE)));
        assertFalse(pstRangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", OUTAGE)));
        assertFalse(pstRangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertFalse(pstRangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", CURATIVE)));

        assertEquals(Double.NaN, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getPreventiveState()));
        assertEquals(Double.NaN, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", OUTAGE)));
        assertEquals(Double.NaN, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", OUTAGE)));
        assertEquals(Double.NaN, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertEquals(Double.NaN, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", CURATIVE)));

        assertEquals(Double.NaN, pstRangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()));
        assertEquals(Double.NaN, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", CURATIVE)));
    }

    @Test
    public void pstActivatedInPreventiveTest() {
        PstRangeActionResult pstRangeActionResult = new PstRangeActionResult();

        pstRangeActionResult.setInitialTap(3);
        pstRangeActionResult.setInitialSetpoint(0.3);
        pstRangeActionResult.setPostPraTap(33);
        pstRangeActionResult.setPostPraSetpoint(0.33);
        pstRangeActionResult.addActivationForState(crac.getPreventiveState(), 16, 1.6);

        //is activated

        assertTrue(pstRangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertFalse(pstRangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertFalse(pstRangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", CURATIVE)));

        // initial values

        // preventive state
        assertEquals(3, pstRangeActionResult.getPreOptimizedTapOnState(crac.getPreventiveState()));
        assertEquals(0.3, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);
        assertEquals(16, pstRangeActionResult.getOptimizedTapOnState(crac.getPreventiveState()));
        assertEquals(1.6, pstRangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);

        // outage state
        assertEquals(16, pstRangeActionResult.getPreOptimizedTapOnState(crac.getState("Contingency FR1 FR2", OUTAGE)));
        assertEquals(1.6, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", OUTAGE)), 1e-3);
        assertEquals(16, pstRangeActionResult.getOptimizedTapOnState(crac.getState("Contingency FR1 FR2", OUTAGE)));
        assertEquals(1.6, pstRangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", OUTAGE)), 1e-3);

        // curative state
        assertEquals(16, pstRangeActionResult.getPreOptimizedTapOnState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertEquals(1.6, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CURATIVE)), 1e-3);
        assertEquals(16, pstRangeActionResult.getOptimizedTapOnState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertEquals(1.6, pstRangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CURATIVE)), 1e-3);
    }

    @Test
    public void pstActivatedInCurativeTest() {
        PstRangeActionResult pstRangeActionResult = new PstRangeActionResult();

        pstRangeActionResult.setInitialTap(3);
        pstRangeActionResult.setInitialSetpoint(0.3);
        pstRangeActionResult.setPostPraTap(33);
        pstRangeActionResult.setPostPraSetpoint(0.33);
        pstRangeActionResult.addActivationForState(crac.getState("Contingency FR1 FR2", CURATIVE), -16, -1.6);

        //is activated

        assertFalse(pstRangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertTrue(pstRangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertFalse(pstRangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", CURATIVE)));

        // preventive state
        assertEquals(3, pstRangeActionResult.getPreOptimizedTapOnState(crac.getPreventiveState()));
        assertEquals(0.3, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);
        assertEquals(33, pstRangeActionResult.getOptimizedTapOnState(crac.getPreventiveState()));
        assertEquals(0.33, pstRangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);

        // outage state
        assertEquals(33, pstRangeActionResult.getPreOptimizedTapOnState(crac.getState("Contingency FR1 FR2", OUTAGE)));
        assertEquals(0.33, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", OUTAGE)), 1e-3);
        assertEquals(33, pstRangeActionResult.getOptimizedTapOnState(crac.getState("Contingency FR1 FR2", OUTAGE)));
        assertEquals(0.33, pstRangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", OUTAGE)), 1e-3);

        // curative state
        assertEquals(33, pstRangeActionResult.getPreOptimizedTapOnState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertEquals(0.33, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CURATIVE)), 1e-3);
        assertEquals(-16, pstRangeActionResult.getOptimizedTapOnState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertEquals(-1.6, pstRangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CURATIVE)), 1e-3);

        // other curative state (not activated
        assertEquals(33, pstRangeActionResult.getPreOptimizedTapOnState(crac.getState("Contingency FR1 FR3", CURATIVE)));
        assertEquals(0.33, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", CURATIVE)), 1e-3);
        assertEquals(33, pstRangeActionResult.getOptimizedTapOnState(crac.getState("Contingency FR1 FR3", CURATIVE)));
        assertEquals(0.33, pstRangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", CURATIVE)), 1e-3);
    }

    @Test
    public void pstActivatedInPreventiveAndCurative() {
        PstRangeActionResult pstRangeActionResult = new PstRangeActionResult();

        pstRangeActionResult.setInitialTap(3);
        pstRangeActionResult.setInitialSetpoint(0.3);
        pstRangeActionResult.setPostPraTap(33);
        pstRangeActionResult.setPostPraSetpoint(0.33);
        pstRangeActionResult.addActivationForState(crac.getPreventiveState(), 16, 1.6);
        pstRangeActionResult.addActivationForState(crac.getState("Contingency FR1 FR2", CURATIVE), -16, -1.6);

        //is activated

        assertTrue(pstRangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertTrue(pstRangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertFalse(pstRangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", CURATIVE)));
        assertEquals(2, pstRangeActionResult.getStatesWithActivation().size());

        // preventive state
        assertEquals(3, pstRangeActionResult.getPreOptimizedTapOnState(crac.getPreventiveState()));
        assertEquals(0.3, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);
        assertEquals(16, pstRangeActionResult.getOptimizedTapOnState(crac.getPreventiveState()));
        assertEquals(1.6, pstRangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);

        // outage state
        assertEquals(16, pstRangeActionResult.getPreOptimizedTapOnState(crac.getState("Contingency FR1 FR2", OUTAGE)));
        assertEquals(1.6, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", OUTAGE)), 1e-3);
        assertEquals(16, pstRangeActionResult.getOptimizedTapOnState(crac.getState("Contingency FR1 FR2", OUTAGE)));
        assertEquals(1.6, pstRangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", OUTAGE)), 1e-3);

        // curative state
        assertEquals(16, pstRangeActionResult.getPreOptimizedTapOnState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertEquals(1.6, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CURATIVE)), 1e-3);
        assertEquals(-16, pstRangeActionResult.getOptimizedTapOnState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertEquals(-1.6, pstRangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CURATIVE)), 1e-3);

        // other curative state (not activated
        assertEquals(16, pstRangeActionResult.getPreOptimizedTapOnState(crac.getState("Contingency FR1 FR3", CURATIVE)));
        assertEquals(1.6, pstRangeActionResult.getPreOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", CURATIVE)), 1e-3);
        assertEquals(16, pstRangeActionResult.getOptimizedTapOnState(crac.getState("Contingency FR1 FR3", CURATIVE)));
        assertEquals(1.6, pstRangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", CURATIVE)), 1e-3);
    }
}
