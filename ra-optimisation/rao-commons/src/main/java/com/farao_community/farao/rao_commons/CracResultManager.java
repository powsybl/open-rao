/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracResultManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CracResultManager.class);

    private final RaoData raoData;

    CracResultManager(RaoData raoData) {
        this.raoData = raoData;
    }

    /**
     * This method works from the working variant. It is filling CRAC result extension of the working variant
     * with values in network of the working variant.
     */
    public void fillRangeActionResultsWithNetworkValues() {
        for (RangeAction rangeAction : raoData.getAvailableRangeActions()) {
            double valueInNetwork = rangeAction.getCurrentValue(raoData.getNetwork());
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            RangeActionResult rangeActionResult = rangeActionResultMap.getVariant(raoData.getWorkingVariantId());
            rangeActionResult.setSetPoint(raoData.getOptimizedState().getId(), valueInNetwork);
            if (rangeAction instanceof PstRange) {
                ((PstRangeResult) rangeActionResult).setTap(raoData.getOptimizedState().getId(), ((PstRange) rangeAction).computeTapPosition(valueInNetwork));
            }
        }
    }

    /**
     * This method works from the working variant. It is applying on the network working variant
     * according to the values present in the CRAC result extension of the working variant.
     */
    public void applyRangeActionResultsOnNetwork() {
        for (RangeAction rangeAction : raoData.getAvailableRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            rangeAction.apply(raoData.getNetwork(),
                    rangeActionResultMap.getVariant(raoData.getWorkingVariantId()).getSetPoint(raoData.getOptimizedState().getId()));
        }
    }

    /**
     * This method compares CRAC result extension of two different variants. It compares the set point values
     * of all the range actions.
     *
     * @param variantId1: First variant to compare.
     * @param variantId2: Second variant to compare.
     * @return True if all the range actions are set at the same values and false otherwise.
     */
    public boolean sameRemedialActions(String variantId1, String variantId2) {
        for (RangeAction rangeAction : raoData.getAvailableRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            double value1 = rangeActionResultMap.getVariant(variantId1).getSetPoint(raoData.getOptimizedState().getId());
            double value2 = rangeActionResultMap.getVariant(variantId2).getSetPoint(raoData.getOptimizedState().getId());
            if (value1 != value2 && (!Double.isNaN(value1) || !Double.isNaN(value2))) {
                return false;
            }
        }
        return true;
    }

    public void fillRangeActionResultsWithLinearProblem(LinearProblem linearProblem) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Expected minimum margin: %.2f", linearProblem.getMinimumMarginVariable().solutionValue()));
            LOGGER.debug(String.format("Expected optimisation criterion: %.2f", linearProblem.getObjective().value()));
        }
        for (RangeAction rangeAction : raoData.getAvailableRangeActions()) {
            if (rangeAction instanceof PstRange) {
                String networkElementId = rangeAction.getNetworkElements().iterator().next().getId();
                double rangeActionVal = linearProblem.getRangeActionSetPointVariable(rangeAction).solutionValue();
                PstRange pstRange = (PstRange) rangeAction;
                TwoWindingsTransformer transformer = raoData.getNetwork().getTwoWindingsTransformer(networkElementId);

                int approximatedPostOptimTap = pstRange.computeTapPosition(rangeActionVal);
                double approximatedPostOptimAngle = transformer.getPhaseTapChanger().getStep(approximatedPostOptimTap).getAlpha();

                RangeActionResultExtension pstRangeResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
                PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(raoData.getWorkingVariantId());
                pstRangeResult.setSetPoint(raoData.getOptimizedState().getId(), approximatedPostOptimAngle);
                pstRangeResult.setTap(raoData.getOptimizedState().getId(), approximatedPostOptimTap);
                LOGGER.debug("Range action {} has been set to tap {}", pstRange.getName(), approximatedPostOptimTap);
            }
        }
    }

    public void fillCracResultWithCosts(double functionalCost, double virtualCost) {
        raoData.getCracResult().setFunctionalCost(functionalCost);
        raoData.getCracResult().addVirtualCost(virtualCost);
        raoData.getCracResult().setNetworkSecurityStatus(functionalCost < 0 ?
                CracResult.NetworkSecurityStatus.SECURED : CracResult.NetworkSecurityStatus.UNSECURED);
    }

    public void fillCnecResultWithFlows() {
        raoData.getCnecs().forEach(cnec -> {
            CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId());
            cnecResult.setFlowInMW(raoData.getSystematicSensitivityResult().getReferenceFlow(cnec));
            cnecResult.setFlowInA(raoData.getSystematicSensitivityResult().getReferenceIntensity(cnec));
            cnecResult.setThresholds(cnec);
        });
    }

    public void fillCnecLoopFlowExtensionsWithInitialResults(LoopFlowResult loopFlowResult, double loopFlowAcceptableAugmentation) {
        raoData.getLoopflowCnecs().forEach(cnec -> {
            CnecLoopFlowExtension cnecLoopFlowExtension = cnec.getExtension(CnecLoopFlowExtension.class);

            if (!Objects.isNull(cnecLoopFlowExtension)) {
                double loopFlowThreshold = Math.abs(cnecLoopFlowExtension.getInputThreshold(Unit.MEGAWATT));
                double initialLoopFlow = Math.abs(loopFlowResult.getLoopFlow(cnec));

                cnecLoopFlowExtension.setLoopFlowConstraintInMW(Math.max(initialLoopFlow + loopFlowAcceptableAugmentation, loopFlowThreshold - cnec.getReliabilityMargin()));
            }
        });
    }

    public void fillCnecResultsWithLoopFlows(LoopFlowResult loopFlowResult) {
        raoData.getLoopflowCnecs().forEach(cnec -> {
            CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId());
            if (!Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class))) {
                cnecResult.setLoopflowInMW(loopFlowResult.getLoopFlow(cnec));
                cnecResult.setLoopflowThresholdInMW(cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraintInMW());
                cnecResult.setCommercialFlowInMW(loopFlowResult.getCommercialFlow(cnec));
            }
        });
    }

    public void fillCnecResultsWithApproximatedLoopFlows() {
        raoData.getLoopflowCnecs().forEach(cnec -> {
            CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId());
            if (!Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class))) {
                double loopFLow = raoData.getSystematicSensitivityResult().getReferenceFlow(cnec) - cnecResult.getCommercialFlowInMW();
                cnecResult.setLoopflowInMW(loopFLow);
                cnecResult.setLoopflowThresholdInMW(cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraintInMW());
            }
        });
    }

    public void fillCnecResultsWithAbsolutePtdfSums(Map<BranchCnec, Double> ptdfSums) {
        ptdfSums.forEach((key, value) -> key.getExtension(CnecResultExtension.class).getVariant(raoData.getInitialVariantId()).setAbsolutePtdfSum(value));
    }

    public void copyCommercialFlowsBetweenVariants(String originVariant, String destinationVariant) {
        raoData.getLoopflowCnecs().forEach(cnec ->
                cnec.getExtension(CnecResultExtension.class).getVariant(destinationVariant)
                        .setCommercialFlowInMW(cnec.getExtension(CnecResultExtension.class).getVariant(originVariant).getCommercialFlowInMW())
        );
    }
}
