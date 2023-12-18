/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.search_tree_rao.search_tree.parameters;

import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.rao_api.parameters.ObjectiveFunctionParameters;
import com.powsybl.open_rao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.powsybl.open_rao.rao_api.parameters.RaoParameters;
import com.powsybl.open_rao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.open_rao.rao_api.parameters.extensions.MnecParametersExtension;
import com.powsybl.open_rao.rao_api.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.open_rao.search_tree_rao.commons.parameters.*;

/**
 * @author Baptiste Seguinot {@literal <joris.mancini at rte-france.com>}
 */
public class SearchTreeParameters {

    private final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction;

    // required for the search tree algorithm
    private final TreeParameters treeParameters;
    private final NetworkActionParameters networkActionParameters;
    private final GlobalRemedialActionLimitationParameters raLimitationParameters;

    // required for sub-module iterating linear optimizer
    private final RangeActionsOptimizationParameters rangeActionParameters;
    private final MnecParametersExtension mnecParameters;
    private final RelativeMarginsParametersExtension maxMinRelativeMarginParameters;
    private final LoopFlowParametersExtension loopFlowParameters;
    private final UnoptimizedCnecParameters unoptimizedCnecParameters;
    private final RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters;
    private final int maxNumberOfIterations;

    public SearchTreeParameters(ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                TreeParameters treeParameters,
                                NetworkActionParameters networkActionParameters,
                                GlobalRemedialActionLimitationParameters raLimitationParameters,
                                RangeActionsOptimizationParameters rangeActionParameters,
                                MnecParametersExtension mnecParameters,
                                RelativeMarginsParametersExtension maxMinRelativeMarginParameters,
                                LoopFlowParametersExtension loopFlowParameters,
                                UnoptimizedCnecParameters unoptimizedCnecParameters,
                                RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters,
                                int maxNumberOfIterations) {
        this.objectiveFunction = objectiveFunction;
        this.treeParameters = treeParameters;
        this.networkActionParameters = networkActionParameters;
        this.raLimitationParameters = raLimitationParameters;
        this.rangeActionParameters = rangeActionParameters;
        this.mnecParameters = mnecParameters;
        this.maxMinRelativeMarginParameters = maxMinRelativeMarginParameters;
        this.loopFlowParameters = loopFlowParameters;
        this.unoptimizedCnecParameters = unoptimizedCnecParameters;
        this.solverParameters = solverParameters;
        this.maxNumberOfIterations = maxNumberOfIterations;
    }

    public ObjectiveFunctionParameters.ObjectiveFunctionType getObjectiveFunction() {
        return objectiveFunction;
    }

    public TreeParameters getTreeParameters() {
        return treeParameters;
    }

    public NetworkActionParameters getNetworkActionParameters() {
        return networkActionParameters;
    }

    public GlobalRemedialActionLimitationParameters getRaLimitationParameters() {
        return raLimitationParameters;
    }

    public RangeActionsOptimizationParameters getRangeActionParameters() {
        return rangeActionParameters;
    }

    public MnecParametersExtension getMnecParameters() {
        return mnecParameters;
    }

    public RelativeMarginsParametersExtension getMaxMinRelativeMarginParameters() {
        return maxMinRelativeMarginParameters;
    }

    public LoopFlowParametersExtension getLoopFlowParameters() {
        return loopFlowParameters;
    }

    public UnoptimizedCnecParameters getUnoptimizedCnecParameters() {
        return unoptimizedCnecParameters;
    }

    public RangeActionsOptimizationParameters.LinearOptimizationSolver getSolverParameters() {
        return solverParameters;
    }

    public int getMaxNumberOfIterations() {
        return maxNumberOfIterations;
    }

    public static SearchTreeParametersBuilder create() {
        return new SearchTreeParametersBuilder();
    }

