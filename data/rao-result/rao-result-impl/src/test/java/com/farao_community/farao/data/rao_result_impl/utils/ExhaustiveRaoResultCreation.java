/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl.utils;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_impl.utils.ExhaustiveCracCreation;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_impl.*;

import static com.farao_community.farao.commons.Unit.*;
import static com.farao_community.farao.data.rao_result_api.OptimizationState.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class ExhaustiveRaoResultCreation {

    /*
    Small CRAC used in unit tests of farao-core

    The idea of this RaoResult is to be quite exhaustive regarding the diversity of its object.
    It contains numerous object to ensure that they are all covered when testing the RaoResult
     */

    private ExhaustiveRaoResultCreation() {
    }

    public static RaoResult create() {
        Crac crac = ExhaustiveCracCreation.create();

        RaoResultImpl raoResult = new RaoResultImpl();
        raoResult.setComputationStatus(ComputationStatus.DEFAULT);

        // --------------------
        // --- Cost results ---
        // --------------------

        // CostResult at initial state
        CostResult costResult = raoResult.getAndCreateIfAbsentCostResult(INITIAL);
        costResult.setFunctionalCost(100.);
        costResult.setVirtualCost("loopFlow", 0.);
        costResult.setVirtualCost("MNEC", 0.);

        // CostResult after PRA
        costResult = raoResult.getAndCreateIfAbsentCostResult(AFTER_PRA);
        costResult.setFunctionalCost(80.);
        costResult.setVirtualCost("loopFlow", 0.);
        costResult.setVirtualCost("MNEC", 0.);

        // CostResult after ARA
        costResult = raoResult.getAndCreateIfAbsentCostResult(AFTER_ARA);
        costResult.setFunctionalCost(-20.);
        costResult.setVirtualCost("loopFlow", 15.);
        costResult.setVirtualCost("MNEC", 20.);

        // CostResult after CRA
        costResult = raoResult.getAndCreateIfAbsentCostResult(AFTER_CRA);
        costResult.setFunctionalCost(-50.);
        costResult.setVirtualCost("loopFlow", 10.);
        costResult.setVirtualCost("MNEC", 2.);

        // ------------------------
        // --- FlowCnec results ---
        // ------------------------

        /*
         use the following logic:
         value = XXXX + YYY + ZZ + A
         with:
         - XXXX = 1000 for cnec1, 2000 for cnec2, ...
         - YYY = 000 for initial, 100 for after_pra, 200 for after_ara and 300 for after_cra
         - ZZ = 10 for MW, 20 for AMPERE, 30 for DEGREE and 40 for KILOVOLT
         - A = 0 for flow, 1 for margin, 2 for relativeMargin, 3 for loop-flows, 4 for commercial flow, 5 for angle and 6 for voltage

         Moreover:
         - only cnec 1 and cnec 2 have loop-flows and commercial flows (in practice, only cross-border CNECs)
         - pure MNEC does not have a relative margin or a PTDF sum

         Note that there is no consistency within the FlowCnecResults, and between the FlowCnecResults
         and the CostResults
         */

        for (FlowCnec cnec : crac.getFlowCnecs()) {
            FlowCnecResult flowCnecResult = raoResult.getAndCreateIfAbsentFlowCnecResult(cnec);
            fillFlowCnecResult(flowCnecResult, cnec);
        }

        for (AngleCnec cnec : crac.getAngleCnecs()) {
            AngleCnecResult angleCnecResult = raoResult.getAndCreateIfAbsentAngleCnecResult(cnec);
            fillAngleCnecResult(angleCnecResult, cnec);
        }

        for (VoltageCnec cnec : crac.getVoltageCnecs()) {
            VoltageCnecResult voltageCnecResult = raoResult.getAndCreateIfAbsentVoltageCnecResult(cnec);
            fillVoltageCnecResult(voltageCnecResult, cnec);
        }

        // -----------------------------
        // --- NetworkAction results ---
        // -----------------------------

        for (NetworkAction networkAction : crac.getNetworkActions()) {
            NetworkActionResult nar = raoResult.getAndCreateIfAbsentNetworkActionResult(networkAction);

            switch (networkAction.getId()) {
                case "complexNetworkActionId":
                    // free to use preventive, activated
                    nar.addActivationForState(crac.getPreventiveState());
                    break;
                case "injectionSetpointRaId" :
                    // automaton, activated
                    nar.addActivationForState(crac.getState("contingency2Id", Instant.AUTO));
                    break;
                case "pstSetpointRaId" :
                    // forced in curative, activated
                    nar.addActivationForState(crac.getState("contingency1Id", Instant.CURATIVE));
                    break;
                case "switchPairRaId" :
                    // available in curative, not activated
                    break;
                default:
                    // do nothing
            }
        }

        // ------------------------------
        // --- PstRangeAction results ---
        // ------------------------------

        for (PstRangeAction pstRangeAction : crac.getPstRangeActions()) {
            PstRangeActionResult prar = (PstRangeActionResult) raoResult.getAndCreateIfAbsentRangeActionResult(pstRangeAction);

            switch (pstRangeAction.getId()) {
                case "pstRange1Id":
                    // free to use preventive, activated
                    prar.setInitialTap(0);
                    prar.setInitialSetpoint(0);
                    prar.addActivationForState(crac.getPreventiveState(), -7, -3.2);
                    break;
                case "pstRange2Id":
                    // on flow in preventive state, not activated
                    prar.setInitialTap(3);
                    prar.setInitialSetpoint(1.7);
                    break;
                case "pstRange3Id":
                    // on angle in curative state, not activated
                    prar.setInitialTap(2);
                    prar.setInitialSetpoint(1.3);
                    break;
                default:
                    // do nothing
            }
        }

        // -------------------------------
        // --- HvdcRangeAction results ---
        // -------------------------------

        for (HvdcRangeAction hvdcRangeAction : crac.getHvdcRangeActions()) {
            RangeActionResult hrar = raoResult.getAndCreateIfAbsentRangeActionResult(hvdcRangeAction);

            switch (hvdcRangeAction.getId()) {
                case "hvdcRange1Id":
                    // free to use preventive, activated
                    hrar.setInitialSetpoint(0);
                    hrar.addActivationForState(crac.getPreventiveState(), -1000);
                    break;
                case "hvdcRange2Id":
                    // activated for two curative states
                    hrar.setInitialSetpoint(-100);
                    hrar.addActivationForState(crac.getState("contingency1Id", Instant.CURATIVE), 100);
                    hrar.addActivationForState(crac.getState("contingency2Id", Instant.CURATIVE), 400);
                    break;
                default:
                    // do nothing
            }
        }

        // ------------------------------------
        // --- InjectionRangeAction results ---
        // ------------------------------------
        RangeActionResult irar = raoResult.getAndCreateIfAbsentRangeActionResult(crac.getInjectionRangeAction("injectionRange1Id"));
        irar.setInitialSetpoint(100);
        irar.addActivationForState(crac.getState("contingency1Id", Instant.CURATIVE), -300);

        return raoResult;
    }

    private static void fillFlowCnecResult(FlowCnecResult flowCnecResult, FlowCnec cnec) {

        double x = Integer.parseInt(String.valueOf(cnec.getId().charAt(4))) * 1000;
        boolean hasLoopFlow = cnec.getId().startsWith("cnec1") || cnec.getId().startsWith("cnec2");
        boolean isPureMnec = cnec.isMonitored() && !cnec.isOptimized();

        ElementaryFlowCnecResult initialEfcr = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(INITIAL);
        fillElementaryResult(initialEfcr, x, 100, hasLoopFlow, isPureMnec);
        ElementaryFlowCnecResult afterPraEfcr = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(AFTER_PRA);
        fillElementaryResult(afterPraEfcr, x, 200, hasLoopFlow, isPureMnec);

        if (cnec.getState().getInstant() == Instant.AUTO || cnec.getState().getInstant() == Instant.CURATIVE) {
            ElementaryFlowCnecResult afterAraEfcr = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(AFTER_ARA);
            fillElementaryResult(afterAraEfcr, x, 300, hasLoopFlow, isPureMnec);
        }
        if (cnec.getState().getInstant() == Instant.CURATIVE) {
            ElementaryFlowCnecResult afterCraEfcr = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(AFTER_CRA);
            fillElementaryResult(afterCraEfcr, x, 400, hasLoopFlow, isPureMnec);
        }
    }

    private static void fillAngleCnecResult(AngleCnecResult angleCnecResult, AngleCnec cnec) {

        double x = 3000;

        ElementaryAngleCnecResult initialEacr = angleCnecResult.getAndCreateIfAbsentResultForOptimizationState(INITIAL);
        fillElementaryResult(initialEacr, x, 100);
        ElementaryAngleCnecResult afterPraEacr = angleCnecResult.getAndCreateIfAbsentResultForOptimizationState(AFTER_PRA);
        fillElementaryResult(afterPraEacr, x, 200);

        if (cnec.getState().getInstant() == Instant.AUTO || cnec.getState().getInstant() == Instant.CURATIVE) {
            ElementaryAngleCnecResult afterAraEacr = angleCnecResult.getAndCreateIfAbsentResultForOptimizationState(AFTER_ARA);
            fillElementaryResult(afterAraEacr, x, 300);
        }
        if (cnec.getState().getInstant() == Instant.CURATIVE) {
            ElementaryAngleCnecResult afterCraEacr = angleCnecResult.getAndCreateIfAbsentResultForOptimizationState(AFTER_CRA);
            fillElementaryResult(afterCraEacr, x, 400);
        }
    }

    private static void fillVoltageCnecResult(VoltageCnecResult voltageCnecResult, VoltageCnec cnec) {

        double x = 4000;

        ElementaryVoltageCnecResult initialEacr = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(INITIAL);
        fillElementaryResult(initialEacr, x, 100);
        ElementaryVoltageCnecResult afterPraEacr = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(AFTER_PRA);
        fillElementaryResult(afterPraEacr, x, 200);

        if (cnec.getState().getInstant() == Instant.AUTO || cnec.getState().getInstant() == Instant.CURATIVE) {
            ElementaryVoltageCnecResult afterAraEacr = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(AFTER_ARA);
            fillElementaryResult(afterAraEacr, x, 300);
        }
        if (cnec.getState().getInstant() == Instant.CURATIVE) {
            ElementaryVoltageCnecResult afterCraEacr = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(AFTER_CRA);
            fillElementaryResult(afterCraEacr, x, 400);
        }
    }

    private static void fillElementaryResult(ElementaryFlowCnecResult elementaryFlowCnecResult, double x, double y, boolean hasLoopFlow, boolean isPureMnec) {

        elementaryFlowCnecResult.setFlow(x + y + 10, MEGAWATT);
        elementaryFlowCnecResult.setFlow(x + y + 20, AMPERE);

        elementaryFlowCnecResult.setMargin(x + y + 11, MEGAWATT);
        elementaryFlowCnecResult.setMargin(x + y + 21, AMPERE);

        if (!isPureMnec) {
            elementaryFlowCnecResult.setRelativeMargin(x + y + 12, MEGAWATT);
            elementaryFlowCnecResult.setRelativeMargin(x + y + 22, AMPERE);
            elementaryFlowCnecResult.setPtdfZonalSum(x / 10000);
        }
        if (hasLoopFlow) {
            elementaryFlowCnecResult.setLoopFlow(x + y + 13., MEGAWATT);
            elementaryFlowCnecResult.setLoopFlow(x + y + 23., AMPERE);
            elementaryFlowCnecResult.setCommercialFlow(x + y + 14, MEGAWATT);
            elementaryFlowCnecResult.setCommercialFlow(x + y + 24, AMPERE);
        }
    }

    private static void fillElementaryResult(ElementaryAngleCnecResult elementaryAngleCnecResult, double x, double y) {
        elementaryAngleCnecResult.setAngle(x + y + 35, DEGREE);
        elementaryAngleCnecResult.setMargin(x + y + 31, DEGREE);
    }

    private static void fillElementaryResult(ElementaryVoltageCnecResult elementaryVoltageCnecResult, double x, double y) {
        elementaryVoltageCnecResult.setVoltage(x + y + 46, KILOVOLT);
        elementaryVoltageCnecResult.setMargin(x + y + 41, KILOVOLT);
    }
}
