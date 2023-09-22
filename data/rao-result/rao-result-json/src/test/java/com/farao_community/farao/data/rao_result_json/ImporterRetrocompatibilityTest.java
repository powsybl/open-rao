/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.*;
import com.farao_community.farao.data.crac_io_json.JsonImport;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.*;
import static com.farao_community.farao.data.crac_api.cnec.Side.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class ImporterRetrocompatibilityTest {

    private static final double DOUBLE_TOLERANCE = 1e-6;

    /*
    The goal of this test class is to ensure that former JSON RaoResult files are
    still importable, even when modifications are brought to the JSON importer.
     */

    /*
    CARE: the existing json file used in this test case SHOULD NOT BE MODIFIED. If
    the current tests do not pass, it means that formerly generated JSON RaoResult
    will not be compatible anymore with the next version of farao-core -> This is NOT
    desirable.

    Instead, we need to ensure that the JSON RaoResult files used in this class can
    still be imported as is. Using versioning of the importer if needed.
     */

    @Test
    void importV1Point0Test() {

        // JSON file of farao-core v3.4.3
        /*
         versioning was not yet in place, and version number does not explicitly appear
         in v1.0 files
         */
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.0/rao-result-v1.0.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.0/crac-for-rao-result-v1.0.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1RaoResult(raoResult, crac);
    }

    @Test
    void importV1Point1Test() {

        // JSON file of farao-core v3.5.0
        /*
         addition of versioning, no changes apart from the fact that version numbers
         are now added in the first lines of the json
         */

        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.1/rao-result-v1.1.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.1/crac-for-rao-result-v1.1.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1RaoResult(raoResult, crac);
        testExtraContentOfV1Point1RaoResult(raoResult, crac);
    }

    @Test
    void importAfterV1Point1FieldDeprecationTest() {

        // unused field should throw an exception

        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.1/rao-result-v1.2-error.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.1/crac-for-rao-result-v1.1.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResultImporter importer = new RaoResultImporter();
        assertThrows(FaraoException.class, () -> importer.importRaoResult(raoResultFile, crac));
    }

    @Test
    void importV1Point2Test() {
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/rao-result-v1.2.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/crac-for-rao-result-v1.2.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1Point2RaoResult(raoResult, crac);
    }

    @Test
    void importV1Point2FieldDeprecationTest() {
        // RaoResult copied from v1.1 but version set to v1.2
        // Should not be imported because CNEC side is not defined properly
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/rao-result-v1.2-error.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/crac-for-rao-result-v1.2.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResultImporter importer = new RaoResultImporter();
        assertThrows(FaraoException.class, () -> importer.importRaoResult(raoResultFile, crac));
    }

    @Test
    void importV1Point3Test() {
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/rao-result-v1.3.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/crac-for-rao-result-v1.3.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1Point2RaoResult(raoResult, crac);
        // Test computation status map
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus(crac.getPreventiveState()));
        assertEquals(ComputationStatus.FAILURE, raoResult.getComputationStatus(crac.getState("contingency1Id", crac.getInstant(Instant.Kind.CURATIVE))));
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus(crac.getState("contingency2Id", crac.getInstant(Instant.Kind.AUTO))));
    }

    @Test
    void importV1Point3TestFieldDeprecationTest() {
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/crac-for-rao-result-v1.3.json");
        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResultImporter importer = new RaoResultImporter();

        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/rao-result-v1.3-error1.json");
        assertThrows(FaraoException.class, () -> importer.importRaoResult(raoResultFile, crac));

        InputStream raoResultFile2 = getClass().getResourceAsStream("/retrocompatibility/v1.3/rao-result-v1.3-error2.json");
        assertThrows(FaraoException.class, () -> importer.importRaoResult(raoResultFile2, crac));
    }

    private void testBaseContentOfV1RaoResult(RaoResult importedRaoResult, Crac crac) {
        OptimizationState initial = OptimizationState.initial(crac);
        OptimizationState afterPra = OptimizationState.afterPra(crac);
        OptimizationState afterAra = OptimizationState.afterAra(crac);
        OptimizationState afterCra = OptimizationState.afterCra(crac);

        // --------------------------
        // --- Computation status ---
        // --------------------------
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus());

        // --------------------------
        // --- test Costs results ---
        // --------------------------
        assertEquals(Set.of("loopFlow", "MNEC"), importedRaoResult.getVirtualCostNames());

        assertEquals(100., importedRaoResult.getFunctionalCost(initial), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(initial, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(initial, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(initial), DOUBLE_TOLERANCE);
        assertEquals(100., importedRaoResult.getCost(initial), DOUBLE_TOLERANCE);

        assertEquals(80., importedRaoResult.getFunctionalCost(afterPra), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(afterPra, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(afterPra, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(afterPra), DOUBLE_TOLERANCE);
        assertEquals(80., importedRaoResult.getCost(afterPra), DOUBLE_TOLERANCE);

        assertEquals(-20., importedRaoResult.getFunctionalCost(afterAra), DOUBLE_TOLERANCE);
        assertEquals(15., importedRaoResult.getVirtualCost(afterAra, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(20., importedRaoResult.getVirtualCost(afterAra, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(35., importedRaoResult.getVirtualCost(afterAra), DOUBLE_TOLERANCE);
        assertEquals(15., importedRaoResult.getCost(afterAra), DOUBLE_TOLERANCE);

        assertEquals(-50., importedRaoResult.getFunctionalCost(afterCra), DOUBLE_TOLERANCE);
        assertEquals(10., importedRaoResult.getVirtualCost(afterCra, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(2., importedRaoResult.getVirtualCost(afterCra, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(12., importedRaoResult.getVirtualCost(afterCra), DOUBLE_TOLERANCE);
        assertEquals(-38, importedRaoResult.getCost(afterCra), DOUBLE_TOLERANCE);

        // -----------------------------
        // --- test FlowCnec results ---
        // -----------------------------

        /*
        cnec4prevId: preventive, no loop-flows, optimized
        - contains result in INITIAL and in AFTER_PRA. Results in AFTER_ARA and AFTER_CRA are the same as AFTER_PRA because the CNEC is preventive
        - contains result relative margin and PTDF sum but not for loop and commercial flows
         */
        FlowCnec cnecP = crac.getFlowCnec("cnec4prevId");
        assertEquals(4110., importedRaoResult.getFlow(initial, cnecP, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(4111., importedRaoResult.getMargin(initial, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(4112., importedRaoResult.getRelativeMargin(initial, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(initial, cnecP, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(initial, cnecP, LEFT, MEGAWATT)));

        assertEquals(4220., importedRaoResult.getFlow(afterPra, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(4221., importedRaoResult.getMargin(afterPra, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(4222., importedRaoResult.getRelativeMargin(afterPra, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(initial, cnecP, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(initial, cnecP, LEFT, MEGAWATT)));

        assertEquals(0.4, importedRaoResult.getPtdfZonalSum(afterPra, cnecP, LEFT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(afterAra, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(afterPra, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(afterCra, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(afterPra, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);

        /*
        cnec1outageId: outage, with loop-flows, optimized
        - contains result in INITIAL and in AFTER_PRA. Results in AFTER_ARA and AFTER_CRA are the same as AFTER_PRA because the CNEC is preventive
        - contains result for loop-flows, commercial flows, relative margin and PTDF sum
         */

        FlowCnec cnecO = crac.getFlowCnec("cnec1outageId");
        assertEquals(1120., importedRaoResult.getFlow(initial, cnecO, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1121., importedRaoResult.getMargin(initial, cnecO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1122., importedRaoResult.getRelativeMargin(initial, cnecO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1123., importedRaoResult.getLoopFlow(initial, cnecO, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1124., importedRaoResult.getCommercialFlow(initial, cnecO, LEFT, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1210., importedRaoResult.getFlow(afterPra, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1211., importedRaoResult.getMargin(afterPra, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1212., importedRaoResult.getRelativeMargin(afterPra, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1213., importedRaoResult.getLoopFlow(afterPra, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1214., importedRaoResult.getCommercialFlow(afterPra, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(0.1, importedRaoResult.getPtdfZonalSum(afterPra, cnecO, LEFT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(afterAra, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(afterPra, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(afterCra, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(afterPra, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);

        /*
        cnec3autoId: auto, without loop-flows, pureMNEC
        - contains result in INITIAL, AFTER_PRA, and AFTER_ARA. Results in AFTER_CRA are the same as AFTER_ARA because the CNEC is auto
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecA = crac.getFlowCnec("cnec3autoId");
        assertEquals(3110., importedRaoResult.getFlow(initial, cnecA, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3111., importedRaoResult.getMargin(initial, cnecA, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(initial, cnecA, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(initial, cnecA, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(initial, cnecA, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(initial, cnecA, LEFT)));

        assertEquals(3220., importedRaoResult.getFlow(afterPra, cnecA, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(afterPra, cnecA, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(afterAra, cnecA, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(afterAra, cnecA, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(afterCra, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(afterAra, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);

         /*
        cnec3curId: curative, without loop-flows, pureMNEC
        - contains result in INITIAL, AFTER_PRA, and AFTER_ARA and in AFTER_CRA
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecC = crac.getFlowCnec("cnec3curId");
        assertEquals(3110., importedRaoResult.getFlow(initial, cnecC, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3111., importedRaoResult.getMargin(initial, cnecC, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(initial, cnecC, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(initial, cnecC, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(initial, cnecC, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(initial, cnecC, LEFT)));

        assertEquals(3220., importedRaoResult.getFlow(afterPra, cnecC, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(afterPra, cnecC, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(afterAra, cnecC, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(afterAra, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(3410., importedRaoResult.getFlow(afterCra, cnecC, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3411., importedRaoResult.getMargin(afterCra, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        // -----------------------------
        // --- NetworkAction results ---
        // -----------------------------

        State pState = crac.getPreventiveState();
        State oState2 = crac.getState("contingency2Id", crac.getInstant(Instant.Kind.OUTAGE));
        State aState2 = crac.getState("contingency2Id", crac.getInstant(Instant.Kind.AUTO));
        State cState1 = crac.getState("contingency1Id", crac.getInstant(Instant.Kind.CURATIVE));
        State cState2 = crac.getState("contingency2Id", crac.getInstant(Instant.Kind.CURATIVE));

        /*
        complexNetworkActionId, activated in preventive
        */
        NetworkAction naP = crac.getNetworkAction("complexNetworkActionId");
        assertTrue(importedRaoResult.isActivatedDuringState(pState, naP));
        assertTrue(importedRaoResult.isActivated(pState, naP));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naP));
        assertFalse(importedRaoResult.isActivatedDuringState(aState2, naP));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, naP));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naP));
        assertTrue(importedRaoResult.isActivated(cState1, naP));
        assertTrue(importedRaoResult.isActivated(cState2, naP));

        /*
        injectionSetpointRaId, activated in auto
        */
        NetworkAction naA = crac.getNetworkAction("injectionSetpointRaId");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, naA));
        assertFalse(importedRaoResult.isActivated(pState, naA));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naA));
        assertTrue(importedRaoResult.isActivatedDuringState(aState2, naA));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, naA));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naA));
        assertFalse(importedRaoResult.isActivated(cState1, naA));
        assertTrue(importedRaoResult.isActivated(cState2, naA));

        /*
        pstSetpointRaId, activated curative1
        */
        NetworkAction naC = crac.getNetworkAction("pstSetpointRaId");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, naC));
        assertFalse(importedRaoResult.isActivated(pState, naC));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naC));
        assertFalse(importedRaoResult.isActivatedDuringState(aState2, naC));
        assertTrue(importedRaoResult.isActivatedDuringState(cState1, naC));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naC));
        assertTrue(importedRaoResult.isActivated(cState1, naC));
        assertFalse(importedRaoResult.isActivated(cState2, naC));

        /*
        switchPairRaId, never activated
        */
        NetworkAction naN = crac.getNetworkAction("switchPairRaId");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, naN));
        assertFalse(importedRaoResult.isActivated(pState, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(aState2, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naN));
        assertFalse(importedRaoResult.isActivated(cState1, naN));
        assertFalse(importedRaoResult.isActivated(cState2, naN));

        // ------------------------------
        // --- PstRangeAction results ---
        // ------------------------------

        /*
        pstRange1Id, activated in preventive
        */
        PstRangeAction pstP = crac.getPstRangeAction("pstRange1Id");
        assertTrue(importedRaoResult.isActivatedDuringState(pState, pstP));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, pstP));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, pstP));
        assertEquals(-3, importedRaoResult.getPreOptimizationTapOnState(pState, pstP));
        assertEquals(0., importedRaoResult.getPreOptimizationSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(pState, pstP));
        assertEquals(3., importedRaoResult.getOptimizedSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        assertEquals(3., importedRaoResult.getPreOptimizationSetPointOnState(cState1, pstP), DOUBLE_TOLERANCE);
        assertEquals(3, importedRaoResult.getPreOptimizationTapOnState(cState1, pstP));
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(cState1, pstP));
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(cState2, pstP));

        /*
        pstRange2Id, not activated
        */
        PstRangeAction pstN = crac.getPstRangeAction("pstRange2Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, pstN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, pstN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, pstN));
        assertEquals(0, importedRaoResult.getPreOptimizationTapOnState(pState, pstN));
        assertEquals(0, importedRaoResult.getOptimizedTapOnState(pState, pstN));
        assertEquals(0, importedRaoResult.getOptimizedTapOnState(cState1, pstN));
        assertEquals(0, importedRaoResult.getOptimizedTapOnState(cState2, pstN));

        // ---------------------------
        // --- RangeAction results ---
        // ---------------------------

        /*
        hvdcRange2Id, two different activations in the two curative states
        */
        HvdcRangeAction hvdcC = crac.getHvdcRangeAction("hvdcRange2Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, hvdcC));
        assertTrue(importedRaoResult.isActivatedDuringState(cState1, hvdcC));
        assertTrue(importedRaoResult.isActivatedDuringState(cState2, hvdcC));
        assertEquals(-100, importedRaoResult.getPreOptimizationSetPointOnState(pState, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(-100, importedRaoResult.getOptimizedSetPointOnState(pState, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(-100, importedRaoResult.getPreOptimizationSetPointOnState(cState1, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(100, importedRaoResult.getOptimizedSetPointOnState(cState1, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(400, importedRaoResult.getOptimizedSetPointOnState(cState2, hvdcC), DOUBLE_TOLERANCE);
    }

    private void testExtraContentOfV1Point1RaoResult(RaoResult importedRaoResult, Crac crac) {
        OptimizationState initial = OptimizationState.initial(crac);
        OptimizationState afterPra = OptimizationState.afterPra(crac);
        OptimizationState afterAra = OptimizationState.afterAra(crac);
        OptimizationState afterCra = OptimizationState.afterCra(crac);

        assertEquals(-1, importedRaoResult.getOptimizedTapOnState(crac.getPreventiveState(), crac.getPstRangeAction("pstRange3Id")));

        InjectionRangeAction rangeAction = crac.getInjectionRangeAction("injectionRange1Id");
        assertEquals(100., importedRaoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), rangeAction), DOUBLE_TOLERANCE);
        assertEquals(-300., importedRaoResult.getOptimizedSetPointOnState(crac.getState("contingency1Id", crac.getInstant(Instant.Kind.CURATIVE)), rangeAction), DOUBLE_TOLERANCE);

        AngleCnec angleCnec = crac.getAngleCnec("angleCnecId");
        assertEquals(3135., importedRaoResult.getAngle(initial, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3131., importedRaoResult.getMargin(initial, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3235., importedRaoResult.getAngle(afterPra, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3231., importedRaoResult.getMargin(afterPra, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3335., importedRaoResult.getAngle(afterAra, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3331., importedRaoResult.getMargin(afterAra, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3435., importedRaoResult.getAngle(afterCra, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3431., importedRaoResult.getMargin(afterCra, angleCnec, DEGREE), DOUBLE_TOLERANCE);

        VoltageCnec voltageCnec = crac.getVoltageCnec("voltageCnecId");
        assertEquals(4146., importedRaoResult.getVoltage(initial, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4141., importedRaoResult.getMargin(initial, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4246., importedRaoResult.getVoltage(afterPra, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4241., importedRaoResult.getMargin(afterPra, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4346., importedRaoResult.getVoltage(afterAra, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4341., importedRaoResult.getMargin(afterAra, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4446., importedRaoResult.getVoltage(afterCra, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4441., importedRaoResult.getMargin(afterCra, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
    }

    private void testBaseContentOfV1Point2RaoResult(RaoResult importedRaoResult, Crac crac) {
        OptimizationState initial = OptimizationState.initial(crac);
        OptimizationState afterPra = OptimizationState.afterPra(crac);
        OptimizationState afterAra = OptimizationState.afterAra(crac);
        OptimizationState afterCra = OptimizationState.afterCra(crac);
        // --------------------------
        // --- Computation status ---
        // --------------------------
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus());

        // --------------------------
        // --- test Costs results ---
        // --------------------------
        assertEquals(Set.of("loopFlow", "MNEC"), importedRaoResult.getVirtualCostNames());

        assertEquals(100., importedRaoResult.getFunctionalCost(initial), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(initial, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(initial, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(initial), DOUBLE_TOLERANCE);
        assertEquals(100., importedRaoResult.getCost(initial), DOUBLE_TOLERANCE);

        assertEquals(80., importedRaoResult.getFunctionalCost(afterPra), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(afterPra, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(afterPra, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(afterPra), DOUBLE_TOLERANCE);
        assertEquals(80., importedRaoResult.getCost(afterPra), DOUBLE_TOLERANCE);

        assertEquals(-20., importedRaoResult.getFunctionalCost(afterAra), DOUBLE_TOLERANCE);
        assertEquals(15., importedRaoResult.getVirtualCost(afterAra, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(20., importedRaoResult.getVirtualCost(afterAra, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(35., importedRaoResult.getVirtualCost(afterAra), DOUBLE_TOLERANCE);
        assertEquals(15., importedRaoResult.getCost(afterAra), DOUBLE_TOLERANCE);

        assertEquals(-50., importedRaoResult.getFunctionalCost(afterCra), DOUBLE_TOLERANCE);
        assertEquals(10., importedRaoResult.getVirtualCost(afterCra, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(2., importedRaoResult.getVirtualCost(afterCra, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(12., importedRaoResult.getVirtualCost(afterCra), DOUBLE_TOLERANCE);
        assertEquals(-38, importedRaoResult.getCost(afterCra), DOUBLE_TOLERANCE);

        // -----------------------------
        // --- test FlowCnec results ---
        // -----------------------------

        /*
        cnec4prevId: preventive, no loop-flows, optimized
        - contains result in INITIAL and in AFTER_PRA. Results in AFTER_ARA and AFTER_CRA are the same as AFTER_PRA because the CNEC is preventive
        - contains result relative margin and PTDF sum but not for loop and commercial flows
         */
        FlowCnec cnecP = crac.getFlowCnec("cnec4prevId");
        assertEquals(4110., importedRaoResult.getFlow(initial, cnecP, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getFlow(initial, cnecP, Side.RIGHT, MEGAWATT)));
        assertEquals(4111., importedRaoResult.getMargin(initial, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(4112., importedRaoResult.getRelativeMargin(initial, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(initial, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(initial, cnecP, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(initial, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(initial, cnecP, Side.RIGHT, MEGAWATT)));

        assertEquals(4220., importedRaoResult.getFlow(afterPra, cnecP, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getFlow(afterPra, cnecP, Side.RIGHT, AMPERE)));
        assertEquals(4221., importedRaoResult.getMargin(afterPra, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(4222., importedRaoResult.getRelativeMargin(afterPra, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(initial, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(initial, cnecP, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(initial, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(initial, cnecP, Side.RIGHT, MEGAWATT)));

        assertEquals(0.4, importedRaoResult.getPtdfZonalSum(afterPra, cnecP, Side.LEFT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(afterPra, cnecP, Side.RIGHT)));

        assertEquals(importedRaoResult.getFlow(afterAra, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(afterPra, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(afterAra, cnecP, RIGHT, AMPERE), importedRaoResult.getFlow(afterPra, cnecP, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(afterCra, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(afterPra, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(afterCra, cnecP, RIGHT, AMPERE), importedRaoResult.getFlow(afterPra, cnecP, RIGHT, AMPERE), DOUBLE_TOLERANCE);

        /*
        cnec1outageId: outage, with loop-flows, optimized
        - contains result in INITIAL and in AFTER_PRA. Results in AFTER_ARA and AFTER_CRA are the same as AFTER_PRA because the CNEC is preventive
        - contains result for loop-flows, commercial flows, relative margin and PTDF sum
         */

        FlowCnec cnecO = crac.getFlowCnec("cnec1outageId");
        assertTrue(Double.isNaN(importedRaoResult.getFlow(initial, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1120.5, importedRaoResult.getFlow(initial, cnecO, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1121., importedRaoResult.getMargin(initial, cnecO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1122., importedRaoResult.getRelativeMargin(initial, cnecO, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(initial, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1123.5, importedRaoResult.getLoopFlow(initial, cnecO, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(initial, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1124.5, importedRaoResult.getCommercialFlow(initial, cnecO, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getFlow(afterPra, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1210.5, importedRaoResult.getFlow(afterPra, cnecO, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1211., importedRaoResult.getMargin(afterPra, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1212., importedRaoResult.getRelativeMargin(afterPra, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(afterPra, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1213.5, importedRaoResult.getLoopFlow(afterPra, cnecO, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(afterPra, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1214.5, importedRaoResult.getCommercialFlow(afterPra, cnecO, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(afterPra, cnecO, Side.LEFT)));
        assertEquals(0.6, importedRaoResult.getPtdfZonalSum(afterPra, cnecO, Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(afterAra, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(afterPra, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(afterAra, cnecO, RIGHT, MEGAWATT), importedRaoResult.getFlow(afterPra, cnecO, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(afterCra, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(afterPra, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(afterCra, cnecO, RIGHT, MEGAWATT), importedRaoResult.getFlow(afterPra, cnecO, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

        /*
        cnec3autoId: auto, without loop-flows, pureMNEC
        - contains result in INITIAL, AFTER_PRA, and AFTER_ARA. Results in AFTER_CRA are the same as AFTER_ARA because the CNEC is auto
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecA = crac.getFlowCnec("cnec3autoId");
        assertEquals(3110., importedRaoResult.getFlow(initial, cnecA, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3110.5, importedRaoResult.getFlow(initial, cnecA, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3111., importedRaoResult.getMargin(initial, cnecA, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(initial, cnecA, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(initial, cnecA, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(initial, cnecA, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(initial, cnecA, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(initial, cnecA, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(initial, cnecA, Side.LEFT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(initial, cnecA, Side.RIGHT)));

        assertEquals(3220., importedRaoResult.getFlow(afterPra, cnecA, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3220.5, importedRaoResult.getFlow(afterPra, cnecA, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(afterPra, cnecA, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(afterAra, cnecA, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3310.5, importedRaoResult.getFlow(afterAra, cnecA, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(afterAra, cnecA, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(afterCra, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(afterAra, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(afterCra, cnecO, RIGHT, MEGAWATT), importedRaoResult.getFlow(afterAra, cnecO, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

         /*
        cnec3curId: curative, without loop-flows, pureMNEC
        - contains result in INITIAL, AFTER_PRA, and AFTER_ARA and in AFTER_CRA
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecC = crac.getFlowCnec("cnec3curId");
        assertEquals(3110., importedRaoResult.getFlow(initial, cnecC, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3110.5, importedRaoResult.getFlow(initial, cnecC, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3111., importedRaoResult.getMargin(initial, cnecC, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(initial, cnecC, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(initial, cnecC, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(initial, cnecC, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(initial, cnecC, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(initial, cnecC, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(initial, cnecC, Side.LEFT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(initial, cnecC, Side.RIGHT)));

        assertEquals(3220., importedRaoResult.getFlow(afterPra, cnecC, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3220.5, importedRaoResult.getFlow(afterPra, cnecC, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(afterPra, cnecC, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(afterAra, cnecC, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3310.5, importedRaoResult.getFlow(afterAra, cnecC, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(afterAra, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(3410., importedRaoResult.getFlow(afterCra, cnecC, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3410.5, importedRaoResult.getFlow(afterCra, cnecC, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3411., importedRaoResult.getMargin(afterCra, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        // -----------------------------
        // --- NetworkAction results ---
        // -----------------------------

        State pState = crac.getPreventiveState();
        State oState2 = crac.getState("contingency2Id", crac.getInstant(Instant.Kind.OUTAGE));
        State aState2 = crac.getState("contingency2Id", crac.getInstant(Instant.Kind.AUTO));
        State cState1 = crac.getState("contingency1Id", crac.getInstant(Instant.Kind.CURATIVE));
        State cState2 = crac.getState("contingency2Id", crac.getInstant(Instant.Kind.CURATIVE));

        /*
        complexNetworkActionId, activated in preventive
        */
        NetworkAction naP = crac.getNetworkAction("complexNetworkActionId");
        assertTrue(importedRaoResult.isActivatedDuringState(pState, naP));
        assertTrue(importedRaoResult.isActivated(pState, naP));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naP));
        assertFalse(importedRaoResult.isActivatedDuringState(aState2, naP));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, naP));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naP));
        assertTrue(importedRaoResult.isActivated(cState1, naP));
        assertTrue(importedRaoResult.isActivated(cState2, naP));

        /*
        injectionSetpointRaId, activated in auto
        */
        NetworkAction naA = crac.getNetworkAction("injectionSetpointRaId");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, naA));
        assertFalse(importedRaoResult.isActivated(pState, naA));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naA));
        assertTrue(importedRaoResult.isActivatedDuringState(aState2, naA));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, naA));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naA));
        assertFalse(importedRaoResult.isActivated(cState1, naA));
        assertTrue(importedRaoResult.isActivated(cState2, naA));

        /*
        pstSetpointRaId, activated curative1
        */
        NetworkAction naC = crac.getNetworkAction("pstSetpointRaId");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, naC));
        assertFalse(importedRaoResult.isActivated(pState, naC));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naC));
        assertFalse(importedRaoResult.isActivatedDuringState(aState2, naC));
        assertTrue(importedRaoResult.isActivatedDuringState(cState1, naC));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naC));
        assertTrue(importedRaoResult.isActivated(cState1, naC));
        assertFalse(importedRaoResult.isActivated(cState2, naC));

        /*
        switchPairRaId, never activated
        */
        NetworkAction naN = crac.getNetworkAction("switchPairRaId");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, naN));
        assertFalse(importedRaoResult.isActivated(pState, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(aState2, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naN));
        assertFalse(importedRaoResult.isActivated(cState1, naN));
        assertFalse(importedRaoResult.isActivated(cState2, naN));

        // ------------------------------
        // --- PstRangeAction results ---
        // ------------------------------

        /*
        pstRange1Id, activated in preventive
        */
        PstRangeAction pstP = crac.getPstRangeAction("pstRange1Id");
        assertTrue(importedRaoResult.isActivatedDuringState(pState, pstP));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, pstP));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, pstP));
        assertEquals(-3, importedRaoResult.getPreOptimizationTapOnState(pState, pstP));
        assertEquals(0., importedRaoResult.getPreOptimizationSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(pState, pstP));
        assertEquals(3., importedRaoResult.getOptimizedSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        assertEquals(3., importedRaoResult.getPreOptimizationSetPointOnState(cState1, pstP), DOUBLE_TOLERANCE);
        assertEquals(3, importedRaoResult.getPreOptimizationTapOnState(cState1, pstP));
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(cState1, pstP));
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(cState2, pstP));

        /*
        pstRange2Id, not activated
        */
        PstRangeAction pstN = crac.getPstRangeAction("pstRange2Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, pstN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, pstN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, pstN));
        assertEquals(0, importedRaoResult.getPreOptimizationTapOnState(pState, pstN));
        assertEquals(0, importedRaoResult.getOptimizedTapOnState(pState, pstN));
        assertEquals(0, importedRaoResult.getOptimizedTapOnState(cState1, pstN));
        assertEquals(0, importedRaoResult.getOptimizedTapOnState(cState2, pstN));

        // ---------------------------
        // --- RangeAction results ---
        // ---------------------------

        /*
        hvdcRange2Id, two different activations in the two curative states
        */
        HvdcRangeAction hvdcC = crac.getHvdcRangeAction("hvdcRange2Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, hvdcC));
        assertTrue(importedRaoResult.isActivatedDuringState(cState1, hvdcC));
        assertTrue(importedRaoResult.isActivatedDuringState(cState2, hvdcC));
        assertEquals(-100, importedRaoResult.getPreOptimizationSetPointOnState(pState, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(-100, importedRaoResult.getOptimizedSetPointOnState(pState, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(-100, importedRaoResult.getPreOptimizationSetPointOnState(cState1, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(100, importedRaoResult.getOptimizedSetPointOnState(cState1, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(400, importedRaoResult.getOptimizedSetPointOnState(cState2, hvdcC), DOUBLE_TOLERANCE);

        /*
        InjectionRange1Id, one activation in curative
        */
        InjectionRangeAction injectionC = crac.getInjectionRangeAction("injectionRange1Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, injectionC));
        assertTrue(importedRaoResult.isActivatedDuringState(cState1, injectionC));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, injectionC));
        assertEquals(100, importedRaoResult.getPreOptimizationSetPointOnState(pState, injectionC), DOUBLE_TOLERANCE);
        assertEquals(100, importedRaoResult.getPreOptimizationSetPointOnState(cState1, injectionC), DOUBLE_TOLERANCE);
        assertEquals(-300, importedRaoResult.getOptimizedSetPointOnState(cState1, injectionC), DOUBLE_TOLERANCE);

        /*
        AngleCnec
        */
        AngleCnec angleCnec = crac.getAngleCnec("angleCnecId");
        assertEquals(3135., importedRaoResult.getAngle(initial, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3131., importedRaoResult.getMargin(initial, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3235., importedRaoResult.getAngle(afterPra, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3231., importedRaoResult.getMargin(afterPra, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3335., importedRaoResult.getAngle(afterAra, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3331., importedRaoResult.getMargin(afterAra, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3435., importedRaoResult.getAngle(afterCra, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3431., importedRaoResult.getMargin(afterCra, angleCnec, DEGREE), DOUBLE_TOLERANCE);

        /*
        VoltageCnec
        */
        VoltageCnec voltageCnec = crac.getVoltageCnec("voltageCnecId");
        assertEquals(4146., importedRaoResult.getVoltage(initial, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4141., importedRaoResult.getMargin(initial, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4246., importedRaoResult.getVoltage(afterPra, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4241., importedRaoResult.getMargin(afterPra, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4346., importedRaoResult.getVoltage(afterAra, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4341., importedRaoResult.getMargin(afterAra, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4446., importedRaoResult.getVoltage(afterCra, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4441., importedRaoResult.getMargin(afterCra, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
    }

    // TODO : test multiple curative instants
}
