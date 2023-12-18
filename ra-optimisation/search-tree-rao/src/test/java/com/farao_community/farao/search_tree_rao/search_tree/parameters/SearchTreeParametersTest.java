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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class SearchTreeParametersTest {
    SearchTreeParameters.SearchTreeParametersBuilder builder;

    @BeforeEach
    public void setup() {
        builder = SearchTreeParameters.create();
    }

    @Test
    void testWithConstantParametersOverAllRao() {
        RaoParameters raoParameters = new RaoParameters();
        Crac crac = Mockito.mock(Crac.class);
        builder.withConstantParametersOverAllRao(raoParameters, crac);
        SearchTreeParameters searchTreeParameters = builder.build();
        assertNotNull(searchTreeParameters);

        assertEquals(raoParameters.getObjectiveFunctionParameters().getType(), searchTreeParameters.getObjectiveFunction());
        assertEquals(NetworkActionParameters.buildFromRaoParameters(raoParameters.getTopoOptimizationParameters(), crac), searchTreeParameters.getNetworkActionParameters());
        assertEquals(GlobalRemedialActionLimitationParameters.buildFromRaoParameters(raoParameters.getRaUsageLimitsPerContingencyParameters()), searchTreeParameters.getRaLimitationParameters());
        assertEquals(RangeActionsOptimizationParameters.buildFromRaoParameters(raoParameters), searchTreeParameters.getRangeActionParameters());
        assertEquals(raoParameters.getExtension(MnecParametersExtension.class), searchTreeParameters.getMnecParameters());
        assertEquals(raoParameters.getExtension(RelativeMarginsParametersExtension.class), searchTreeParameters.getMaxMinRelativeMarginParameters());
        assertEquals(raoParameters.getExtension(LoopFlowParametersExtension.class), searchTreeParameters.getLoopFlowParameters());
        assertEquals(raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver(), searchTreeParameters.getSolverParameters());
        assertEquals(raoParameters.getRangeActionsOptimizationParameters().getMaxMipIterations(), searchTreeParameters.getMaxNumberOfIterations());
    }

    @Test
    void testIndividualSetters() {
        ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction = Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class);
        TreeParameters treeParameters = Mockito.mock(TreeParameters.class);
        NetworkActionParameters networkActionParameters = Mockito.mock(NetworkActionParameters.class);
        GlobalRemedialActionLimitationParameters raLimitationParameters = Mockito.mock(GlobalRemedialActionLimitationParameters.class);
        RangeActionsOptimizationParameters rangeActionParameters = Mockito.mock(RangeActionsOptimizationParameters.class);
        MnecParametersExtension mnecParameters = Mockito.mock(MnecParametersExtension.class);
        RelativeMarginsParametersExtension maxMinRelativeMarginParameters = Mockito.mock(RelativeMarginsParametersExtension.class);
        LoopFlowParametersExtension loopFlowParameters = Mockito.mock(LoopFlowParametersExtension.class);
        UnoptimizedCnecParameters unoptimizedCnecParameters = Mockito.mock(UnoptimizedCnecParameters.class);
        RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters = Mockito.mock(RangeActionsOptimizationParameters.LinearOptimizationSolver.class);
        int maxNumberOfIterations = 3;

        SearchTreeParameters searchTreeParameters = builder
            .with0bjectiveFunction(objectiveFunction)
            .withTreeParameters(treeParameters)
            .withNetworkActionParameters(networkActionParameters)
            .withGlobalRemedialActionLimitationParameters(raLimitationParameters)
            .withRangeActionParameters(rangeActionParameters)
            .withMnecParameters(mnecParameters)
            .withMaxMinRelativeMarginParameters(maxMinRelativeMarginParameters)
            .withLoopFlowParameters(loopFlowParameters)
            .withUnoptimizedCnecParameters(unoptimizedCnecParameters)
            .withSolverParameters(solverParameters)
            .withMaxNumberOfIterations(maxNumberOfIterations)
            .build();

        assertEquals(objectiveFunction, searchTreeParameters.getObjectiveFunction());
        assertEquals(treeParameters, searchTreeParameters.getTreeParameters());
        assertEquals(networkActionParameters, searchTreeParameters.getNetworkActionParameters());
        assertEquals(raLimitationParameters, searchTreeParameters.getRaLimitationParameters());
        assertEquals(rangeActionParameters, searchTreeParameters.getRangeActionParameters());
        assertEquals(mnecParameters, searchTreeParameters.getMnecParameters());
        assertEquals(maxMinRelativeMarginParameters, searchTreeParameters.getMaxMinRelativeMarginParameters());
        assertEquals(loopFlowParameters, searchTreeParameters.getLoopFlowParameters());
        assertEquals(unoptimizedCnecParameters, searchTreeParameters.getUnoptimizedCnecParameters());
        assertEquals(solverParameters, searchTreeParameters.getSolverParameters());
        assertEquals(maxNumberOfIterations, searchTreeParameters.getMaxNumberOfIterations());
    }
}
