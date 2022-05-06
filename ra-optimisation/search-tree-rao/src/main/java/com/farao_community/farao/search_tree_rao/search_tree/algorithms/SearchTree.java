/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.search_tree.algorithms;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.logs.FaraoLogger;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.search_tree_rao.commons.RaoLogger;
import com.farao_community.farao.search_tree_rao.commons.SensitivityComputer;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.CurativeOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.GlobalOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.result.api.OptimizationResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.commons.NetworkActionCombination;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.search_tree.inputs.SearchTreeInput;
import com.farao_community.farao.search_tree_rao.search_tree.parameters.SearchTreeParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.TreeParameters;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.farao_community.farao.util.AbstractNetworkPool;
import com.google.common.hash.Hashing;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.*;

/**
 * The "tree" is one of the core object of the search-tree algorithm.
 * It aims at finding a good combination of Network Actions.
 * <p>
 * The tree is composed of leaves which evaluate the impact of Network Actions,
 * one by one. The tree is orchestrating the leaves : it looks for a smart
 * routing among the leaves in order to converge as quickly as possible to a local
 * minimum of the objective function.
 * <p>
 * The leaves of a same depth can be evaluated simultaneously.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SearchTree {
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_TREE = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_TREE = 5;

    /**
     * attribute defined in constructor of the search tree class
     */

    private final SearchTreeInput input;
    private final SearchTreeParameters parameters;
    private final FaraoLogger topLevelLogger;

    /**
     * attribute defined and used within the class
     */

    private boolean purelyVirtual;
    private SearchTreeBloomer bloomer;

    private Leaf rootLeaf;
    private Leaf optimalLeaf;
    private Leaf previousDepthOptimalLeaf;

    private double preOptimFunctionalCost;
    private double preOptimVirtualCost;
    
    private Optional<NetworkActionCombination> combinationFulfillingStopCriterion = Optional.empty();

    /**
     * Detects forced network actions and applies them on root leaf, re-evaluating the leaf if needed.
     */
    private void applyForcedNetworkActionsOnRootLeaf() {
        // Fetch available network actions, then apply those that should be forced
        Set<NetworkAction> forcedNetworkActions = availableNetworkActions.stream()
            .filter(ra -> isRemedialActionAvailable(ra, optimizedState, rootLeaf))
            .filter(ra -> ra.getUsageRules().stream().anyMatch(usageRule -> usageRule.getUsageMethod(optimizedState).equals(UsageMethod.FORCED)))
            .collect(Collectors.toSet());
        forcedNetworkActions.forEach(ra -> {
            TECHNICAL_LOGS.debug("Network action {} is available and forced. It will be applied on the root leaf.", ra.getId());
            ra.apply(network);
        });
        if (!forcedNetworkActions.isEmpty()) {
            TECHNICAL_LOGS.info("{} network actions were forced on the network. The root leaf will be re-evaluated.", forcedNetworkActions.size());
            rootLeaf = new Leaf(network, forcedNetworkActions, null, rootLeaf);
            optimalLeaf = rootLeaf;
            previousDepthOptimalLeaf = rootLeaf;
            availableNetworkActions.removeAll(forcedNetworkActions);
        }
    }

    Leaf makeLeaf(Network network, PrePerimeterResult prePerimeterOutput) {
        return new Leaf(network, prePerimeterOutput);
    }

    public SearchTree(SearchTreeInput input,
                      SearchTreeParameters parameters,
                      boolean verbose) {
        // inputs
        this.input = input;
        this.parameters = parameters;
        this.topLevelLogger = verbose ? BUSINESS_LOGS : TECHNICAL_LOGS;

        // build from inputs
        this.purelyVirtual = input.getOptimizationPerimeter().getOptimizedFlowCnecs().isEmpty();

        if (input.getOptimizationPerimeter() instanceof CurativeOptimizationPerimeter) {
            this.bloomer = new SearchTreeBloomer(
                input.getNetwork(),
                input.getPrePerimeterResult(),
                parameters.getRaLimitationParameters().getMaxCurativeRa(),
                parameters.getRaLimitationParameters().getMaxCurativeTso(),
                parameters.getRaLimitationParameters().getMaxCurativeTopoPerTso(),
                parameters.getRaLimitationParameters().getMaxCurativeRaPerTso(),
                parameters.getNetworkActionParameters().skipNetworkActionFarFromMostLimitingElements(),
                parameters.getNetworkActionParameters().getMaxNumberOfBoundariesForSkippingNetworkActions(),
                parameters.getNetworkActionParameters().getNetworkActionCombinations(),
                input.getOptimizationPerimeter().getMainOptimizationState());
        } else {
            this.bloomer = new SearchTreeBloomer(
                input.getNetwork(),
                input.getPrePerimeterResult(),
                Integer.MAX_VALUE, //no limitation of RA in preventive
                Integer.MAX_VALUE, //no limitation of RA in preventive
                new HashMap<>(),   //no limitation of RA in preventive
                new HashMap<>(),   //no limitation of RA in preventive
                parameters.getNetworkActionParameters().skipNetworkActionFarFromMostLimitingElements(),
                parameters.getNetworkActionParameters().getMaxNumberOfBoundariesForSkippingNetworkActions(),
                parameters.getNetworkActionParameters().getNetworkActionCombinations(),
                input.getOptimizationPerimeter().getMainOptimizationState());
        }
    }

    public CompletableFuture<OptimizationResult> run() {

        initLeaves(input);

        applyForcedNetworkActionsOnRootLeaf();

        TECHNICAL_LOGS.info("Evaluating root leaf");
        rootLeaf.evaluate(input.getObjectiveFunction(), getSensitivityComputerForEvaluation(true));
        this.preOptimFunctionalCost = rootLeaf.getFunctionalCost();
        this.preOptimVirtualCost = rootLeaf.getVirtualCost();

        // todo in castor.java ?
        // this.availableRangeActions = searchTreeInput.getRangeActions().stream().filter(ra -> isRemedialActionAvailable(ra, optimizedStateForNetworkActions, rootLeaf)).collect(Collectors.toSet());

        if (rootLeaf.getStatus().equals(Leaf.Status.ERROR)) {
            topLevelLogger.info("Could not evaluate leaf: {}", rootLeaf);
            logOptimizationSummary(rootLeaf);
            return CompletableFuture.completedFuture(rootLeaf);
        } else if (stopCriterionReached(rootLeaf)) {
            topLevelLogger.info("Stop criterion reached on {}", rootLeaf);
            RaoLogger.logMostLimitingElementsResults(topLevelLogger, rootLeaf, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_TREE);
            logOptimizationSummary(rootLeaf);
            return CompletableFuture.completedFuture(rootLeaf);
        }

        TECHNICAL_LOGS.info("{}", rootLeaf);
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, rootLeaf, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_TREE);

        TECHNICAL_LOGS.info("Linear optimization on root leaf");
        optimizeLeaf(rootLeaf);

        topLevelLogger.info("{}", rootLeaf);
        RaoLogger.logRangeActions(TECHNICAL_LOGS, optimalLeaf, input.getOptimizationPerimeter());
        RaoLogger.logMostLimitingElementsResults(topLevelLogger, optimalLeaf, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_TREE);

        if (stopCriterionReached(rootLeaf)) {
            logOptimizationSummary(optimalLeaf);
            return CompletableFuture.completedFuture(rootLeaf);
        }

        iterateOnTree();

        TECHNICAL_LOGS.info("Search-tree RAO completed with status {}", optimalLeaf.getSensitivityStatus());
        TECHNICAL_LOGS.info("Best leaf: {}", optimalLeaf);
        RaoLogger.logRangeActions(TECHNICAL_LOGS, optimalLeaf, input.getOptimizationPerimeter(), "Best leaf: ");
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, optimalLeaf, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_TREE);

        logOptimizationSummary(optimalLeaf);
        return CompletableFuture.completedFuture(optimalLeaf);
    }

    void initLeaves(SearchTreeInput input) {
        rootLeaf = makeLeaf(input.getOptimizationPerimeter(), input.getNetwork(), input.getPrePerimeterResult(), input.getPreOptimizationAppliedNetworkActions());
        optimalLeaf = rootLeaf;
        previousDepthOptimalLeaf = rootLeaf;
    }

    Leaf makeLeaf(OptimizationPerimeter optimizationPerimeter, Network network, PrePerimeterResult prePerimeterOutput, AppliedRemedialActions appliedRemedialActionsInSecondaryStates) {
        return new Leaf(optimizationPerimeter, network, prePerimeterOutput, appliedRemedialActionsInSecondaryStates);
    }

    private void logOptimizationSummary(Leaf leaf) {
        RaoLogger.logOptimizationSummary(BUSINESS_LOGS, input.getOptimizationPerimeter().getMainOptimizationState(), leaf.getActivatedNetworkActions().size(), getNumberOfActivatedRangeActions(leaf), preOptimFunctionalCost, preOptimVirtualCost, leaf);
    }

    private long getNumberOfActivatedRangeActions(Leaf leaf) {
        return leaf.getNumberOfActivatedRangeActions();
    }

    private void iterateOnTree() {
        int depth = 0;
        boolean hasImproved = true;
        if (input.getOptimizationPerimeter().getNetworkActions().isEmpty()) {
            topLevelLogger.info("No network action available");
            return;
        }

        int leavesInParallel = Math.min(input.getOptimizationPerimeter().getNetworkActions().size(), parameters.getTreeParameters().getLeavesInParallel());
        TECHNICAL_LOGS.debug("Evaluating {} leaves in parallel", leavesInParallel);
        try (AbstractNetworkPool networkPool = makeFaraoNetworkPool(input.getNetwork(), leavesInParallel)) {
            while (depth < parameters.getTreeParameters().getMaximumSearchDepth() && hasImproved && !stopCriterionReached(optimalLeaf)) {
                TECHNICAL_LOGS.info("Search depth {} [start]", depth + 1);
                previousDepthOptimalLeaf = optimalLeaf;
                updateOptimalLeafWithNextDepthBestLeaf(networkPool);
                hasImproved = previousDepthOptimalLeaf != optimalLeaf; // It means this depth evaluation has improved the global cost
                if (hasImproved) {
                    TECHNICAL_LOGS.info("Search depth {} [end]", depth + 1);
                    topLevelLogger.info("Search depth {} best leaf: {}", depth + 1, optimalLeaf);
                    RaoLogger.logRangeActions(TECHNICAL_LOGS, optimalLeaf, input.getOptimizationPerimeter(), String.format("Search depth %s best leaf: ", depth + 1));
                    RaoLogger.logMostLimitingElementsResults(topLevelLogger, optimalLeaf, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_TREE);
                } else {
                    topLevelLogger.info("No better result found in search depth {}, exiting search tree", depth + 1);
                }
                depth += 1;
                if (depth >= parameters.getTreeParameters().getMaximumSearchDepth()) {
                    topLevelLogger.info("maximum search depth has been reached, exiting search tree");
                }
            }
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            TECHNICAL_LOGS.warn("A computation thread was interrupted");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Evaluate all the leaves. We use FaraoNetworkPool to parallelize the computation
     */
    private void updateOptimalLeafWithNextDepthBestLeaf(AbstractNetworkPool networkPool) throws InterruptedException {

        final List<NetworkActionCombination> naCombinations = bloomer.bloom(optimalLeaf, input.getOptimizationPerimeter().getNetworkActions());
        naCombinations.sort(this::arbitraryNetworkActionCombinationComparison);
        if (naCombinations.isEmpty()) {
            TECHNICAL_LOGS.info("No more network action available");
            return;
        } else {
            TECHNICAL_LOGS.info("Leaves to evaluate: {}", naCombinations.size());
        }
        AtomicInteger remainingLeaves = new AtomicInteger(naCombinations.size());
        CountDownLatch latch = new CountDownLatch(naCombinations.size());
        naCombinations.forEach(naCombination ->
            networkPool.submit(() -> {
                try {
                    Network networkClone = networkPool.getAvailableNetwork(); //This is where the threads actually wait for available networks
                    if (combinationFulfillingStopCriterion.isEmpty() || arbitraryNetworkActionCombinationComparison(naCombination, combinationFulfillingStopCriterion.get()) < 0) {
                        // Apply range actions that has been changed by the previous leaf on the network to start next depth leaves
                        // from previous optimal leaf starting point
                        // TODO: we can wonder if it's better to do this here or at creation of each leaves or at each evaluation/optimization
                        previousDepthOptimalLeaf.getRangeActions()
                            .forEach(ra -> ra.apply(networkClone, previousDepthOptimalLeaf.getOptimizedSetpoint(ra, input.getOptimizationPerimeter().getMainOptimizationState())));

                        // todo
                        // set alreadyAppliedRa

                        optimizeNextLeafAndUpdate(naCombination, networkClone, networkPool);
                        networkPool.releaseUsedNetwork(networkClone);
                    } else {
                        topLevelLogger.info("Skipping {} optimization because earlier combination fulfills stop criterion.", naCombination.getConcatenatedId());
                        networkPool.releaseUsedNetwork(networkClone);
                    }
                } catch (InterruptedException e) {
                    BUSINESS_WARNS.warn("Cannot apply remedial action combination {}: {}", naCombination.getConcatenatedId(), e.getMessage());
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    BUSINESS_WARNS.warn("Cannot apply remedial action combination {}: {}", naCombination.getConcatenatedId(), e.getMessage());
                } finally {
                    TECHNICAL_LOGS.info("Remaining leaves to evaluate: {}", remainingLeaves.decrementAndGet());
                    latch.countDown();
                }
            })
        );
        // TODO : change the 24 hours to something more useful when a target end time is known by the RAO
        boolean success = latch.await(24, TimeUnit.HOURS);
        if (!success) {
            throw new FaraoException("At least one network action combination could not be evaluated within the given time (24 hours). This should not happen.");
        }
    }

    private int arbitraryNetworkActionCombinationComparison(NetworkActionCombination ra1, NetworkActionCombination ra2) {
        return Hashing.crc32().hashString(ra1.getConcatenatedId(), StandardCharsets.UTF_8).hashCode() - Hashing.crc32().hashString(ra2.getConcatenatedId(), StandardCharsets.UTF_8).hashCode();
    }

    private String printNetworkActions(Set<NetworkAction> networkActions) {
        return networkActions.stream().map(NetworkAction::getId).collect(Collectors.joining(" + "));
    }

    AbstractNetworkPool makeFaraoNetworkPool(Network network, int leavesInParallel) {
        return AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), leavesInParallel);
    }

    void optimizeNextLeafAndUpdate(NetworkActionCombination naCombination, Network network, AbstractNetworkPool networkPool) throws InterruptedException {
        Leaf leaf;
        try {
            // We get initial range action results from the previous optimal leaf
            leaf = createChildLeaf(network, naCombination);
        } catch (FaraoException e) {
            Set<NetworkAction> networkActions = new HashSet<>(previousDepthOptimalLeaf.getActivatedNetworkActions());
            networkActions.addAll(naCombination.getNetworkActionSet());
            topLevelLogger.info("Could not evaluate network action combination \"{}\": {}", printNetworkActions(networkActions), e.getMessage());
            return;
        } catch (NotImplementedException e) {
            networkPool.releaseUsedNetwork(network);
            throw e;
        }
        // We evaluate the leaf with taking the results of the previous optimal leaf if we do not want to update some results
        leaf.evaluate(input.getObjectiveFunction(), getSensitivityComputerForEvaluation(false));
        TECHNICAL_LOGS.debug("Evaluated {}", leaf);
        if (!leaf.getStatus().equals(Leaf.Status.ERROR)) {
            if (!stopCriterionReached(leaf)) {
                if (combinationFulfillingStopCriterion.isPresent() && arbitraryNetworkActionCombinationComparison(naCombination, combinationFulfillingStopCriterion.get()) > 0) {
                    topLevelLogger.info("Skipping {} optimization because earlier combination fulfills stop criterion.", naCombination.getConcatenatedId());
                } else {
                    optimizeLeaf(leaf);
                    topLevelLogger.info("Optimized {}", leaf);
                }
            } else {
                topLevelLogger.info("Evaluated {}", leaf);
            }
            updateOptimalLeaf(leaf, naCombination);
        } else {
            topLevelLogger.info("Could not evaluate leaf: {}", leaf);
        }
    }

    Leaf createChildLeaf(Network network, NetworkActionCombination naCombination) {
        return new Leaf(
            input.getOptimizationPerimeter(),
            network,
            previousDepthOptimalLeaf.getActivatedNetworkActions(),
            naCombination,
            previousDepthOptimalLeaf.getRangeActionActivationResult(),
            input.getPrePerimeterResult(),
            getAppliedRemedialActions(previousDepthOptimalLeaf));
    }

    private void optimizeLeaf(Leaf leaf) {
        if (!input.getOptimizationPerimeter().getRangeActions().isEmpty()) {
            leaf.optimize(input, parameters);
            if (!leaf.getStatus().equals(Leaf.Status.OPTIMIZED)) {
                topLevelLogger.info("Failed to optimize leaf: {}", leaf);
            }
        } else {
            TECHNICAL_LOGS.info("No range actions to optimize");
        }
        leaf.finalizeOptimization();
    }

    private SensitivityComputer getSensitivityComputerForEvaluation(boolean isRootLeaf) {

        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder =  SensitivityComputer.create()
            .withToolProvider(input.getToolProvider())
            .withCnecs(input.getOptimizationPerimeter().getFlowCnecs())
            .withRangeActions(input.getOptimizationPerimeter().getRangeActions());

        if (isRootLeaf) {
            sensitivityComputerBuilder.withAppliedRemedialActions(input.getPreOptimizationAppliedNetworkActions());
        } else {
            sensitivityComputerBuilder.withAppliedRemedialActions(getAppliedRemedialActions(previousDepthOptimalLeaf));
        }

        if (parameters.getObjectiveFunction().relativePositiveMargins()) {
            sensitivityComputerBuilder.withPtdfsResults(input.getInitialFlowResult());
        }

        if (parameters.getLoopFlowParameters() != null  && parameters.getLoopFlowParameters().getLoopFlowApproximationLevel().shouldUpdatePtdfWithTopologicalChange()) {
            sensitivityComputerBuilder.withCommercialFlowsResults(input.getToolProvider().getLoopFlowComputation(), input.getOptimizationPerimeter().getLoopFlowCnecs());
        } else if (parameters.getLoopFlowParameters() != null) {
            sensitivityComputerBuilder.withCommercialFlowsResults(input.getInitialFlowResult());
        }

        return sensitivityComputerBuilder.build();
    }

    private synchronized void updateOptimalLeaf(Leaf leaf, NetworkActionCombination networkActionCombination) {
        if (improvedEnough(leaf)) {
            // nominal case: stop criterion hasn't been reached yet
            if (combinationFulfillingStopCriterion.isEmpty() && leaf.getCost() < optimalLeaf.getCost()) {
                optimalLeaf = leaf;
                if (stopCriterionReached(leaf)) {
                    TECHNICAL_LOGS.info("Stop criterion reached, other threads may skip optimization.");
                    combinationFulfillingStopCriterion = Optional.of(networkActionCombination);
                }
            }
            // special case: stop criterion has been reached
            if (combinationFulfillingStopCriterion.isPresent()
                && stopCriterionReached(leaf)
                && arbitraryNetworkActionCombinationComparison(networkActionCombination, combinationFulfillingStopCriterion.get()) < 0) {
                optimalLeaf = leaf;
                combinationFulfillingStopCriterion = Optional.of(networkActionCombination);
            }
        }
    }

    /**
     * This method evaluates stop criterion on the leaf.
     *
     * @param leaf: Leaf to evaluate.
     * @return True if the stop criterion has been reached on this leaf.
     */
    private boolean stopCriterionReached(Leaf leaf) {
        if (purelyVirtual && leaf.getVirtualCost() < 1e-6) {
            TECHNICAL_LOGS.debug("Perimeter is purely virtual and virtual cost is zero. Exiting search tree.");
            return true;
        }
        if (parameters.getTreeParameters().getStopCriterion().equals(TreeParameters.StopCriterion.MIN_OBJECTIVE)) {
            return false;
        } else if (parameters.getTreeParameters().getStopCriterion().equals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE)) {
            return leaf.getCost() < parameters.getTreeParameters().getTargetObjectiveValue();
        } else {
            throw new FaraoException("Unexpected stop criterion: " + parameters.getTreeParameters().getStopCriterion());
        }
    }

    /**
     * This method checks if the leaf's cost respects the minimum impact thresholds
     * (absolute and relative) compared to the previous depth's optimal leaf.
     *
     * @param leaf: Leaf that has to be compared with the optimal leaf.
     * @return True if the leaf cost diminution is enough compared to optimal leaf.
     */
    private boolean improvedEnough(Leaf leaf) {
        double relativeImpact = Math.max(parameters.getNetworkActionParameters().getRelativeNetworkActionMinimumImpactThreshold(), 0);
        double absoluteImpact = Math.max(parameters.getNetworkActionParameters().getAbsoluteNetworkActionMinimumImpactThreshold(), 0);

        double previousDepthBestCost = previousDepthOptimalLeaf.getCost();
        double newCost = leaf.getCost();

        return previousDepthBestCost - absoluteImpact > newCost // enough absolute impact
            && (1 - Math.signum(previousDepthBestCost) * relativeImpact) * previousDepthBestCost > newCost; // enough relative impact
    }

    private AppliedRemedialActions getAppliedRemedialActions(RangeActionActivationResult previousDepthRangeActionActivations) {
        AppliedRemedialActions alreadyAppliedRa = input.getPreOptimizationAppliedNetworkActions().copy();
        if (input.getOptimizationPerimeter() instanceof GlobalOptimizationPerimeter) {
            input.getOptimizationPerimeter().getRangeActionsPerState().entrySet().stream()
                .filter(e -> !e.getKey().equals(input.getOptimizationPerimeter().getMainOptimizationState())) // remove preventive state
                .forEach(e -> e.getValue().forEach(ra -> alreadyAppliedRa.addAppliedRangeAction(e.getKey(), ra, previousDepthRangeActionActivations.getOptimizedSetpoint(ra, e.getKey()))));
        }
        return alreadyAppliedRa;
    }
}