    public static class SearchTreeParametersBuilder {
        private ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction;
        private TreeParameters treeParameters;
        private NetworkActionParameters networkActionParameters;
        private GlobalRemedialActionLimitationParameters raLimitationParameters;
        private RangeActionsOptimizationParameters rangeActionParameters;
        private MnecParametersExtension mnecParameters;
        private RelativeMarginsParametersExtension maxMinRelativeMarginParameters;
        private LoopFlowParametersExtension loopFlowParameters;
        private UnoptimizedCnecParameters unoptimizedCnecParameters;
        private RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters;
        private int maxNumberOfIterations;

        public SearchTreeParametersBuilder withConstantParametersOverAllRao(RaoParameters raoParameters, Crac crac) {
            this.objectiveFunction = raoParameters.getObjectiveFunctionParameters().getType();
            this.networkActionParameters = NetworkActionParameters.buildFromRaoParameters(raoParameters.getTopoOptimizationParameters(), crac);
            this.raLimitationParameters = GlobalRemedialActionLimitationParameters.buildFromRaoParameters(raoParameters.getRaUsageLimitsPerContingencyParameters());
            this.rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(raoParameters);
            this.mnecParameters = raoParameters.getExtension(MnecParametersExtension.class);
            this.maxMinRelativeMarginParameters = raoParameters.getExtension(RelativeMarginsParametersExtension.class);
            this.loopFlowParameters = raoParameters.getExtension(LoopFlowParametersExtension.class);
            this.solverParameters = raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver();
            this.maxNumberOfIterations = raoParameters.getRangeActionsOptimizationParameters().getMaxMipIterations();
            return this;
        }

        public SearchTreeParametersBuilder with0bjectiveFunction(ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
            return this;
        }

        public SearchTreeParametersBuilder withTreeParameters(TreeParameters treeParameters) {
            this.treeParameters = treeParameters;
            return this;
        }

        public SearchTreeParametersBuilder withNetworkActionParameters(NetworkActionParameters networkActionParameters) {
            this.networkActionParameters = networkActionParameters;
            return this;
        }

        public SearchTreeParametersBuilder withGlobalRemedialActionLimitationParameters(GlobalRemedialActionLimitationParameters raLimitationParameters) {
            this.raLimitationParameters = raLimitationParameters;
            return this;
        }

        public SearchTreeParametersBuilder withRangeActionParameters(RangeActionsOptimizationParameters rangeActionParameters) {
            this.rangeActionParameters = rangeActionParameters;
            return this;
        }

        public SearchTreeParametersBuilder withMnecParameters(MnecParametersExtension mnecParameters) {
            this.mnecParameters = mnecParameters;
            return this;
        }

        public SearchTreeParametersBuilder withMaxMinRelativeMarginParameters(RelativeMarginsParametersExtension maxMinRelativeMarginParameters) {
            this.maxMinRelativeMarginParameters = maxMinRelativeMarginParameters;
            return this;
        }

        public SearchTreeParametersBuilder withLoopFlowParameters(LoopFlowParametersExtension loopFlowParameters) {
            this.loopFlowParameters = loopFlowParameters;
            return this;
        }

        public SearchTreeParametersBuilder withUnoptimizedCnecParameters(UnoptimizedCnecParameters unoptimizedCnecParameters) {
            this.unoptimizedCnecParameters = unoptimizedCnecParameters;
            return this;
        }

        public SearchTreeParametersBuilder withSolverParameters(RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters) {
            this.solverParameters = solverParameters;
            return this;
        }

        public SearchTreeParametersBuilder withMaxNumberOfIterations(int maxNumberOfIterations) {
            this.maxNumberOfIterations = maxNumberOfIterations;
            return this;
        }

        public SearchTreeParameters build() {
            return new SearchTreeParameters(objectiveFunction,
                treeParameters,
                networkActionParameters,
                raLimitationParameters,
                rangeActionParameters,
                mnecParameters,
                maxMinRelativeMarginParameters,
                loopFlowParameters,
                unoptimizedCnecParameters,
                solverParameters,
                maxNumberOfIterations);
        }
    }
}
