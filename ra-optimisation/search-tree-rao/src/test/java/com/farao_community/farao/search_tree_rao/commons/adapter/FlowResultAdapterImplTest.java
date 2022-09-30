/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.adapter;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.search_tree_rao.commons.AbsolutePtdfSumsComputation;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.impl.FlowResultFromMapImpl;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static com.farao_community.farao.data.crac_api.cnec.Side.RIGHT;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowResultAdapterImplTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private SystematicSensitivityResult systematicSensitivityResult;
    private BranchResultAdapterImpl.BranchResultAdpaterBuilder branchResultAdpaterBuilder;

    @Before
    public void setUp() {
        cnec1 = Mockito.mock(FlowCnec.class);
        when(cnec1.getMonitoredSides()).thenReturn(Collections.singleton(LEFT));
        cnec2 = Mockito.mock(FlowCnec.class);
        when(cnec1.getMonitoredSides()).thenReturn(Collections.singleton(RIGHT));
        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        branchResultAdpaterBuilder = BranchResultAdapterImpl.create();
    }

    @Test
    public void testBasicReturns() {
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder
                .build();

        when(systematicSensitivityResult.getReferenceFlow(cnec1, LEFT)).thenReturn(200.);
        when(systematicSensitivityResult.getReferenceIntensity(cnec1, LEFT)).thenReturn(58.);
        when(systematicSensitivityResult.getReferenceFlow(cnec2, RIGHT)).thenReturn(500.);
        when(systematicSensitivityResult.getReferenceIntensity(cnec2, RIGHT)).thenReturn(235.);
        FlowResult flowResult = branchResultAdapter.getResult(systematicSensitivityResult);

        assertEquals(200., flowResult.getFlow(cnec1, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(58., flowResult.getFlow(cnec1, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(500., flowResult.getFlow(cnec2, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(235., flowResult.getFlow(cnec2, RIGHT, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(flowResult.getPtdfZonalSum(cnec1, LEFT)));
    }

    @Test
    public void testWithFixedPtdfs() {
        FlowResult fixedPtdfFlowResult = new FlowResultFromMapImpl(systematicSensitivityResult, new HashMap<>(), Map.of(cnec1, Map.of(LEFT, 20.)));
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder
            .withPtdfsResults(fixedPtdfFlowResult)
            .build();

        FlowResult flowResult = branchResultAdapter.getResult(systematicSensitivityResult);

        assertEquals(20., flowResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWithFixedPtdfsAndCommercialFlows() {
        FlowResult ptdfFlowResult = new FlowResultFromMapImpl(systematicSensitivityResult, new HashMap<>(), Map.of(cnec1, Map.of(LEFT, 20.)));
        FlowResult commercialFlowFlowResult = new FlowResultFromMapImpl(systematicSensitivityResult, Map.of(cnec2, Map.of(RIGHT, 300.)), new HashMap<>());
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder
                .withPtdfsResults(ptdfFlowResult)
                .withCommercialFlowsResults(commercialFlowFlowResult)
                .build();

        FlowResult flowResult = branchResultAdapter.getResult(systematicSensitivityResult);

        assertEquals(20., flowResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);
        assertEquals(300., flowResult.getCommercialFlow(cnec2, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWithFixedPtdfsAndUpdatedCommercialFlows() {
        LoopFlowComputation loopFlowComputation = Mockito.mock(LoopFlowComputation.class);
        FlowResult ptdfFlowResult = new FlowResultFromMapImpl(systematicSensitivityResult, new HashMap<>(), Map.of(cnec1, Map.of(LEFT, 20.)));
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder.withPtdfsResults(ptdfFlowResult)
                .withCommercialFlowsResults(loopFlowComputation, Set.of(cnec2))
                .build();

        LoopFlowResult loopFlowResult = Mockito.mock(LoopFlowResult.class);
        when(loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(systematicSensitivityResult, Set.of(cnec2)))
                .thenReturn(loopFlowResult);
        when(loopFlowResult.getCommercialFlow(cnec2, RIGHT)).thenReturn(300.);
        FlowResult flowResult = branchResultAdapter.getResult(systematicSensitivityResult);

        assertEquals(20., flowResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);
        assertEquals(300., flowResult.getCommercialFlow(cnec2, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWithAbsolutePtdfSumsComputation() {
        AbsolutePtdfSumsComputation absolutePtdfSumsComputation = Mockito.mock(AbsolutePtdfSumsComputation.class);
        Map<FlowCnec, Map<Side, Double>> ptdfZonalSums = Map.of(cnec1, Map.of(LEFT, 1.63), cnec2, Map.of(RIGHT, 0.57));
        when(absolutePtdfSumsComputation.computeAbsolutePtdfSums(any(), any())).thenReturn(ptdfZonalSums);
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder
                .withPtdfsResults(absolutePtdfSumsComputation, Set.of(cnec1, cnec2))
                .build();
        FlowResult flowResult = branchResultAdapter.getResult(systematicSensitivityResult);
        assertEquals(1.63, flowResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);
        assertEquals(0.57, flowResult.getPtdfZonalSum(cnec2, RIGHT), DOUBLE_TOLERANCE);
        assertEquals(ptdfZonalSums, flowResult.getPtdfZonalSums());
        assertThrows(FaraoException.class, () -> flowResult.getPtdfZonalSum(Mockito.mock(FlowCnec.class), LEFT));
    }
}
