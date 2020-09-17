/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_computation.SensitivityComputationException;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class LinearRao implements RaoProvider {

    static {
        NativeLibraryLoader.loadNativeLibrary("jniortools");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LinearRao.class);

    private Unit unit;

    @Override
    public String getName() {
        return "LinearRao";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, ComputationManager computationManager, RaoParameters raoParameters) {
        RaoData raoData = RaoUtil.initRaoData(raoInput, raoParameters);
        this.unit = raoParameters.getObjectiveFunction().getUnit();
        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface
            .builder()
            .build();

        IteratingLinearOptimizer iteratingLinearOptimizer = RaoUtil.createLinearOptimizer(raoParameters, systematicSensitivityInterface);

        return run(raoData, systematicSensitivityInterface, iteratingLinearOptimizer, raoParameters);
    }


    // This method is useful for testing to be able to mock systematicSensitivityComputation
    CompletableFuture<RaoResult> run(RaoData raoData, SystematicSensitivityInterface systematicSensitivityInterface,
                                     IteratingLinearOptimizer iteratingLinearOptimizer, RaoParameters raoParameters) {
        try {
            LOGGER.info("Initial systematic analysis [start]");
            raoData.setSystematicSensitivityResult(systematicSensitivityInterface.run(raoData.getNetwork(), raoData.getCrac(), unit));
            raoData.getRaoDataManager().fillCracResultsWithSensis(
                RaoUtil.createObjectiveFunction(raoParameters).getFunctionalCost(raoData),
                systematicSensitivityInterface.isFallback() ? raoParameters.getFallbackOverCost() : 0);
            LOGGER.info("Initial systematic analysis [end] - with initial min margin of {} MW", -raoData.getCracResult().getCost());
        } catch (SensitivityComputationException e) {
            return CompletableFuture.completedFuture(buildFailedRaoResultAndClearVariants(raoData, e));
        }

        // stop here if no optimisation should be done
        StringBuilder skipReason = new StringBuilder();
        if (skipOptim(raoParameters, raoData.getCrac(), skipReason)) {
            LOGGER.warn(format("Linear optimization is skipped. Cause: %s", skipReason));
            return CompletableFuture.completedFuture(buildSuccessfulRaoResultAndClearVariants(raoData, raoData.getInitialVariantId(), systematicSensitivityInterface));
        }

        String bestVariantId = iteratingLinearOptimizer.optimize(raoData);

        return CompletableFuture.completedFuture(buildSuccessfulRaoResultAndClearVariants(raoData, bestVariantId, systematicSensitivityInterface));
    }

    /**
     * Method returning a boolean indicating whether an optimisation should be done,
     * or whether the LinearRao should only perform a security analysis
     */
    private boolean skipOptim(RaoParameters raoParameters, Crac crac, StringBuilder skipReason) {
        if (raoParameters.getExtension(LinearRaoParameters.class).isSecurityAnalysisWithoutRao()) {
            skipReason.append("security analysis without RAO");
            return true;
        } else if (raoParameters.getMaxIterations() == 0) {
            skipReason.append("max number of iterations is null");
            return true;
        } else if (crac.getRangeActions().isEmpty()) {
            skipReason.append("no range actions available in the CRAC");
            return true;
        } else {
            return false;
        }
    }

    /**
     * Build the RaoResult in case of optimisation success
     */
    private RaoResult buildSuccessfulRaoResultAndClearVariants(RaoData raoData, String postOptimVariantId, SystematicSensitivityInterface systematicSensitivityInterface) {
        // build RaoResult
        RaoResult raoResult = new RaoResult(RaoResult.Status.SUCCESS);
        raoResult.setPreOptimVariantId(raoData.getInitialVariantId());
        raoResult.setPostOptimVariantId(postOptimVariantId);

        // build extension
        LinearRaoResult resultExtension = new LinearRaoResult();
        resultExtension.setSuccessfulSystematicSensitivityAnalysisStatus(systematicSensitivityInterface.isFallback());
        raoResult.addExtension(LinearRaoResult.class, resultExtension);

        // log
        double minMargin = -raoData.getCracResult(postOptimVariantId).getFunctionalCost();
        double objFunctionValue = raoData.getCracResult(postOptimVariantId).getCost();
        LOGGER.info(format("LinearRaoResult: minimum margin = %.2f %s, security status = %s, optimisation criterion = %.2f",
            minMargin, unit, raoData.getCracResult(postOptimVariantId).getNetworkSecurityStatus(),
            objFunctionValue));

        raoData.clearWithKeepingCracResults(Arrays.asList(raoData.getInitialVariantId(), postOptimVariantId));
        return raoResult;
    }

    /**
     * Build the RaoResult in case of optimisation failure
     */
    private RaoResult buildFailedRaoResultAndClearVariants(RaoData raoData, Exception e) {
        // build RaoResult
        RaoResult raoResult = new RaoResult(RaoResult.Status.FAILURE);
        raoResult.setPreOptimVariantId(raoData.getInitialVariantId());
        raoResult.setPostOptimVariantId(raoData.getInitialVariantId());

        // build extension
        LinearRaoResult resultExtension = new LinearRaoResult();
        resultExtension.setSystematicSensitivityAnalysisStatus(LinearRaoResult.SystematicSensitivityAnalysisStatus.FAILURE);
        resultExtension.setErrorMessage(e.getMessage());
        raoResult.addExtension(LinearRaoResult.class, resultExtension);

        raoData.clearWithKeepingCracResults(Collections.singletonList(raoData.getInitialVariantId()));

        return raoResult;
    }
}
