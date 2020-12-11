/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.json;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.*;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.range_domain.RangeType;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.AbstractElementaryNetworkAction;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.ComplexNetworkAction;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.PstSetpoint;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.Topology;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.AlignedRangeAction;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.threshold.*;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUseImpl;
import com.farao_community.farao.data.crac_impl.usage_rule.OnStateImpl;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.farao_community.farao.data.crac_api.RangeDefinition.CENTERED_ON_ZERO;
import static com.farao_community.farao.data.crac_impl.json.RoundTripUtil.roundTrip;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracImportExportTest {

    @Test
    public void cracTest() {
        SimpleCrac simpleCrac = new SimpleCrac("cracId");

        Instant initialInstant = simpleCrac.addInstant("N", 0);
        State preventiveState = simpleCrac.addState(null, initialInstant);
        Contingency contingency = simpleCrac.addContingency("contingencyId", "neId");
        simpleCrac.addContingency("contingency2Id", "neId1", "neId2");
        Instant outageInstant = simpleCrac.addInstant("postContingencyId", 5);
        State postContingencyState = simpleCrac.addState(contingency, outageInstant);
        simpleCrac.addState("contingency2Id", "postContingencyId");

        simpleCrac.addCnec("cnec1prev", "neId1", Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -500., null, BranchThresholdRule.ON_LEFT_SIDE)), preventiveState.getId());
        BranchCnec preventiveCnec1 = simpleCrac.getBranchCnec("cnec1prev");

        Set<BranchThreshold> thresholds = new HashSet<>();
        thresholds.add(new BranchThresholdImpl(Unit.PERCENT_IMAX, -0.3, null, BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, -800., null, BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, -800., null, BranchThresholdRule.ON_HIGH_VOLTAGE_LEVEL));
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, null, 1200., BranchThresholdRule.ON_LOW_VOLTAGE_LEVEL));

        simpleCrac.addCnec("cnec2prev", "neId2", thresholds, preventiveState.getId());
        simpleCrac.addCnec("cnec1cur", "neId1", Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -800., null, BranchThresholdRule.ON_LEFT_SIDE)), postContingencyState.getId());

        double positiveFrmMw = 20.0;
        BranchThreshold absoluteFlowThreshold = new BranchThresholdImpl(Unit.MEGAWATT, null, 500., BranchThresholdRule.ON_LEFT_SIDE);
        Set<BranchThreshold> thresholdSet = new HashSet<>();
        thresholdSet.add(absoluteFlowThreshold);
        simpleCrac.addCnec("cnec3prevId", "cnec3prevName", "neId2", thresholdSet, preventiveState.getId(), positiveFrmMw, false, true);
        simpleCrac.addCnec("cnec4prevId", "cnec4prevName", "neId2", thresholdSet, preventiveState.getId(), 0.0, true, true);

        List<UsageRule> usageRules = new ArrayList<>();
        usageRules.add(new FreeToUseImpl(UsageMethod.AVAILABLE, preventiveState.getInstant()));
        usageRules.add(new OnStateImpl(UsageMethod.FORCED, postContingencyState));

        simpleCrac.addNetworkElement(new NetworkElement("pst"));
        simpleCrac.addNetworkAction(new PstSetpoint("pstSetpointId", "pstSetpointName", "RTE", usageRules, simpleCrac.getNetworkElement("pst"), 15, CENTERED_ON_ZERO));

        Set<AbstractElementaryNetworkAction> elementaryNetworkActions = new HashSet<>();
        PstSetpoint pstSetpoint = new PstSetpoint(
                "pstSetpointId",
                "pstSetpointName",
                "RTE",
                new ArrayList<>(),
                simpleCrac.getNetworkElement("pst"),
                5,
                CENTERED_ON_ZERO
        );
        Topology topology = new Topology(
                "topologyId",
                "topologyName",
                "RTE",
                new ArrayList<>(),
                simpleCrac.getNetworkElement("neId"),
                ActionType.CLOSE
        );
        elementaryNetworkActions.add(pstSetpoint);
        elementaryNetworkActions.add(topology);
        ComplexNetworkAction complexNetworkAction = new ComplexNetworkAction(
                "complexNetworkActionId",
                "complexNetworkActionName",
                "RTE",
                new ArrayList<>(),
                elementaryNetworkActions
        );
        simpleCrac.addNetworkAction(complexNetworkAction);

        simpleCrac.addRangeAction(new PstWithRange(
                "pstRangeId",
                "pstRangeName",
                "RTE",
                Collections.singletonList(new FreeToUseImpl(UsageMethod.AVAILABLE, preventiveState.getInstant())),
                Arrays.asList(new Range(0, 16, RangeType.ABSOLUTE_FIXED, RangeDefinition.STARTS_AT_ONE),
                        new Range(-3, 3, RangeType.RELATIVE_FIXED, CENTERED_ON_ZERO)),
                simpleCrac.getNetworkElement("pst")
        ));

        simpleCrac.addRangeAction(new AlignedRangeAction(
                "alignedRangeId",
                "alignedRangeName",
                "RTE",
                Collections.singletonList(new OnStateImpl(UsageMethod.AVAILABLE, preventiveState)),
                Collections.singletonList(new Range(-3, 3, RangeType.RELATIVE_DYNAMIC, CENTERED_ON_ZERO)),
                Stream.of(simpleCrac.getNetworkElement("pst"), simpleCrac.addNetworkElement("pst2")).collect(Collectors.toSet())
        ));

        simpleCrac.setNetworkDate(new DateTime(2020, 5, 14, 11, 35));

        Crac crac = roundTrip(simpleCrac, SimpleCrac.class);

        assertEquals(5, crac.getNetworkElements().size());
        assertEquals(2, crac.getInstants().size());
        assertEquals(2, crac.getContingencies().size());
        assertEquals(5, crac.getBranchCnecs().size());
        assertEquals(2, crac.getRangeActions().size());
        assertEquals(2, crac.getNetworkActions().size());
        assertEquals(4, crac.getBranchCnec("cnec2prev").getThresholds().size());
        assertFalse(crac.getBranchCnec("cnec3prevId").isOptimized());
        assertTrue(crac.getBranchCnec("cnec4prevId").isMonitored());
        assertTrue(crac.getNetworkAction("pstSetpointId") instanceof PstSetpoint);
        assertEquals(CENTERED_ON_ZERO, ((PstSetpoint) crac.getNetworkAction("pstSetpointId")).getRangeDefinition());
    }
}
