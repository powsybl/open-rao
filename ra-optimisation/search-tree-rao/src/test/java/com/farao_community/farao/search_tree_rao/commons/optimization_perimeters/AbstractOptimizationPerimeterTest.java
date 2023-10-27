package com.farao_community.farao.search_tree_rao.commons.optimization_perimeters;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThresholdAdder;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

abstract class AbstractOptimizationPerimeterTest {

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
        crac.newInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.newInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.newInstant("auto", InstantKind.AUTO, "outage");
        crac.newInstant("curative", InstantKind.CURATIVE, "auto");

        crac.newContingency().withId("outage-1").withNetworkElement("FFR1AA1  FFR3AA1  1").add();
        crac.newContingency().withId("outage-2").withNetworkElement("FFR2AA1  DDE3AA1  1").add();

        // one preventive CNEC
        pCnec = crac.newFlowCnec()
            .withId("cnec-prev")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstantId("preventive")
            .withOptimized(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(Side.LEFT).add()
            .add();

        // one outage CNEC for each CO
        oCnec1 = crac.newFlowCnec()
            .withId("cnec-co-outage-1")
            .withNetworkElement("FFR2AA1  FFR3AA1  1")
            .withInstantId("outage")
            .withContingency("outage-1")
            .withMonitored(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(Side.LEFT).add()
            .add();
        oCnec1.newExtension(LoopFlowThresholdAdder.class).withUnit(Unit.MEGAWATT).withValue(100.).add();

        oCnec2 = crac.newFlowCnec()
            .withId("cnec-co-outage-2")
            .withNetworkElement("FFR2AA1  FFR3AA1  1")
            .withInstantId("outage")
            .withContingency("outage-2")
            .withOptimized(true).withMonitored(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(Side.LEFT).add()
            .add();

        cCnec1 = crac.newFlowCnec()
            .withId("cnec-co-curative-1")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstantId("curative")
            .withContingency("outage-1")
            .withMonitored(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(Side.LEFT).add()
            .add();

        cCnec2 = crac.newFlowCnec()
            .withId("cnec-co-curative-2")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstantId("curative")
            .withContingency("outage-2")
            .withOptimized(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(Side.LEFT).add()
            .add();
        cCnec2.newExtension(LoopFlowThresholdAdder.class).withUnit(Unit.MEGAWATT).withValue(100.).add();

        // one preventive range action and one curative
        pRA = (RangeAction<?>) crac.newInjectionRangeAction().withId("preventive-ra")
            .withNetworkElementAndKey(1, "BBE2AA1 _generator")
            .newRange().withMin(-1000).withMax(1000).add()
            .newOnInstantUsageRule().withInstantId("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        cRA = (RangeAction<?>) crac.newInjectionRangeAction().withId("curative-ra")
            .withNetworkElementAndKey(1, "BBE2AA1 _generator")
            .newRange().withMin(-1000).withMax(1000).add()
            .newOnContingencyStateUsageRule().withInstantId("curative").withContingency("outage-1").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        // one preventive network action and one curative
        pNA = crac.newNetworkAction().withId("preventive-na")
            .newOnInstantUsageRule().withInstantId("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("BBE2AA1  FFR3AA1  1").add()
            .add();

        cNA = crac.newNetworkAction().withId("curative-na")
            .withName("complexNetworkActionName")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("BBE2AA1  FFR3AA1  1").add()
            .newOnContingencyStateUsageRule().withInstantId("curative").withContingency("outage-1").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        pState = crac.getPreventiveState();
        oState1 = crac.getState("outage-1", "outage");
        oState2 = crac.getState("outage-2", "outage");
        cState1 = crac.getState("outage-1", "curative");
        cState2 = crac.getState("outage-2", "curative");

        prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
    }
}
