/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_.glsk_provider;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import java.time.Instant;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNull;

/**
 * FlowBased Glsk Values Provider Test for Ucte format
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class UcteGlskValueProviderTest {

    private static final double EPSILON = 0.0001;

    @Test
    public void testProvideOkUcteGlsk() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");

        UcteGlskProvider ucteGlskProvider = new UcteGlskProvider(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml"), network, instant);
        assertEquals(3, ucteGlskProvider.getGlsk(network, "10YFR-RTE------C").getGLSKs().size());
        assertEquals(0.3, ucteGlskProvider.getGlsk(network, "10YFR-RTE------C").getGLSKs().get("FFR1AA1 _generator"), EPSILON);
    }

    @Test
    public void testR4CA() {
        /*Network network = Importers.loadNetwork("20190903_0130_2D2_UC2.UCT", getClass().getResourceAsStream("/20190903_0130_2D2_UC2.UCT"));
        Instant instant = Instant.parse("2019-09-03T06:00:00Z");
        GlskDocumentLinearGlskConverter converter = new UcteGlskDocumentLinearGlskConverter();
        GlskProvider glskProvider = new ChronologyGlskProvider(converter.convert(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml"), network), instant);*/
        //GlskProvider glskProvider = new ChronologyGlskProvider(GlskDocumentLinearGlskConverter.convert(getClass().getResourceAsStream("/testR4CA.xml"), network), instant);

        int i = 1;
        //UcteGlskProvider ucteGlskProvider = new UcteGlskProvider(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml"), network, instant);
        //assertEquals(3, ucteGlskProvider.getGlsk(network, "10YFR-RTE------C").getGLSKs().size());
        //assertEquals(0.3, ucteGlskProvider.getGlsk(network, "10YFR-RTE------C").getGLSKs().get("FFR1AA1 _generator"), EPSILON);
    }

    @Test
    public void testProvideUcteGlskEmptyInstant() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2020-07-29T10:00:00Z");

        UcteGlskProvider ucteGlskProvider = new UcteGlskProvider(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml"), network, instant);

        assertTrue(ucteGlskProvider.getAllGlsk(network).isEmpty());
    }

    @Test
    public void testProvideUcteGlskUnknownCountry() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");

        UcteGlskProvider ucteGlskProvider = new UcteGlskProvider(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml"), network);

        assertNull(ucteGlskProvider.selectInstant(instant).getGlsk(network, "unknowncountry"));
    }

    @Test
    public void testProvideUcteGlskWithWrongFormat() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");
        UcteGlskProvider ucteGlskProvider = new UcteGlskProvider(getClass().getResourceAsStream("/GlskCountry.xml"), network, instant);
        assertTrue(ucteGlskProvider.getAllGlsk(network).isEmpty());
    }

}
