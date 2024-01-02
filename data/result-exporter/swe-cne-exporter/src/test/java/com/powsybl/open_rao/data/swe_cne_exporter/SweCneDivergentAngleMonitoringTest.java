/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.swe_cne_exporter;

import com.powsybl.open_rao.data.cne_exporter_commons.CneExporterParameters;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_creation.creator.api.CracCreationContext;
import com.powsybl.open_rao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.powsybl.open_rao.data.crac_creation.creator.cim.CimCrac;
import com.powsybl.open_rao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.powsybl.open_rao.data.crac_creation.creator.cim.crac_creator.CimCracCreator;
import com.powsybl.open_rao.data.crac_creation.creator.cim.importer.CimCracImporter;
import com.powsybl.open_rao.data.crac_creation.creator.cim.parameters.CimCracCreationParameters;
import com.powsybl.open_rao.data.crac_creation.creator.cim.parameters.RangeActionSpeed;
import com.powsybl.open_rao.data.rao_result_api.RaoResult;
import com.powsybl.open_rao.data.rao_result_json.RaoResultImporter;
import com.powsybl.open_rao.monitoring.angle_monitoring.AngleMonitoringResult;
import com.powsybl.open_rao.monitoring.angle_monitoring.RaoResultWithAngleMonitoring;
import com.powsybl.open_rao.monitoring.angle_monitoring.json.AngleMonitoringResultImporter;
import com.powsybl.open_rao.rao_api.parameters.RaoParameters;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static com.powsybl.open_rao.data.swe_cne_exporter.SweCneTest.compareCneFiles;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class SweCneDivergentAngleMonitoringTest {
    private Crac crac;
    private CracCreationContext cracCreationContext;
    private Network network;
    private RaoResultWithAngleMonitoring raoResultWithAngleMonitoring;

    @BeforeEach
    public void setUp() {
        network = Network.read(new File(SweCneTest.class.getResource("/TestCase16NodesWith2Hvdc.xiidm").getFile()).toString());
        InputStream is = getClass().getResourceAsStream("/CIM_CRAC.xml");
        CimCracImporter cracImporter = new CimCracImporter();
        CimCrac cimCrac = cracImporter.importNativeCrac(is);
        CimCracCreator cimCracCreator = new CimCracCreator();

        Set<RangeActionSpeed> rangeActionSpeeds = Set.of(new RangeActionSpeed("BBE2AA11 FFR3AA11 1", 1), new RangeActionSpeed("BBE2AA12 FFR3AA12 1", 2), new RangeActionSpeed("PRA_1", 3));
        CimCracCreationParameters cimCracCreationParameters = new CimCracCreationParameters();
        cimCracCreationParameters.setRemedialActionSpeed(rangeActionSpeeds);
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.setCracFactoryName("CracImplFactory");
        cracCreationParameters.addExtension(CimCracCreationParameters.class, cimCracCreationParameters);

        cracCreationContext = cimCracCreator.createCrac(cimCrac, network, OffsetDateTime.of(2021, 4, 2, 12, 30, 0, 0, ZoneOffset.UTC), cracCreationParameters);
        crac = cracCreationContext.getCrac();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(SweCneDivergentAngleMonitoringTest.class.getResource("/RaoResult.json").getFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        RaoResult raoResult = new RaoResultImporter().importRaoResult(inputStream, crac);
        InputStream inputStream2 = null;
        try {
            inputStream2 = new FileInputStream(SweCneDivergentAngleMonitoringTest.class.getResource("/AngleMonitoringDivergentResult.json").getFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        AngleMonitoringResult angleMonitoringResult = new AngleMonitoringResultImporter().importAngleMonitoringResult(inputStream2, crac);
        raoResultWithAngleMonitoring = new RaoResultWithAngleMonitoring(raoResult, angleMonitoringResult);
    }

    @Test
    void testExport() {
        CneExporterParameters params = new CneExporterParameters(
                "documentId", 1, null, CneExporterParameters.ProcessType.Z01,
                "senderId", CneExporterParameters.RoleType.SYSTEM_OPERATOR,
                "receiverId", CneExporterParameters.RoleType.CAPACITY_COORDINATOR,
                "2021-04-02T12:00:00Z/2021-04-02T13:00:00Z");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new SweCneExporter().exportCne(crac, network, (CimCracCreationContext) cracCreationContext, raoResultWithAngleMonitoring, new RaoParameters(), params, outputStream);
        try {
            InputStream inputStream = new FileInputStream(SweCneDivergentAngleMonitoringTest.class.getResource("/SweCNEDivergentAngleMonitoring_Z01.xml").getFile());
            compareCneFiles(inputStream, new ByteArrayInputStream(outputStream.toByteArray()));
        } catch (IOException e) {
            Assertions.fail();
        }
    }
}
