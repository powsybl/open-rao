/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLogger;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.commons.logs.RaoBusinessLogs;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Perimeter;
import com.powsybl.openrao.searchtreerao.castor.algorithm.ContingencyScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static com.powsybl.openrao.commons.Unit.AMPERE;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RaoLoggerTest {

    private ObjectiveFunctionResult objectiveFunctionResult;
    private FlowResult flowResult;
    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private FlowCnec cnec3;
    private FlowCnec cnec4;
    private FlowCnec cnec5;
    private State statePreventive;
    private State stateCo1Auto;
    private State stateCo1Curative;
    private State stateCo2Curative;
    private OptimizationResult basecaseOptimResult;

    @BeforeEach
    public void setUp() {
        objectiveFunctionResult = mock(ObjectiveFunctionResult.class);
        flowResult = mock(FlowResult.class);
        basecaseOptimResult = mock(OptimizationResult.class);
        Instant preventiveInstant = mock(Instant.class);
        when(preventiveInstant.isPreventive()).thenReturn(true);
        when(preventiveInstant.getKind()).thenReturn(InstantKind.PREVENTIVE);
        Instant autoInstant = mock(Instant.class);
        when(autoInstant.getKind()).thenReturn(InstantKind.AUTO);
        Instant curativeInstant = mock(Instant.class);
        when(curativeInstant.getKind()).thenReturn(InstantKind.CURATIVE);
        when(curativeInstant.getOrder()).thenReturn(3);
        statePreventive = mockState("preventive", preventiveInstant);
        stateCo1Auto = mockState("co1 - auto", autoInstant);
        stateCo1Curative = mockState("co1 - curative", curativeInstant);
        stateCo2Curative = mockState("co2 - curative", curativeInstant);
        when(preventiveInstant.comesBefore(curativeInstant)).thenReturn(true);

        cnec1 = mockCnec("ne1", stateCo1Curative, -10, -10, 30, 300, 0.1);
        cnec2 = mockCnec("ne2", statePreventive, 0, 0, -10, -10, 0.2);
        cnec3 = mockCnec("ne3", stateCo2Curative, 10, 100, 10, 200, 0.3);
        cnec4 = mockCnec("ne4", stateCo1Auto, 20, 200, 0, 0, 0.4);
        cnec5 = mockCnec("ne5", stateCo1Curative, 30, 300, 20, 100, 0.5);
    }

    private State mockState(String stateId, Instant instant) {
        State state = mock(State.class);
        when(state.getId()).thenReturn(stateId);
        when(state.getInstant()).thenReturn(instant);
        return state;
    }

    private FlowCnec mockCnec(String neName, State state, double marginMw, double relMarginMw, double marginA, double relMarginA, double ptdf) {
        NetworkElement ne = mock(NetworkElement.class);
        when(ne.getName()).thenReturn(neName);
        FlowCnec cnec = mock(FlowCnec.class);
        when(cnec.getNetworkElement()).thenReturn(ne);
        when(cnec.getState()).thenReturn(state);
        String cnecId = neName + " @ " + state.getId();
        when(cnec.getId()).thenReturn(cnecId);
        mockCnecFlowResult(flowResult, cnec, marginMw, relMarginMw, marginA, relMarginA, ptdf);
        mockCnecFlowResult(basecaseOptimResult, cnec, marginMw, relMarginMw, marginA, relMarginA, ptdf);
        when(cnec.getMonitoredSides()).thenReturn(Set.of(Side.LEFT));
        return cnec;
    }

    private void mockCnecFlowResult(FlowResult flowResult, FlowCnec cnec, double marginMw, double relMarginMw, double marginA, double relMarginA, double ptdf) {
        when(flowResult.getMargin(cnec, Unit.MEGAWATT)).thenReturn(marginMw);
        when(flowResult.getRelativeMargin(cnec, Unit.MEGAWATT)).thenReturn(relMarginMw);
        when(flowResult.getMargin(cnec, Unit.AMPERE)).thenReturn(marginA);
        when(flowResult.getRelativeMargin(cnec, Unit.AMPERE)).thenReturn(relMarginA);
        when(flowResult.getPtdfZonalSum(cnec, Side.LEFT)).thenReturn(ptdf);
    }

    private String absoluteMarginLog(int order, double margin, Unit unit, FlowCnec cnec) {
        return marginLog(order, margin, false, null, unit, cnec);
    }

    private String relativeMarginLog(int order, double margin, Double ptdf, Unit unit, FlowCnec cnec) {
        return marginLog(order, margin, true, ptdf, unit, cnec);
    }

    private String marginLog(int order, double margin, boolean relative, Double ptdf, Unit unit, FlowCnec cnec) {
        String relativeMargin = relative ? " relative" : "";
        String ptdfString = (ptdf != null) ? format(" (PTDF %f)", ptdf) : "";
        String descriptor = format("%s at state %s", cnec.getNetworkElement().getName(), cnec.getState().getId());
        return format(Locale.ENGLISH, "Limiting element #%02d:%s margin = %.2f %s%s, element %s, CNEC ID = '%s'", order, relativeMargin, margin, unit, ptdfString, descriptor, cnec.getId());
    }

    @Test
    void testGetSummaryFromObjFunctionResultOnAllStates() {
        // Absolute MW
        ReportNode reportNode = buildNewRootNode();
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        RaoLogger.logMostLimitingElementsResults(objectiveFunctionResult, flowResult, null, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT, 5, reportNode, TypedValue.INFO_SEVERITY);
        assertEquals(5, reportNode.getChildren().size());
        assertEquals(absoluteMarginLog(1, -10, MEGAWATT, cnec1), reportNode.getChildren().get(0).getMessage());
        assertEquals(absoluteMarginLog(2, 0, MEGAWATT, cnec2), reportNode.getChildren().get(1).getMessage());
        assertEquals(absoluteMarginLog(3, 10, MEGAWATT, cnec3), reportNode.getChildren().get(2).getMessage());
        assertEquals(absoluteMarginLog(4, 20, MEGAWATT, cnec4), reportNode.getChildren().get(3).getMessage());
        assertEquals(absoluteMarginLog(5, 30, MEGAWATT, cnec5), reportNode.getChildren().get(4).getMessage());

        // Relative MW
        reportNode = buildNewRootNode();
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        RaoLogger.logMostLimitingElementsResults(objectiveFunctionResult, flowResult, null, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT, 5, reportNode, TypedValue.DEBUG_SEVERITY);
        assertEquals(5, reportNode.getChildren().size());
        assertEquals(absoluteMarginLog(1, -10, MEGAWATT, cnec1), reportNode.getChildren().get(0).getMessage());
        assertEquals(absoluteMarginLog(2, 0, MEGAWATT, cnec2), reportNode.getChildren().get(1).getMessage());
        assertEquals(relativeMarginLog(3, 100, .3, MEGAWATT, cnec3), reportNode.getChildren().get(2).getMessage());
        assertEquals(relativeMarginLog(4, 200, .4, MEGAWATT, cnec4), reportNode.getChildren().get(3).getMessage());
        assertEquals(relativeMarginLog(5, 300, .5, MEGAWATT, cnec5), reportNode.getChildren().get(4).getMessage());

        // Absolute A
        reportNode = buildNewRootNode();
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec3, cnec5, cnec1));
        RaoLogger.logMostLimitingElementsResults(objectiveFunctionResult, flowResult, null, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE, 5, reportNode, TypedValue.INFO_SEVERITY);
        assertEquals(5, reportNode.getChildren().size());
        assertEquals(absoluteMarginLog(1, -10, AMPERE, cnec2), reportNode.getChildren().get(0).getMessage());
        assertEquals(absoluteMarginLog(2, 0, AMPERE, cnec4), reportNode.getChildren().get(1).getMessage());
        assertEquals(absoluteMarginLog(3, 10, AMPERE, cnec3), reportNode.getChildren().get(2).getMessage());
        assertEquals(absoluteMarginLog(4, 20, AMPERE, cnec5), reportNode.getChildren().get(3).getMessage());
        assertEquals(absoluteMarginLog(5, 30, AMPERE, cnec1), reportNode.getChildren().get(4).getMessage());

        // Relative A
        reportNode = buildNewRootNode();
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec5, cnec3, cnec1));
        RaoLogger.logMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(statePreventive, stateCo1Auto, stateCo1Curative, stateCo2Curative), ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, 5, reportNode, TypedValue.INFO_SEVERITY);
        assertEquals(5, reportNode.getChildren().size());
        assertEquals(absoluteMarginLog(1, -10, AMPERE, cnec2), reportNode.getChildren().get(0).getMessage());
        assertEquals(absoluteMarginLog(2, 0, AMPERE, cnec4), reportNode.getChildren().get(1).getMessage());
        assertEquals(relativeMarginLog(3, 100, .5, AMPERE, cnec5), reportNode.getChildren().get(2).getMessage());
        assertEquals(relativeMarginLog(4, 200, .3, AMPERE, cnec3), reportNode.getChildren().get(3).getMessage());
        assertEquals(relativeMarginLog(5, 300, .1, AMPERE, cnec1), reportNode.getChildren().get(4).getMessage());
    }

    @Test
    void testGetSummaryFromObjFunctionResultOnSomeStates() {
        // Absolute MW
        ReportNode reportNode = buildNewRootNode();
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        RaoLogger.logMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(), ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT, 5, reportNode, TypedValue.INFO_SEVERITY);
        assertEquals(0, reportNode.getChildren().size());
        reportNode = buildNewRootNode();
        RaoLogger.logMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(statePreventive), ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT, 5, reportNode, TypedValue.INFO_SEVERITY);
        assertEquals(1, reportNode.getChildren().size());
        assertEquals(absoluteMarginLog(1, 0, MEGAWATT, cnec2), reportNode.getChildren().get(0).getMessage());

        // Relative MW
        reportNode = buildNewRootNode();
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        RaoLogger.logMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(statePreventive, stateCo1Curative), ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT, 5, reportNode, TypedValue.INFO_SEVERITY);
        assertEquals(3, reportNode.getChildren().size());
        assertEquals(absoluteMarginLog(1, -10, MEGAWATT, cnec1), reportNode.getChildren().get(0).getMessage());
        assertEquals(absoluteMarginLog(2, 0, MEGAWATT, cnec2), reportNode.getChildren().get(1).getMessage());
        assertEquals(relativeMarginLog(3, 300, .5, MEGAWATT, cnec5), reportNode.getChildren().get(2).getMessage());

        // Absolute A
        reportNode = buildNewRootNode();
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec3, cnec5, cnec1));
        RaoLogger.logMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(stateCo2Curative), ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE, 5, reportNode, TypedValue.INFO_SEVERITY);
        assertEquals(1, reportNode.getChildren().size());
        assertEquals(absoluteMarginLog(1, 10, AMPERE, cnec3), reportNode.getChildren().get(0).getMessage());

        // Relative A
        reportNode = buildNewRootNode();
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec5, cnec3, cnec1));
        RaoLogger.logMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(stateCo2Curative, stateCo1Auto), ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, 5, reportNode, TypedValue.INFO_SEVERITY);
        assertEquals(2, reportNode.getChildren().size());
        assertEquals(absoluteMarginLog(1, 0, AMPERE, cnec4), reportNode.getChildren().get(0).getMessage());
        assertEquals(relativeMarginLog(2, 200, .3, AMPERE, cnec3), reportNode.getChildren().get(1).getMessage());
    }

    private static ReportNode buildNewRootNode() {
        return ReportNode.newRootReportNode().withMessageTemplate("Test report node", "This is a parent report node for report tests").build();
    }

    @Test
    void testGetSummaryFromScenarios() {
        Contingency contingency2 = mock(Contingency.class);
        when(stateCo2Curative.getContingency()).thenReturn(Optional.of(contingency2));

        Contingency contingency1 = mock(Contingency.class);
        when(stateCo1Auto.getContingency()).thenReturn(Optional.of(contingency1));
        when(stateCo1Curative.getContingency()).thenReturn(Optional.of(contingency1));

        Perimeter preventivePerimeter = new Perimeter(statePreventive, Set.of(stateCo2Curative));
        Perimeter curativePerimeter = new Perimeter(stateCo1Curative, null);
        Set<ContingencyScenario> contingencyScenarios = Set.of(
            ContingencyScenario.create()
                .withContingency(stateCo1Auto.getContingency().get())
                .withAutomatonState(stateCo1Auto)
                .withCurativePerimeter(curativePerimeter)
                .build());

        OptimizationResult co1AutoOptimResult = mock(OptimizationResult.class);
        OptimizationResult co1CurativeOptimResult = mock(OptimizationResult.class);
        Map<State, OptimizationResult> contingencyOptimizationResults = Map.of(stateCo1Auto, co1AutoOptimResult, stateCo1Curative, co1CurativeOptimResult);

        mockCnecFlowResult(co1AutoOptimResult, cnec1, 25, 40, 15, 11, .1);
        mockCnecFlowResult(co1AutoOptimResult, cnec4, 35, 50, -21, -21, .4);
        mockCnecFlowResult(co1AutoOptimResult, cnec5, -45, -45, 10, 12, .5);

        mockCnecFlowResult(co1CurativeOptimResult, cnec1, 2, 1, -8, -8, .1);
        mockCnecFlowResult(co1CurativeOptimResult, cnec5, -8, -8, 12, 100, .5);

        // Absolute MW
        ReportNode reportNode = buildNewRootNode();
        when(basecaseOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        when(co1AutoOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec1, cnec4));
        when(co1CurativeOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec1));
        RaoLogger.logMostLimitingElementsResults(preventivePerimeter, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT, 5, reportNode);
        assertEquals(5, reportNode.getChildren().size());
        assertEquals(absoluteMarginLog(1, -8, MEGAWATT, cnec5), reportNode.getChildren().get(0).getMessage());
        assertEquals(absoluteMarginLog(2, 0, MEGAWATT, cnec2), reportNode.getChildren().get(1).getMessage());
        assertEquals(absoluteMarginLog(3, 2, MEGAWATT, cnec1), reportNode.getChildren().get(2).getMessage());
        assertEquals(absoluteMarginLog(4, 10, MEGAWATT, cnec3), reportNode.getChildren().get(3).getMessage());
        assertEquals(absoluteMarginLog(5, 35, MEGAWATT, cnec4), reportNode.getChildren().get(4).getMessage());

        // Relative MW
        reportNode = buildNewRootNode();
        when(basecaseOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec4, cnec3, cnec2, cnec1));
        when(co1AutoOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec1, cnec4));
        when(co1CurativeOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec1));
        RaoLogger.logMostLimitingElementsResults(preventivePerimeter, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT, 5, reportNode);
        assertEquals(5, reportNode.getChildren().size());
        assertEquals(absoluteMarginLog(1, -8, MEGAWATT, cnec5), reportNode.getChildren().get(0).getMessage());
        assertEquals(absoluteMarginLog(2, 0, MEGAWATT, cnec2), reportNode.getChildren().get(1).getMessage());
        assertEquals(relativeMarginLog(3, 1, null, MEGAWATT, cnec1), reportNode.getChildren().get(2).getMessage());
        assertEquals(relativeMarginLog(4, 50, null, MEGAWATT, cnec4), reportNode.getChildren().get(3).getMessage());
        assertEquals(relativeMarginLog(5, 100, null, MEGAWATT, cnec3), reportNode.getChildren().get(4).getMessage());

        // Absolute A
        reportNode = buildNewRootNode();
        when(basecaseOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec5, cnec1, cnec3, cnec4));
        when(co1AutoOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec4, cnec5, cnec1));
        when(co1CurativeOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec5));
        RaoLogger.logMostLimitingElementsResults(preventivePerimeter, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE, 5, reportNode);
        assertEquals(5, reportNode.getChildren().size());
        assertEquals(absoluteMarginLog(1, -21, AMPERE, cnec4), reportNode.getChildren().get(0).getMessage());
        assertEquals(absoluteMarginLog(2, -10, AMPERE, cnec2), reportNode.getChildren().get(1).getMessage());
        assertEquals(absoluteMarginLog(3, -8, AMPERE, cnec1), reportNode.getChildren().get(2).getMessage());
        assertEquals(absoluteMarginLog(4, 10, AMPERE, cnec3), reportNode.getChildren().get(3).getMessage());
        assertEquals(absoluteMarginLog(5, 12, AMPERE, cnec5), reportNode.getChildren().get(4).getMessage());

        // Relative A
        reportNode = buildNewRootNode();
        when(basecaseOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec4, cnec3, cnec5, cnec1, cnec2));
        when(co1AutoOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec4, cnec1, cnec5));
        when(co1CurativeOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec5));
        RaoLogger.logMostLimitingElementsResults(preventivePerimeter, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, 5, reportNode);
        assertEquals(5, reportNode.getChildren().size());
        assertEquals(absoluteMarginLog(1, -21, AMPERE, cnec4), reportNode.getChildren().get(0).getMessage());
        assertEquals(absoluteMarginLog(2, -10, AMPERE, cnec2), reportNode.getChildren().get(1).getMessage());
        assertEquals(absoluteMarginLog(3, -8, AMPERE, cnec1), reportNode.getChildren().get(2).getMessage());
        assertEquals(relativeMarginLog(4, 100, null, AMPERE, cnec5), reportNode.getChildren().get(3).getMessage());
        assertEquals(relativeMarginLog(5, 200, null, AMPERE, cnec3), reportNode.getChildren().get(4).getMessage());
    }

    @Test
    void testFormatDouble() {
        assertEquals("10.00", RaoLogger.formatDouble(10.));
        assertEquals("-53.63", RaoLogger.formatDouble(-53.634));
        assertEquals("-53.64", RaoLogger.formatDouble(-53.635));
        assertEquals("-infinity", RaoLogger.formatDouble(-Double.MAX_VALUE));
        assertEquals("+infinity", RaoLogger.formatDouble(Double.MAX_VALUE));
        assertEquals("-infinity", RaoLogger.formatDouble(-179769313486231570000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.00));
        assertEquals("+infinity", RaoLogger.formatDouble(179769313486231570000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.00));
    }

    private ListAppender<ILoggingEvent> registerLogs(Class clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    @Test
    void testLogOptimizationSummary() {
        State preventive = Mockito.mock(State.class);
        Instant preventiveInstant = Mockito.mock(Instant.class);
        when(preventiveInstant.toString()).thenReturn("preventive");
        when(preventive.getInstant()).thenReturn(preventiveInstant);
        State curative = Mockito.mock(State.class);
        Instant curativeInstant = Mockito.mock(Instant.class);
        when(curativeInstant.toString()).thenReturn("curative");
        when(curative.getInstant()).thenReturn(curativeInstant);
        Contingency contingency = Mockito.mock(Contingency.class);
        when(contingency.getName()).thenReturn(Optional.of("contingency"));
        when(curative.getContingency()).thenReturn(Optional.of(contingency));
        OpenRaoLogger logger = OpenRaoLoggerProvider.BUSINESS_LOGS;
        List<ILoggingEvent> logsList = registerLogs(RaoBusinessLogs.class).list;

        // initial objective
        ObjectiveFunctionResult initialObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        when(initialObjectiveFunctionResult.getCost()).thenReturn(-200.);
        when(initialObjectiveFunctionResult.getFunctionalCost()).thenReturn(-210.3);
        when(initialObjectiveFunctionResult.getVirtualCost()).thenReturn(10.3);
        when(initialObjectiveFunctionResult.getVirtualCostNames()).thenReturn(Set.of("sensi-fallback-cost"));
        when(initialObjectiveFunctionResult.getVirtualCost("sensi-fallback-cost")).thenReturn(10.3);
        // final objective
        when(objectiveFunctionResult.getCost()).thenReturn(-100.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-150.);
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(50.);
        when(objectiveFunctionResult.getVirtualCostNames()).thenReturn(Set.of("mnec-violation-cost", "loopflow-violation-cost"));
        when(objectiveFunctionResult.getVirtualCost("mnec-violation-cost")).thenReturn(42.2);
        when(objectiveFunctionResult.getVirtualCost("loopflow-violation-cost")).thenReturn(7.8);

        // Create Remedial actions
        NetworkAction fakeRA = Mockito.mock(NetworkAction.class);
        when(fakeRA.getName()).thenReturn("Open_fake_RA");
        Set<NetworkAction> networkActions = Set.of(fakeRA);
        Map<RangeAction<?>, Double> rangeActions = new HashMap<>();
        RangeAction<?> fakePST1 = Mockito.mock(RangeAction.class);
        RangeAction<?> fakePST2 = Mockito.mock(RangeAction.class);
        when(fakePST1.getName()).thenReturn("PST_1");
        when(fakePST2.getName()).thenReturn("PST_2");
        rangeActions.put(fakePST1, -2.);
        rangeActions.put(fakePST2, 4.);

        RaoLogger.logOptimizationSummary(preventive, networkActions, rangeActions, initialObjectiveFunctionResult, objectiveFunctionResult, ReportNode.NO_OP);
        assertEquals("[INFO] Scenario \"preventive\": initial cost = -200.00 (functional: -210.30, virtual: 10.30 {sensi-fallback-cost=10.3})," +
            " 1 network action(s) and 2 range action(s) activated : Open_fake_RA and PST_2: 4, PST_1: -2," +
            " cost after preventive optimization = -100.00 (functional: -150.00, virtual: 50.00 {mnec-violation-cost=42.2, loopflow-violation-cost=7.8})", logsList.get(logsList.size() - 1).toString());

        // Remove virtual cost for visibility
        when(initialObjectiveFunctionResult.getCost()).thenReturn(-200.);
        when(initialObjectiveFunctionResult.getFunctionalCost()).thenReturn(-200.);
        when(initialObjectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(initialObjectiveFunctionResult.getVirtualCost("sensi-fallback-cost")).thenReturn(0.);
        when(objectiveFunctionResult.getCost()).thenReturn(-100.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-100.);
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getVirtualCostNames()).thenReturn(Set.of("mnec-violation-cost", "loopflow-violation-cost"));
        when(objectiveFunctionResult.getVirtualCost("mnec-violation-cost")).thenReturn(0.);
        when(objectiveFunctionResult.getVirtualCost("loopflow-violation-cost")).thenReturn(0.);

        RaoLogger.logOptimizationSummary(curative, Collections.emptySet(), rangeActions, initialObjectiveFunctionResult, objectiveFunctionResult, ReportNode.NO_OP);
        assertEquals("[INFO] Scenario \"contingency\": initial cost = -200.00 (functional: -200.00, virtual: 0.00)," +
            " 2 range action(s) activated : PST_2: 4, PST_1: -2, cost after curative optimization = -100.00 (functional: -100.00, virtual: 0.00)", logsList.get(logsList.size() - 1).toString());

        RaoLogger.logOptimizationSummary(preventive, Collections.emptySet(), Collections.emptyMap(), initialObjectiveFunctionResult, objectiveFunctionResult, ReportNode.NO_OP);
        assertEquals("[INFO] Scenario \"preventive\": initial cost = -200.00 (functional: -200.00, virtual: 0.00)," +
            " no remedial actions activated, cost after preventive optimization = -100.00 (functional: -100.00, virtual: 0.00)", logsList.get(logsList.size() - 1).toString());

        RaoLogger.logOptimizationSummary(preventive, networkActions, Collections.emptyMap(), initialObjectiveFunctionResult, objectiveFunctionResult, ReportNode.NO_OP);
        assertEquals("[INFO] Scenario \"preventive\": initial cost = -200.00 (functional: -200.00, virtual: 0.00)," +
            " 1 network action(s) activated : Open_fake_RA, cost after preventive optimization = -100.00 (functional: -100.00, virtual: 0.00)", logsList.get(logsList.size() - 1).toString());

        RaoLogger.logOptimizationSummary(preventive, Collections.emptySet(), Collections.emptyMap(), null, objectiveFunctionResult, ReportNode.NO_OP);
        assertEquals("[INFO] Scenario \"preventive\":" +
            " no remedial actions activated, cost after preventive optimization = -100.00 (functional: -100.00, virtual: 0.00)", logsList.get(logsList.size() - 1).toString());

        assertThrows(java.lang.NullPointerException.class, () -> RaoLogger.logOptimizationSummary(preventive, Collections.emptySet(), Collections.emptyMap(), initialObjectiveFunctionResult, null, ReportNode.NO_OP));
    }

    @Test
    void testLogFailedOptimizationSummary() {
        State preventive = Mockito.mock(State.class);
        State curative = Mockito.mock(State.class);
        Contingency contingency = Mockito.mock(Contingency.class);
        when(contingency.getName()).thenReturn(Optional.of("contingency"));
        when(curative.getContingency()).thenReturn(Optional.of(contingency));
        OpenRaoLogger logger = OpenRaoLoggerProvider.BUSINESS_LOGS;
        List<ILoggingEvent> logsList = registerLogs(RaoBusinessLogs.class).list;

        // Create Objective Function
        ObjectiveFunctionResult initialObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);

        // Create Remedial actions
        NetworkAction fakeRA = Mockito.mock(NetworkAction.class);
        when(fakeRA.getName()).thenReturn("Open_fake_RA");
        Set<NetworkAction> networkActions = Set.of(fakeRA);
        Map<RangeAction<?>, Double> rangeActions = new HashMap<>();
        RangeAction<?> fakePST1 = Mockito.mock(RangeAction.class);
        RangeAction<?> fakePST2 = Mockito.mock(RangeAction.class);
        when(fakePST1.getName()).thenReturn("PST_1");
        when(fakePST2.getName()).thenReturn("PST_2");
        rangeActions.put(fakePST1, -2.);
        rangeActions.put(fakePST2, 4.);

        RaoLogger.logFailedOptimizationSummary(logger, preventive, Collections.emptySet(), Collections.emptyMap());
        assertEquals("[INFO] Scenario \"preventive\": no remedial actions activated", logsList.get(logsList.size() - 1).toString());

        RaoLogger.logFailedOptimizationSummary(logger, curative, networkActions, Collections.emptyMap());
        assertEquals("[INFO] Scenario \"contingency\": 1 network action(s) activated : Open_fake_RA", logsList.get(logsList.size() - 1).toString());

        RaoLogger.logFailedOptimizationSummary(logger, curative, Collections.emptySet(), rangeActions);
        assertEquals("[INFO] Scenario \"contingency\": 2 range action(s) activated : PST_2: 4, PST_1: -2", logsList.get(logsList.size() - 1).toString());

        RaoLogger.logFailedOptimizationSummary(logger, curative, networkActions, rangeActions);
        assertEquals("[INFO] Scenario \"contingency\": 1 network action(s) and 2 range action(s) activated : Open_fake_RA and PST_2: 4, PST_1: -2", logsList.get(logsList.size() - 1).toString());
    }
}
