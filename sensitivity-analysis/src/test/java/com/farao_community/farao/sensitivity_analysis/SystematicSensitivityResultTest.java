/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.Unit;
import com.powsybl.glsk.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class SystematicSensitivityResultTest {
    private static final double EPSILON = 1e-2;

    private Network network;
    private FlowCnec nStateCnec;
    private FlowCnec contingencyCnec;
    private RangeAction rangeAction;
    private SensitivityVariableSet linearGlsk;

    private RangeActionSensitivityProvider rangeActionSensitivityProvider;
    private PtdfSensitivityProvider ptdfSensitivityProvider;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange();

        ZonalData<SensitivityVariableSet> glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/glsk_proportional_12nodes.xml"))
            .getZonalGlsks(network, Instant.parse("2016-07-28T22:30:00Z"));

        // Ra Provider
        rangeActionSensitivityProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));

        // Ptdf Provider
        ptdfSensitivityProvider = new PtdfSensitivityProvider(glskProvider, crac.getFlowCnecs(), Collections.singleton(Unit.MEGAWATT));

        nStateCnec = crac.getFlowCnec("cnec1basecase");
        rangeAction = crac.getRangeAction("pst");
        contingencyCnec = crac.getFlowCnec("cnec1stateCurativeContingency1");
        linearGlsk = glskProvider.getData("10YFR-RTE------C");
    }

    @Test
    public void testPostTreatIntensities() {
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            rangeActionSensitivityProvider.getAllFactors(network),
            ptdfSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, false);

        // Before postTreating intensities
        assertEquals(-20, result.getReferenceFlow(contingencyCnec), EPSILON);
        assertEquals(200, result.getReferenceIntensity(contingencyCnec), EPSILON);

        // After postTreating intensities
        result.postTreatIntensities();
        assertEquals(-20, result.getReferenceFlow(contingencyCnec), EPSILON);
        assertEquals(-200, result.getReferenceIntensity(contingencyCnec), EPSILON);
    }

    @Test
    public void testPstResultManipulation() {
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            rangeActionSensitivityProvider.getAllFactors(network),
            rangeActionSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, false).postTreatIntensities();

        // Then
        assertTrue(result.isSuccess());

        //  in basecase
        assertEquals(10, result.getReferenceFlow(nStateCnec), EPSILON);
        assertEquals(25, result.getReferenceIntensity(nStateCnec), EPSILON);
        assertEquals(0.5, result.getSensitivityOnFlow(rangeAction, nStateCnec), EPSILON);
        assertThrows(UnsupportedOperationException.class, () -> result.getSensitivityOnIntensity(rangeAction, nStateCnec));

        //  after contingency
        assertEquals(-20, result.getReferenceFlow(contingencyCnec), EPSILON);
        assertEquals(-200, result.getReferenceIntensity(contingencyCnec), EPSILON);
        assertEquals(-5, result.getSensitivityOnFlow(rangeAction, contingencyCnec), EPSILON);
        assertThrows(UnsupportedOperationException.class, () -> result.getSensitivityOnIntensity(rangeAction, contingencyCnec));
    }

    @Test
    public void testPtdfResultManipulation() {
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            ptdfSensitivityProvider.getAllFactors(network),
            ptdfSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, false).postTreatIntensities();

        // Then
        assertTrue(result.isSuccess());

        //  in basecase
        assertEquals(10, result.getReferenceFlow(nStateCnec), EPSILON);
        assertEquals(0.140, result.getSensitivityOnFlow(linearGlsk, nStateCnec), EPSILON);

        //  after contingency
        assertEquals(-20, result.getReferenceFlow(contingencyCnec), EPSILON);
        assertEquals(6, result.getSensitivityOnFlow(linearGlsk, contingencyCnec), EPSILON);
    }

    @Test
    public void testFailureSensiResult() {
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = Mockito.mock(SensitivityAnalysisResult.class);
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, false).postTreatIntensities();

        // Then
        assertFalse(result.isSuccess());
    }

}
