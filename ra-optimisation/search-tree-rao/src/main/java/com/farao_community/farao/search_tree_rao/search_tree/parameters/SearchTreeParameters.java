/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.search_tree.parameters;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.*;

/**
 * @author Baptiste Seguinot {@literal <joris.mancini at rte-france.com>}
 */
public class SearchTreeParameters {

    private final RaoParameters.ObjectiveFunction objectiveFunction;

    // required for the search tree algorithm
    private final TreeParameters treeParameters;
    private final NetworkActionParameters networkActionParameters;
    private final GlobalRemedialActionLimitationParameters raLimitationParameters;

    // required for sub-module iterating linear optimizer
    private final RangeActionParameters rangeActionParameters;
    private final MnecParameters mnecParameters;
    private final MaxMinRelativeMarginParameters maxMinRelativeMarginParameters;
    private final LoopFlowParameters loopFlowParameters;
    private final UnoptimizedCnecParameters unoptimizedCnecParameters;
    private final SolverParameters solverParameters;
    private final int maxNumberOfIterations;

    public SearchTreeParameters(RaoParameters.ObjectiveFunction objectiveFunction,
                                TreeParameters treeParameters,
                                NetworkActionParameters networkActionParameters,
                                GlobalRemedialActionLimitationParameters raLimitationParameters,
                                RangeActionParameters rangeActionParameters,
                                MnecParameters mnecParameters,
                                MaxMinRelativeMarginParameters maxMinRelativeMarginParameters,
                                LoopFlowParameters loopFlowParameters,
                                UnoptimizedCnecParameters unoptimizedCnecParameters,
                                SolverParameters solverParameters,
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

    public RaoParameters.ObjectiveFunction getObjectiveFunction() {
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

    public RangeActionParameters getRangeActionParameters() {
        return rangeActionParameters;
    }

    public MnecParameters getMnecParameters() {
        return mnecParameters;
    }

    public MaxMinRelativeMarginParameters getMaxMinRelativeMarginParameters() {
        return maxMinRelativeMarginParameters;
    }

    public LoopFlowParameters getLoopFlowParameters() {
        return loopFlowParameters;
    }

    public UnoptimizedCnecParameters getUnoptimizedCnecParameters() {
        return unoptimizedCnecParameters;
    }

    public SolverParameters getSolverParameters() {
        return solverParameters;
    }

    public int getMaxNumberOfIterations() {
        return maxNumberOfIterations;
    }

    public static SearchTreeParametersBuilder create() {
        return new SearchTreeParametersBuilder();
    }

    public static class SearchTreeParametersBuilder {
        private RaoParameters.ObjectiveFunction objectiveFunction;
        private TreeParameters treeParameters;
        private NetworkActionParameters networkActionParameters;
        private GlobalRemedialActionLimitationParameters raLimitationParameters;
        private RangeActionParameters rangeActionParameters;
        private MnecParameters mnecParameters;
        private MaxMinRelativeMarginParameters maxMinRelativeMarginParameters;
        private LoopFlowParameters loopFlowParameters;
        private UnoptimizedCnecParameters unoptimizedCnecParameters;
        private SolverParameters solverParameters;
        private int maxNumberOfIterations;

        public SearchTreeParametersBuilder withConstantParametersOverAllRao(RaoParameters raoParameters, Crac crac) {
            this.objectiveFunction = raoParameters.getObjectiveFunction();
            this.networkActionParameters = NetworkActionParameters.buildFromRaoParameters(raoParameters, crac);
            this.raLimitationParameters = GlobalRemedialActionLimitationParameters.buildFromRaoParameters(raoParameters);
            this.rangeActionParameters = RangeActionParameters.buildFromRaoParameters(raoParameters);
            this.mnecParameters = MnecParameters.buildFromRaoParameters(raoParameters);
            this.maxMinRelativeMarginParameters = MaxMinRelativeMarginParameters.buildFromRaoParameters(raoParameters);
            this.loopFlowParameters = LoopFlowParameters.buildFromRaoParameters(raoParameters);
            this.solverParameters = SolverParameters.buildFromRaoParameters(raoParameters);
            this.maxNumberOfIterations = raoParameters.getMaxIterations();
            return this;
        }

        public SearchTreeParametersBuilder with0bjectiveFunction(RaoParameters.ObjectiveFunction objectiveFunction) {
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

        public SearchTreeParametersBuilder withRangeActionParameters(RangeActionParameters rangeActionParameters) {
            this.rangeActionParameters = rangeActionParameters;
            return this;
        }

        public SearchTreeParametersBuilder withMnecParameters(MnecParameters mnecParameters) {
            this.mnecParameters = mnecParameters;
            return this;
        }

        public SearchTreeParametersBuilder withMaxMinRelativeMarginParameters(MaxMinRelativeMarginParameters maxMinRelativeMarginParameters) {
            this.maxMinRelativeMarginParameters = maxMinRelativeMarginParameters;
            return this;
        }

        public SearchTreeParametersBuilder withLoopFlowParameters(LoopFlowParameters loopFlowParameters) {
            this.loopFlowParameters = loopFlowParameters;
            return this;
        }

        public SearchTreeParametersBuilder withUnoptimizedCnecParameters(UnoptimizedCnecParameters unoptimizedCnecParameters) {
            this.unoptimizedCnecParameters = unoptimizedCnecParameters;
            return this;
        }

        public SearchTreeParametersBuilder withSolverParameters(SolverParameters solverParameters) {
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
