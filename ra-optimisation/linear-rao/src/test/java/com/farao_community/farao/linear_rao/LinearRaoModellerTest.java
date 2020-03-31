/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.linear_rao.mocks.MPSolverMock;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class LinearRaoModellerTest {
    private LinearRaoModeller linearRaoModeller;
    private LinearRaoProblem linearRaoProblemMock;

    @Before
    public void setUp() {
        linearRaoProblemMock = Mockito.mock(LinearRaoProblem.class);

        Crac cracMock = Mockito.mock(Crac.class);
        Network networkMock = Mockito.mock(Network.class);
        SystematicSensitivityAnalysisResult sensitivityResultMock = Mockito.mock(SystematicSensitivityAnalysisResult.class);
        RaoParameters raoParametersMock = Mockito.mock(RaoParameters.class);

        linearRaoModeller = new LinearRaoModeller(cracMock, networkMock, sensitivityResultMock, linearRaoProblemMock, raoParametersMock);
    }

    @Test
    public void testOptimalSolve() {
        Mockito.when(linearRaoProblemMock.solve()).thenReturn(MPSolverMock.ResultStatusMock.OPTIMAL);

        RaoComputationResult raoComputationResult = linearRaoModeller.solve();
        assertNotNull(raoComputationResult);
        assertEquals(RaoComputationResult.Status.SUCCESS, raoComputationResult.getStatus());
    }

    @Test
    public void testUnboundedSolve() {
        Mockito.when(linearRaoProblemMock.solve()).thenReturn(MPSolverMock.ResultStatusMock.UNBOUNDED);

        RaoComputationResult raoComputationResult = linearRaoModeller.solve();
        assertNotNull(raoComputationResult);
        assertEquals(RaoComputationResult.Status.FAILURE, raoComputationResult.getStatus());
    }
}
