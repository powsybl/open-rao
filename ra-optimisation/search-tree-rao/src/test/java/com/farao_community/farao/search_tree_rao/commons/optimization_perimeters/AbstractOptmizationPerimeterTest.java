package com.farao_community.farao.search_tree_rao.commons.optimization_perimeters;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThresholdAdder;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.mockito.Mockito;

public abstract class AbstractOptmizationPerimeterTest {

    protected Network network;
    protected Crac crac;
    protected State pState;
    protected State oState1;
    protected State oState2;
    protected State cState1;
    protected State cState2;
    protected FlowCnec pCnec;
    protected FlowCnec oCnec1;
    protected FlowCnec oCnec2;
    protected FlowCnec cCnec1;
    protected FlowCnec cCnec2;
    protected RangeAction<?> pRA;
    protected RangeAction<?> cRA;
    protected NetworkAction pNA;
    protected NetworkAction cNA;
    protected RaoParameters raoParameters;
    protected PrePerimeterResult prePerimeterResult;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        raoParameters = new RaoParameters();
        raoParameters.addExtension(SearchTreeRaoParameters.class, new SearchTreeRaoParameters());

        crac = CracFactory.findDefault().create("cracId");

        crac.newContingency().withId("outage-1").withNetworkElement("FFR1AA1  FFR3AA1  1").add();
        crac.newContingency().withId("outage-2").withNetworkElement("FFR2AA1  DDE3AA1  1").add();

        // one preventive CNEC
        pCnec = crac.newFlowCnec()
            .withId("cnec-prev")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(Instant.PREVENTIVE)
            .withOptimized(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();

        // one outage CNEC for each CO
        oCnec1 = crac.newFlowCnec()
            .withId("cnec-co-outage-1")
            .withNetworkElement("FFR2AA1  FFR3AA1  1")
            .withInstant(Instant.OUTAGE)
            .withContingency("outage-1")
            .withMonitored(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add().newExtension(LoopFlowThresholdAdder.class).withUnit(Unit.MEGAWATT).withValue(100.).add();

        oCnec2 = crac.newFlowCnec()
            .withId("cnec-co-outage-2")
            .withNetworkElement("FFR2AA1  FFR3AA1  1")
            .withInstant(Instant.OUTAGE)
            .withContingency("outage-2")
            .withOptimized(true).withMonitored(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();

        cCnec1 = crac.newFlowCnec()
            .withId("cnec-co-curative-1")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(Instant.CURATIVE)
            .withContingency("outage-1")
            .withMonitored(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();

        cCnec2 = crac.newFlowCnec()
            .withId("cnec-co-curative-2")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(Instant.CURATIVE)
            .withContingency("outage-2")
            .withOptimized(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add().newExtension(LoopFlowThresholdAdder.class).withUnit(Unit.MEGAWATT).withValue(100.).add();

        // one preventive range action and one curative
        pRA = crac.newInjectionRangeAction().withId("preventive-ra")
            .withNetworkElementAndKey(1, "BBE2AA1 _generator")
            .newRange().withMin(-1000).withMax(1000).add()
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        cRA = crac.newInjectionRangeAction().withId("curative-ra")
            .withNetworkElementAndKey(1, "BBE2AA1 _generator")
            .newRange().withMin(-1000).withMax(1000).add()
            .newOnStateUsageRule().withInstant(Instant.CURATIVE).withContingency("outage-1").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        // one preventive networkaction on one curative
        pNA = crac.newNetworkAction().withId("preventive-na")
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("BBE2AA1  FFR3AA1  1").add()
            .add();

        cNA = crac.newNetworkAction().withId("curative-na")
            .withName("complexNetworkActionName")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("BBE2AA1  FFR3AA1  1").add()
            .newOnStateUsageRule().withInstant(Instant.CURATIVE).withContingency("outage-1").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        pState = crac.getPreventiveState();
        oState1 = crac.getState("outage-1", Instant.OUTAGE);
        oState2 = crac.getState("outage-2", Instant.OUTAGE);
        cState1 = crac.getState("outage-1", Instant.CURATIVE);
        cState2 = crac.getState("outage-2", Instant.CURATIVE);

        prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
    }
}
