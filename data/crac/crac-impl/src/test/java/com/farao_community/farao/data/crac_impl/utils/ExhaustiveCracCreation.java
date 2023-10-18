/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.utils;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.InstantImpl;
import com.powsybl.iidm.network.Country;

import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class ExhaustiveCracCreation {
    private static final Instant INSTANT_PREV = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant INSTANT_OUTAGE = new InstantImpl("outage", InstantKind.OUTAGE, INSTANT_PREV);
    private static final Instant INSTANT_AUTO = new InstantImpl("auto", InstantKind.AUTO, INSTANT_OUTAGE);
    private static final Instant INSTANT_CURATIVE = new InstantImpl("curative", InstantKind.CURATIVE, INSTANT_AUTO);

    /*
    Small CRAC used in I/O unit tests of farao-core

    The idea of this CRAC is to be quite exhaustive regarding the diversity of the CRAC objects.
    It contains numerous variations of the CRAC objects, to ensure that they are all tested in
    the manipulations of the CRAC.
     */

    private ExhaustiveCracCreation() {
    }

    public static Crac create() {
        return create(CracFactory.findDefault());
    }

    public static Crac create(CracFactory cracFactory) {

        Crac crac = cracFactory.create("exhaustiveCracId", "exhaustiveCracName");
        crac.addInstant(INSTANT_PREV);
        crac.addInstant(INSTANT_OUTAGE);
        crac.addInstant(INSTANT_AUTO);
        crac.addInstant(INSTANT_CURATIVE);

        String contingency1Id = "contingency1Id";
        crac.newContingency().withId(contingency1Id).withNetworkElement("ne1Id").add();

        String contingency2Id = "contingency2Id";
        crac.newContingency().withId(contingency2Id).withNetworkElement("ne2Id", "ne2Name").withNetworkElement("ne3Id").add();

        crac.newFlowCnec().withId("cnec1prevId")
            .withNetworkElement("ne4Id")
            .withInstantId(INSTANT_PREV.getId())
            .withOperator("operator1")
            .withOptimized()
            .newThreshold().withSide(Side.RIGHT).withUnit(Unit.AMPERE).withMin(-500.).add()
            .withIMax(1000., Side.RIGHT)
            .withNominalVoltage(220.)
            .add();

        crac.newFlowCnec().withId("cnec1outageId")
            .withNetworkElement("ne4Id")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withOperator("operator1")
            .withOptimized()
            .newThreshold().withSide(Side.RIGHT).withUnit(Unit.AMPERE).withMin(-800.).add()
            .withNominalVoltage(220.)
            .add();

        crac.newFlowCnec().withId("cnec2prevId")
            .withNetworkElement("ne5Id", "ne5Name")
            .withInstantId(INSTANT_PREV.getId())
            .withOperator("operator2")
            .withOptimized()
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.PERCENT_IMAX).withMin(-0.3).add()
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.AMPERE).withMin(-800.).add()
            .newThreshold().withSide(Side.RIGHT).withUnit(Unit.AMPERE).withMin(-800.).add()
            .newThreshold().withSide(Side.RIGHT).withUnit(Unit.AMPERE).withMax(1200.).add()
            .withNominalVoltage(220., Side.RIGHT)
            .withNominalVoltage(380., Side.LEFT)
            .withIMax(2000.)
            .add();

        crac.newFlowCnec().withId("cnec3prevId")
            .withName("cnec3prevName")
            .withNetworkElement("ne2Id", "ne2Name")
            .withInstantId(INSTANT_PREV.getId())
            .withOperator("operator3")
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withSide(Side.LEFT).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withSide(Side.RIGHT).add()
            .withReliabilityMargin(20.)
            .withMonitored()
            .add();

        crac.newFlowCnec().withId("cnec3autoId")
            .withName("cnec3autoName")
            .withNetworkElement("ne2Id", "ne2Name")
            .withInstantId(INSTANT_AUTO.getId())
            .withContingency(contingency2Id)
            .withOperator("operator3")
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withSide(Side.LEFT).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withSide(Side.RIGHT).add()
            .withReliabilityMargin(20.)
            .withMonitored()
            .add();

        crac.newFlowCnec().withId("cnec3curId")
            .withNetworkElement("ne2Id", "ne2Name")
            .withInstantId(INSTANT_CURATIVE.getId())
            .withContingency(contingency2Id)
            .withOperator("operator3")
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withSide(Side.LEFT).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withSide(Side.RIGHT).add()
            .withReliabilityMargin(20.)
            .withMonitored()
            .add();

        crac.newFlowCnec().withId("cnec4prevId")
            .withName("cnec4prevName")
            .withNetworkElement("ne3Id")
            .withInstantId(INSTANT_PREV.getId())
            .withOperator("operator4")
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withSide(Side.LEFT).add()
            .withReliabilityMargin(0.)
            .withOptimized()
            .withMonitored()
            .add();

        crac.newAngleCnec().withId("angleCnecId")
            .withName("angleCnecName")
            .withExportingNetworkElement("eneId", "eneName")
            .withImportingNetworkElement("ineId", "ineName")
            .withInstantId(INSTANT_CURATIVE.getId())
            .withContingency(contingency1Id)
            .withOperator("operator1")
            .newThreshold().withUnit(Unit.DEGREE).withMin(-100.).withMax(100.).add()
            .withReliabilityMargin(10.)
            .withMonitored()
            .add();

        crac.newVoltageCnec().withId("voltageCnecId")
            .withName("voltageCnecName")
            .withNetworkElement("voltageCnecNeId", "voltageCnecNeName")
            .withInstantId(INSTANT_CURATIVE.getId())
            .withContingency(contingency1Id)
            .withOperator("operator1")
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(380.).add()
            .withReliabilityMargin(1.)
            .withMonitored()
            .add();

        // network action with one pst set point
        crac.newNetworkAction().withId("pstSetpointRaId")
            .withName("pstSetpointRaName")
            .withOperator("RTE")
            .newPstSetPoint().withSetpoint(15).withNetworkElement("pst").add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstantId(INSTANT_PREV.getId()).add()
            .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency(contingency1Id).withInstantId(INSTANT_CURATIVE.getId()).add()
            .add();

        // complex network action with one pst set point and one topology
        crac.newNetworkAction().withId("complexNetworkActionId")
            .withName("complexNetworkActionName")
            .withOperator("RTE")
            .newPstSetPoint().withSetpoint(5).withNetworkElement("pst").add()
            .newTopologicalAction().withActionType(ActionType.CLOSE).withNetworkElement("ne1Id").add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstantId(INSTANT_PREV.getId()).add()
            .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withInstantId(INSTANT_PREV.getId()).add()
            .add();

        // network action with one injection set point
        crac.newNetworkAction().withId("injectionSetpointRaId")
            .withName("injectionSetpointRaName")
            .withOperator("RTE")
            .newInjectionSetPoint().withSetpoint(260).withNetworkElement("injection").withUnit(Unit.SECTION_COUNT).add()
            .newOnFlowConstraintUsageRule().withFlowCnec("cnec3autoId").withInstantId(INSTANT_AUTO.getId()).add()
            .add();

        // network action with one switch pair
        crac.newNetworkAction().withId("switchPairRaId")
            .withName("switchPairRaName")
            .withOperator("RTE")
            .newSwitchPair().withSwitchToOpen("to-open").withSwitchToClose("to-close", "to-close-name").add()
            .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency(contingency2Id).withInstantId(INSTANT_CURATIVE.getId()).add()
            .add();

        // range actions
        crac.newPstRangeAction().withId("pstRange1Id")
            .withName("pstRange1Name")
            .withOperator("RTE")
            .withNetworkElement("pst")
            .withInitialTap(2)
            .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
            .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(1).withMaxTap(7).add()
            .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstantId(INSTANT_PREV.getId()).add()
            .add();

        crac.newPstRangeAction().withId("pstRange2Id")
            .withName("pstRange2Name")
            .withOperator("RTE")
            .withNetworkElement("pst2")
            .withGroupId("group-1-pst")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
            .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(1).withMaxTap(7).add()
            .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
            .newOnFlowConstraintUsageRule().withInstantId(INSTANT_PREV.getId()).withFlowCnec("cnec3prevId").add()
            .add();

        crac.newPstRangeAction().withId("pstRange3Id")
            .withName("pstRange3Name")
            .withOperator("RTE")
            .withNetworkElement("pst3")
            .withGroupId("group-3-pst")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
            .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(1).withMaxTap(7).add()
            .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
            .newOnAngleConstraintUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withAngleCnec("angleCnecId").add()
            .add();

        crac.newPstRangeAction().withId("pstRange4Id")
            .withName("pstRange4Name")
            .withOperator("RTE")
            .withNetworkElement("pst3")
            .withGroupId("group-3-pst")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
            .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(1).withMaxTap(7).add()
            .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
            .newOnVoltageConstraintUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withVoltageCnec("voltageCnecId").add()
            .add();

        crac.newHvdcRangeAction().withId("hvdcRange1Id")
            .withName("hvdcRange1Name")
            .withOperator("RTE")
            .withNetworkElement("hvdc")
            .newRange().withMin(-1000).withMax(1000).add()
            .newOnFlowConstraintInCountryUsageRule().withInstantId(INSTANT_PREV.getId()).withCountry(Country.FR).add()
            .add();

        crac.newHvdcRangeAction().withId("hvdcRange2Id")
            .withName("hvdcRange2Name")
            .withOperator("RTE")
            .withNetworkElement("hvdc2")
            .withGroupId("group-1-hvdc")
            .newRange().withMin(-1000).withMax(1000).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1Id").withInstantId(INSTANT_CURATIVE.getId()).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency2Id").withInstantId(INSTANT_CURATIVE.getId()).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnFlowConstraintUsageRule().withInstantId(INSTANT_PREV.getId()).withFlowCnec("cnec3curId").add()
            .add();

        crac.newInjectionRangeAction().withId("injectionRange1Id")
            .withName("injectionRange1Name")
            .withNetworkElementAndKey(1., "generator1Id")
            .withNetworkElementAndKey(-1., "generator2Id", "generator2Name")
            .newRange().withMin(-500).withMax(500).add()
            .newRange().withMin(-1000).withMax(1000).add()
            .newOnFlowConstraintInCountryUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withCountry(Country.ES).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1Id").withInstantId(INSTANT_CURATIVE.getId()).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        return crac;
    }
}
