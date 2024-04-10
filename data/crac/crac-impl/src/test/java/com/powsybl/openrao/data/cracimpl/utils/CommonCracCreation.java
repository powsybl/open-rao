/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl.utils;

import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.openrao.data.cracimpl.CracImplFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil.import12NodesNetwork;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CommonCracCreation {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private static class IidmPstHelper {

        private final String pstId;
        private int initialTapPosition;
        private Map<Integer, Double> tapToAngleConversionMap;

        public IidmPstHelper(String pstId, Network network) {
            this.pstId = pstId;
            interpretWithNetwork(network);
        }

        public int getInitialTap() {
            return initialTapPosition;
        }

        public Map<Integer, Double> getTapToAngleConversionMap() {
            return tapToAngleConversionMap;
        }

        private void interpretWithNetwork(Network network) {
            TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(pstId);
            if (Objects.isNull(transformer)) {
                return;
            }
            PhaseTapChanger phaseTapChanger = transformer.getPhaseTapChanger();
            if (Objects.isNull(phaseTapChanger)) {
                return;
            }
            this.initialTapPosition = phaseTapChanger.getTapPosition();
            buildTapToAngleConversionMap(phaseTapChanger);
        }

        private void buildTapToAngleConversionMap(PhaseTapChanger phaseTapChanger) {
            tapToAngleConversionMap = new HashMap<>();
            phaseTapChanger.getAllSteps().forEach((tap, step) -> tapToAngleConversionMap.put(tap, step.getAlpha()));
        }
    }

    private CommonCracCreation() {
        // nothing
    }

    public static Crac create(Set<Side> monitoredCnecSides) {
        return create(new CracImplFactory(), monitoredCnecSides);
    }

    public static Crac create() {
        return create(new CracImplFactory(), Set.of(Side.LEFT));
    }

    public static Crac create(CracFactory cracFactory, Set<Side> monitoredCnecSides) {

        Crac crac = cracFactory.create("idSimpleCracTestUS", "nameSimpleCracTestUS")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);

        // Contingencies
        crac.newContingency()
            .withId("Contingency FR1 FR3")
            .withName("Trip of FFR1AA1 FFR3AA1 1")
            .withContingencyElement("FFR1AA1  FFR3AA1  1", ContingencyElementType.BRANCH)
            .add();
        crac.newContingency()
            .withId("Contingency FR1 FR2")
            .withName("Trip of FFR1AA1 FFR2AA1 1")
            .withContingencyElement("FFR1AA1  FFR2AA1  1", ContingencyElementType.BRANCH)
            .add();

        // Cnecs
        FlowCnecAdder cnecAdder1 = crac.newFlowCnec()
            .withId("cnec1basecase")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withOptimized(true)
            .withOperator("operator1")
            .withNominalVoltage(380.)
            .withIMax(5000.);
        monitoredCnecSides.forEach(side ->
            cnecAdder1.newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withSide(side)
                .withMin(-1500.)
                .withMax(1500.)
                .add());
        cnecAdder1.add();

        FlowCnecAdder cnecAdder2 = crac.newFlowCnec()
            .withId("cnec1stateCurativeContingency1")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator1")
            .withNominalVoltage(380.)
            .withIMax(5000.);
        monitoredCnecSides.forEach(side ->
            cnecAdder2.newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withSide(side)
                .withMin(-1500.)
                .withMax(1500.)
                .add());
        cnecAdder2.add();

        FlowCnecAdder cnecAdder3 = crac.newFlowCnec()
            .withId("cnec1stateCurativeContingency2")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR2")
            .withOptimized(true)
            .withOperator("operator1")
            .withNominalVoltage(380.)
            .withIMax(5000.);
        monitoredCnecSides.forEach(side ->
            cnecAdder3.newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withSide(side)
                .withMin(-1500.)
                .withMax(1500.)
                .add());
        cnecAdder3.add();

        FlowCnecAdder cnecAdder4 = crac.newFlowCnec()
            .withId("cnec2basecase")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withOptimized(true)
            .withOperator("operator2")
            .withNominalVoltage(380.)
            .withIMax(5000.);
        monitoredCnecSides.forEach(side ->
            cnecAdder4.newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withSide(side)
                .withMin(-1500.)
                .withMax(1500.)
                .add()
                .newThreshold()
                .withUnit(Unit.PERCENT_IMAX)
                .withSide(side)
                .withMin(-0.3)
                .withMax(0.3)
                .add());
        cnecAdder4.add();

        FlowCnecAdder cnecAdder5 = crac.newFlowCnec()
            .withId("cnec2stateCurativeContingency1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator2")
            .withNominalVoltage(380.)
            .withIMax(5000.);
        monitoredCnecSides.forEach(side ->
            cnecAdder5.newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withSide(side)
                .withMin(-1500.)
                .withMax(1500.)
                .add()
                .newThreshold()
                .withUnit(Unit.PERCENT_IMAX)
                .withSide(side)
                .withMin(-0.3)
                .withMax(0.3)
                .add());
        cnecAdder5.add();

        FlowCnecAdder cnecAdder6 = crac.newFlowCnec()
            .withId("cnec2stateCurativeContingency2")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR2")
            .withOptimized(true)
            .withOperator("operator2")
            .withReliabilityMargin(95.)
            .withNominalVoltage(380.)
            .withIMax(5000.);
        monitoredCnecSides.forEach(side ->
            cnecAdder6.newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withSide(Side.LEFT)
                .withMin(-1500.)
                .withMax(1500.)
                .add()
                .newThreshold()
                .withUnit(Unit.PERCENT_IMAX)
                .withSide(Side.LEFT)
                .withMin(-0.3)
                .withMax(0.3)
                .add());
        cnecAdder6.add();

        return crac;
    }

    public static Crac createWithPreventivePstRange() {
        return createWithPreventivePstRange(Set.of(Side.LEFT));
    }

    public static Crac createWithPreventivePstRange(Set<Side> monitoredCnecSides) {
        Crac crac = create(monitoredCnecSides);
        Network network = import12NodesNetwork();
        IidmPstHelper pstHelper = new IidmPstHelper("BBE2AA1  BBE3AA1  1", network);

        crac.newPstRangeAction()
            .withId("pst")
            .withNetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name")
            .withOperator("operator1")
            .newOnInstantUsageRule()
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .newTapRange()
            .withRangeType(RangeType.ABSOLUTE)
            .withMinTap(-16)
            .withMaxTap(16)
            .add()
            .withInitialTap(pstHelper.getInitialTap())
            .withTapToAngleConversionMap(pstHelper.getTapToAngleConversionMap())
            .add();

        return crac;
    }

    public static Crac createWithCurativePstRange() {
        return createWithCurativePstRange(Set.of(Side.LEFT));
    }

    public static Crac createWithCurativePstRange(Set<Side> monitoredCnecSides) {
        Crac crac = create();
        Network network = import12NodesNetwork();
        IidmPstHelper pstHelper = new IidmPstHelper("BBE2AA1  BBE3AA1  1", network);

        crac.newPstRangeAction()
            .withId("pst")
            .withNetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name")
            .withOperator("operator1")
            .newOnContingencyStateUsageRule()
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .newTapRange()
            .withRangeType(RangeType.ABSOLUTE)
            .withMinTap(-16)
            .withMaxTap(16)
            .add()
            .withInitialTap(pstHelper.getInitialTap())
            .withTapToAngleConversionMap(pstHelper.getTapToAngleConversionMap())
            .add();

        return crac;
    }

    public static Crac createWithPreventiveAndCurativePstRange() {
        Crac crac = create();
        Network network = import12NodesNetwork();
        IidmPstHelper pstHelper = new IidmPstHelper("BBE2AA1  BBE3AA1  1", network);

        crac.newPstRangeAction()
            .withId("pst")
            .withNetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name")
            .withOperator("operator1")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withInstant(CURATIVE_INSTANT_ID).withContingency("Contingency FR1 FR3").withUsageMethod(UsageMethod.AVAILABLE).add()
            .newTapRange()
            .withRangeType(RangeType.ABSOLUTE)
            .withMinTap(-16)
            .withMaxTap(16)
            .add()
            .withInitialTap(pstHelper.getInitialTap())
            .withTapToAngleConversionMap(pstHelper.getTapToAngleConversionMap())
            .add();

        return crac;
    }
}
