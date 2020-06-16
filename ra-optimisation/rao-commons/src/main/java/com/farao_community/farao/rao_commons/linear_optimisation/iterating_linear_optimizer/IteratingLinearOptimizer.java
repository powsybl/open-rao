/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.data.crac_result_extensions.CracResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimisationException;
import com.farao_community.farao.rao_commons.linear_optimisation.SimpleLinearOptimizer;
import com.farao_community.farao.rao_commons.systematic_sensitivity.SystematicSensitivityComputation;
import com.farao_community.farao.util.SensitivityComputationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizer {
    protected static final Logger LOGGER = LoggerFactory.getLogger(IteratingLinearOptimizer.class);
    protected static final String UNEXPECTED_BEHAVIOR = "Iteration %d - Linear Optimization found a worse result than previous iteration, with a minimum margin from %.2f to %.2f %s (optimisation criterion : from %.2f to %.2f)";
    protected static final String IMPROVEMENT = "Iteration %d - Better solution found with a minimum margin of %.2f %s (optimisation criterion : %.2f)";
    protected static final String SAME_RESULTS = "Iteration %d - same results as previous iterations, optimal solution found";
    protected static final String SYSTEMATIC_SENSITIVITY_COMPUTATION_START = "Iteration %d - systematic analysis [start]";
    protected static final String SYSTEMATIC_SENSITIVITY_COMPUTATION_END = "Iteration %d - systematic analysis [end]";
    protected static final String SYSTEMATIC_SENSITIVITY_COMPUTATION_ERROR = "Sensitivity computation failed at iteration %d on %s mode: %s";
    protected static final String LINEAR_OPTIMIZATION_START = "Iteration %d - linear optimization [start]";
    protected static final String LINEAR_OPTIMIZATION_END = "Iteration %d - linear optimization [end]";
    protected static final String LINEAR_OPTIMIZATION_INFEASIBLE = "Iteration %d - linear optimization is infeasible";
    protected static final String LINEAR_OPTIMIZATION_ERROR = "Linear optimization failed at iteration %d: %s";

    protected RaoData raoData;
    protected String bestVariantId;
    protected final SystematicSensitivityComputation systematicSensitivityComputation;
    protected final SimpleLinearOptimizer simpleLinearOptimizer;
    protected final IteratingLinearOptimizerParameters parameters;

    public IteratingLinearOptimizer(SystematicSensitivityComputation systematicSensitivityComputation,
                                    RaoParameters raoParameters) {
        this.systematicSensitivityComputation = systematicSensitivityComputation;
        this.simpleLinearOptimizer = new SimpleLinearOptimizer(raoParameters);
        if (!Objects.isNull(raoParameters.getExtension(IteratingLinearOptimizerParameters.class))) {
            parameters = raoParameters.getExtension(IteratingLinearOptimizerParameters.class);
        } else {
            parameters = new IteratingLinearOptimizerParameters();
        }
    }

    // Used to mock SimpleLinearOptimizer and SystematicSensitivityComputation in tests
    IteratingLinearOptimizer(SystematicSensitivityComputation systematicSensitivityComputation,
                             SimpleLinearOptimizer simpleLinearOptimizer,
                             IteratingLinearOptimizerParameters parameters) {
        this.systematicSensitivityComputation = systematicSensitivityComputation;
        this.simpleLinearOptimizer = simpleLinearOptimizer;
        this.parameters = parameters;
    }

    public String optimize(RaoData raoData) {
        this.raoData = raoData;
        bestVariantId = raoData.getInitialVariantId();
        String optimizedVariantId;
        for (int iteration = 1; iteration <= parameters.getMaxIterations(); iteration++) {
            optimizedVariantId = raoData.cloneWorkingVariant();
            raoData.setWorkingVariant(optimizedVariantId);
            if (!optimize(iteration)
                || !hasRemedialActionsChanged(optimizedVariantId, iteration)
                || !evaluateNewCost(optimizedVariantId, iteration)
                || !hasCostImproved(optimizedVariantId, iteration)) {
                return getBestVariantWithSafeDelete(optimizedVariantId);
            }
            updateBestVariantId(optimizedVariantId);
        }
        return bestVariantId;
    }

    private boolean optimize(int iteration) {
        // If optimization fails iteration can stop
        try {
            LOGGER.info(format(LINEAR_OPTIMIZATION_START, iteration));
            simpleLinearOptimizer.optimize(raoData);
            if (!simpleLinearOptimizer.getSolverResultStatusString().equals("OPTIMAL")) {
                LOGGER.info(format(LINEAR_OPTIMIZATION_INFEASIBLE, iteration)); //handle INFEASIBLE solver status
                return false;
            }
            LOGGER.info(format(LINEAR_OPTIMIZATION_END, iteration));
            return true;
        } catch (LinearOptimisationException e) {
            LOGGER.error(format(LINEAR_OPTIMIZATION_ERROR, iteration, e.getMessage()));
            return false;
        }
    }

    private boolean hasRemedialActionsChanged(String optimizedVariantId, int iteration) {
        // If the solution has not changed, no need to run a new sensitivity computation and iteration can stop
        if (raoData.getRaoDataManager().sameRemedialActions(bestVariantId, optimizedVariantId)) {
            LOGGER.info(format(SAME_RESULTS, iteration));
            return false;
        } else {
            return true;
        }
    }

    protected boolean evaluateNewCost(String optimizedVariantId, int iteration) {
        // If evaluating the new cost fails iteration can stop
        raoData.setWorkingVariant(optimizedVariantId);
        try {
            LOGGER.info(format(SYSTEMATIC_SENSITIVITY_COMPUTATION_START, iteration));
            systematicSensitivityComputation.run(raoData);
            raoData.getRaoDataManager().fillCracResultsWithSensis(simpleLinearOptimizer.getParameters().getObjectiveFunction(), systematicSensitivityComputation);
            LOGGER.info(format(SYSTEMATIC_SENSITIVITY_COMPUTATION_END, iteration));
            return true;
        } catch (SensitivityComputationException e) {
            LOGGER.error(format(SYSTEMATIC_SENSITIVITY_COMPUTATION_ERROR, iteration, systematicSensitivityComputation.isFallback() ? "Fallback" : "Default", e.getMessage()));
            return false;
        }
    }

    private boolean hasCostImproved(String optimizedVariantId, int iteration) {
        // If the cost has not improved iteration can stop
        CracResult bestVariantResult = raoData.getCracResult(bestVariantId);
        CracResult optimizedVariantResult = raoData.getCracResult(optimizedVariantId);
        String unit = simpleLinearOptimizer.getParameters().getObjectiveFunction().getUnit();
        if (optimizedVariantResult.getCost() < bestVariantResult.getCost()) {
            LOGGER.warn(format(IMPROVEMENT, iteration, -optimizedVariantResult.getFunctionalCost(), unit,
                optimizedVariantResult.getCost()));
            return true;
        } else { // unexpected behaviour, stop the search
            LOGGER.warn(format(UNEXPECTED_BEHAVIOR, iteration, -bestVariantResult.getFunctionalCost(),
                -optimizedVariantResult.getFunctionalCost(),  unit, bestVariantResult.getCost(), optimizedVariantResult.getCost()));
            return false;
        }
    }

    private String getBestVariantWithSafeDelete(String variantToDelete) {
        raoData.setWorkingVariant(bestVariantId);
        if (!variantToDelete.equals(raoData.getInitialVariantId())) {
            raoData.deleteVariant(variantToDelete, false);
        }
        return bestVariantId;
    }

    private void updateBestVariantId(String optimizedVariantId) {
        raoData.setWorkingVariant(optimizedVariantId);
        if (!bestVariantId.equals(raoData.getInitialVariantId())) {
            raoData.deleteVariant(bestVariantId, false);
        }
        bestVariantId = optimizedVariantId;
    }
}
