/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class XnodeGlskHandlerTest {

    @Test
    void test() {
        String networkFileName = "network_with_virtual_hubs.xiidm";
        String glskFileName = "glsk_with_virtual_hubs.xml";

        Network network = Network.read(networkFileName, getClass().getResourceAsStream("/" + networkFileName));
        ZonalData<SensitivityVariableSet> glskZonalData = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/" + glskFileName)).getZonalGlsks(network, java.time.Instant.parse("2016-07-28T22:30:00Z"));

        Crac crac = CracFactory.findDefault().create("cracId");

        Contingency classicContingency = crac.newContingency()
            .withId("internalBranch")
            .withNetworkElement("DDE1AA1  DDE3AA1  1")
            .add();

        Contingency dlContingency = crac.newContingency()
            .withId("danglingLine")
            .withNetworkElement("FFR1AA1  XLI_OB1B 1")
            .add();

        FlowCnec cnec1 = crac.newFlowCnec()
            .withId("cnec1")
            .withNetworkElement("anyNetworkElement")
            .withInstant(Instant.PREVENTIVE)
            .newThreshold()
                .withSide(Side.RIGHT)
                .withUnit(Unit.MEGAWATT)
                .withMax(1000.0)
                .add()
            .add();

        FlowCnec cnec2 = crac.newFlowCnec()
            .withId("cnec2")
            .withNetworkElement("anyNetworkElement")
            .withInstant(Instant.OUTAGE)
            .withContingency("internalBranch")
                .newThreshold()
                .withSide(Side.RIGHT)
                .withUnit(Unit.MEGAWATT)
                .withMax(1000.0)
                .add()
            .add();

        FlowCnec cnec3 = crac.newFlowCnec()
            .withId("cnec3")
            .withNetworkElement("anyNetworkElement")
            .withInstant(Instant.OUTAGE)
            .withContingency("danglingLine")
            .newThreshold()
                .withSide(Side.RIGHT)
                .withUnit(Unit.MEGAWATT)
                .withMax(1000.0)
                .add()
            .add();

        XnodeGlskHandler xnodeGlskHandler = new XnodeGlskHandler(glskZonalData, Set.of(classicContingency, dlContingency), network);

        assertTrue(xnodeGlskHandler.isLinearGlskValidForCnec(cnec1, glskZonalData.getData("10YNL----------L")));
        assertTrue(xnodeGlskHandler.isLinearGlskValidForCnec(cnec2, glskZonalData.getData("10YNL----------L")));
        assertTrue(xnodeGlskHandler.isLinearGlskValidForCnec(cnec3, glskZonalData.getData("10YNL----------L")));

        assertTrue(xnodeGlskHandler.isLinearGlskValidForCnec(cnec1, glskZonalData.getData("22Y201903144---9")));
        assertTrue(xnodeGlskHandler.isLinearGlskValidForCnec(cnec2, glskZonalData.getData("22Y201903144---9")));
        assertFalse(xnodeGlskHandler.isLinearGlskValidForCnec(cnec3, glskZonalData.getData("22Y201903144---9")));
    }

}
