/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.networkaction.*;
import com.powsybl.openrao.data.cracapi.usagerule.OnContingencyState;
import com.powsybl.openrao.data.cracapi.usagerule.OnFlowConstraint;
import com.powsybl.openrao.data.cracapi.usagerule.OnInstant;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracapi.usagerule.UsageRule;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupRemedialActionTest {

    @Test
    void importGroupedRemedialActions() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/remedialactions/RemedialActionGroups.zip", NETWORK);
        assertEquals(5, cracCreationContext.getCrac().getRemedialActions().size());
        assertRaNotImported(cracCreationContext, "92f45b99-44c2-499d-8c4e-723bd1829dbe", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group 92f45b99-44c2-499d-8c4e-723bd1829dbe will not be imported because all depending the remedial actions must have the same usage rules. All RA's depending in that group will be ignored: open_be1_be4_cra, open_be1_be4_pra");
        assertRaNotImported(cracCreationContext, "95ba7539-6067-4f7e-a30b-e53eae7c042a", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group 95ba7539-6067-4f7e-a30b-e53eae7c042a will not be imported because all depending the remedial actions must have the same usage rules. All RA's depending in that group will be ignored: open_be1_be4_cra_ae1, open_be1_be4_pra");
        assertRaNotImported(cracCreationContext, "a8c92deb-6a3a-49a2-aecd-fb3bccbd005c", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group a8c92deb-6a3a-49a2-aecd-fb3bccbd005c will not be imported because all depending the remedial actions must have the same usage rules. All RA's depending in that group will be ignored: open_be1_be4_cra, open_be1_be4_cra_co1");
        assertRaNotImported(cracCreationContext, "b9dc54f8-6f8f-4878-8d7d-5d08751e5977", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group b9dc54f8-6f8f-4878-8d7d-5d08751e5977 will not be imported because all related RemedialActionDependency must be of the same kind. All RA's depending in that group will be ignored: open_be1_be4_pra, open_de3_de4_pra");
        assertRaNotImported(cracCreationContext, "fc0403cc-c774-4966-b7cb-2fc75b7ebdbc", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group fc0403cc-c774-4966-b7cb-2fc75b7ebdbc will not be imported because all depending the remedial actions must have the same usage rules. All RA's depending in that group will be ignored: open_be1_be4_cra_ae1, open_be1_be4_cra_ae2");

        assertNetworkActionImported(cracCreationContext, "23ff9c5d-9501-4141-a4b3-f4468b2eb636", Set.of("DDE3AA1  DDE4AA1  1", "BBE1AA1  BBE4AA1  1"), true, 1, "RTE");
        assertEquals("Topo 1 + Topo 2 - PRA", cracCreationContext.getCrac().getRemedialAction("23ff9c5d-9501-4141-a4b3-f4468b2eb636").getName());
        UsageRule ur0 = cracCreationContext.getCrac().getNetworkAction("23ff9c5d-9501-4141-a4b3-f4468b2eb636").getUsageRules().iterator().next();
        assertTrue(ur0 instanceof OnInstant);
        assertEquals(InstantKind.PREVENTIVE, ur0.getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ur0.getUsageMethod());

        assertNetworkActionImported(cracCreationContext, "5569e363-59ab-497a-99b0-c4ae239cbe73", Set.of("DDE3AA1  DDE4AA1  1", "BBE1AA1  BBE4AA1  1"), true, 1, "RTE");
        assertEquals("Topo 1 + Topo 2 - CRA", cracCreationContext.getCrac().getRemedialAction("5569e363-59ab-497a-99b0-c4ae239cbe73").getName());
        UsageRule ur1 = cracCreationContext.getCrac().getNetworkAction("5569e363-59ab-497a-99b0-c4ae239cbe73").getUsageRules().iterator().next();
        assertTrue(ur1 instanceof OnInstant);
        assertEquals(InstantKind.CURATIVE, ur1.getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ur1.getUsageMethod());

        assertNetworkActionImported(cracCreationContext, "f4e1e04c-0184-42ea-a3cb-75dfe70112f5", Set.of("DDE3AA1  DDE4AA1  1", "BBE1AA1  BBE4AA1  1"), true, 1, "RTE");
        assertEquals("Topo 1 + Topo 2 - CRA x CO1", cracCreationContext.getCrac().getRemedialAction("f4e1e04c-0184-42ea-a3cb-75dfe70112f5").getName());
        UsageRule ur2 = cracCreationContext.getCrac().getNetworkAction("f4e1e04c-0184-42ea-a3cb-75dfe70112f5").getUsageRules().iterator().next();
        assertTrue(ur2 instanceof OnContingencyState);
        assertEquals(InstantKind.CURATIVE, ur2.getInstant().getKind());
        assertEquals(UsageMethod.FORCED, ur2.getUsageMethod());
        assertEquals("co1_fr2_fr3_1", ((OnContingencyState) ur2).getContingency().getId());

        assertNetworkActionImported(cracCreationContext, "7d2833e4-c5a8-4d79-b936-c735a58f1774", Set.of("DDE3AA1  DDE4AA1  1", "BBE1AA1  BBE4AA1  1"), true, 1, "RTE");
        assertEquals("Topo 1 + Topo 2 - CRA x AE1", cracCreationContext.getCrac().getRemedialAction("7d2833e4-c5a8-4d79-b936-c735a58f1774").getName());
        UsageRule ur3 = cracCreationContext.getCrac().getNetworkAction("7d2833e4-c5a8-4d79-b936-c735a58f1774").getUsageRules().iterator().next();
        assertTrue(ur3 instanceof OnFlowConstraint);
        assertEquals(InstantKind.CURATIVE, ur3.getInstant().getKind());
        assertEquals(UsageMethod.FORCED, ur3.getUsageMethod());
        assertEquals("RTE_AE1 (f7708112-b880-4674-98a1-b005a01a61d5) - RTE_CO1 - curative", ((OnFlowConstraint) ur3).getFlowCnec().getId());

        assertNetworkActionImported(cracCreationContext, "66979f64-3c52-486c-84f7-b5439cd71765", Set.of("BBE1AA1  BBE4AA1  1"), true, 1, "RTE");
        assertEquals("Topo 1 - PRA", cracCreationContext.getCrac().getRemedialAction("66979f64-3c52-486c-84f7-b5439cd71765").getName());
        UsageRule ur4 = cracCreationContext.getCrac().getNetworkAction("66979f64-3c52-486c-84f7-b5439cd71765").getUsageRules().iterator().next();
        assertTrue(ur4 instanceof OnInstant);
        assertEquals(InstantKind.PREVENTIVE, ur4.getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ur4.getUsageMethod());
    }

    @Test
    void importGroupedHvdcRemedialActions() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/remedialactions/RemedialActionGroups_HVDC.zip", NETWORK);
        assertNetworkActionImported(cracCreationContext, "hdvc-200-be-de", Set.of("BBE1AA1 _generator", "DDE2AA1 _generator", "BBE1AA1  BBE4AA1  1", "DDE1AA1 _generator", "BBE2AA1 _generator", "DDE3AA1  DDE4AA1  1"), true, 1, "RTE");
        NetworkAction networkAction1 = cracCreationContext.getCrac().getNetworkAction("hdvc-200-be-de");
        assertEquals("HDVC Action - 200 MW BE to DE", networkAction1.getName());
        assertEquals(6, networkAction1.getElementaryActions().size());
        assertTrue(hasTopologicalAction(networkAction1.getElementaryActions(), "BBE1AA1  BBE4AA1  1", ActionType.OPEN));
        assertTrue(hasTopologicalAction(networkAction1.getElementaryActions(), "DDE3AA1  DDE4AA1  1", ActionType.OPEN));
        assertTrue(hasInjectionSetPointAction(networkAction1.getElementaryActions(), "BBE1AA1 _generator", -200));
        assertTrue(hasInjectionSetPointAction(networkAction1.getElementaryActions(), "BBE2AA1 _generator", -200));
        assertTrue(hasInjectionSetPointAction(networkAction1.getElementaryActions(), "DDE1AA1 _generator", 200));
        assertTrue(hasInjectionSetPointAction(networkAction1.getElementaryActions(), "DDE2AA1 _generator", 200));
        UsageRule ur1 = networkAction1.getUsageRules().iterator().next();
        assertEquals(InstantKind.CURATIVE, ur1.getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ur1.getUsageMethod());

        assertNetworkActionImported(cracCreationContext, "hdvc-200-de-be", Set.of("BBE1AA1 _generator", "DDE2AA1 _generator", "BBE1AA1  BBE4AA1  1", "DDE1AA1 _generator", "BBE2AA1 _generator", "DDE3AA1  DDE4AA1  1"), true, 1, "RTE");
        NetworkAction networkAction2 = cracCreationContext.getCrac().getNetworkAction("hdvc-200-de-be");
        assertEquals("HDVC Action - 200 MW DE to BE", networkAction2.getName());
        assertEquals(6, networkAction2.getElementaryActions().size());
        assertTrue(hasTopologicalAction(networkAction2.getElementaryActions(), "BBE1AA1  BBE4AA1  1", ActionType.OPEN));
        assertTrue(hasTopologicalAction(networkAction2.getElementaryActions(), "DDE3AA1  DDE4AA1  1", ActionType.OPEN));
        assertTrue(hasInjectionSetPointAction(networkAction2.getElementaryActions(), "BBE1AA1 _generator", 200));
        assertTrue(hasInjectionSetPointAction(networkAction2.getElementaryActions(), "BBE2AA1 _generator", 200));
        assertTrue(hasInjectionSetPointAction(networkAction2.getElementaryActions(), "DDE1AA1 _generator", -200));
        assertTrue(hasInjectionSetPointAction(networkAction2.getElementaryActions(), "DDE2AA1 _generator", -200));
        UsageRule ur2 = networkAction2.getUsageRules().iterator().next();
        assertEquals(InstantKind.CURATIVE, ur2.getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ur2.getUsageMethod());

        assertNetworkActionImported(cracCreationContext, "hdvc-0", Set.of("BBE1AA1 _generator", "DDE2AA1 _generator", "BBE1AA1  BBE4AA1  1", "DDE1AA1 _generator", "BBE2AA1 _generator", "DDE3AA1  DDE4AA1  1"), true, 1, "RTE");
        NetworkAction networkAction3 = cracCreationContext.getCrac().getNetworkAction("hdvc-0");
        assertEquals("HDVC Action - 0 MW", networkAction3.getName());
        assertEquals(6, networkAction3.getElementaryActions().size());
        assertTrue(hasTopologicalAction(networkAction3.getElementaryActions(), "BBE1AA1  BBE4AA1  1", ActionType.OPEN));
        assertTrue(hasTopologicalAction(networkAction3.getElementaryActions(), "DDE3AA1  DDE4AA1  1", ActionType.OPEN));
        assertTrue(hasInjectionSetPointAction(networkAction3.getElementaryActions(), "BBE1AA1 _generator", 0));
        assertTrue(hasInjectionSetPointAction(networkAction3.getElementaryActions(), "BBE2AA1 _generator", 0));
        assertTrue(hasInjectionSetPointAction(networkAction3.getElementaryActions(), "DDE1AA1 _generator", 0));
        assertTrue(hasInjectionSetPointAction(networkAction3.getElementaryActions(), "DDE2AA1 _generator", 0));
        UsageRule ur3 = networkAction3.getUsageRules().iterator().next();
        assertEquals(InstantKind.CURATIVE, ur3.getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ur3.getUsageMethod());
    }

    private boolean hasInjectionSetPointAction(Set<ElementaryAction> elementaryActions, String elementId, double setpoint) {
        return elementaryActions.stream()
            .filter(InjectionSetpoint.class::isInstance)
            .anyMatch(action -> ((InjectionSetpoint) action).getNetworkElement().getId().equals(elementId) && ((InjectionSetpoint) action).getSetpoint() == setpoint);
    }

    private boolean hasTopologicalAction(Set<ElementaryAction> elementaryActions, String elementId, ActionType actionType) {
        return elementaryActions.stream()
            .filter(TopologicalAction.class::isInstance)
            .anyMatch(action -> ((TopologicalAction) action).getNetworkElement().getId().equals(elementId) && ((TopologicalAction) action).getActionType().equals(actionType));
    }

}
