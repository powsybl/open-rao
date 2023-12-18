/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_impl.utils;

import com.powsybl.iidm.network.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class NetworkImportsUtil {

    private NetworkImportsUtil() {

    }

    public static Network import12NodesNetwork() {
        return Network.read("utils/TestCase12Nodes.uct", NetworkImportsUtil.class.getResourceAsStream("/utils/TestCase12Nodes.uct"));
    }

    public static Network import12NodesNoPstNetwork() {
        return Network.read("TestCase12Nodes_no_pst.uct", NetworkImportsUtil.class.getResourceAsStream("/TestCase12Nodes_no_pst.uct"));
    }

    public static Network import12NodesWith2PstsNetwork() {
        return Network.read("utils/TestCase12Nodes_2_PSTS.uct", NetworkImportsUtil.class.getResourceAsStream("/utils/TestCase12Nodes_2_PSTS.uct"));
    }

    public static Network import12NodesNetworkWithSwitch() {
        return Network.read("utils/TestCase12NodesWithSwitch.uct", NetworkImportsUtil.class.getResourceAsStream("/utils/TestCase12NodesWithSwitch.uct"));
    }

    public static Network import16NodesNetworkWithHvdc() {
        return Network.read("utils/TestCase16NodesWithHvdc.xiidm", NetworkImportsUtil.class.getResourceAsStream("/utils/TestCase16NodesWithHvdc.xiidm"));
    }

    public static Network import16NodesNetworkWithAngleDroopHvdcs() {
        return Network.read("utils/TestCase16NodesWithAngleDroopHvdcs.xiidm", NetworkImportsUtil.class.getResourceAsStream("/utils/TestCase16NodesWithAngleDroopHvdcs.xiidm"));
    }

    public static void addHvdcLine(Network network) {
        VoltageLevel vl1 = network.getVoltageLevel("BBE1AA1");
        vl1.getBusBreakerView().newBus().setId("B1").add();
        VscConverterStation cs1 = vl1.newVscConverterStation()
            .setId("C1")
            .setName("Converter1")
            .setConnectableBus("B1")
            .setBus("B1")
            .setLossFactor(1.1f)
            .setVoltageSetpoint(405.0)
            .setVoltageRegulatorOn(true)
            .add();
        cs1.getTerminal()
            .setP(100.0)
            .setQ(50.0);
        cs1.newReactiveCapabilityCurve()
            .beginPoint()
            .setP(5.0)
            .setMinQ(0.0)
            .setMaxQ(10.0)
            .endPoint()
            .beginPoint()
            .setP(10.0)
            .setMinQ(0.0)
            .setMaxQ(10.0)
            .endPoint()
            .add();
        VoltageLevel vl2 = network.getVoltageLevel("DDE3AA1");
        vl2.getBusBreakerView().newBus().setId("D1").add();
        VscConverterStation cs2 = vl2.newVscConverterStation()
            .setId("C2")
            .setName("Converter2")
            .setConnectableBus("D1")
            .setBus("D1")
            .setLossFactor(1.1f)
            .setReactivePowerSetpoint(123)
            .setVoltageRegulatorOn(false)
            .add();
        cs2.newMinMaxReactiveLimits()
            .setMinQ(0.0)
            .setMaxQ(10.0)
            .add();

        network.newHvdcLine()
            .setId("HVDC1")
            .setR(1.0)
            .setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER)
            .setNominalV(400)
            .setActivePowerSetpoint(500)
            .setMaxP(700)
            .setConverterStationId1("C1")
            .setConverterStationId2("C2")
            .add();
    }

    public static void addDanglingLine(Network network) {
        VoltageLevel vl1 = network.getVoltageLevel("FFR1AA1");
        vl1.getBusBreakerView().newBus().setId("B1").add();
        DanglingLine danglingLine = vl1.newDanglingLine()
                .setId("DL1")
                .setName("DL1")
                .setConnectableBus("B1")
                .setBus("B1")
                .setR(0.5)
                .setX(0.5)
                .setB(0.5)
                .setG(0.5)
                .setP0(0.)
                .setQ0(0.)
                .add();
        danglingLine.getTerminal()
                .setP(0.)
                .setQ(0.);
    }

    public static void addShuntCompensator(Network network) {
        VoltageLevel vl1 = network.getVoltageLevel("FFR1AA1");
        vl1.getBusBreakerView().newBus().setId("B1").add();
        ShuntCompensator shuntCompensator = vl1.newShuntCompensator()
                .setId("SC1")
                .setName("SC1")
                .setConnectableBus("B1")
                .setBus("B1")
                .setTargetV(400.)
                .setSectionCount(1)
                .setVoltageRegulatorOn(true)
                .setTargetDeadband(5.0)
                .newLinearModel().setBPerSection(1E-2).setGPerSection(0.0).setMaximumSectionCount(2).add()
                .add();
        shuntCompensator.getTerminal().setP(0.).setQ(0.);
    }

}
