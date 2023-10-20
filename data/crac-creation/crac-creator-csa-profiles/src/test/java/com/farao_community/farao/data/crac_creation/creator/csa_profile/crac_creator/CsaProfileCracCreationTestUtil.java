package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.threshold.Threshold;
import com.farao_community.farao.data.crac_api.usage_rule.*;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action.CsaProfileRemedialActionCreationContext;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CsaProfileCracCreationTestUtil {

    private CsaProfileCracCreationTestUtil() {
    }

    public static void assertContingencyEquality(Contingency c, String expectedContingencyId, String expectedContingencyName, int expectedNetworkElementsSize, List<String> expectedNetworkElementsIds) {
        assertEquals(expectedContingencyId, c.getId());
        assertEquals(expectedContingencyName, c.getName());
        List<NetworkElement> networkElements = c.getNetworkElements().stream()
                .sorted(Comparator.comparing(NetworkElement::getId)).toList();
        assertEquals(expectedNetworkElementsSize, networkElements.size());
        for (int i = 0; i < expectedNetworkElementsSize; i++) {
            assertEquals(expectedNetworkElementsIds.get(i), networkElements.get(i).getId());
        }
    }

    public static void assertFlowCnecEquality(FlowCnec fc, String expectedFlowCnecId, String expectedFlowCnecName, String expectedNetworkElementId,
                                              Instant expectedInstant, String expectedContingencyId, Double expectedThresholdMax, Double expectedThresholdMin, Side expectedThresholdSide) {
        assertEquals(expectedFlowCnecId, fc.getId());
        assertEquals(expectedFlowCnecName, fc.getName());
        assertEquals(expectedNetworkElementId, fc.getNetworkElement().getId());
        assertEquals(expectedInstant, fc.getState().getInstant());
        if (expectedContingencyId == null) {
            assertFalse(fc.getState().getContingency().isPresent());
        } else {
            assertEquals(expectedContingencyId, fc.getState().getContingency().get().getId());
        }

        BranchThreshold threshold = fc.getThresholds().stream().toList().iterator().next();
        assertEquals(expectedThresholdMax, threshold.max().orElse(null));
        assertEquals(expectedThresholdMin, threshold.min().orElse(null));
        assertEquals(Set.of(expectedThresholdSide), fc.getMonitoredSides());
    }

    public static void assertAngleCnecEquality(AngleCnec angleCnec, String expectedFlowCnecId, String expectedFlowCnecName, String expectedImportingNetworkElementId, String expectedExportingNetworkElementId,
                                               Instant expectedInstant, String expectedContingencyId, Double expectedThresholdMax, Double expectedThresholdMin, boolean isMonitored) {
        assertEquals(expectedFlowCnecId, angleCnec.getId());
        assertEquals(expectedFlowCnecName, angleCnec.getName());
        assertEquals(expectedImportingNetworkElementId, angleCnec.getImportingNetworkElement().getId());
        assertEquals(expectedExportingNetworkElementId, angleCnec.getExportingNetworkElement().getId());
        assertEquals(expectedInstant, angleCnec.getState().getInstant());
        if (expectedContingencyId == null) {
            assertFalse(angleCnec.getState().getContingency().isPresent());
        } else {
            assertEquals(expectedContingencyId, angleCnec.getState().getContingency().get().getId());
        }

        Threshold threshold = angleCnec.getThresholds().stream().toList().iterator().next();
        assertEquals(expectedThresholdMax, threshold.max().orElse(null));
        assertEquals(expectedThresholdMin, threshold.min().orElse(null));
        assertEquals(isMonitored, angleCnec.isMonitored());
    }

    public static void assertVoltageCnecEquality(VoltageCnec voltageCnec, String expectedVoltageCnecId, String expectedFlowCnecName, String expectedNetworkElementId,
                                                 Instant expectedInstant, String expectedContingencyId, Double expectedThresholdMax, Double expectedThresholdMin, boolean isMonitored) {
        assertEquals(expectedVoltageCnecId, voltageCnec.getId());
        assertEquals(expectedFlowCnecName, voltageCnec.getName());
        assertEquals(expectedNetworkElementId, voltageCnec.getNetworkElement().getId());
        assertEquals(expectedInstant, voltageCnec.getState().getInstant());
        if (expectedContingencyId == null) {
            assertFalse(voltageCnec.getState().getContingency().isPresent());
        } else {
            assertEquals(expectedContingencyId, voltageCnec.getState().getContingency().get().getId());
        }

        Threshold threshold = voltageCnec.getThresholds().stream().toList().iterator().next();
        assertEquals(expectedThresholdMax, threshold.max().orElse(null));
        assertEquals(expectedThresholdMin, threshold.min().orElse(null));
        assertEquals(isMonitored, voltageCnec.isMonitored());
    }

    public static void assertPstRangeActionImported(CsaProfileCracCreationContext cracCreationContext, String id, String networkElement, boolean isAltered, int numberOfUsageRules) {
        CsaProfileRemedialActionCreationContext remedialActionCreationContext = cracCreationContext.getRemedialActionCreationContext(id);
        assertNotNull(remedialActionCreationContext);
        assertTrue(remedialActionCreationContext.isImported());
        assertEquals(isAltered, remedialActionCreationContext.isAltered());
        assertNotNull(cracCreationContext.getCrac().getPstRangeAction(id));
        String actualNetworkElement = cracCreationContext.getCrac().getPstRangeAction(id).getNetworkElement().toString();
        assertEquals(networkElement, actualNetworkElement);
        assertEquals(numberOfUsageRules, cracCreationContext.getCrac().getPstRangeAction(id).getUsageRules().size());
    }

    public static void assertNetworkActionImported(CsaProfileCracCreationContext cracCreationContext, String id, Set<String> networkElements, boolean isAltered, int numberOfUsageRules) {
        CsaProfileRemedialActionCreationContext remedialActionSeriesCreationContext = cracCreationContext.getRemedialActionCreationContext(id);
        assertNotNull(remedialActionSeriesCreationContext);
        assertTrue(remedialActionSeriesCreationContext.isImported());
        assertEquals(isAltered, remedialActionSeriesCreationContext.isAltered());
        assertNotNull(cracCreationContext.getCrac().getNetworkAction(id));
        Set<String> actualNetworkElements = cracCreationContext.getCrac().getNetworkAction(id).getNetworkElements().stream().map(NetworkElement::getId).collect(Collectors.toSet());
        assertEquals(networkElements, actualNetworkElements);
        assertEquals(numberOfUsageRules, cracCreationContext.getCrac().getNetworkAction(id).getUsageRules().size());
    }

    public static void assertHasOnInstantUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
                cracCreationContext.getCrac().getRemedialAction(raId).getUsageRules().stream().filter(OnInstant.class::isInstance)
                        .map(OnInstant.class::cast)
                        .anyMatch(ur -> ur.getInstant().equals(instant) && ur.getUsageMethod().equals(usageMethod))
        );
    }

    public static void assertHasOnContingencyStateUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, String contingencyId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
                cracCreationContext.getCrac().getRemedialAction(raId).getUsageRules().stream().filter(OnContingencyState.class::isInstance)
                        .map(OnContingencyState.class::cast)
                        .anyMatch(ur -> ur.getContingency().getId().equals(contingencyId) && ur.getInstant().equals(instant) && ur.getUsageMethod().equals(usageMethod))
        );
    }

    public static void assertHasOnFlowConstraintUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, String flowCnecId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
                cracCreationContext.getCrac().getRemedialAction(raId).getUsageRules().stream().filter(OnFlowConstraint.class::isInstance)
                        .map(OnFlowConstraint.class::cast)
                        .anyMatch(ur -> ur.getFlowCnec().getId().equals(flowCnecId) && ur.getInstant().equals(instant) && ur.getUsageMethod().equals(usageMethod))
        );
    }

    public static void assertHasOnAngleConstraintUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, String angleCnecId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
                cracCreationContext.getCrac().getRemedialAction(raId).getUsageRules().stream().filter(OnAngleConstraint.class::isInstance)
                        .map(OnAngleConstraint.class::cast)
                        .anyMatch(ur -> ur.getAngleCnec().getId().equals(angleCnecId) && ur.getInstant().equals(instant) && ur.getUsageMethod().equals(usageMethod))
        );
    }

    public static void assertHasOnVoltageConstraintUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, String voltageCnecId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
                cracCreationContext.getCrac().getRemedialAction(raId).getUsageRules().stream().filter(OnVoltageConstraint.class::isInstance)
                        .map(OnVoltageConstraint.class::cast)
                        .anyMatch(ur -> ur.getVoltageCnec().getId().equals(voltageCnecId) && ur.getInstant().equals(instant) && ur.getUsageMethod().equals(usageMethod))
        );
    }

    public static void assertRaNotImported(CsaProfileCracCreationContext cracCreationContext, String raId, ImportStatus importStatus, String importStatusDetail) {
        CsaProfileRemedialActionCreationContext context = cracCreationContext.getRemedialActionCreationContext(raId);
        assertNotNull(context);
        assertFalse(context.isImported());
        assertEquals(importStatusDetail, context.getImportStatusDetail());
        assertEquals(importStatus, context.getImportStatus());
    }
}
