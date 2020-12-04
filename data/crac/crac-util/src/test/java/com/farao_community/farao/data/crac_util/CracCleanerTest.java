/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_util;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.range_domain.RangeType;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.ComplexNetworkAction;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.Topology;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.RelativeFlowThreshold;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public class CracCleanerTest {

    private Network network;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
    }

    @Test
    public void testCleanCrac() {
        SimpleCrac simpleCrac = new SimpleCrac("cracId");

        Contingency contingency = simpleCrac.addContingency("contingencyId", "FFR1AA1  FFR2AA1  1");
        simpleCrac.addContingency("contingency2Id", "BBE1AA1  BBE2AA1  1", "BBE1AA1  BBE3AA1  1");
        simpleCrac.addContingency("contThatShouldBeRemoved", "element that does not exist");

        Instant initialInstant = simpleCrac.addInstant("N", 0);
        Instant outageInstant = simpleCrac.addInstant("postContingencyId", 5);

        State preventiveState = simpleCrac.addState(null, initialInstant);
        State postContingencyState = simpleCrac.addState(contingency, outageInstant);
        State stateThatShouldBeRemoved = simpleCrac.addState("contThatShouldBeRemoved", "postContingencyId");

        simpleCrac.addNetworkElement("neId1");
        simpleCrac.addNetworkElement("neId2");
        simpleCrac.addNetworkElement(new NetworkElement("pst"));

        simpleCrac.addCnec("cnec1prev", "FFR1AA1  FFR2AA1  1", Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 500)), preventiveState.getId());
        simpleCrac.addCnec("cnec2prev", "neId2", Collections.singleton(new RelativeFlowThreshold(Side.LEFT, Direction.OPPOSITE, 30)), preventiveState.getId());
        simpleCrac.addCnec("cnec1cur", "neId1", Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 800)), postContingencyState.getId());
        simpleCrac.addCnec("cnec3cur", "BBE1AA1  BBE2AA1  1", Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 500)), stateThatShouldBeRemoved.getId());

        Topology topology1 = new Topology(
            "topologyId1",
            "topologyName",
            "RTE",
            new ArrayList<>(),
            simpleCrac.getNetworkElement("neId1"),
            ActionType.CLOSE
        );
        Topology topology2 = new Topology(
            "topologyId2",
            "topologyName",
            "RTE",
            new ArrayList<>(),
            simpleCrac.getNetworkElement("FFR1AA1  FFR2AA1  1"),
            ActionType.CLOSE
        );
        ComplexNetworkAction complexNetworkAction = new ComplexNetworkAction("complexNextworkActionId", "RTE");
        PstWithRange pstWithRange = new PstWithRange(
            "pstRangeId",
            "pstRangeName",
            "RTE",
            Collections.singletonList(new FreeToUse(UsageMethod.AVAILABLE, preventiveState)),
            Collections.singletonList(new Range(0, 16, RangeType.ABSOLUTE_FIXED, RangeDefinition.STARTS_AT_ONE)),
            simpleCrac.getNetworkElement("pst")
        );

        simpleCrac.addNetworkAction(topology1);
        simpleCrac.addNetworkAction(topology2);
        simpleCrac.addNetworkAction(complexNetworkAction);
        simpleCrac.addRangeAction(pstWithRange);
        assertEquals(4, simpleCrac.getCnecs().size());
        assertEquals(3, simpleCrac.getNetworkActions().size());
        assertEquals(1, simpleCrac.getRangeActions().size());
        assertEquals(3, simpleCrac.getContingencies().size());
        assertEquals(3, simpleCrac.getStates().size());

        CracCleaner cracCleaner = new CracCleaner();
        List<String> qualityReport = cracCleaner.cleanCrac(simpleCrac, network);

        assertEquals(1, simpleCrac.getCnecs().size());
        assertEquals(1, simpleCrac.getNetworkActions().size());
        assertEquals(0, simpleCrac.getRangeActions().size());
        assertEquals(2, simpleCrac.getContingencies().size());
        assertEquals(2, simpleCrac.getStates().size());

        assertEquals(7, qualityReport.size());
        int removedCount = 0;
        for (String line : qualityReport) {
            if (line.contains("[REMOVED]")) {
                removedCount++;
            }
        }
        assertEquals(7, removedCount);
    }

    private Crac createTestCrac() {
        CracFactory factory = CracFactory.find("SimpleCracFactory");
        Crac crac = factory.create("test-crac");
        Instant inst = crac.newInstant().setId("inst1").setSeconds(10).add();
        crac.newCnec().setId("BBE1AA1  BBE2AA1  1").setOptimized(true).setMonitored(true)
            .newNetworkElement().setId("BBE1AA1  BBE2AA1  1").add()
            .newThreshold().setUnit(Unit.MEGAWATT).setMaxValue(0.0).setDirection(Direction.BOTH).setSide(Side.LEFT).add()
            .setInstant(inst)
            .add();
        crac.newCnec().setId("BBE1AA1  BBE3AA1  1").setOptimized(true).setMonitored(false)
            .newNetworkElement().setId("BBE1AA1  BBE3AA1  1").add()
            .newThreshold().setUnit(Unit.MEGAWATT).setMaxValue(0.0).setDirection(Direction.BOTH).setSide(Side.LEFT).add()
            .setInstant(inst)
            .add();
        crac.newCnec().setId("FFR1AA1  FFR2AA1  1").setOptimized(false).setMonitored(true)
            .newNetworkElement().setId("FFR1AA1  FFR2AA1  1").add()
            .newThreshold().setUnit(Unit.MEGAWATT).setMaxValue(0.0).setDirection(Direction.BOTH).setSide(Side.LEFT).add()
            .setInstant(inst)
            .add();
        crac.newCnec().setId("FFR1AA1  FFR3AA1  1").setOptimized(false).setMonitored(false)
            .newNetworkElement().setId("FFR1AA1  FFR3AA1  1").add()
            .newThreshold().setUnit(Unit.MEGAWATT).setMaxValue(0.0).setDirection(Direction.BOTH).setSide(Side.LEFT).add()
            .setInstant(inst)
            .add();

        return crac;
    }

    @Test
    public void testRemoveUnmonitoredCnecs() {
        Crac crac = createTestCrac();
        CracCleaner cracCleaner = new CracCleaner();
        List<String> qualityReport = cracCleaner.cleanCrac(crac, network);
        assertEquals(1, qualityReport.size());
        assertEquals(3, crac.getCnecs().size());
        assertNull(crac.getCnec("FFR1AA1  FFR3AA1  1"));
    }

    /*@Test
    public void testIgnoreRemoveUnmonitoredCnecs() {
        Crac crac = createTestCrac();
        CracCleaner cracCleaner = new CracCleaner();
        cracCleaner.disableFeature(CracCleaningFeature.CHECK_CNEC_MNEC);
        List<String> qualityReport = cracCleaner.cleanCrac(crac, network);
        assertEquals(0, qualityReport.size());
        assertEquals(4, crac.getCnecs().size());
    }*/
}
