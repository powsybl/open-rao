/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraintAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnFlowConstraintAdderImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Crac crac;
    private NetworkActionAdder remedialActionAdder;
    private Instant preventiveInstant;
    private Instant curativeInstant;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

        crac.newContingency()
            .withId("Contingency FR1 FR3")
            .withName("Trip of FFR1AA1 FFR3AA1 1")
            .withNetworkElement("FFR1AA1  FFR3AA1  1")
            .add();

        crac.newFlowCnec()
            .withId("cnec2stateCurativeContingency1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold()
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
            .add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();

        remedialActionAdder = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add();
    }

    @Test
    void testOkPreventive() {
        RemedialAction<?> remedialAction = remedialActionAdder.newOnFlowConstraintUsageRule()
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withFlowCnec("cnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();
        UsageRule usageRule = (UsageRule) remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnFlowConstraint);
        OnFlowConstraint onFlowConstraint = (OnFlowConstraint) usageRule;
        assertEquals(preventiveInstant, onFlowConstraint.getInstant());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod(crac.getPreventiveState()));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        RemedialAction<?> remedialAction = remedialActionAdder.newOnFlowConstraintUsageRule()
            .withInstant(CURATIVE_INSTANT_ID)
            .withFlowCnec("cnec2stateCurativeContingency1")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();
        UsageRule usageRule = (UsageRule) remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnFlowConstraint);
        OnFlowConstraint onFlowConstraint = (OnFlowConstraint) usageRule;
        assertEquals(curativeInstant, onFlowConstraint.getInstant());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod(crac.getState(crac.getContingency("Contingency FR1 FR3"), curativeInstant)));
        assertEquals(1, crac.getStates().size());
    }

    @Test
    void testOutageException() {
        OnFlowConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(OUTAGE_INSTANT_ID).withFlowCnec("cnec2stateCurativeContingency1").withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("OnFlowConstraint usage rules are not allowed for OUTAGE instant.", exception.getMessage());
    }

    @Test
    void testAbsentCnecException() {
        OnFlowConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID)
            .withFlowCnec("fake_cnec").withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("FlowCnec fake_cnec does not exist in crac. Consider adding it first.", exception.getMessage());
    }

    @Test
    void testNoCnecException() {
        OnFlowConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Cannot add OnFlowConstraint without a flow cnec. Please use withFlowCnec() with a non null value", exception.getMessage());
    }

    @Test
    void testNoInstantException() {
        OnFlowConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintUsageRule().withFlowCnec("cnec2stateCurativeContingency1").withUsageMethod(UsageMethod.AVAILABLE);
        Exception exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Cannot add OnFlowConstraint without a instant. Please use withInstant() with a non null value", exception.getMessage());
    }

    @Test
    void testNoUsageMethodException() {
        OnFlowConstraintAdder<NetworkActionAdder> adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withFlowCnec("cnec2stateCurativeContingency1");
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Cannot add OnFlowConstraint without a usage method. Please use withUsageMethod() with a non null value", exception.getMessage());
    }

    private void addCnec(String id, String instantId) {
        FlowCnecAdder adder = crac.newFlowCnec()
            .withId(id)
            .withNetworkElement(id)
            .withInstant(instantId)
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(Side.LEFT).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(Side.LEFT).withMin(-0.3).withMax(0.3).add()
            .withNominalVoltage(380.)
            .withIMax(5000.);
        if (!crac.getInstant(instantId).isPreventive()) {
            adder.withContingency("Contingency FR1 FR3");
        }
        adder.add();
    }

    @Test
    void testOnConstraintInstantCheck() {
        // todo : mm chose pour on flow constraint in country, dans le code
        addCnec("cnec-prev", PREVENTIVE_INSTANT_ID);
        addCnec("cnec-out", OUTAGE_INSTANT_ID);
        addCnec("cnec-auto", AUTO_INSTANT_ID);
        addCnec("cnec-cur", CURATIVE_INSTANT_ID);

        OnFlowConstraintAdder<NetworkActionAdder> adder;

        // PREVENTIVE RA
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withFlowCnec("cnec-prev").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withFlowCnec("cnec-out").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withFlowCnec("cnec-auto").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withFlowCnec("cnec-cur").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok

        // AUTO RA
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withFlowCnec("cnec-prev").withUsageMethod(UsageMethod.FORCED); // nok
        Exception exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'auto' on a CNEC constraint at instant 'preventive' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withFlowCnec("cnec-out").withUsageMethod(UsageMethod.FORCED); // nok
        exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'auto' on a CNEC constraint at instant 'outage' are not allowed.", exception.getMessage());
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withFlowCnec("cnec-auto").withUsageMethod(UsageMethod.FORCED).add(); // ok
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(AUTO_INSTANT_ID).withFlowCnec("cnec-cur").withUsageMethod(UsageMethod.FORCED).add(); // ok

        // CURATIVE RA
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withFlowCnec("cnec-prev").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'preventive' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withFlowCnec("cnec-out").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'outage' are not allowed.", exception.getMessage());
        adder = remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withFlowCnec("cnec-auto").withUsageMethod(UsageMethod.AVAILABLE); // nok
        exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("Remedial actions available at instant 'curative' on a CNEC constraint at instant 'auto' are not allowed.", exception.getMessage());
        remedialActionAdder.newOnFlowConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withFlowCnec("cnec-cur").withUsageMethod(UsageMethod.AVAILABLE).add(); // ok
    }
}
