/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.search_tree_rao.commons.parameters;

import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.CracFactory;
import com.powsybl.open_rao.data.crac_api.InstantKind;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_api.range.RangeType;
import com.powsybl.open_rao.data.crac_api.range_action.RangeAction;
import com.powsybl.open_rao.data.crac_api.usage_rule.UsageMethod;
import com.powsybl.open_rao.data.crac_impl.CracImplFactory;
import com.powsybl.open_rao.rao_api.parameters.RaoParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class UnoptimizedCnecParametersTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Crac crac;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
    }

    @Test
    void buildWithoutOptimizingOperatorsNotSharingCras() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(true);

        UnoptimizedCnecParameters ocp = UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), Set.of("BE"), crac);

        assertNotNull(ocp);
        assertEquals(Set.of("BE"), ocp.getOperatorsNotToOptimize());
    }

    @Test
    void buildWhileOptimizingOperatorsNotSharingCras() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(false);

        UnoptimizedCnecParameters ocp = UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), Set.of("BE"), crac);
        assertNull(ocp);
    }

    @Test
    void testUnoptimizedCnecsInSeriesWithPsts() {

        Crac crac = CracFactory.findDefault().create("crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);

        crac.newFlowCnec().withId("flowCnec-1")
                .withNetworkElement("ne1Id")
                .withInstant(PREVENTIVE_INSTANT_ID)
                .withOperator("operator1")
                .withOptimized()
                .newThreshold().withSide(Side.RIGHT).withUnit(Unit.AMPERE).withMin(-500.).add()
                .withIMax(1000., Side.RIGHT)
                .withNominalVoltage(220.)
                .add();

        crac.newContingency().withId("co2").withNetworkElement("ne22").add();

        crac.newFlowCnec().withId("flowCnec-2")
                .withNetworkElement("ne2Id")
                .withInstant(CURATIVE_INSTANT_ID)
                .withContingency("co2")
                .withOperator("operator1")
                .withOptimized()
                .newThreshold().withSide(Side.RIGHT).withUnit(Unit.AMPERE).withMin(-500.).add()
                .withIMax(1000., Side.RIGHT)
                .withNominalVoltage(220.)
                .add();

        crac.newPstRangeAction().withId("pstRange1Id")
                .withName("pstRange1Name")
                .withOperator("RTE")
                .withNetworkElement("pst1")
                .withInitialTap(2)
                .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
                .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(1).withMaxTap(7).add()
                .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
                .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT_ID).add()
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
                .add();

        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCnecsSecuredByTheirPst(Map.of("ne1Id", "pst1",
                "ne2Id", "fakeId",
                "fakeId", "pst2"));
        UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), Set.of("BE"), crac);

        Map<FlowCnec, RangeAction<?>> map = UnoptimizedCnecParameters.getDoNotOptimizeCnecsSecuredByTheirPst(raoParameters.getNotOptimizedCnecsParameters(), crac);
        assertEquals(3, raoParameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCnecsSecuredByTheirPst().size());
        assertEquals(1, map.size());
        assertTrue(map.containsKey(crac.getFlowCnec("flowCnec-1")));
        assertEquals(crac.getPstRangeAction("pstRange1Id"), map.get(crac.getFlowCnec("flowCnec-1")));
        assertFalse(map.containsKey(crac.getFlowCnec("flowCnec-2")));

        // Add pst with same networkElement to crac
        crac.newPstRangeAction().withId("pstRange3Id")
                .withName("pstRange3Name")
                .withOperator("RTE")
                .withNetworkElement("pst2")
                .withGroupId("group-1-pst")
                .withInitialTap(1)
                .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
                .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(1).withMaxTap(7).add()
                .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
                .add();
        RaoParameters newRaoParameters = new RaoParameters();
        newRaoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCnecsSecuredByTheirPst(Map.of("ne1Id", "pst2"));
        Map<FlowCnec, RangeAction<?>> newMap = UnoptimizedCnecParameters.getDoNotOptimizeCnecsSecuredByTheirPst(newRaoParameters.getNotOptimizedCnecsParameters(), crac);
        assertEquals(0, newMap.size());
    }

}
