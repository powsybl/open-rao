package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec;

import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreator;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.importer.CsaProfileCracImporter;
import com.powsybl.iidm.network.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Set;

import static com.farao_community.farao.data.crac_api.Instant.CURATIVE;
import static com.farao_community.farao.data.crac_api.Instant.PREVENTIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VoltageCnecCreationTest {

    @Test
    public void checkOnConstraintWith4VoltageCnecs() {
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-11/CSA_11_5_OnVoltageConstraint.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);
        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();

        Network network = Mockito.spy(Network.create("Test", "code"));
        BusbarSection terminal1Mock = Mockito.mock(BusbarSection.class);
        BusbarSection terminal2Mock = Mockito.mock(BusbarSection.class);
        Switch switch1Mock = Mockito.mock(Switch.class);
        Branch networkElementMock = Mockito.mock(Branch.class);
        Switch switch2Mock = Mockito.mock(Switch.class);
        Switch switch3Mock = Mockito.mock(Switch.class);
        Switch switch4Mock = Mockito.mock(Switch.class);

        Mockito.when(terminal1Mock.getId()).thenReturn("60038442-5c02-21a9-22ad-f0554a65a466");
        Mockito.when(terminal2Mock.getId()).thenReturn("65e9a6a7-8488-7b17-6344-cb7d61b7920b");
        Mockito.when(terminal1Mock.getType()).thenReturn(IdentifiableType.BUS);
        Mockito.when(terminal2Mock.getType()).thenReturn(IdentifiableType.BUS);
        Mockito.when(switch1Mock.getId()).thenReturn("f9c8d9ce-6c44-4293-b60e-93c658411d68");
        Mockito.when(networkElementMock.getId()).thenReturn("3a88a6a7-66fe-4988-9019-b3b288fd54ee");
        Mockito.when(switch1Mock.isOpen()).thenReturn(false);
        Mockito.when(network.getIdentifiable("60038442-5c02-21a9-22ad-f0554a65a466")).thenReturn((Identifiable) terminal1Mock);
        Mockito.when(network.getIdentifiable("65e9a6a7-8488-7b17-6344-cb7d61b7920b")).thenReturn((Identifiable) terminal2Mock);
        Mockito.when(network.getSwitch("f9c8d9ce-6c44-4293-b60e-93c658411d68")).thenReturn(switch1Mock);
        Mockito.when(network.getSwitch("c8fcaef5-67f2-42c5-b736-ca91dcbcfe59")).thenReturn(switch2Mock);
        Mockito.when(network.getSwitch("468fdb4a-49d6-4ea9-b216-928d057b65f0")).thenReturn(switch3Mock);
        Mockito.when(network.getSwitch("50719289-6406-4d69-9dd7-6de60aecd2d4")).thenReturn(switch4Mock);

        Mockito.when(network.getIdentifiable("3a88a6a7-66fe-4988-9019-b3b288fd54ee")).thenReturn(networkElementMock);

        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        CsaProfileCracCreationTestUtil.assertVoltageCnecEquality(cracCreationContext.getCrac().getVoltageCnec("RTE_AE1 - RTE_CO1 - curative"),
                "RTE_AE1 - RTE_CO1 - curative",
                "RTE_AE1 - RTE_CO1 - curative",
                "60038442-5c02-21a9-22ad-f0554a65a466",
                CURATIVE,
                "6c9656a6-84c2-4967-aabc-51f63a7abdf1",
                817.,
                null,
                true);

        CsaProfileCracCreationTestUtil.assertVoltageCnecEquality(cracCreationContext.getCrac().getVoltageCnec("RTE_AE1 - preventive"),
                "RTE_AE1 - preventive",
                "RTE_AE1 - preventive",
                "60038442-5c02-21a9-22ad-f0554a65a466",
                PREVENTIVE,
                null,
                817.,
                null,
                true);

        CsaProfileCracCreationTestUtil.assertVoltageCnecEquality(cracCreationContext.getCrac().getVoltageCnec("RTE_AE2 - RTE_CO2 - curative"),
                "RTE_AE2 - RTE_CO2 - curative",
                "RTE_AE2 - RTE_CO2 - curative",
                "65e9a6a7-8488-7b17-6344-cb7d61b7920b",
                CURATIVE,
                "410a7075-51df-4c5c-aa80-0bb1bbe41190",
                null,
                520.,
                true);

        CsaProfileCracCreationTestUtil.assertVoltageCnecEquality(cracCreationContext.getCrac().getVoltageCnec("RTE_AE2 - preventive"),
                "RTE_AE2 - preventive",
                "RTE_AE2 - preventive",
                "65e9a6a7-8488-7b17-6344-cb7d61b7920b",
                PREVENTIVE,
                null,
                null,
                520.,
                true);

        //4 remedial actions and a total of 8 onVoltageConstraint usage rules.
        assertEquals(4, cracCreationContext.getCrac().getRemedialActions().size());
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "6c283463-9aac-4d9b-9d0b-6710c5b2aa00", Set.of("f9c8d9ce-6c44-4293-b60e-93c658411d68"), true, 2);
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "0af9ce7e-8013-4362-96a0-40ac0a970eb6", Set.of("c8fcaef5-67f2-42c5-b736-ca91dcbcfe59"), false, 2);
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "f17a745b-60a1-4acd-887f-ebc8349b4597", Set.of("50719289-6406-4d69-9dd7-6de60aecd2d4"), true, 2);
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "a8f21a9a-49dc-4c2a-9745-405392f0d87b", Set.of("468fdb4a-49d6-4ea9-b216-928d057b65f0"), false, 2);

        CsaProfileCracCreationTestUtil.assertHasOnVoltageConstraintUsageRule(cracCreationContext, "6c283463-9aac-4d9b-9d0b-6710c5b2aa00", "RTE_AE1 - preventive", PREVENTIVE, UsageMethod.TO_BE_EVALUATED); // TODO change TO_BE_EVALUATED by AVAILABLE
        CsaProfileCracCreationTestUtil.assertHasOnVoltageConstraintUsageRule(cracCreationContext, "6c283463-9aac-4d9b-9d0b-6710c5b2aa00", "RTE_AE1 - RTE_CO1 - curative", PREVENTIVE, UsageMethod.TO_BE_EVALUATED);

        CsaProfileCracCreationTestUtil.assertHasOnVoltageConstraintUsageRule(cracCreationContext, "0af9ce7e-8013-4362-96a0-40ac0a970eb6", "RTE_AE2 - preventive", PREVENTIVE, UsageMethod.TO_BE_EVALUATED); // TODO change TO_BE_EVALUATED by AVAILABLE
        CsaProfileCracCreationTestUtil.assertHasOnVoltageConstraintUsageRule(cracCreationContext, "0af9ce7e-8013-4362-96a0-40ac0a970eb6", "RTE_AE2 - RTE_CO2 - curative", PREVENTIVE, UsageMethod.TO_BE_EVALUATED);

        CsaProfileCracCreationTestUtil.assertHasOnVoltageConstraintUsageRule(cracCreationContext, "f17a745b-60a1-4acd-887f-ebc8349b4597", "RTE_AE2 - preventive", PREVENTIVE, UsageMethod.TO_BE_EVALUATED); // TODO change TO_BE_EVALUATED by AVAILABLE
        CsaProfileCracCreationTestUtil.assertHasOnVoltageConstraintUsageRule(cracCreationContext, "f17a745b-60a1-4acd-887f-ebc8349b4597", "RTE_AE2 - RTE_CO2 - curative", PREVENTIVE, UsageMethod.TO_BE_EVALUATED);

        CsaProfileCracCreationTestUtil.assertHasOnVoltageConstraintUsageRule(cracCreationContext, "a8f21a9a-49dc-4c2a-9745-405392f0d87b", "RTE_AE1 - RTE_CO1 - curative", CURATIVE, UsageMethod.TO_BE_EVALUATED); // TODO change TO_BE_EVALUATED by AVAILABLE
        CsaProfileCracCreationTestUtil.assertHasOnVoltageConstraintUsageRule(cracCreationContext, "a8f21a9a-49dc-4c2a-9745-405392f0d87b", "RTE_AE2 - RTE_CO2 - curative", CURATIVE, UsageMethod.TO_BE_EVALUATED);
    }

}
