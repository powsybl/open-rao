/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.openrao.commons.Unit.*;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class SensitivityResultImplTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    void testSensitivitiesOnRangeAction() {
        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        SensitivityResultImpl sensitivityResultImpl = new SensitivityResultImpl(
                systematicSensitivityResult
        );

        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        FlowCnec cnec = Mockito.mock(FlowCnec.class);
        when(systematicSensitivityResult.getSensitivityOnFlow(rangeAction, cnec, ONE)).thenReturn(8.);

        assertEquals(8, sensitivityResultImpl.getSensitivityValue(cnec, ONE, rangeAction, MEGAWATT), DOUBLE_TOLERANCE);

        assertThrows(OpenRaoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, ONE, rangeAction, KILOVOLT));
        assertThrows(OpenRaoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, ONE, rangeAction, DEGREE));
        assertThrows(OpenRaoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, ONE, rangeAction, PERCENT_IMAX));
        assertThrows(OpenRaoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, ONE, rangeAction, TAP));
    }

    @Test
    void testSensitivitiesOnLinearGLSK() {
        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        SensitivityResultImpl sensitivityResultImpl = new SensitivityResultImpl(
                systematicSensitivityResult
        );

        SensitivityVariableSet linearGlsk = Mockito.mock(SensitivityVariableSet.class);
        FlowCnec cnec = Mockito.mock(FlowCnec.class);
        when(systematicSensitivityResult.getSensitivityOnFlow(linearGlsk, cnec, ONE)).thenReturn(8.);

        assertEquals(8, sensitivityResultImpl.getSensitivityValue(cnec, ONE, linearGlsk, MEGAWATT), DOUBLE_TOLERANCE);
        assertThrows(OpenRaoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, ONE, linearGlsk, AMPERE));
    }

    @Test
    void testStatus() {
        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        SensitivityResultImpl sensitivityResultImpl = new SensitivityResultImpl(
                systematicSensitivityResult
        );

        when(systematicSensitivityResult.getStatus()).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS);
        assertEquals(ComputationStatus.DEFAULT, sensitivityResultImpl.getSensitivityStatus());
        when(systematicSensitivityResult.getStatus()).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, sensitivityResultImpl.getSensitivityStatus());
    }
}
