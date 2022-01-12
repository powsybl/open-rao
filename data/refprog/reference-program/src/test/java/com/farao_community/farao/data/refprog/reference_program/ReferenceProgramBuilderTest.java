/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.refprog.reference_program;

import com.farao_community.farao.commons.EICode;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ReferenceProgramBuilderTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;

    private EICode eiCodeFrance;
    private EICode eiCodeBelgium;
    private EICode eiCodeNetherlands;
    private EICode eiCodeGermany;

    @Before
    public void setUp() {
        eiCodeFrance = new EICode(Country.FR);
        eiCodeBelgium = new EICode(Country.BE);
        eiCodeNetherlands = new EICode(Country.NL);
        eiCodeGermany = new EICode(Country.DE);
    }

    @Test
    public void testLines() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        ReferenceProgram referenceProgram = ReferenceProgramBuilder.buildReferenceProgram(network, "default-impl-name", new LoadFlowParameters());
        assertEquals(1000.0, referenceProgram.getGlobalNetPosition(eiCodeFrance), DOUBLE_TOLERANCE);
        assertEquals(1500.0, referenceProgram.getGlobalNetPosition(eiCodeBelgium), DOUBLE_TOLERANCE);
        assertEquals(0.0, referenceProgram.getGlobalNetPosition(eiCodeNetherlands), DOUBLE_TOLERANCE);
        assertEquals(-2500.0, referenceProgram.getGlobalNetPosition(eiCodeGermany), DOUBLE_TOLERANCE);
    }

    @Test
    public void testDanglingLines() {
        Network network = Importers.loadNetwork("TestCaseDangling.xiidm", getClass().getResourceAsStream("/TestCaseDangling.xiidm"));
        ReferenceProgram referenceProgram = ReferenceProgramBuilder.buildReferenceProgram(network, "default-impl-name", new LoadFlowParameters());
        assertEquals(0.0, referenceProgram.getGlobalNetPosition(eiCodeFrance), DOUBLE_TOLERANCE);
        assertEquals(300.0, referenceProgram.getGlobalNetPosition(eiCodeBelgium), DOUBLE_TOLERANCE);
    }

    @Test
    public void testHvdcLines() {
        Network network = Importers.loadNetwork("TestCaseHvdc.xiidm", getClass().getResourceAsStream("/TestCaseHvdc.xiidm"));
        ReferenceProgram referenceProgram = ReferenceProgramBuilder.buildReferenceProgram(network, "default-impl-name", new LoadFlowParameters());
        assertEquals(272.0, referenceProgram.getGlobalNetPosition(eiCodeFrance), DOUBLE_TOLERANCE);
        assertEquals(-272.0, referenceProgram.getGlobalNetPosition(eiCodeGermany), DOUBLE_TOLERANCE);
    }
}
