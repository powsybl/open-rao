/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class UnitTest {

    @Test
    void checkPhysicalParameterTestOk() {
        Unit.AMPERE.checkPhysicalParameter(PhysicalParameter.FLOW);
        Unit.DEGREE.checkPhysicalParameter(PhysicalParameter.ANGLE);
        Unit.KILOVOLT.checkPhysicalParameter(PhysicalParameter.VOLTAGE);
        Unit.MEGAWATT.checkPhysicalParameter(PhysicalParameter.FLOW);
    }

    @Test
    void checkPhysicalParameterTestNok() {

        try {
            Unit.AMPERE.checkPhysicalParameter(PhysicalParameter.ANGLE);
            fail();
        } catch (FaraoException e) {
            // should throw
        }

        try {
            Unit.DEGREE.checkPhysicalParameter(PhysicalParameter.VOLTAGE);
            fail();
        } catch (FaraoException e) {
            // should throw
        }

        try {
            Unit.KILOVOLT.checkPhysicalParameter(PhysicalParameter.FLOW);
            fail();
        } catch (FaraoException e) {
            // should throw
        }

        try {
            Unit.MEGAWATT.checkPhysicalParameter(PhysicalParameter.VOLTAGE);
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }
}
