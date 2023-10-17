/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.InjectionSetpoint;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.TopologicalAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.threshold.Threshold;
import com.farao_community.farao.data.crac_api.usage_rule.*;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec.CsaProfileCnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action.CsaProfileRemedialActionCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.importer.CsaProfileCracImporter;
import com.farao_community.farao.data.crac_impl.OnContingencyStateImpl;
import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.Identifiable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static com.farao_community.farao.data.crac_api.Instant.*;

public class CsaProfileCracCreatorTest {
    private void assertContingencyEquality(Contingency c, String expectedContingencyId, String expectedContingencyName, int expectedNetworkElementsSize, List<String> expectedNetworkElementsIds) {
        assertEquals(expectedContingencyId, c.getId());
        assertEquals(expectedContingencyName, c.getName());
        List<NetworkElement> networkElements = c.getNetworkElements().stream()
                .sorted(Comparator.comparing(NetworkElement::getId)).toList();
        assertEquals(expectedNetworkElementsSize, networkElements.size());
        for (int i = 0; i < expectedNetworkElementsSize; i++) {
            assertEquals(expectedNetworkElementsIds.get(i), networkElements.get(i).getId());
        }
    }

    private void assertFlowCnecEquality(FlowCnec fc, String expectedFlowCnecId, String expectedFlowCnecName, String expectedNetworkElementId,
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

    private void assertAngleCnecEquality(AngleCnec angleCnec, String expectedFlowCnecId, String expectedFlowCnecName, String expectedImportingNetworkElementId, String expectedExportingNetworkElementId,
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

    private void assertVoltageCnecEquality(VoltageCnec voltageCnec, String expectedVoltageCnecId, String expectedFlowCnecName, String expectedNetworkElementId,
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

    private void assertPstRangeActionImported(CsaProfileCracCreationContext cracCreationContext, String id, String networkElement, boolean isAltered, int numberOfUsageRules) {
        CsaProfileRemedialActionCreationContext remedialActionCreationContext = cracCreationContext.getRemedialActionCreationContext(id);
        assertNotNull(remedialActionCreationContext);
        assertTrue(remedialActionCreationContext.isImported());
        assertEquals(isAltered, remedialActionCreationContext.isAltered());
        assertNotNull(cracCreationContext.getCrac().getPstRangeAction(id));
        String actualNetworkElement = cracCreationContext.getCrac().getPstRangeAction(id).getNetworkElement().toString();
        assertEquals(networkElement, actualNetworkElement);
        assertEquals(numberOfUsageRules, cracCreationContext.getCrac().getPstRangeAction(id).getUsageRules().size());
    }

    private void assertNetworkActionImported(CsaProfileCracCreationContext cracCreationContext, String id, Set<String> networkElements, boolean isAltered, int numberOfUsageRules) {
        CsaProfileRemedialActionCreationContext remedialActionSeriesCreationContext = cracCreationContext.getRemedialActionCreationContext(id);
        assertNotNull(remedialActionSeriesCreationContext);
        assertTrue(remedialActionSeriesCreationContext.isImported());
        assertEquals(isAltered, remedialActionSeriesCreationContext.isAltered());
        assertNotNull(cracCreationContext.getCrac().getNetworkAction(id));
        Set<String> actualNetworkElements = cracCreationContext.getCrac().getNetworkAction(id).getNetworkElements().stream().map(NetworkElement::getId).collect(Collectors.toSet());
        assertEquals(networkElements, actualNetworkElements);
        assertEquals(numberOfUsageRules, cracCreationContext.getCrac().getNetworkAction(id).getUsageRules().size());
    }

    private void assertHasOnInstantUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
                cracCreationContext.getCrac().getRemedialAction(raId).getUsageRules().stream().filter(OnInstant.class::isInstance)
                        .map(OnInstant.class::cast)
                        .anyMatch(ur -> ur.getInstant().equals(instant) && ur.getUsageMethod().equals(usageMethod))
        );
    }

    private void assertHasOnContingencyStateUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, String contingencyId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
                cracCreationContext.getCrac().getRemedialAction(raId).getUsageRules().stream().filter(OnContingencyState.class::isInstance)
                        .map(OnContingencyState.class::cast)
                        .anyMatch(ur -> ur.getContingency().getId().equals(contingencyId) && ur.getInstant().equals(instant) && ur.getUsageMethod().equals(usageMethod))
        );
    }

    private void assertHasOnFlowConstraintUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, String flowCnecId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
                cracCreationContext.getCrac().getRemedialAction(raId).getUsageRules().stream().filter(OnFlowConstraint.class::isInstance)
                        .map(OnFlowConstraint.class::cast)
                        .anyMatch(ur -> ur.getFlowCnec().getId().equals(flowCnecId) && ur.getInstant().equals(instant) && ur.getUsageMethod().equals(usageMethod))
        );
    }

    private void assertHasOnAngleConstraintUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, String angleCnecId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
                cracCreationContext.getCrac().getRemedialAction(raId).getUsageRules().stream().filter(OnAngleConstraint.class::isInstance)
                        .map(OnAngleConstraint.class::cast)
                        .anyMatch(ur -> ur.getAngleCnec().getId().equals(angleCnecId) && ur.getInstant().equals(instant) && ur.getUsageMethod().equals(usageMethod))
        );
    }

    private void assertHasOnVoltageConstraintUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, String voltageCnecId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
                cracCreationContext.getCrac().getRemedialAction(raId).getUsageRules().stream().filter(OnVoltageConstraint.class::isInstance)
                        .map(OnVoltageConstraint.class::cast)
                        .anyMatch(ur -> ur.getVoltageCnec().getId().equals(voltageCnecId) && ur.getInstant().equals(instant) && ur.getUsageMethod().equals(usageMethod))
        );
    }

    @Test
    public void testTC1ContingenciesAndFlowCnecs() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/TestConfiguration_TC1_v29Mar2023.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/TestConfiguration_TC1_v29Mar2023.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(1, cracCreationContext.getCreationReport().getReport().size());
        assertEquals(2, cracCreationContext.getCrac().getContingencies().size());
        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
                .stream().sorted(Comparator.comparing(Contingency::getId)).collect(Collectors.toList());

        this.assertContingencyEquality(listContingencies.iterator().next(),
                "493480ba-93c3-426e-bee5-347d8dda3749", "ELIA_CO1",
                1, Arrays.asList("17086487-56ba-4979-b8de-064025a6b4da + 8fdc7abd-3746-481a-a65e-3df56acd8b13"));
        this.assertContingencyEquality(listContingencies.get(1),
                "c0a25fd7-eee0-4191-98a5-71a74469d36e", "TENNET_TSO_CO1",
                1, Arrays.asList("b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc"));

        assertEquals(4, cracCreationContext.getCrac().getFlowCnecs().size());
        List<FlowCnec> listFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).collect(Collectors.toList());

        this.assertFlowCnecEquality(listFlowCnecs.iterator().next(),
                "ELIA_AE1 - ELIA_CO1 - curative",
                "ELIA_AE1 - ELIA_CO1 - curative",
                "ffbabc27-1ccd-4fdc-b037-e341706c8d29",
                CURATIVE, "493480ba-93c3-426e-bee5-347d8dda3749",
                +1312., -1312., Side.LEFT);
        this.assertFlowCnecEquality(listFlowCnecs.get(1),
                "ELIA_AE1 - preventive",
                "ELIA_AE1 - preventive",
                "ffbabc27-1ccd-4fdc-b037-e341706c8d29",
                PREVENTIVE, null,
                +1312., -1312., Side.LEFT);
        this.assertFlowCnecEquality(listFlowCnecs.get(2),
                "TENNET_TSO_AE1NL - TENNET_TSO_CO1 - curative",
                "TENNET_TSO_AE1NL - TENNET_TSO_CO1 - curative",
                "b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc",
                CURATIVE, "c0a25fd7-eee0-4191-98a5-71a74469d36e",
                +1876., -1876., Side.RIGHT);
        this.assertFlowCnecEquality(listFlowCnecs.get(3),
                "TENNET_TSO_AE1NL - preventive",
                "TENNET_TSO_AE1NL - preventive",
                "b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc",
                PREVENTIVE, null,
                +1876., -1876., Side.RIGHT);

        // csa-9-1
        assertTrue(cracCreationContext.getCrac().getNetworkActions().isEmpty());
    }

    @Test
    public void testTC2ContingenciesAndFlowCnecs() {
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/CSA_TestConfiguration_TC2_Draft_v14Apr2023.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/CSA_TestConfiguration_TC2_Draft_v14Apr2023.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(23, cracCreationContext.getCreationReport().getReport().size());
        assertEquals(15, cracCreationContext.getCrac().getContingencies().size());
        assertEquals(12, cracCreationContext.getCrac().getFlowCnecs().size());

        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
                .stream().sorted(Comparator.comparing(Contingency::getId)).collect(Collectors.toList());

        this.assertContingencyEquality(listContingencies.iterator().next(),
                "13334fdf-9cc2-4341-adb6-1281269040b4", "REE_CO3",
                2, Arrays.asList("04566cf8-c766-11e1-8775-005056c00008", "0475dbd8-c766-11e1-8775-005056c00008"));
        this.assertContingencyEquality(listContingencies.get(1),
                "264e9a19-ae28-4c85-a43c-6b7818ca0e6c", "RTE_CO4",
                1, Arrays.asList("536f4b84-db4c-4545-96e9-bb5a87f65d13 + d9622e7f-5bf0-4e7e-b766-b8596c6fe4ae"));
        this.assertContingencyEquality(listContingencies.get(2),
                "37997e71-cb7d-4a8c-baa6-2a1594956da9", "ELIA_CO3",
                1, Arrays.asList("550ebe0d-f2b2-48c1-991f-cebea43a21aa"));
        this.assertContingencyEquality(listContingencies.get(3),
                "475ba18f-cbf5-490b-b65d-e8e03f9bcbc4", "RTE_CO2",
                1, Arrays.asList("e02e1166-1c43-4a4d-8c5a-82298ee0c8f5"));
        this.assertContingencyEquality(listContingencies.get(4),
                "5d587c7e-9ced-416a-ad17-6ef9b241a998", "RTE_CO3",
                1, Arrays.asList("2ab1b800-0c93-4517-86b5-8fd6a3a24ee7"));
        this.assertContingencyEquality(listContingencies.get(5),
                "7e31c67d-67ba-4592-8ac1-9e806d697c8e", "ELIA_CO2",
                1, Arrays.asList("536f4b84-db4c-4545-96e9-bb5a87f65d13 + d9622e7f-5bf0-4e7e-b766-b8596c6fe4ae"));
        this.assertContingencyEquality(listContingencies.get(6),
                "8cdec4c6-10c3-40c1-9eeb-7f6ae8d9b3fe", "REE_CO1",
                1, Arrays.asList("044bbe91-c766-11e1-8775-005056c00008"));
        this.assertContingencyEquality(listContingencies.get(7),
                "96c96ad8-844c-4f3b-8b38-c886ba2c0214", "REE_CO5",
                1, Arrays.asList("891e77ff-39c6-4648-8eda-d81f730271f9 + a04e4e41-c0b4-496e-9ef3-390ea089411f"));
        this.assertContingencyEquality(listContingencies.get(8),
                "9d17b84c-33b5-4a68-b8b9-ed5b31038d40", "REE_CO4",
                2, Arrays.asList("04566cf8-c766-11e1-8775-005056c00008", "0475dbd8-c766-11e1-8775-005056c00008"));
        this.assertContingencyEquality(listContingencies.get(9),
                "b6b780cb-9fe5-4c45-989d-447a927c3874", "REE_CO2",
                1, Arrays.asList("048481d0-c766-11e1-8775-005056c00008"));
        this.assertContingencyEquality(listContingencies.get(10),
                "bd7bb012-f7b9-45e0-9e15-4e2aa3592829", "TENNET_TSO_CO3",
                1, Arrays.asList("9c3b8f97-7972-477d-9dc8-87365cc0ad0e"));
        this.assertContingencyEquality(listContingencies.get(11),
                "ce19dd34-429e-4b72-8813-7615cc57b4a4", "RTE_CO6",
                1, Arrays.asList("04839777-c766-11e1-8775-005056c00008"));
        this.assertContingencyEquality(listContingencies.get(12),
                "d9ef0d5e-732d-441e-9611-c817b0afbc41", "RTE_CO5",
                1, Arrays.asList("f0dee14e-aa43-411e-a2ea-b9879c20f3be"));
        this.assertContingencyEquality(listContingencies.get(13),
                "e05bbe20-9d4a-40da-9777-8424d216785d", "RTE_CO1",
                1, Arrays.asList("f1c13f90-6d89-4a37-a51c-94742ad2dd72"));
        this.assertContingencyEquality(listContingencies.get(14),
                "e9eab3fe-c328-4f78-9bc1-77adb59f6ba7", "ELIA_CO1",
                1, Arrays.asList("dad02278-bd25-476f-8f58-dbe44be72586 + ed0c5d75-4a54-43c8-b782-b20d7431630b"));

        List<FlowCnec> listFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).collect(Collectors.toList());
        // TODO : check flow cnecs
    }

    @Test
    public void testCreateCracCSATestWithRejectedFiles() {
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/CSA_Test_With_Rejected_Files.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/CSA_Test_With_Rejected_Files.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(42, cracCreationContext.getCreationReport().getReport().size());
        assertEquals(7, cracCreationContext.getCrac().getContingencies().size());
        assertEquals(4, cracCreationContext.getCrac().getFlowCnecs().size());
        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
                .stream().sorted(Comparator.comparing(Contingency::getId)).collect(Collectors.toList());

        this.assertContingencyEquality(listContingencies.iterator().next(),
                "264e9a19-ae28-4c85-a43c-6b7818ca0e6c", "RTE_CO4",
                1, Arrays.asList("536f4b84-db4c-4545-96e9-bb5a87f65d13 + d9622e7f-5bf0-4e7e-b766-b8596c6fe4ae"));
        this.assertContingencyEquality(listContingencies.get(1),
                "475ba18f-cbf5-490b-b65d-e8e03f9bcbc4", "RTE_CO2",
                1, Arrays.asList("e02e1166-1c43-4a4d-8c5a-82298ee0c8f5"));
        this.assertContingencyEquality(listContingencies.get(2),
                "5d587c7e-9ced-416a-ad17-6ef9b241a998", "RTE_CO3",
                1, Arrays.asList("2ab1b800-0c93-4517-86b5-8fd6a3a24ee7"));
        this.assertContingencyEquality(listContingencies.get(3),
                "bd7bb012-f7b9-45e0-9e15-4e2aa3592829", "TENNET_TSO_CO3",
                1, Arrays.asList("9c3b8f97-7972-477d-9dc8-87365cc0ad0e"));
        this.assertContingencyEquality(listContingencies.get(4),
                "ce19dd34-429e-4b72-8813-7615cc57b4a4", "RTE_CO6",
                1, Arrays.asList("04839777-c766-11e1-8775-005056c00008"));
        this.assertContingencyEquality(listContingencies.get(5),
                "d9ef0d5e-732d-441e-9611-c817b0afbc41", "RTE_CO5",
                1, Arrays.asList("f0dee14e-aa43-411e-a2ea-b9879c20f3be"));
        this.assertContingencyEquality(listContingencies.get(6),
                "e05bbe20-9d4a-40da-9777-8424d216785d", "RTE_CO1",
                1, Arrays.asList("f1c13f90-6d89-4a37-a51c-94742ad2dd72"));

        List<FlowCnec> listFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).collect(Collectors.toList());
        // TODO : check flow cnecs
    }

    @Test
    public void testCreateCracCSATestWithRefusedContingencies() {
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/Test_With_Refused_Contingencies.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/Test_With_Refused_Contingencies.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(6, cracCreationContext.getCreationReport().getReport().size());
        assertEquals(2, cracCreationContext.getCrac().getContingencies().size());
        assertEquals(4, cracCreationContext.getCrac().getFlowCnecs().size());

        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
                .stream().sorted(Comparator.comparing(Contingency::getId)).collect(Collectors.toList());

        this.assertContingencyEquality(listContingencies.iterator().next(),
                "493480ba-93c3-426e-bee5-347d8dda3749", "ELIA_CO1",
                1, Arrays.asList("17086487-56ba-4979-b8de-064025a6b4da + 8fdc7abd-3746-481a-a65e-3df56acd8b13"));
        this.assertContingencyEquality(listContingencies.get(1),
                "c0a25fd7-eee0-4191-98a5-71a74469d36e", "TENNET_TSO_CO1",
                1, Arrays.asList("b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc"));

        List<FlowCnec> listFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).collect(Collectors.toList());
        // TODO : check flow cnecs
    }

    @Test
    public void testTC2ImportNetworkActions() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_TestConfiguration_TC2_27Apr2023.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_TestConfiguration_TC2_27Apr2023.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-04-27T12:00Z"), new CracCreationParameters());

        assertNotNull(cracCreationContext);
        assertEquals(9, cracCreationContext.getCrac().getNetworkActions().size());
        // RA17 (on instant)
        NetworkAction ra17 = cracCreationContext.getCrac().getNetworkAction("cfabf356-c5e1-4391-b91b-3330bc24f0c9");
        assertEquals("RA17", ra17.getName());
        assertEquals("2db971f1-ed3d-4ea6-acf5-983c4289d51b", ra17.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra17.getElementaryActions().iterator().next()).getActionType());
        assertEquals(PREVENTIVE, ra17.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra17.getUsageRules().iterator().next().getUsageMethod());
        // RA11 (on instant)
        NetworkAction ra11 = cracCreationContext.getCrac().getNetworkAction("b2555ccc-6562-4887-8abc-19a6e51cfe36");
        assertEquals("RA11", ra11.getName());
        assertEquals("86dff3a9-afae-4122-afeb-651f2c01c795", ra11.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra11.getElementaryActions().iterator().next()).getActionType());
        assertEquals(PREVENTIVE, ra11.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra11.getUsageRules().iterator().next().getUsageMethod());
        // RA2 (on instant)
        NetworkAction ra2 = cracCreationContext.getCrac().getNetworkAction("d9bd3aaf-cda3-4b54-bb2e-b03dd9925817");
        assertEquals("RA2", ra2.getName());
        assertEquals(2, ra2.getNetworkElements().size());
        assertTrue(ra2.getElementaryActions().stream().allMatch(TopologicalAction.class::isInstance));
        List<TopologicalAction> topologicalActions = ra2.getElementaryActions().stream().map(TopologicalAction.class::cast).collect(Collectors.toList());
        assertTrue(topologicalActions.stream().anyMatch(action -> action.getNetworkElement().getId().equals("39428c75-098b-4366-861d-2df2a857a805")));
        assertTrue(topologicalActions.stream().anyMatch(action -> action.getNetworkElement().getId().equals("902046a4-40e9-421d-9ef1-9adab0d9d41d")));
        assertTrue(topologicalActions.stream().allMatch(action -> action.getActionType().equals(ActionType.OPEN)));
        assertEquals(PREVENTIVE, ra2.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra2.getUsageRules().iterator().next().getUsageMethod());
        // RA13 (on state)
        NetworkAction ra13 = cracCreationContext.getCrac().getNetworkAction("1fd630a9-b9d8-414b-ac84-b47a093af936");
        assertEquals("RA13", ra13.getName());
        assertEquals(UsageMethod.FORCED, ra13.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra13.getUsageRules().iterator().next().getInstant());
        assertEquals("b6b780cb-9fe5-4c45-989d-447a927c3874", ((OnContingencyStateImpl) ra13.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("52effb0d-091b-4867-a0a2-387109cdad5c", ra13.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra13.getElementaryActions().iterator().next()).getActionType());

        // RA22 (on state)
        NetworkAction ra22 = cracCreationContext.getCrac().getNetworkAction("d856a2a2-3de4-4a7b-aea4-d363c13d9014");
        assertEquals("RA22", ra22.getName());
        assertEquals(UsageMethod.FORCED, ra22.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra22.getUsageRules().iterator().next().getInstant());
        assertEquals("96c96ad8-844c-4f3b-8b38-c886ba2c0214", ((OnContingencyStateImpl) ra22.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("c871da6f-816f-4398-82a4-698550cbee58", ra22.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra22.getElementaryActions().iterator().next()).getActionType());

        // RA14 (on state)
        NetworkAction ra14 = cracCreationContext.getCrac().getNetworkAction("c8bf6b19-1c3b-4ce6-a15c-99995a3c88ce");
        assertEquals("RA14", ra14.getName());
        assertEquals(UsageMethod.FORCED, ra14.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra14.getUsageRules().iterator().next().getInstant());
        assertEquals("13334fdf-9cc2-4341-adb6-1281269040b4", ((OnContingencyStateImpl) ra14.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("88e2e417-fc08-41a7-a711-4c6d0784ac4f", ra14.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra14.getElementaryActions().iterator().next()).getActionType());

        // RA21 (on state)
        NetworkAction ra21 = cracCreationContext.getCrac().getNetworkAction("fb487cc2-0f7b-4958-8f66-1d3fabf7840d");
        assertEquals("RA21", ra21.getName());
        assertEquals(UsageMethod.FORCED, ra21.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra21.getUsageRules().iterator().next().getInstant());
        assertEquals("9d17b84c-33b5-4a68-b8b9-ed5b31038d40", ((OnContingencyStateImpl) ra21.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("65b97d2e-d749-41df-aa8f-0be4629d5e0e", ra21.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra21.getElementaryActions().iterator().next()).getActionType());

        // RA3 (on state)
        NetworkAction ra3 = cracCreationContext.getCrac().getNetworkAction("5e401955-387e-45ce-b126-dd142b06b20c");
        assertEquals("RA3", ra3.getName());
        assertEquals(UsageMethod.FORCED, ra3.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra3.getUsageRules().iterator().next().getInstant());
        assertEquals("475ba18f-cbf5-490b-b65d-e8e03f9bcbc4", ((OnContingencyStateImpl) ra3.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("8e55fb9d-e514-4f4b-8a5d-8fd05b1dc02e", ra3.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra3.getElementaryActions().iterator().next()).getActionType());

        // RA5 (on state)
        NetworkAction ra5 = cracCreationContext.getCrac().getNetworkAction("587cb391-ed16-4a1d-876e-f90241addce5");
        assertEquals("RA5", ra5.getName());
        assertEquals(UsageMethod.FORCED, ra5.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra5.getUsageRules().iterator().next().getInstant());
        assertEquals("5d587c7e-9ced-416a-ad17-6ef9b241a998", ((OnContingencyStateImpl) ra5.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("21f21596-302e-4e0e-8009-2b8c3c23517f", ra5.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra5.getElementaryActions().iterator().next()).getActionType());
    }

    @Test
    public void testImportNetworkActions() {
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getSwitch("switch")).thenReturn(Mockito.mock(Switch.class));
        Branch networkElementMock = Mockito.mock(Branch.class);
        Mockito.when(networkElementMock.getId()).thenReturn("equipment-with-contingency");
        Mockito.when(network.getIdentifiable("equipment-with-contingency")).thenReturn(networkElementMock);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_9_4_ValidProfiles.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        // RA1 (on instant)
        NetworkAction ra1 = cracCreationContext.getCrac().getNetworkAction("on-instant-preventive-topological-action-parent-remedial-action");
        assertEquals("RA1", ra1.getName());
        assertEquals(PREVENTIVE, ra1.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra1.getUsageRules().iterator().next().getUsageMethod());

        // RA2 (on instant)
        NetworkAction ra2 = cracCreationContext.getCrac().getNetworkAction("on-instant-curative-topological-action-parent-remedial-action");
        assertEquals("RA2", ra2.getName());
        assertEquals(CURATIVE, ra2.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra2.getUsageRules().iterator().next().getUsageMethod());

        // RA3 (on state)
        NetworkAction ra3 = cracCreationContext.getCrac().getNetworkAction("on-state-considered-curative-topological-action-parent-remedial-action");
        assertEquals("RA3", ra3.getName());
        assertEquals(CURATIVE, ra3.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra3.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("switch", ra3.getNetworkElements().iterator().next().getId());

        // RA4 (on state)
        NetworkAction ra4 = cracCreationContext.getCrac().getNetworkAction("on-state-included-curative-topological-action-parent-remedial-action");
        assertEquals("RA4", ra4.getName());
        assertEquals(CURATIVE, ra4.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.FORCED, ra4.getUsageRules().iterator().next().getUsageMethod());

        // RA5 (on instant + on instant)
        NetworkAction ra5 = cracCreationContext.getCrac().getNetworkAction("on-state-excluded-curative-topological-action-parent-remedial-action");
        assertEquals("RA5", ra5.getName());
        List<UsageRule> usageRules = ra5.getUsageRules().stream().sorted(Comparator.comparing(UsageRule::getUsageMethod)).collect(Collectors.toList());
        assertEquals(2, usageRules.size());
        assertEquals(CURATIVE, usageRules.iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, usageRules.iterator().next().getUsageMethod());

        assertEquals(CURATIVE, usageRules.get(1).getInstant());
        assertEquals(UsageMethod.UNAVAILABLE, usageRules.get(1).getUsageMethod());
        assertEquals("contingency", ((OnContingencyStateImpl) usageRules.get(1)).getState().getContingency().get().getId());

        // RTE_RA7 (on instant)
        NetworkAction ra7 = cracCreationContext.getCrac().getNetworkAction("topological-action-with-tso-name-parent-remedial-action");
        assertEquals("RTE_RA7", ra7.getName());
        assertEquals("RTE", ra7.getOperator());
        assertEquals(PREVENTIVE, ra7.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra7.getUsageRules().iterator().next().getUsageMethod());

        // nameless-topological-action-with-speed-parent-remedial-action (on instant)
        NetworkAction raNameless = cracCreationContext.getCrac().getNetworkAction("nameless-topological-action-with-speed-parent-remedial-action");
        assertEquals("nameless-topological-action-with-speed-parent-remedial-action", raNameless.getName());
        assertEquals(PREVENTIVE, raNameless.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, raNameless.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(137, raNameless.getSpeed().get());

        // nameless-topological-action-with-tso-name-parent-remedial-action (on instant)
        NetworkAction raNameless2 = cracCreationContext.getCrac().getNetworkAction("nameless-topological-action-with-tso-name-parent-remedial-action");
        assertEquals("nameless-topological-action-with-tso-name-parent-remedial-action", raNameless2.getName());
        assertEquals("RTE", raNameless2.getOperator());
        assertEquals(PREVENTIVE, raNameless2.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, raNameless2.getUsageRules().iterator().next().getUsageMethod());
    }

    @Test
    public void testIgnoreWrongRAKeyword() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_9_5_WrongKeyword.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_9_5_WrongKeyword.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());
    }

    @Test
    public void testIgnoreUnhandledProfile() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_9_6_NotYetValidProfile.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_9_6_NotYetValidProfile.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());
    }

    @Test
    public void testIgnoreOutdatedProfile() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_9_7_OutdatedProfile.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_9_7_OutdatedProfile.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());
    }

    @Test
    public void testIgnoreInvalidNetworkActions() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_9_8_InvalidRemedialActions.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_9_8_InvalidRemedialActions.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());
    }

    @Test
    public void testIgnoreInvalidTopologicalActions() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_9_9_InvalidTopologicalActions.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_9_9_InvalidTopologicalActions.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());
    }

    @Test
    public void testIgnoreInvalidContingenciesWithNetworkActions() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_9_10_InvalidContingenciesWithRemedialActions.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_9_10_InvalidContingenciesWithRemedialActions.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());
    }

    @Test
    public void testImportInjectionSetpointActions() {
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-23/CSA_23_1_ValidProfiles.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();

        Network network = Mockito.mock(Network.class);
        Branch networkElementMock = Mockito.mock(Branch.class);
        Mockito.when(networkElementMock.getId()).thenReturn("equipment-with-contingency");
        Mockito.when(network.getIdentifiable("equipment-with-contingency")).thenReturn(networkElementMock);

        Load loadMock = Mockito.mock(Load.class);
        Mockito.when(loadMock.getId()).thenReturn("rotating-machine");
        Mockito.when(network.getLoadStream()).thenAnswer(invocation -> {
            Stream<Load> loadStream = Stream.of(loadMock);
            Stream<Load> filteredStream = loadStream.filter(load ->
                    load.getId().equals("rotating-machine")
            );
            return filteredStream;
        });

        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(8, cracCreationContext.getCrac().getRemedialActions().size());
        Set<RemedialAction<?>> remedialActions = cracCreationContext.getCrac().getRemedialActions();
        // RA1 (on instant)
        NetworkAction ra1 = cracCreationContext.getCrac().getNetworkAction("on-instant-preventive-remedial-action");
        assertEquals("RA1", ra1.getName());
        assertEquals(PREVENTIVE, ra1.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra1.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) ra1.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(75., ((InjectionSetpoint) ra1.getElementaryActions().iterator().next()).getSetpoint());

        // RA2 (on instant)
        NetworkAction ra2 = cracCreationContext.getCrac().getNetworkAction("on-instant-curative-remedial-action");
        assertEquals("RA2", ra2.getName());
        assertEquals(CURATIVE, ra2.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra2.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) ra2.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(17.3, ((InjectionSetpoint) ra2.getElementaryActions().iterator().next()).getSetpoint(), 0.1);

        // on-instant-preventive-nameless-remedial-action-with-speed (on instant)
        NetworkAction namelessRa = cracCreationContext.getCrac().getNetworkAction("on-instant-preventive-nameless-remedial-action-with-speed");
        assertEquals("on-instant-preventive-nameless-remedial-action-with-speed", namelessRa.getName());
        assertEquals(PREVENTIVE, namelessRa.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, namelessRa.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) namelessRa.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(22.4, ((InjectionSetpoint) namelessRa.getElementaryActions().iterator().next()).getSetpoint(), 0.1);
        assertEquals(137, namelessRa.getSpeed().get());

        // RTE_RA7 (on instant)
        NetworkAction ra7 = cracCreationContext.getCrac().getNetworkAction("on-instant-preventive-remedial-with-tso-name");
        assertEquals("RTE_RA7", ra7.getName());
        assertEquals(PREVENTIVE, ra7.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra7.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) ra7.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(100., ((InjectionSetpoint) ra7.getElementaryActions().iterator().next()).getSetpoint(), 0.1);
        assertEquals("RTE", ra7.getOperator());

        // on-instant-nameless-preventive-remedial-with-tso-name (on instant)
        NetworkAction namelessRa2 = cracCreationContext.getCrac().getNetworkAction("on-instant-nameless-preventive-remedial-with-tso-name");
        assertEquals("on-instant-nameless-preventive-remedial-with-tso-name", namelessRa2.getName());
        assertEquals(PREVENTIVE, namelessRa2.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, namelessRa2.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) namelessRa2.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(98., ((InjectionSetpoint) namelessRa2.getElementaryActions().iterator().next()).getSetpoint(), 0.1);
        assertEquals("RTE", ra7.getOperator());

        // on-state-included-curative-remedial-action (on state)
        NetworkAction ra3 = cracCreationContext.getCrac().getNetworkAction("on-state-included-curative-remedial-action");
        assertEquals("RA3", ra3.getName());
        assertEquals(UsageMethod.FORCED, ra3.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra3.getUsageRules().iterator().next().getInstant());
        assertEquals("contingency", ((OnContingencyStateImpl) ra3.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("rotating-machine", ra3.getNetworkElements().iterator().next().getId());
        assertEquals(2.8, ((InjectionSetpoint) ra3.getElementaryActions().iterator().next()).getSetpoint(), 0.1);

        // on-state-considered-curative-remedial-action (on state)
        NetworkAction ra4 = cracCreationContext.getCrac().getNetworkAction("on-state-considered-curative-remedial-action");
        assertEquals("RA4", ra4.getName());
        assertEquals(UsageMethod.AVAILABLE, ra4.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra4.getUsageRules().iterator().next().getInstant());
        assertEquals("contingency", ((OnContingencyStateImpl) ra4.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("rotating-machine", ra4.getNetworkElements().iterator().next().getId());
        assertEquals(15.6, ((InjectionSetpoint) ra4.getElementaryActions().iterator().next()).getSetpoint(), 0.1);

        // on-state-excluded-curative-remedial-action (on state + on instant)
        NetworkAction ra5 = cracCreationContext.getCrac().getNetworkAction("on-state-excluded-curative-remedial-action");
        assertEquals("RA5", ra5.getName());
        List<UsageRule> usageRules = ra5.getUsageRules().stream().sorted(Comparator.comparing(UsageRule::getUsageMethod)).collect(Collectors.toList());
        assertEquals(2, usageRules.size());
        assertTrue(ra5.getUsageRules().stream().map(UsageRule::getInstant).allMatch(i -> i.equals(CURATIVE)));
        assertEquals(UsageMethod.AVAILABLE, usageRules.iterator().next().getUsageMethod());
        assertEquals(UsageMethod.UNAVAILABLE, usageRules.get(1).getUsageMethod());
        assertEquals("contingency", ((OnContingencyStateImpl) usageRules.get(1)).getState().getContingency().get().getId());
        assertEquals("rotating-machine", ra5.getNetworkElements().iterator().next().getId());
        assertEquals(25.7, ((InjectionSetpoint) ra5.getElementaryActions().iterator().next()).getSetpoint(), 0.1);
    }

    @Test
    public void testIgnoreInvalidInjectionSetpointProfile() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-23/CSA_23_2_InvalidProfiles.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-23/CSA_23_2_InvalidProfiles.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);
        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());
        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());
    }

    /* // TODO check with thomas to change use case after behaviour change
    @Test
    public void testTC1ImportPstRangeActions() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/TestConfiguration_TC1_v29Mar2023.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/TestConfiguration_TC1_v29Mar2023.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        // ELIA_RA1 (on instant)
        PstRangeAction eliaRa1 = cracCreationContext.getCrac().getPstRangeAction("7fc2fc14-eea6-4e69-b8d9-a3edc218e687");
        assertEquals("ELIA_RA1", eliaRa1.getName());
        assertEquals("ELIA", eliaRa1.getOperator());
        assertEquals("36b83adb-3d45-4693-8967-96627b5f9ec9", eliaRa1.getNetworkElement().getId());
        assertEquals(10, eliaRa1.getInitialTap());
        assertEquals(1, eliaRa1.getRanges().size());
        assertEquals(5., eliaRa1.getRanges().iterator().next().getMinTap());
        assertEquals(20., eliaRa1.getRanges().iterator().next().getMaxTap());
        assertEquals(1, eliaRa1.getUsageRules().size());
        assertEquals(CURATIVE, eliaRa1.getUsageRules().iterator().next().getInstant());
        assertEquals("493480ba-93c3-426e-bee5-347d8dda3749", ((OnContingencyStateImpl) eliaRa1.getUsageRules().iterator().next()).getState().getContingency().get().getId());
        Map<Integer, Double> expectedTapToAngleMap = Map.ofEntries(
                Map.entry(1, 4.926567934889113),
                Map.entry(2, 4.4625049779277965),
                Map.entry(3, 4.009142308337196),
                Map.entry(4, 3.5661689080738133),
                Map.entry(5, 3.133282879390916),
                Map.entry(6, 2.7101913084587235),
                Map.entry(7, 2.296610111393503),
                Map.entry(8, 1.892263865774221),
                Map.entry(9, 1.496885630374893),
                Map.entry(10, 1.1102167555229658),
                Map.entry(11, 0.7320066862066437),
                Map.entry(12, 0.36201275979482317),
                Map.entry(13, -0.0),
                Map.entry(14, -0.3542590914949466),
                Map.entry(15, -0.7009847445128217),
                Map.entry(16, -1.040390129895497),
                Map.entry(17, -1.3726815681386877),
                Map.entry(18, -1.698058736365395),
                Map.entry(19, -2.016714872973585),
                Map.entry(20, -2.32883697939856),
                Map.entry(21, -2.6346060185232267),
                Map.entry(22, -2.9341971093513304),
                Map.entry(23, -3.227779717630807),
                Map.entry(24, -3.515517842177712),
                Map.entry(25, -3.797570196706609)
        );
        assertEquals(expectedTapToAngleMap, eliaRa1.getTapToAngleConversionMap());
    }*/

    @Test
    public void testTC2ImportPstRangeActions() {
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/CSA_TestConfiguration_TC2_Draft_v14Apr2023.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/CSA_TestConfiguration_TC2_Draft_v14Apr2023.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        PstRangeAction reeRa1 = cracCreationContext.getCrac().getPstRangeAction("5898c268-9b32-4ab5-9cfc-64546135a337");
        assertEquals("RA1", reeRa1.getName());
        assertEquals("f6e8823f-d431-6fc7-37cf-b7a0d80035dd", reeRa1.getNetworkElement().getId());
        assertEquals(13, reeRa1.getInitialTap());
        assertEquals(0, reeRa1.getRanges().size());
        assertEquals(1, reeRa1.getUsageRules().size());
        assertEquals(CURATIVE, reeRa1.getUsageRules().iterator().next().getInstant());
        assertEquals("8cdec4c6-10c3-40c1-9eeb-7f6ae8d9b3fe", ((OnContingencyStateImpl) reeRa1.getUsageRules().iterator().next()).getState().getContingency().get().getId());
        Map<Integer, Double> expectedTapToAngleMap = Map.ofEntries(
                Map.entry(-1, -2.0),
                Map.entry(0, 0.0),
                Map.entry(-2, -4.0),
                Map.entry(1, 2.0),
                Map.entry(-3, -6.0),
                Map.entry(2, 4.0),
                Map.entry(-4, -8.0),
                Map.entry(3, 6.0),
                Map.entry(-5, -10.0),
                Map.entry(4, 8.0),
                Map.entry(-6, -12.0),
                Map.entry(5, 10.0),
                Map.entry(-7, -14.0),
                Map.entry(6, 12.0),
                Map.entry(-8, -16.0),
                Map.entry(7, 14.0),
                Map.entry(-9, -18.0),
                Map.entry(8, 16.0),
                Map.entry(-10, -20.0),
                Map.entry(9, 18.0),
                Map.entry(-11, -22.0),
                Map.entry(10, 20.0),
                Map.entry(-12, -24.0),
                Map.entry(11, 22.0),
                Map.entry(-13, -26.0),
                Map.entry(12, 24.0),
                Map.entry(-14, -28.0),
                Map.entry(13, 26.0),
                Map.entry(-15, -30.0),
                Map.entry(14, 28.0),
                Map.entry(-16, -32.0),
                Map.entry(15, 30.0),
                Map.entry(-17, -34.0),
                Map.entry(16, 32.0),
                Map.entry(-18, -36.0),
                Map.entry(17, 34.0),
                Map.entry(-19, -38.0),
                Map.entry(18, 36.0),
                Map.entry(-20, -40.0),
                Map.entry(19, 38.0),
                Map.entry(20, 40.0)
        );
        assertEquals(expectedTapToAngleMap, reeRa1.getTapToAngleConversionMap());

        assertEquals(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, cracCreationContext.getRemedialActionCreationContexts().stream().filter(ra -> ra.getNativeId().equals("5e5ff13e-2043-4468-9351-01920d3d9504")).findAny().get().getImportStatus());
        assertEquals(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, cracCreationContext.getRemedialActionCreationContexts().stream().filter(ra -> ra.getNativeId().equals("2e4f4212-7b30-4316-9fce-ca618f2a8a05")).findAny().get().getImportStatus());
    }

    @Test
    public void testCustomImportCase() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/TestCase_13_5_4.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/TestCase_13_5_4.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());
        Crac importedCrac = cracCreationContext.getCrac();

        assertTrue(cracCreationContext.isCreationSuccessful());

        // Check contingencies
        assertEquals(1, importedCrac.getContingencies().size());
        assertContingencyEquality(importedCrac.getContingencies().iterator().next(), "co1_fr2_fr3_1", "RTE_co1_fr2_fr3_1", 1, List.of("FFR2AA1--FFR3AA1--1"));

        // Check Flow Cnecs
        assertEquals(6, importedCrac.getFlowCnecs().size());
        assertFlowCnecEquality(importedCrac.getFlowCnec("RTE_FFR2AA1--FFR3AA1--2 - RTE_co1_fr2_fr3_1 - curative"), "RTE_FFR2AA1--FFR3AA1--2 - RTE_co1_fr2_fr3_1 - curative", "RTE_FFR2AA1--FFR3AA1--2 - RTE_co1_fr2_fr3_1 - curative",
                "FFR2AA1--FFR3AA1--2", CURATIVE, "co1_fr2_fr3_1", 2500., -2500., Side.RIGHT);
        assertFlowCnecEquality(importedCrac.getFlowCnec("RTE_FFR3AA1--FFR5AA1--1 - RTE_co1_fr2_fr3_1 - outage"), "RTE_FFR3AA1--FFR5AA1--1 - RTE_co1_fr2_fr3_1 - outage", "RTE_FFR3AA1--FFR5AA1--1 - RTE_co1_fr2_fr3_1 - outage",
                "FFR3AA1--FFR5AA1--1", OUTAGE, "co1_fr2_fr3_1", 1500., -1500., Side.RIGHT);
        assertFlowCnecEquality(importedCrac.getFlowCnec("RTE_FFR2AA1--DDE3AA1--1 - preventive"), "RTE_FFR2AA1--DDE3AA1--1 - preventive", "RTE_FFR2AA1--DDE3AA1--1 - preventive",
                "FFR2AA1--DDE3AA1--1", PREVENTIVE, null, 1000., -1000., Side.RIGHT);
        assertFlowCnecEquality(importedCrac.getFlowCnec("RTE_FFR3AA1--FFR5AA1--1 - RTE_co1_fr2_fr3_1 - curative"), "RTE_FFR3AA1--FFR5AA1--1 - RTE_co1_fr2_fr3_1 - curative", "RTE_FFR3AA1--FFR5AA1--1 - RTE_co1_fr2_fr3_1 - curative",
                "FFR3AA1--FFR5AA1--1", CURATIVE, "co1_fr2_fr3_1", 1000., -1000., Side.RIGHT);
        assertFlowCnecEquality(importedCrac.getFlowCnec("TENNET_TSO_NNL2AA1--BBE3AA1--1 - preventive"), "TENNET_TSO_NNL2AA1--BBE3AA1--1 - preventive", "TENNET_TSO_NNL2AA1--BBE3AA1--1 - preventive",
                "NNL2AA1--BBE3AA1--1", PREVENTIVE, null, 5000., -5000., Side.RIGHT);
        assertFlowCnecEquality(importedCrac.getFlowCnec("RTE_FFR2AA1--DDE3AA1--1 - RTE_co1_fr2_fr3_1 - outage"), "RTE_FFR2AA1--DDE3AA1--1 - RTE_co1_fr2_fr3_1 - outage", "RTE_FFR2AA1--DDE3AA1--1 - RTE_co1_fr2_fr3_1 - outage",
                "FFR2AA1--DDE3AA1--1", OUTAGE, "co1_fr2_fr3_1", 1200., -1200., Side.RIGHT);

        // Check PST RAs
        assertPstRangeActionImported(cracCreationContext, "pst_be", "BBE2AA1--BBE3AA1--1", false, 1);
        assertHasOnInstantUsageRule(cracCreationContext, "pst_be", CURATIVE, UsageMethod.AVAILABLE);
        assertPstRangeActionImported(cracCreationContext, "pst_fr_cra", "FFR2AA1--FFR4AA1--1", false, 1);
        assertHasOnInstantUsageRule(cracCreationContext, "pst_fr_cra", CURATIVE, UsageMethod.AVAILABLE);
        assertPstRangeActionImported(cracCreationContext, "pst_fr_pra", "FFR2AA1--FFR4AA1--1", false, 1);
        assertHasOnInstantUsageRule(cracCreationContext, "pst_fr_pra", PREVENTIVE, UsageMethod.AVAILABLE);

        // Check topo RAs
        assertNetworkActionImported(cracCreationContext, "close_fr1_fr5", Set.of("FFR1AA1Z-FFR1AA1--1"), false, 1);
        assertHasOnInstantUsageRule(cracCreationContext, "close_fr1_fr5", CURATIVE, UsageMethod.AVAILABLE);
        assertNetworkActionImported(cracCreationContext, "open_fr1_fr2", Set.of("FFR1AA1Y-FFR1AA1--1"), false, 1);
        assertHasOnInstantUsageRule(cracCreationContext, "open_fr1_fr2", PREVENTIVE, UsageMethod.AVAILABLE);
        assertNetworkActionImported(cracCreationContext, "open_fr1_fr3", Set.of("FFR1AA1X-FFR1AA1--1"), false, 1);
        assertHasOnInstantUsageRule(cracCreationContext, "open_fr1_fr3", PREVENTIVE, UsageMethod.AVAILABLE);
    }

    @Test
    public void testCustomForAssessedElementWithContingencyRejection() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/CSA_42_CustomExample.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/CSA_42_CustomExample.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(4, cracCreationContext.getCrac().getFlowCnecs().size());
        List<FlowCnec> listFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).collect(Collectors.toList());

        this.assertFlowCnecEquality(listFlowCnecs.iterator().next(),
                "RTE_AE - RTE_CO1 - curative",
                "RTE_AE - RTE_CO1 - curative",
                "FFR3AA1--FFR5AA1--1",
                CURATIVE, "0451f8be-83d7-45da-b80b-4014259ff624",
                +1000., -1000., Side.RIGHT);
        this.assertFlowCnecEquality(listFlowCnecs.get(1),
                "RTE_AE - RTE_CO3 - curative",
                "RTE_AE - RTE_CO3 - curative",
                "FFR3AA1--FFR5AA1--1",
                CURATIVE, "4491d904-93c4-41d4-a509-57f9fed2e31c",
                +1000., -1000., Side.RIGHT);
        this.assertFlowCnecEquality(listFlowCnecs.get(2),
                "RTE_AE - preventive",
                "RTE_AE - preventive",
                "FFR3AA1--FFR5AA1--1",
                PREVENTIVE, null,
                +1000., -1000., Side.RIGHT);
        this.assertFlowCnecEquality(listFlowCnecs.get(3),
                "RTE_AE2 - preventive",
                "RTE_AE2 - preventive",
                "FFR3AA1--FFR5AA1--1",
                PREVENTIVE, null,
                +1000., -1000., Side.RIGHT);
    }

    @Test
    void testAngleCnecImportFromValidProfiles() {
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-13/CSA_13_3_ValidProfiles.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();

        Network network = Mockito.mock(Network.class);
        BusbarSection terminal1Mock = Mockito.mock(BusbarSection.class);
        BusbarSection terminal2Mock = Mockito.mock(BusbarSection.class);
        Switch switchMock = Mockito.mock(Switch.class);
        Branch networkElementMock = Mockito.mock(Branch.class);

        Mockito.when(terminal1Mock.getId()).thenReturn("bdfd51d2-f48a-424e-a42d-0f6e712094bb");
        Mockito.when(terminal2Mock.getId()).thenReturn("601ac88b-14bc-448a-b8a7-e0b8874a478d");
        Mockito.when(terminal1Mock.getType()).thenReturn(IdentifiableType.BUS);
        Mockito.when(terminal2Mock.getType()).thenReturn(IdentifiableType.BUS);
        Mockito.when(switchMock.getId()).thenReturn("40ed5398-3a74-4581-a3c1-688f9764a2b5");
        Mockito.when(networkElementMock.getId()).thenReturn("1bac939d-d873-48e0-9640-5743f389f3de");
        Mockito.when(switchMock.isOpen()).thenReturn(false);
        Mockito.when(network.getIdentifiable("bdfd51d2-f48a-424e-a42d-0f6e712094bb")).thenReturn((Identifiable) terminal1Mock);
        Mockito.when(network.getIdentifiable("601ac88b-14bc-448a-b8a7-e0b8874a478d")).thenReturn((Identifiable) terminal2Mock);
        Mockito.when(network.getIdentifiable("40ed5398-3a74-4581-a3c1-688f9764a2b5")).thenReturn((Identifiable) switchMock);
        Mockito.when(network.getIdentifiable("1bac939d-d873-48e0-9640-5743f389f3de")).thenReturn(networkElementMock);

        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(4, cracCreationContext.getCrac().getAngleCnecs().size());
        List<AngleCnec> angleCnecs = cracCreationContext.getCrac().getAngleCnecs().stream()
                .sorted(Comparator.comparing(AngleCnec::getId)).toList();

        // RTE_AE1 - preventive
        AngleCnec angleCnec1 = angleCnecs.iterator().next();
        assertEquals("RTE_AE1 - preventive", angleCnec1.getId());
        assertEquals("RTE_AE1 - preventive", angleCnec1.getName());
        assertEquals("601ac88b-14bc-448a-b8a7-e0b8874a478d", angleCnec1.getImportingNetworkElement().getId());
        assertEquals("bdfd51d2-f48a-424e-a42d-0f6e712094bb", angleCnec1.getExportingNetworkElement().getId());
        assertTrue(angleCnec1.getLowerBound(Unit.DEGREE).isPresent());
        assertEquals(-60.0, angleCnec1.getLowerBound(Unit.DEGREE).get());
        assertFalse(angleCnec1.getUpperBound(Unit.DEGREE).isPresent());

        // RTE_AE2 - RTE_CO1 - curative
        AngleCnec angleCnec2 = angleCnecs.get(1);
        assertEquals("RTE_AE2 - RTE_CO1 - curative", angleCnec2.getId());
        assertEquals("RTE_AE2 - RTE_CO1 - curative", angleCnec2.getName());
        assertEquals("bdfd51d2-f48a-424e-a42d-0f6e712094bb", angleCnec2.getImportingNetworkElement().getId());
        assertEquals("601ac88b-14bc-448a-b8a7-e0b8874a478d", angleCnec2.getExportingNetworkElement().getId());
        assertFalse(angleCnec2.getLowerBound(Unit.DEGREE).isPresent());
        assertTrue(angleCnec2.getUpperBound(Unit.DEGREE).isPresent());
        assertEquals(35.0, angleCnec2.getUpperBound(Unit.DEGREE).get());

        // RTE_AE2 - preventive
        AngleCnec angleCnec3 = angleCnecs.get(2);
        assertEquals("RTE_AE2 - preventive", angleCnec3.getId());
        assertEquals("RTE_AE2 - preventive", angleCnec3.getName());
        assertEquals("bdfd51d2-f48a-424e-a42d-0f6e712094bb", angleCnec3.getImportingNetworkElement().getId());
        assertEquals("601ac88b-14bc-448a-b8a7-e0b8874a478d", angleCnec3.getExportingNetworkElement().getId());
        assertFalse(angleCnec3.getLowerBound(Unit.DEGREE).isPresent());
        assertTrue(angleCnec3.getUpperBound(Unit.DEGREE).isPresent());
        assertEquals(35.0, angleCnec3.getUpperBound(Unit.DEGREE).get());

        // RTE_AE3 - RTE_CO1 - curative
        AngleCnec angleCnec4 = angleCnecs.get(3);
        assertEquals("RTE_AE3 - RTE_CO1 - curative", angleCnec4.getId());
        assertEquals("RTE_AE3 - RTE_CO1 - curative", angleCnec4.getName());
        assertEquals("601ac88b-14bc-448a-b8a7-e0b8874a478d", angleCnec4.getImportingNetworkElement().getId());
        assertEquals("bdfd51d2-f48a-424e-a42d-0f6e712094bb", angleCnec4.getExportingNetworkElement().getId());
        assertTrue(angleCnec4.getLowerBound(Unit.DEGREE).isPresent());
        assertEquals(-120.0, angleCnec4.getLowerBound(Unit.DEGREE).get());
        assertTrue(angleCnec4.getUpperBound(Unit.DEGREE).isPresent());
        assertEquals(120.0, angleCnec4.getUpperBound(Unit.DEGREE).get());

        // TODO: add onAngleConstraint usage rules checks when CSA-11 is merged
        // TODO: add ER profile with wrong header
    }

    @Test
    void testAngleCnecImportFromInvalidProfiles() {
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-13/CSA_13_4_InvalidProfiles.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();

        Network network = Mockito.mock(Network.class);
        BusbarSection terminal1Mock = Mockito.mock(BusbarSection.class);
        BusbarSection terminal2Mock = Mockito.mock(BusbarSection.class);
        Mockito.when(terminal1Mock.getId()).thenReturn("7ce8103f-e4d4-4f1a-94a0-ffaf76049e38");
        Mockito.when(terminal2Mock.getId()).thenReturn("008952f4-0b93-4622-af28-49934dde3db3");
        Mockito.when(terminal1Mock.getType()).thenReturn(IdentifiableType.BUS);
        Mockito.when(terminal2Mock.getType()).thenReturn(IdentifiableType.BUS);
        Mockito.when(network.getIdentifiable("7ce8103f-e4d4-4f1a-94a0-ffaf76049e38")).thenReturn((Identifiable) terminal1Mock);
        Mockito.when(network.getIdentifiable("008952f4-0b93-4622-af28-49934dde3db3")).thenReturn((Identifiable) terminal2Mock);

        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());
        assertEquals(0, cracCreationContext.getCrac().getAngleCnecs().size());

        List<CsaProfileCnecCreationContext> cnecCreationContexts = cracCreationContext.getCnecCreationContexts().stream()
                .sorted(Comparator.comparing(CsaProfileCnecCreationContext::getNativeId)).toList();
        assertEquals(6, cnecCreationContexts.size());

        // Missing AngleReferenceTerminal
        assertEquals("61f31133-5d71-4d60-bc17-70bed6610101", cnecCreationContexts.iterator().next().getNativeId());
        assertEquals("angle limit equipment is missing in network : eb090246-2037-481f-baba-36ab347ff119", cnecCreationContexts.iterator().next().getImportStatusDetail());

        // Importing and exporting network elements are the same terminal
        assertEquals("690b90c4-892c-4638-a083-6cf8e8e1cfc2", cnecCreationContexts.get(1).getNativeId());
        assertEquals("AngleCNEC's importing and exporting equipments are the same : 7ce8103f-e4d4-4f1a-94a0-ffaf76049e38", cnecCreationContexts.get(1).getImportStatusDetail());

        // Negative normal value
        assertEquals("c2657640-ff0a-4026-9b18-0e745647ceb0", cnecCreationContexts.get(2).getNativeId());
        assertEquals("angle limit's normal value is negative", cnecCreationContexts.get(2).getImportStatusDetail());

        // Undefined VoltageAngleLimit.isFlowToRefTerminal + OperationalLimitType.direction HIGH
        assertEquals("ca931f31-1f48-43bd-9ff4-59d5701d6552", cnecCreationContexts.get(3).getNativeId());
        assertEquals("ambiguous angle limit direction definition from an undefined VoltageAngleLimit.isFlowToRefTerminal and an OperationalLimit.OperationalLimitType : http://iec.ch/TC57/CIM100#OperationalLimitDirectionKind.high", cnecCreationContexts.get(3).getImportStatusDetail());

        // Undefined VoltageAngleLimit.isFlowToRefTerminal + OperationalLimitType.direction LOW
        assertEquals("e7ce6d03-dd09-4390-8ea8-5e26bf56c009", cnecCreationContexts.get(4).getNativeId());
        assertEquals("ambiguous angle limit direction definition from an undefined VoltageAngleLimit.isFlowToRefTerminal and an OperationalLimit.OperationalLimitType : http://iec.ch/TC57/CIM100#OperationalLimitDirectionKind.low", cnecCreationContexts.get(4).getImportStatusDetail());

        // Missing OperationalLimitSet.Terminal
        assertEquals("eaff2f9c-3fcd-41a3-ac11-79d89bf3a393", cnecCreationContexts.get(5).getNativeId());
        assertEquals("angle limit equipment is missing in network : eb090246-2037-481f-baba-36ab347ff119", cnecCreationContexts.get(5).getImportStatusDetail());
    }

}
