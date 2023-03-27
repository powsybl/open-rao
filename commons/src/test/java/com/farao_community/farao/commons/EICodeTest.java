/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons;

import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot@rte-france.com>}
 */
public class EICodeTest {

    @Test
    public void constructorWithStringOk() {
        EICode eiCodeNotACountry = new EICode("RANDOM_EIC_CODE_");
        assertEquals("RANDOM_EIC_CODE_", eiCodeNotACountry.getAreaCode());
        assertFalse(eiCodeNotACountry.isCountryCode());

        EICode eiCodeCountry = new EICode("10YFR-RTE------C");
        assertEquals("10YFR-RTE------C", eiCodeCountry.getAreaCode());
        assertTrue(eiCodeCountry.isCountryCode());
    }

    @Test
    public void constructorWithStringNOk() {
        assertThrows(IllegalArgumentException.class, () -> new EICode("RANDOM_EIC_CODE_WITH_TOO_MUCH_CHARACTERS"));
    }

    @Test
    public void constructorWithCountryOk() {
        EICode eiCodeFr = new EICode(Country.FR);
        assertEquals("10YFR-RTE------C", eiCodeFr.getAreaCode());
        assertTrue(eiCodeFr.isCountryCode());

        EICode eiCodePt = new EICode(Country.PT);
        assertEquals("10YPT-REN------W", eiCodePt.getAreaCode());
        assertTrue(eiCodePt.isCountryCode());
    }

    @Test
    public void constructorWithCountryNOk() {
        assertThrows(IllegalArgumentException.class, () -> new EICode(Country.NP));
    }

    @Test
    public void toStringTest() {
        EICode eiCodeNotACountry = new EICode("RANDOM_EIC_CODE_");
        assertEquals("RANDOM_EIC_CODE_", eiCodeNotACountry.toString());

        EICode eiCodeCountry = new EICode("10YFR-RTE------C");
        assertEquals("FR", eiCodeCountry.toString());
    }

}
