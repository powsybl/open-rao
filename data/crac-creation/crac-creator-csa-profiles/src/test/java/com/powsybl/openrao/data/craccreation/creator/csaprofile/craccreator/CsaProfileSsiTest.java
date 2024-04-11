/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.ElementaryAction;
import com.powsybl.openrao.data.cracapi.networkaction.InjectionSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.OnAngleConstraint;
import com.powsybl.openrao.data.cracapi.usagerule.OnContingencyState;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.*;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertNetworkActionImported;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet <thomas.bouquet at rte-france.com>
 */
class CsaProfileSsiTest {

    private CsaProfileCracCreationContext cracCreationContext;
    private Crac crac;

    @Test
    void activateDeactivateContingency() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-1_Contingency.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();
        assertEquals(1, crac.getContingencies().size());
        assertContingencyEquality(crac.getContingencies().iterator().next(), "contingency-1", "RTE_CO1", Set.of("FFR1AA1  FFR2AA1  1"));
        assertContingencyNotImported(cracCreationContext, "contingency-2", ImportStatus.NOT_FOR_RAO, "Contingency contingency-2 will not be imported because its field mustStudy is set to false");

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-1_Contingency.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();
        assertEquals(1, crac.getContingencies().size());
        assertContingencyEquality(crac.getContingencies().iterator().next(), "contingency-2", "RTE_CO2", Set.of("FFR1AA1  FFR3AA1  1"));
        assertContingencyNotImported(cracCreationContext, "contingency-1", ImportStatus.NOT_FOR_RAO, "Contingency contingency-1 will not be imported because its field mustStudy is set to false");
    }

    @Test
    void activateDeactivateRemedialAction() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-2_RemedialAction.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action-1", remedialAction.getId());
        assertEquals("RTE_RA1", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        ElementaryAction elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("BBE1AA1  BBE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) elementaryAction).getActionType());

        assertEquals(1, remedialAction.getUsageRules().size());
        assertEquals(crac.getInstant(PREVENTIVE_INSTANT_ID), remedialAction.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, remedialAction.getUsageRules().iterator().next().getUsageMethod());

        assertRaNotImported(cracCreationContext, "remedial-action-2", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-2 will not be imported because normalAvailable is set to false");

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-2_RemedialAction.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action-2", remedialAction.getId());
        assertEquals("RTE_RA2", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("DDE3AA1  DDE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) elementaryAction).getActionType());

        assertEquals(1, remedialAction.getUsageRules().size());
        assertEquals(crac.getInstant(PREVENTIVE_INSTANT_ID), remedialAction.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, remedialAction.getUsageRules().iterator().next().getUsageMethod());

        assertRaNotImported(cracCreationContext, "remedial-action-1", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-1 will not be imported because normalAvailable is set to false");
    }

    @Test
    void changeTopologyActionType() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-3_ChangeTopologicalActionType.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action-1", remedialAction.getId());
        assertEquals("RTE_RA1", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        ElementaryAction elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("BBE1AA1  BBE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) elementaryAction).getActionType());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-3_ChangeTopologicalActionType.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action-1", remedialAction.getId());
        assertEquals("RTE_RA1", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("BBE1AA1  BBE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.CLOSE, ((TopologicalAction) elementaryAction).getActionType());
    }

    @Test
    void restrictPstActionRange() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-4_RestrictPSTActionRange.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getPstRangeActions().size());
        PstRangeAction remedialAction = crac.getPstRangeActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());
        assertEquals("BBE2AA1  BBE3AA1  1", remedialAction.getNetworkElement().getId());
        assertEquals(1, remedialAction.getRanges().size());
        assertEquals(-16, remedialAction.getRanges().iterator().next().getMinTap());
        assertEquals(16, remedialAction.getRanges().iterator().next().getMaxTap());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-4_RestrictPSTActionRange.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getPstRangeActions().size());
        remedialAction = crac.getPstRangeActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());
        assertEquals("BBE2AA1  BBE3AA1  1", remedialAction.getNetworkElement().getId());
        assertEquals(1, remedialAction.getRanges().size());
        assertEquals(-5, remedialAction.getRanges().iterator().next().getMinTap());
        assertEquals(10, remedialAction.getRanges().iterator().next().getMaxTap());
    }

    @Test
    void changeRotatingMachineactionSetpoint() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-5_ChangeRotatingMachineActionSetpoint.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        ElementaryAction elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof InjectionSetpoint);
        assertEquals("FFR1AA1 _generator", ((InjectionSetpoint) elementaryAction).getNetworkElement().getId());
        assertEquals(75d, ((InjectionSetpoint) elementaryAction).getSetpoint());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-5_ChangeRotatingMachineActionSetpoint.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof InjectionSetpoint);
        assertEquals("FFR1AA1 _generator", ((InjectionSetpoint) elementaryAction).getNetworkElement().getId());
        assertEquals(100d, ((InjectionSetpoint) elementaryAction).getSetpoint());
    }

    @Test
    void activateDeactivateTopologyAction() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-6_TopologyAction.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        ElementaryAction elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("BBE1AA1  BBE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) elementaryAction).getActionType());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-6_TopologyAction.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("DDE3AA1  DDE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.CLOSE, ((TopologicalAction) elementaryAction).getActionType());
    }

    @Test
    void activateDeactivateRotatingMachineAction() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-7_RotatingMachineAction.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        ElementaryAction elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof InjectionSetpoint);
        assertEquals("FFR1AA1 _generator", ((InjectionSetpoint) elementaryAction).getNetworkElement().getId());
        assertEquals(75d, ((InjectionSetpoint) elementaryAction).getSetpoint());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-7_RotatingMachineAction.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof InjectionSetpoint);
        assertEquals("FFR2AA1 _generator", ((InjectionSetpoint) elementaryAction).getNetworkElement().getId());
        assertEquals(100d, ((InjectionSetpoint) elementaryAction).getSetpoint());
    }

    @Test
    void activateTapPositionAction() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-8_ActivateTapPositionAction.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(0, crac.getPstRangeActions().size());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-8_ActivateTapPositionAction.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getPstRangeActions().size());
        PstRangeAction remedialAction = crac.getPstRangeActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());
        assertEquals("BBE2AA1  BBE3AA1  1", remedialAction.getNetworkElement().getId());
        assertEquals(1, remedialAction.getRanges().size());
        assertEquals(-4, remedialAction.getRanges().iterator().next().getMinTap());
        assertEquals(8, remedialAction.getRanges().iterator().next().getMaxTap());
    }

    @Test
    void deactivateTapPositionAction() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-9_DeactivateTapPositionAction.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getPstRangeActions().size());
        PstRangeAction remedialAction = crac.getPstRangeActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());
        assertEquals("BBE2AA1  BBE3AA1  1", remedialAction.getNetworkElement().getId());
        assertEquals(1, remedialAction.getRanges().size());
        assertEquals(-4, remedialAction.getRanges().iterator().next().getMinTap());
        assertEquals(8, remedialAction.getRanges().iterator().next().getMaxTap());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-9_DeactivateTapPositionAction.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(0, crac.getPstRangeActions().size());
    }

    @Test
    void activateDeactivateShuntCompensatorModification() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-10_ShuntCompensatorModification.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        ElementaryAction elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof InjectionSetpoint);
        assertEquals("shunt-compensator", ((InjectionSetpoint) elementaryAction).getNetworkElement().getId());
        assertEquals(1, ((InjectionSetpoint) elementaryAction).getSetpoint());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-10_ShuntCompensatorModification.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof InjectionSetpoint);
        assertEquals("shunt-compensator", ((InjectionSetpoint) elementaryAction).getNetworkElement().getId());
        assertEquals(3, ((InjectionSetpoint) elementaryAction).getSetpoint());
    }

    @Test
    void activateDeactivateAllElementaryActions() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-11_ElementaryActions.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action-1", remedialAction.getId());
        assertEquals("RTE_RA1", remedialAction.getName());

        List<ElementaryAction> elementaryActions = remedialAction.getElementaryActions().stream().sorted(Comparator.comparing(ElementaryAction::toString)).toList();
        assertEquals(3, elementaryActions.size());

        assertTrue(elementaryActions.get(0) instanceof InjectionSetpoint);
        assertEquals("FFR1AA1 _generator", ((InjectionSetpoint) elementaryActions.get(0)).getNetworkElement().getId());
        assertEquals(75d, ((InjectionSetpoint) elementaryActions.get(0)).getSetpoint());

        assertTrue(elementaryActions.get(1) instanceof InjectionSetpoint);
        assertEquals("shunt-compensator", ((InjectionSetpoint) elementaryActions.get(1)).getNetworkElement().getId());
        assertEquals(1, ((InjectionSetpoint) elementaryActions.get(1)).getSetpoint());

        assertTrue(elementaryActions.get(2) instanceof TopologicalAction);
        assertEquals("BBE1AA1  BBE4AA1  1", ((TopologicalAction) elementaryActions.get(2)).getNetworkElement().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) elementaryActions.get(2)).getActionType());

        assertRaNotImported(cracCreationContext, "remedial-action-2", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-2 will not be imported because it has no elementary action");

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-11_ElementaryActions.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action-2", remedialAction.getId());
        assertEquals("RTE_RA2", remedialAction.getName());

        elementaryActions = remedialAction.getElementaryActions().stream().sorted(Comparator.comparing(ElementaryAction::toString)).toList();
        assertEquals(3, elementaryActions.size());

        assertTrue(elementaryActions.get(0) instanceof InjectionSetpoint);
        assertEquals("shunt-compensator", ((InjectionSetpoint) elementaryActions.get(0)).getNetworkElement().getId());
        assertEquals(4, ((InjectionSetpoint) elementaryActions.get(0)).getSetpoint());

        assertTrue(elementaryActions.get(1) instanceof InjectionSetpoint);
        assertEquals("FFR2AA1 _generator", ((InjectionSetpoint) elementaryActions.get(1)).getNetworkElement().getId());
        assertEquals(100d, ((InjectionSetpoint) elementaryActions.get(1)).getSetpoint());

        assertTrue(elementaryActions.get(2) instanceof TopologicalAction);
        assertEquals("DDE3AA1  DDE4AA1  1", ((TopologicalAction) elementaryActions.get(2)).getNetworkElement().getId());
        assertEquals(ActionType.CLOSE, ((TopologicalAction) elementaryActions.get(2)).getActionType());

        assertRaNotImported(cracCreationContext, "remedial-action-1", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-1 will not be imported because it has no elementary action");
    }

    @Test
    void activateDeactivateContingencyWithRemedialAction() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-12_ContingencyWithRemedialAction.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        List<Contingency> contingencies = crac.getContingencies().stream().sorted(Comparator.comparing(Contingency::getId)).toList();
        assertEquals(3, contingencies.size());
        assertContingencyEquality(contingencies.get(0), "contingency-1", "RTE_CO1", Set.of("FFR1AA1  FFR2AA1  1"));
        assertContingencyEquality(contingencies.get(1), "contingency-2", "RTE_CO2", Set.of("FFR1AA1  FFR3AA1  1"));
        assertContingencyEquality(contingencies.get(2), "contingency-3", "RTE_CO3", Set.of("FFR1AA1  FFR4AA1  1"));

        List<NetworkAction> networkActions = crac.getNetworkActions().stream().sorted(Comparator.comparing(NetworkAction::getId)).toList();
        assertEquals(2, networkActions.size());

        assertEquals("remedial-action-1", networkActions.get(0).getId());
        assertEquals("RTE_RA1", networkActions.get(0).getName());
        assertEquals(1, networkActions.get(0).getUsageRules().size());
        assertTrue(networkActions.get(0).getUsageRules().iterator().next() instanceof OnContingencyState);
        assertEquals(crac.getInstant(CURATIVE_INSTANT_ID), networkActions.get(0).getUsageRules().iterator().next().getInstant());
        assertEquals("contingency-1", ((OnContingencyState) networkActions.get(0).getUsageRules().iterator().next()).getContingency().getId());

        assertEquals("remedial-action-2", networkActions.get(1).getId());
        assertEquals("RTE_RA2", networkActions.get(1).getName());
        assertEquals(1, networkActions.get(1).getUsageRules().size());
        assertTrue(networkActions.get(1).getUsageRules().iterator().next() instanceof OnContingencyState);
        assertEquals(crac.getInstant(CURATIVE_INSTANT_ID), networkActions.get(1).getUsageRules().iterator().next().getInstant());
        assertEquals("contingency-3", ((OnContingencyState) networkActions.get(1).getUsageRules().iterator().next()).getContingency().getId());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-12_ContingencyWithRemedialAction.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        contingencies = crac.getContingencies().stream().sorted(Comparator.comparing(Contingency::getId)).toList();
        assertEquals(2, contingencies.size());
        assertContingencyEquality(contingencies.get(0), "contingency-1", "RTE_CO1", Set.of("FFR1AA1  FFR2AA1  1"));
        assertContingencyEquality(contingencies.get(1), "contingency-2", "RTE_CO2", Set.of("FFR1AA1  FFR3AA1  1"));
        assertContingencyNotImported(cracCreationContext, "contingency-3", ImportStatus.NOT_FOR_RAO, "Contingency contingency-3 will not be imported because its field mustStudy is set to false");

        networkActions = crac.getNetworkActions().stream().sorted(Comparator.comparing(NetworkAction::getId)).toList();
        assertEquals(2, networkActions.size());

        assertEquals("remedial-action-1", networkActions.get(0).getId());
        assertEquals("RTE_RA1", networkActions.get(0).getName());
        assertEquals(1, networkActions.get(0).getUsageRules().size());
        assertTrue(networkActions.get(0).getUsageRules().iterator().next() instanceof OnContingencyState);
        assertEquals(crac.getInstant(CURATIVE_INSTANT_ID), networkActions.get(0).getUsageRules().iterator().next().getInstant());
        assertEquals("contingency-2", ((OnContingencyState) networkActions.get(0).getUsageRules().iterator().next()).getContingency().getId());

        assertEquals("remedial-action-2", networkActions.get(1).getId());
        assertEquals("RTE_RA2", networkActions.get(1).getName());
        assertEquals(0, networkActions.get(1).getUsageRules().size());
    }

    @Test
    void activateDeactivateAngleCnec() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-13_AngleCNEC.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - preventive"), "RTE_AE1 (assessed-element-1) - preventive", "BBE1AA1 ", "BBE4AA1 ", PREVENTIVE_INSTANT_ID, null, 30d, -30d, "RTE");
        assertCnecNotImported(cracCreationContext, "assessed-element-2", ImportStatus.NOT_FOR_RAO, "AssessedElement.normalEnabled is false");

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-13_AngleCNEC.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - preventive"), "RTE_AE2 (assessed-element-2) - preventive", "BBE4AA1 ", "BBE1AA1 ", PREVENTIVE_INSTANT_ID, null, 45d, -45d, "RTE");
        assertCnecNotImported(cracCreationContext, "assessed-element-1", ImportStatus.NOT_FOR_RAO, "AssessedElement.normalEnabled is false");
    }

    @Test
    void changeAngleCnecThreshold() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-14_VoltageAngleLimit.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE (assessed-element) - preventive"), "RTE_AE (assessed-element) - preventive", "BBE1AA1 ", "BBE4AA1 ", PREVENTIVE_INSTANT_ID, null, 30d, -30d, "RTE");

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-14_VoltageAngleLimit.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE (assessed-element) - preventive"), "RTE_AE (assessed-element) - preventive", "BBE1AA1 ", "BBE4AA1 ", PREVENTIVE_INSTANT_ID, null, 45d, -45d, "RTE");
    }

    @Test
    void activateDeactivateAssessedElementWithContingency() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-15_AssessedElementWithContingency.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(4, crac.getAngleCnecs().size());
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - preventive"), "RTE_AE1 (assessed-element-1) - preventive", "BBE1AA1 ", "BBE4AA1 ", PREVENTIVE_INSTANT_ID, null, 30d, -30d, "RTE");
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - RTE_CO1 - curative"), "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative", "BBE1AA1 ", "BBE4AA1 ", CURATIVE_INSTANT_ID, "contingency-1", 30d, -30d, "RTE");
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - preventive"), "RTE_AE2 (assessed-element-2) - preventive", "BBE4AA1 ", "BBE1AA1 ", PREVENTIVE_INSTANT_ID, null, 45d, -45d, "RTE");
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - RTE_CO3 - curative"), "RTE_AE2 (assessed-element-2) - RTE_CO3 - curative", "BBE4AA1 ", "BBE1AA1 ", CURATIVE_INSTANT_ID, "contingency-3", 45d, -45d, "RTE");
        assertCnecNotImported(cracCreationContext, "assessed-element-1", ImportStatus.NOT_FOR_RAO, "The link between contingency contingency-2 and the assessed element is disabled");

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-15_AssessedElementWithContingency.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(3, crac.getAngleCnecs().size());
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - preventive"), "RTE_AE1 (assessed-element-1) - preventive", "BBE1AA1 ", "BBE4AA1 ", PREVENTIVE_INSTANT_ID, null, 30d, -30d, "RTE");
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - RTE_CO2 - curative"), "RTE_AE1 (assessed-element-1) - RTE_CO2 - curative", "BBE1AA1 ", "BBE4AA1 ", CURATIVE_INSTANT_ID, "contingency-2", 30d, -30d, "RTE");
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - preventive"), "RTE_AE2 (assessed-element-2) - preventive", "BBE4AA1 ", "BBE1AA1 ", PREVENTIVE_INSTANT_ID, null, 45d, -45d, "RTE");
        assertCnecNotImported(cracCreationContext, "assessed-element-1", ImportStatus.NOT_FOR_RAO, "The link between contingency contingency-1 and the assessed element is disabled");
        assertCnecNotImported(cracCreationContext, "assessed-element-2", ImportStatus.INCONSISTENCY_IN_DATA, "The contingency contingency-3 linked to the assessed element does not exist in the CRAC");
    }

    @Test
    void activateDeactivateAssessedElementWithRemedialAction() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-16_AssessedElementWithRemedialAction.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getContingencies().size());
        assertContingencyEquality(crac.getContingencies().iterator().next(), "contingency", "RTE_CO", Set.of("FFR1AA1  FFR2AA1  1"));

        assertEquals(6, crac.getAngleCnecs().size());
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - preventive"), "RTE_AE1 (assessed-element-1) - preventive", "BBE1AA1 ", "BBE4AA1 ", PREVENTIVE_INSTANT_ID, null, 30d, -30d, "RTE");
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - RTE_CO - curative"), "RTE_AE1 (assessed-element-1) - RTE_CO - curative", "BBE1AA1 ", "BBE4AA1 ", CURATIVE_INSTANT_ID, "contingency", 30d, -30d, "RTE");
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - preventive"), "RTE_AE2 (assessed-element-2) - preventive", "BBE4AA1 ", "BBE1AA1 ", PREVENTIVE_INSTANT_ID, null, 45d, -45d, "RTE");
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - RTE_CO - curative"), "RTE_AE2 (assessed-element-2) - RTE_CO - curative", "BBE4AA1 ", "BBE1AA1 ", CURATIVE_INSTANT_ID, "contingency", 45d, -45d, "RTE");
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE3 (assessed-element-3) - preventive"), "RTE_AE3 (assessed-element-3) - preventive", "BBE1AA1 ", "BBE4AA1 ", PREVENTIVE_INSTANT_ID, null, 15d, -15d, "RTE");
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE3 (assessed-element-3) - RTE_CO - curative"), "RTE_AE3 (assessed-element-3) - RTE_CO - curative", "BBE1AA1 ", "BBE4AA1 ", CURATIVE_INSTANT_ID, "contingency", 15d, -15d, "RTE");

        List<NetworkAction> remedialActions = crac.getNetworkActions().stream().sorted(Comparator.comparing(NetworkAction::getId)).toList();
        assertEquals(2, remedialActions.size());

        assertEquals("remedial-action-1", remedialActions.get(0).getId());
        assertEquals("RTE_RA1", remedialActions.get(0).getName());
        assertEquals(1, remedialActions.get(0).getUsageRules().size());
        assertTrue(remedialActions.get(0).getUsageRules().iterator().next() instanceof OnAngleConstraint);
        assertEquals(crac.getInstant(CURATIVE_INSTANT_ID), remedialActions.get(0).getUsageRules().iterator().next().getInstant());
        assertEquals("RTE_AE1 (assessed-element-1) - RTE_CO - curative", ((OnAngleConstraint) remedialActions.get(0).getUsageRules().iterator().next()).getAngleCnec().getId());

        assertEquals("remedial-action-2", remedialActions.get(1).getId());
        assertEquals("RTE_RA2", remedialActions.get(1).getName());
        assertEquals(1, remedialActions.get(1).getUsageRules().size());
        assertTrue(remedialActions.get(1).getUsageRules().iterator().next() instanceof OnAngleConstraint);
        assertEquals(crac.getInstant(CURATIVE_INSTANT_ID), remedialActions.get(1).getUsageRules().iterator().next().getInstant());
        assertEquals("RTE_AE3 (assessed-element-3) - RTE_CO - curative", ((OnAngleConstraint) remedialActions.get(1).getUsageRules().iterator().next()).getAngleCnec().getId());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-16_AssessedElementWithRemedialAction.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getContingencies().size());
        assertContingencyEquality(crac.getContingencies().iterator().next(), "contingency", "RTE_CO", Set.of("FFR1AA1  FFR2AA1  1"));

        assertEquals(4, crac.getAngleCnecs().size());
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - preventive"), "RTE_AE1 (assessed-element-1) - preventive", "BBE1AA1 ", "BBE4AA1 ", PREVENTIVE_INSTANT_ID, null, 30d, -30d, "RTE");
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - RTE_CO - curative"), "RTE_AE1 (assessed-element-1) - RTE_CO - curative", "BBE1AA1 ", "BBE4AA1 ", CURATIVE_INSTANT_ID, "contingency", 30d, -30d, "RTE");
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - preventive"), "RTE_AE2 (assessed-element-2) - preventive", "BBE4AA1 ", "BBE1AA1 ", PREVENTIVE_INSTANT_ID, null, 45d, -45d, "RTE");
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - RTE_CO - curative"), "RTE_AE2 (assessed-element-2) - RTE_CO - curative", "BBE4AA1 ", "BBE1AA1 ", CURATIVE_INSTANT_ID, "contingency", 45d, -45d, "RTE");

        remedialActions = crac.getNetworkActions().stream().sorted(Comparator.comparing(NetworkAction::getId)).toList();
        assertEquals(2, remedialActions.size());

        assertEquals("remedial-action-1", remedialActions.get(0).getId());
        assertEquals("RTE_RA1", remedialActions.get(0).getName());
        assertEquals(1, remedialActions.get(0).getUsageRules().size());
        assertTrue(remedialActions.get(0).getUsageRules().iterator().next() instanceof OnAngleConstraint);
        assertEquals(crac.getInstant(CURATIVE_INSTANT_ID), remedialActions.get(0).getUsageRules().iterator().next().getInstant());
        assertEquals("RTE_AE2 (assessed-element-2) - RTE_CO - curative", ((OnAngleConstraint) remedialActions.get(0).getUsageRules().iterator().next()).getAngleCnec().getId());

        assertEquals("remedial-action-2", remedialActions.get(1).getId());
        assertEquals("RTE_RA2", remedialActions.get(1).getName());
        assertEquals(0, remedialActions.get(1).getUsageRules().size());
    }

    @Test
    void activateDeactivateSchemeRemedialAction() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-17_SchemeRemedialAction.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("ara-1", remedialAction.getId());
        assertEquals("RTE_ARA-1", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        ElementaryAction elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("BBE1AA1  BBE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) elementaryAction).getActionType());

        assertEquals(1, remedialAction.getUsageRules().size());
        assertEquals(crac.getInstant(AUTO_INSTANT_ID), remedialAction.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.FORCED, remedialAction.getUsageRules().iterator().next().getUsageMethod());

        assertRaNotImported(cracCreationContext, "ara-2", ImportStatus.NOT_FOR_RAO, "Remedial action ara-2 will not be imported because normalAvailable is set to false");

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-17_SchemeRemedialAction.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("ara-2", remedialAction.getId());
        assertEquals("RTE_ARA-2", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("DDE3AA1  DDE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) elementaryAction).getActionType());

        assertEquals(1, remedialAction.getUsageRules().size());
        assertEquals(crac.getInstant(AUTO_INSTANT_ID), remedialAction.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.FORCED, remedialAction.getUsageRules().iterator().next().getUsageMethod());

        assertRaNotImported(cracCreationContext, "ara-1", ImportStatus.NOT_FOR_RAO, "Remedial action ara-1 will not be imported because normalAvailable is set to false");
    }

    @Test
    void activateDeactivateRemedialActionScheme() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-18_RemedialActionScheme.zip", NETWORK, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("ara-1", remedialAction.getId());
        assertEquals("RTE_ARA-1", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        ElementaryAction elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("BBE1AA1  BBE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) elementaryAction).getActionType());

        assertEquals(1, remedialAction.getUsageRules().size());
        assertEquals(crac.getInstant(AUTO_INSTANT_ID), remedialAction.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.FORCED, remedialAction.getUsageRules().iterator().next().getUsageMethod());

        assertRaNotImported(cracCreationContext, "ara-2", ImportStatus.NOT_FOR_RAO, "Remedial action ara-2 will not be imported because RemedialActionScheme remedial-action-scheme-2 is not armed");

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-18_RemedialActionScheme.zip", NETWORK, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("ara-2", remedialAction.getId());
        assertEquals("RTE_ARA-2", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("DDE3AA1  DDE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) elementaryAction).getActionType());

        assertEquals(1, remedialAction.getUsageRules().size());
        assertEquals(crac.getInstant(AUTO_INSTANT_ID), remedialAction.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.FORCED, remedialAction.getUsageRules().iterator().next().getUsageMethod());

        assertRaNotImported(cracCreationContext, "ara-1", ImportStatus.NOT_FOR_RAO, "Remedial action ara-1 will not be imported because RemedialActionScheme remedial-action-scheme-1 is not armed");
    }

    @Test
    void activateDeactivateRemedialActionDependency() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-19_RemedialActionDependency.zip", NETWORK, "2023-01-01T22:30Z");
        assertNetworkActionImported(cracCreationContext, "remedial-action-group", Set.of("FFR1AA1 _generator", "BBE1AA1  BBE4AA1  1"), true, 1, "RTE");
        assertEquals("Remedial Action Group", cracCreationContext.getCrac().getRemedialAction("remedial-action-group").getName());
        assertNetworkActionImported(cracCreationContext, "redispatching-action-fr2", Set.of("FFR2AA1 _generator"), false, 1, "RTE");
        assertEquals("RTE_Redispatch -70 MW FR2", cracCreationContext.getCrac().getRemedialAction("redispatching-action-fr2").getName());
        assertEquals("The RemedialActionGroup with mRID remedial-action-group was turned into a remedial action from the following remedial actions: redispatching-action-fr1, topological-action",
            cracCreationContext.getRemedialActionCreationContext("remedial-action-group").getImportStatusDetail());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-19_RemedialActionDependency.zip", NETWORK, "2024-01-31T12:30Z");
        assertNetworkActionImported(cracCreationContext, "remedial-action-group", Set.of("FFR2AA1 _generator", "BBE1AA1  BBE4AA1  1"), true, 1, "RTE");
        assertEquals("Remedial Action Group", cracCreationContext.getCrac().getRemedialAction("remedial-action-group").getName());
        assertNetworkActionImported(cracCreationContext, "redispatching-action-fr1", Set.of("FFR1AA1 _generator"), false, 1, "RTE");
        assertEquals("RTE_Redispatch 70 MW FR1", cracCreationContext.getCrac().getRemedialAction("redispatching-action-fr1").getName());
        assertEquals("The RemedialActionGroup with mRID remedial-action-group was turned into a remedial action from the following remedial actions: redispatching-action-fr2, topological-action",
            cracCreationContext.getRemedialActionCreationContext("remedial-action-group").getImportStatusDetail());
    }

    @Test
    void overrideRemedialActionGroup() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-20_RemedialActionGroup.zip", NETWORK, "2023-01-01T22:30Z");
        assertNetworkActionImported(cracCreationContext, "remedial-action-group", Set.of("BBE1AA1  BBE4AA1  1", "DDE3AA1  DDE4AA1  1"), true, 1, "RTE");
        assertEquals("The RemedialActionGroup with mRID remedial-action-group was turned into a remedial action from the following remedial actions: open-be1-be4, open-de3-de4",
            cracCreationContext.getRemedialActionCreationContext("remedial-action-group").getImportStatusDetail());

        // With SSI 1
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-20_RemedialActionGroup.zip", NETWORK, "2024-01-31T12:30Z");
        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());
        assertEquals("Remedial action group remedial-action-group will not be imported because the remedial action open-be1-be4 does not exist or not imported. All RA's depending in that group will be ignored: open-be1-be4, open-de3-de4",
            cracCreationContext.getRemedialActionCreationContext("remedial-action-group").getImportStatusDetail());
        // With SSI 2
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-20_RemedialActionGroup.zip", NETWORK, "2024-02-01T12:30Z");
        assertEquals(2, cracCreationContext.getCrac().getRemedialActions().size());
        assertEquals("The RemedialActionGroup with mRID remedial-action-group was turned into a remedial action from the following remedial actions: open-de3-de4",
            cracCreationContext.getRemedialActionCreationContext("remedial-action-group").getImportStatusDetail());

        // With SSI 3
        cracCreationContext = getCsaCracCreationContext("/profiles/ssi/SSI-20_RemedialActionGroup.zip", NETWORK, "2024-02-02T12:30Z");
        assertEquals(1, cracCreationContext.getCrac().getRemedialActions().size());
        assertEquals("The RemedialActionGroup with mRID remedial-action-group was turned into a remedial action from the following remedial actions: open-de3-de4",
            cracCreationContext.getRemedialActionCreationContext("remedial-action-group").getImportStatusDetail());
    }
}
