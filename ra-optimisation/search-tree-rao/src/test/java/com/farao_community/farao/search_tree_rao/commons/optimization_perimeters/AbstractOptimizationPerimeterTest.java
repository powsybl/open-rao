package com.farao_community.farao.search_tree_rao.commons.optimization_perimeters;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.InstantImpl;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThresholdAdder;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

abstract class AbstractOptimizationPerimeterTest {
    private static final Instant INSTANT_PREV = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant INSTANT_OUTAGE = new InstantImpl("outage", InstantKind.OUTAGE, INSTANT_PREV);
    private static final Instant INSTANT_AUTO = new InstantImpl("auto", InstantKind.AUTO, INSTANT_OUTAGE);
    private static final Instant INSTANT_CURATIVE = new InstantImpl("curative", InstantKind.CURATIVE, INSTANT_AUTO);

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
    protected RemedialAction<?> pNA;
    protected RemedialAction<?> cNA;
    protected RaoParameters raoParameters;
    protected PrePerimeterResult prePerimeterResult;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        raoParameters = new RaoParameters();

        crac = CracFactory.findDefault().create("cracId");
        crac.addInstant(INSTANT_PREV);
        crac.addInstant(INSTANT_OUTAGE);
        crac.addInstant(INSTANT_AUTO);
        crac.addInstant(INSTANT_CURATIVE);

        crac.newContingency().withId("outage-1").withNetworkElement("FFR1AA1  FFR3AA1  1").add();
        crac.newContingency().withId("outage-2").withNetworkElement("FFR2AA1  DDE3AA1  1").add();

        // one preventive CNEC
        pCnec = crac.newFlowCnec()
            .withId("cnec-prev")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstantId(INSTANT_PREV.getId())
            .withOptimized(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(Side.LEFT).add()
            .add();

        // one outage CNEC for each CO
        oCnec1 = crac.newFlowCnec()
            .withId("cnec-co-outage-1")
            .withNetworkElement("FFR2AA1  FFR3AA1  1")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency("outage-1")
            .withMonitored(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(Side.LEFT).add()
            .add();
        oCnec1.newExtension(LoopFlowThresholdAdder.class).withUnit(Unit.MEGAWATT).withValue(100.).add();

        oCnec2 = crac.newFlowCnec()
            .withId("cnec-co-outage-2")
            .withNetworkElement("FFR2AA1  FFR3AA1  1")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency("outage-2")
            .withOptimized(true).withMonitored(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(Side.LEFT).add()
            .add();

        cCnec1 = crac.newFlowCnec()
            .withId("cnec-co-curative-1")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstantId(INSTANT_CURATIVE.getId())
            .withContingency("outage-1")
            .withMonitored(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(Side.LEFT).add()
            .add();

        cCnec2 = crac.newFlowCnec()
            .withId("cnec-co-curative-2")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstantId(INSTANT_CURATIVE.getId())
            .withContingency("outage-2")
            .withOptimized(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(Side.LEFT).add()
            .add();
        cCnec2.newExtension(LoopFlowThresholdAdder.class).withUnit(Unit.MEGAWATT).withValue(100.).add();

        // one preventive range action and one curative
        pRA = (RangeAction<?>) crac.newInjectionRangeAction().withId("preventive-ra")
            .withNetworkElementAndKey(1, "BBE2AA1 _generator")
            .newRange().withMin(-1000).withMax(1000).add()
            .newOnInstantUsageRule().withInstantId(INSTANT_PREV.getId()).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        cRA = (RangeAction<?>) crac.newInjectionRangeAction().withId("curative-ra")
            .withNetworkElementAndKey(1, "BBE2AA1 _generator")
            .newRange().withMin(-1000).withMax(1000).add()
            .newOnContingencyStateUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withContingency("outage-1").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        // one preventive network action and one curative
        pNA = crac.newNetworkAction().withId("preventive-na")
            .newOnInstantUsageRule().withInstantId(INSTANT_PREV.getId()).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("BBE2AA1  FFR3AA1  1").add()
            .add();

        cNA = crac.newNetworkAction().withId("curative-na")
            .withName("complexNetworkActionName")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("BBE2AA1  FFR3AA1  1").add()
            .newOnContingencyStateUsageRule().withInstantId(INSTANT_CURATIVE.getId()).withContingency("outage-1").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        pState = crac.getPreventiveState();
        oState1 = crac.getState("outage-1", INSTANT_OUTAGE);
        oState2 = crac.getState("outage-2", INSTANT_OUTAGE);
        cState1 = crac.getState("outage-1", INSTANT_CURATIVE);
        cState2 = crac.getState("outage-2", INSTANT_CURATIVE);

        prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
    }
}
