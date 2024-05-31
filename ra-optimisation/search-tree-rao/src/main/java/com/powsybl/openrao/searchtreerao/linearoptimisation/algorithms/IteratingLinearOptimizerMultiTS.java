/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputerMultiTS;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerMultiTSInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.IteratingLinearOptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.LinearProblemResult;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class IteratingLinearOptimizerMultiTS {

    private IteratingLinearOptimizerMultiTS() {

    }

    public static LinearOptimizationResult optimize(IteratingLinearOptimizerMultiTSInput input, IteratingLinearOptimizerParameters parameters, Instant outageInstant) {

        IteratingLinearOptimizationResultImpl bestResult = createResult(
            input.getPreOptimizationFlowResult(),
            input.getPreOptimizationSensitivityResult(),
            input.getRaActivationFromParentLeaf(),
            0,
            input.getObjectiveFunction());

        IteratingLinearOptimizationResultImpl previousResult = bestResult;

        SensitivityComputerMultiTS sensitivityComputerMultiTS = null;

        LinearProblem linearProblem = LinearProblem.createMultiTS()
            .buildFromInputsAndParameters(input, parameters);

        linearProblem.fill(input.getPreOptimizationFlowResult(), input.getPreOptimizationSensitivityResult());

        for (int iteration = 1; iteration <= parameters.getMaxNumberOfIterations(); iteration++) {
            LinearProblemStatus solveStatus = solveLinearProblem(linearProblem, iteration);
            bestResult.setNbOfIteration(iteration);
            if (solveStatus == LinearProblemStatus.FEASIBLE) {
                TECHNICAL_LOGS.warn("The solver was interrupted. A feasible solution has been produced.");
            } else if (solveStatus != LinearProblemStatus.OPTIMAL) {
                BUSINESS_LOGS.error("Linear optimization failed at iteration {}", iteration);
                if (iteration == 1) {
                    bestResult.setStatus(solveStatus);
                    BUSINESS_LOGS.info("Linear problem failed with the following status : {}, initial situation is kept.", solveStatus);
                    return bestResult;
                }
                bestResult.setStatus(LinearProblemStatus.FEASIBLE);
                return bestResult;
            }

            RangeActionActivationResult linearProblemResult = new LinearProblemResult(linearProblem, input.getPrePerimeterSetpoints(), input.getOptimizationPerimeters());
            RangeActionActivationResult currentRangeActionActivationResult = roundResult(linearProblemResult, bestResult, input, parameters);
            currentRangeActionActivationResult = resolveIfApproximatedPstTaps(bestResult, linearProblem, iteration, currentRangeActionActivationResult, input, parameters);

            if (!hasRemedialActionsChanged(currentRangeActionActivationResult, previousResult, input.getOptimizationPerimeters())) {
                // If the solution has not changed, no need to run a new sensitivity computation and iteration can stop
                TECHNICAL_LOGS.info("Iteration {}: same results as previous iterations, optimal solution found", iteration);
                return bestResult;
            }

            sensitivityComputerMultiTS = runSensitivityAnalysis(sensitivityComputerMultiTS, iteration, currentRangeActionActivationResult, input, parameters);
            if (sensitivityComputerMultiTS.getSensitivityResults().getSensitivityStatus() == ComputationStatus.FAILURE) {
                bestResult.setStatus(LinearProblemStatus.SENSITIVITY_COMPUTATION_FAILED);
                return bestResult;
            }

            IteratingLinearOptimizationResultImpl currentResult = createResult(
                sensitivityComputerMultiTS.getBranchResult(input.getNetwork(0), 0), // TODO: network is only useful for loopflows
                sensitivityComputerMultiTS.getSensitivityResults(),
                currentRangeActionActivationResult,
                iteration,
                input.getObjectiveFunction()
            );
            previousResult = currentResult;

            Pair<IteratingLinearOptimizationResultImpl, Boolean> mipShouldStop = updateBestResultAndCheckStopCondition(parameters.getRaRangeShrinking(), linearProblem, input, iteration, currentResult, bestResult);
            if (Boolean.TRUE.equals(mipShouldStop.getRight())) {
                return bestResult;
            } else {
                bestResult = mipShouldStop.getLeft();
            }
        }
        bestResult.setStatus(LinearProblemStatus.MAX_ITERATION_REACHED);
        return bestResult;
    }

    private static SensitivityComputerMultiTS runSensitivityAnalysis(SensitivityComputerMultiTS sensitivityComputerMultiTS, int iteration, RangeActionActivationResult currentRangeActionActivationResult, IteratingLinearOptimizerMultiTSInput input, IteratingLinearOptimizerParameters parameters) {
        SensitivityComputerMultiTS tmpSensitivityComputerMultiTS = sensitivityComputerMultiTS;
        if (input.getOptimizationPerimeter(0) instanceof GlobalOptimizationPerimeter) {
            AppliedRemedialActions appliedRemedialActionsInSecondaryStates = applyRangeActions(currentRangeActionActivationResult, input);
            tmpSensitivityComputerMultiTS = createSensitivityComputer(appliedRemedialActionsInSecondaryStates, input, parameters);
        } else {
            applyRangeActions(currentRangeActionActivationResult, input);
            if (tmpSensitivityComputerMultiTS == null) { // first iteration, do not need to be updated afterwards
                tmpSensitivityComputerMultiTS = createSensitivityComputer(input.getPreOptimizationAppliedRemedialActions(), input, parameters);
            }
        }
        runSensitivityAnalysis(tmpSensitivityComputerMultiTS, input.getNetworks(), iteration);
        return tmpSensitivityComputerMultiTS;
    }

    private static RangeActionActivationResult resolveIfApproximatedPstTaps(IteratingLinearOptimizationResultImpl bestResult, LinearProblem linearProblem, int iteration, RangeActionActivationResult currentRangeActionActivationResult, IteratingLinearOptimizerMultiTSInput input, IteratingLinearOptimizerParameters parameters) {
        LinearProblemStatus solveStatus;
        RangeActionActivationResult rangeActionActivationResult = currentRangeActionActivationResult;
        if (parameters.getRangeActionParameters().getPstModel().equals(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS)) {

            // if the PST approximation is APPROXIMATED_INTEGERS, we re-solve the optimization problem
            // but first, we update it, with an adjustment of the PSTs angleToTap conversion factors, to
            // be more accurate in the neighboring of the previous solution

            // (idea: if too long, we could relax the first MIP, but no so straightforward to do with or-tools)
            linearProblem.updateBetweenMipIteration(rangeActionActivationResult);

            solveStatus = solveLinearProblem(linearProblem, iteration);
            if (solveStatus == LinearProblemStatus.OPTIMAL || solveStatus == LinearProblemStatus.FEASIBLE) {
                RangeActionActivationResult updatedLinearProblemResult = new LinearProblemResult(linearProblem, input.getPrePerimeterSetpoints(), input.getOptimizationPerimeters());
                rangeActionActivationResult = roundResult(updatedLinearProblemResult, bestResult, input, parameters);
            }
        }
        return rangeActionActivationResult;
    }

    private static LinearProblemStatus solveLinearProblem(LinearProblem linearProblem, int iteration) {
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [start]", iteration);
        LinearProblemStatus status = linearProblem.solve();
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [end]", iteration);
        return status;
    }

    private static boolean hasRemedialActionsChanged(RangeActionActivationResult newRangeActionActivationResult, RangeActionActivationResult oldRangeActionActivationResult, List<OptimizationPerimeter> optimizationPerimeters) {
        return optimizationPerimeters.stream().anyMatch(optimizationPerimeter -> optimizationPerimeter.getRangeActionsPerState().entrySet().stream()
            .anyMatch(e -> e.getValue().stream()
                .anyMatch(ra -> Math.abs(newRangeActionActivationResult.getOptimizedSetpoint(ra, e.getKey()) - oldRangeActionActivationResult.getOptimizedSetpoint(ra, e.getKey())) >= 1e-6)));
    }

    private static AppliedRemedialActions applyRangeActions(RangeActionActivationResult rangeActionActivationResult, IteratingLinearOptimizerMultiTSInput input) {

        List<OptimizationPerimeter> optimizationPerimeters = input.getOptimizationPerimeters();
        AppliedRemedialActions appliedRemedialActions = input.getPreOptimizationAppliedRemedialActions().copyNetworkActionsAndAutomaticRangeActions();
        boolean shouldReturnAppliedRemedialActions = false;

        // apply RangeAction from first optimization state
        for (int i = 0; i < optimizationPerimeters.size(); i++) {
            OptimizationPerimeter optimizationPerimeter = optimizationPerimeters.get(i);
            int finalI = i;
            optimizationPerimeter.getRangeActionsPerState().get(optimizationPerimeter.getMainOptimizationState())
                .forEach(ra -> ra.apply(input.getNetwork(finalI), rangeActionActivationResult.getOptimizedSetpoint(ra, optimizationPerimeter.getMainOptimizationState())));

            // add RangeAction activated in the following states
            if (optimizationPerimeter instanceof GlobalOptimizationPerimeter) {
                shouldReturnAppliedRemedialActions = true;
                optimizationPerimeter.getRangeActionsPerState().entrySet().stream()
                    .filter(e -> !e.getKey().equals(optimizationPerimeter.getMainOptimizationState())) // remove preventive state
                    .forEach(e -> e.getValue().forEach(ra -> appliedRemedialActions.addAppliedRangeAction(e.getKey(), ra, rangeActionActivationResult.getOptimizedSetpoint(ra, e.getKey()))));
            }
        }

        return shouldReturnAppliedRemedialActions ? appliedRemedialActions : null;
    }

    private static SensitivityComputerMultiTS createSensitivityComputer(AppliedRemedialActions appliedRemedialActions, IteratingLinearOptimizerMultiTSInput input, IteratingLinearOptimizerParameters parameters) {
        //TODO : adapt sensitivity computer
        List<Set<FlowCnec>> cnecsList = new ArrayList<>();
        List<Set<FlowCnec>> loopFlowCnecsList = new ArrayList<>();
        List<Set<RangeAction<?>>> rangeActionsList = new ArrayList<>();
        for (OptimizationPerimeter perimeter : input.getOptimizationPerimeters()) {
            cnecsList.add(perimeter.getFlowCnecs());
            rangeActionsList.add(perimeter.getRangeActions());
            loopFlowCnecsList.add(perimeter.getLoopFlowCnecs());
        }

        SensitivityComputerMultiTS.SensitivityComputerBuilder builder = SensitivityComputerMultiTS.create()
            .withCnecs(cnecsList)
            .withRangeActions(rangeActionsList)
            .withAppliedRemedialActions(appliedRemedialActions)
            .withToolProvider(input.getToolProvider())
            .withOutageInstant(input.getOutageInstant());

        if (parameters.isRaoWithLoopFlowLimitation() && parameters.getLoopFlowParameters().getPtdfApproximation().shouldUpdatePtdfWithPstChange()) {
            builder.withCommercialFlowsResults(input.getToolProvider().getLoopFlowComputation(), loopFlowCnecsList);
        } else if (parameters.isRaoWithLoopFlowLimitation()) {
            builder.withCommercialFlowsResults(input.getPreOptimizationFlowResult());
        }
        if (parameters.getObjectiveFunction().relativePositiveMargins()) {
            if (parameters.getMaxMinRelativeMarginParameters().getPtdfApproximation().shouldUpdatePtdfWithPstChange()) {
                builder.withPtdfsResults(input.getToolProvider().getAbsolutePtdfSumsComputation(), cnecsList);
            } else {
                builder.withPtdfsResults(input.getPreOptimizationFlowResult());
            }
        }

        return builder.build();
    }

    private static void runSensitivityAnalysis(SensitivityComputerMultiTS sensitivityComputerMultiTS, List<Network> networks, int iteration) {
        sensitivityComputerMultiTS.compute(networks);
        if (sensitivityComputerMultiTS.getSensitivityResults().getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_WARNS.warn("Systematic sensitivity computation failed at iteration {}", iteration);
        }

    }

    private static IteratingLinearOptimizationResultImpl createResult(FlowResult flowResult,
                                                                      SensitivityResult sensitivityResult,
                                                                      RangeActionActivationResult rangeActionActivation,
                                                                      int nbOfIterations,
                                                                      ObjectiveFunction objectiveFunction) {
        return new IteratingLinearOptimizationResultImpl(LinearProblemStatus.OPTIMAL, nbOfIterations, rangeActionActivation, flowResult,
            objectiveFunction.evaluate(flowResult, rangeActionActivation, sensitivityResult, sensitivityResult.getSensitivityStatus()), sensitivityResult);
    }

    private static RangeActionActivationResult roundResult(RangeActionActivationResult linearProblemResult, IteratingLinearOptimizationResultImpl previousResult, IteratingLinearOptimizerMultiTSInput input, IteratingLinearOptimizerParameters parameters) {
        return BestTapFinderMultiTS.round(
            linearProblemResult,
            input.getNetworks(),
            input.getOptimizationPerimeters(),
            input.getPrePerimeterSetpoints(),
            previousResult,
            parameters.getObjectiveFunctionUnit()
        );
    }

    private static Pair<IteratingLinearOptimizationResultImpl, Boolean> updateBestResultAndCheckStopCondition(boolean raRangeShrinking, LinearProblem linearProblem, IteratingLinearOptimizerMultiTSInput input, int iteration, IteratingLinearOptimizationResultImpl currentResult, IteratingLinearOptimizationResultImpl bestResult) {
        if (currentResult.getCost() < bestResult.getCost()) {
            logBetterResult(iteration, currentResult);
            linearProblem.updateBetweenSensiIteration(currentResult.getBranchResult(), currentResult.getSensitivityResult(), currentResult.getRangeActionActivationResult());
            return Pair.of(currentResult, false);
        }
        logWorseResult(iteration, bestResult, currentResult);
        applyRangeActions(bestResult, input);
        if (raRangeShrinking) {
            linearProblem.updateBetweenSensiIteration(currentResult.getBranchResult(), currentResult.getSensitivityResult(), currentResult.getRangeActionActivationResult());
        }
        return Pair.of(bestResult, !raRangeShrinking);
    }

    private static void logBetterResult(int iteration, ObjectiveFunctionResult currentObjectiveFunctionResult) {
        TECHNICAL_LOGS.info(
            "Iteration {}: better solution found with a cost of {} (functional: {})",
            iteration,
            formatDouble(currentObjectiveFunctionResult.getCost()),
            formatDouble(currentObjectiveFunctionResult.getFunctionalCost()));
    }

    private static void logWorseResult(int iteration, ObjectiveFunctionResult bestResult, ObjectiveFunctionResult currentResult) {
        TECHNICAL_LOGS.info(
            "Iteration {}: linear optimization found a worse result than best iteration, with a cost increasing from {} to {} (functional: from {} to {})",
            iteration,
            formatDouble(bestResult.getCost()),
            formatDouble(currentResult.getCost()),
            formatDouble(bestResult.getFunctionalCost()),
            formatDouble(currentResult.getFunctionalCost()));
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ENGLISH, "%.2f", value);
    }

}
