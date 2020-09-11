/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.*;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.*;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import com.powsybl.ucte.util.UcteAliasesCreation;

import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.rao_api.RaoParameters.ObjectiveFunction.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoUtil {

    private RaoUtil() { }

    public static RaoData initRaoData(RaoInput raoInput, RaoParameters raoParameters) {
        Network network = raoInput.getNetwork();
        Crac crac = raoInput.getCrac();
        String variantId = raoInput.getVariantId();

        network.getVariantManager().setWorkingVariant(variantId);
        UcteAliasesCreation.createAliases(network);
        RaoInputHelper.cleanCrac(crac, network);
        RaoInputHelper.synchronize(crac, network);
        RaoData raoData = new RaoData(network, crac);
        crac.getExtension(ResultVariantManager.class).setPreOptimVariantId(raoData.getInitialVariantId());

        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            LoopFlowComputationService.checkDataConsistency(raoData);
            LoopFlowComputationService.computeInitialLoopFlowsAndUpdateCnecLoopFlowConstraint(raoData, raoParameters.getLoopFlowViolationCost());
        }

        return raoData;
    }

    public static IteratingLinearOptimizer createLinearOptimizer(RaoParameters raoParameters, SystematicSensitivityComputation systematicSensitivityComputation) {
        List<ProblemFiller> fillers = new ArrayList<>();
        fillers.add(new CoreProblemFiller(raoParameters.getPstSensitivityThreshold()));
        if (raoParameters.getObjectiveFunction().equals(MAX_MIN_MARGIN_IN_AMPERE)
            || raoParameters.getObjectiveFunction().equals(MAX_MIN_MARGIN_IN_MEGAWATT)) {
            fillers.add(new MaxMinMarginFiller(raoParameters.getObjectiveFunction().getUnit(), raoParameters.getPstPenaltyCost()));
            fillers.add(new MnecFiller(raoParameters.getObjectiveFunction().getUnit(), raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost(), raoParameters.getMnecConstraintAdjustmentCoefficient()));
        }
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            fillers.add(createMaxLoopFlowFiller(raoParameters));
            return new IteratingLinearOptimizerWithLoopFlows(fillers, systematicSensitivityComputation,
                createObjectiveFunction(raoParameters), createIteratingLoopFlowsParameters(raoParameters));
        } else {
            return new IteratingLinearOptimizer(fillers, systematicSensitivityComputation, createObjectiveFunction(raoParameters), createIteratingParameters(raoParameters));
        }
    }

    private static MaxLoopFlowFiller createMaxLoopFlowFiller(RaoParameters raoParameters) {
        return new MaxLoopFlowFiller(raoParameters.isLoopFlowApproximation(),
            raoParameters.getLoopFlowConstraintAdjustmentCoefficient(), raoParameters.getLoopFlowViolationCost());
    }

    private static IteratingLinearOptimizerParameters createIteratingParameters(RaoParameters raoParameters) {
        return new IteratingLinearOptimizerParameters(raoParameters.getMaxIterations(), raoParameters.getFallbackOverCost());
    }

    private static IteratingLinearOptimizerWithLoopFLowsParameters createIteratingLoopFlowsParameters(RaoParameters raoParameters) {
        return new IteratingLinearOptimizerWithLoopFLowsParameters(raoParameters.getMaxIterations(),
            raoParameters.getFallbackOverCost(), raoParameters.isLoopFlowApproximation(), raoParameters.getLoopFlowViolationCost());
    }

    public static ObjectiveFunctionEvaluator createObjectiveFunction(RaoParameters raoParameters) {
        switch (raoParameters.getObjectiveFunction()) {
            case MAX_MIN_MARGIN_IN_AMPERE:
                return new MinMarginObjectiveFunction(Unit.AMPERE, raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost());
            case MAX_MIN_MARGIN_IN_MEGAWATT:
                return new MinMarginObjectiveFunction(Unit.MEGAWATT, raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost());
            default:
                throw new NotImplementedException("Not implemented objective function");
        }
    }
}
