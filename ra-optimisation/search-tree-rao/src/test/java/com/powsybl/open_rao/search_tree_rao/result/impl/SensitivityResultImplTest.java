/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.result.impl;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.range_action.RangeAction;
import com.powsybl.open_rao.data.rao_result_api.ComputationStatus;
import com.powsybl.open_rao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static com.powsybl.open_rao.data.crac_api.cnec.Side.LEFT;
import static com.powsybl.open_rao.commons.Unit.*;
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
        when(systematicSensitivityResult.getSensitivityOnFlow(rangeAction, cnec, LEFT)).thenReturn(8.);

        assertEquals(8, sensitivityResultImpl.getSensitivityValue(cnec, LEFT, rangeAction, MEGAWATT), DOUBLE_TOLERANCE);

        assertThrows(OpenRaoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, LEFT, rangeAction, KILOVOLT));
        assertThrows(OpenRaoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, LEFT, rangeAction, DEGREE));
        assertThrows(OpenRaoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, LEFT, rangeAction, PERCENT_IMAX));
        assertThrows(OpenRaoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, LEFT, rangeAction, TAP));
    }

    @Test
    void testSensitivitiesOnLinearGLSK() {
        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        SensitivityResultImpl sensitivityResultImpl = new SensitivityResultImpl(
                systematicSensitivityResult
        );

        SensitivityVariableSet linearGlsk = Mockito.mock(SensitivityVariableSet.class);
        FlowCnec cnec = Mockito.mock(FlowCnec.class);
        when(systematicSensitivityResult.getSensitivityOnFlow(linearGlsk, cnec, LEFT)).thenReturn(8.);

        assertEquals(8, sensitivityResultImpl.getSensitivityValue(cnec, LEFT, linearGlsk, MEGAWATT), DOUBLE_TOLERANCE);
        assertThrows(OpenRaoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, LEFT, linearGlsk, AMPERE));
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
