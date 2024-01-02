/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.monitoring.angle_monitoring;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.Identifiable;
import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.InstantKind;
import com.powsybl.open_rao.data.crac_io_json.JsonImport;
import com.powsybl.open_rao.data.rao_result_api.ComputationStatus;
import com.powsybl.open_rao.data.rao_result_api.RaoResult;
import com.powsybl.open_rao.data.rao_result_json.RaoResultImporter;
import com.powsybl.open_rao.monitoring.angle_monitoring.json.AngleMonitoringResultImporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class RaoResultWithAngleMonitoringTest {
    private Crac crac;
    private RaoResult raoResult;
    private static final double DOUBLE_TOLERANCE = 0.1;

    @BeforeEach
    public void setUp() {
        InputStream raoResultFile = getClass().getResourceAsStream("/rao-result-v1.4.json");
        InputStream cracFile = getClass().getResourceAsStream("/crac-for-rao-result-v1.4.json");
        crac = new JsonImport().importCrac(cracFile);
        raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);
    }

    @Test
    void testRaoResultWithAngleMonitoring() {
        AngleMonitoringResult angleMonitoringResult = new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/angle-monitoring-result.json"), crac);
        RaoResult raoResultWithAngleMonitoring = new RaoResultWithAngleMonitoring(raoResult, angleMonitoringResult);
        Instant curativeInstant = crac.getInstant(InstantKind.CURATIVE);
        assertEquals(4.6, raoResultWithAngleMonitoring.getAngle(curativeInstant, crac.getAngleCnec("angleCnecId"), Unit.DEGREE), DOUBLE_TOLERANCE);
        assertEquals(85.4, raoResultWithAngleMonitoring.getMargin(curativeInstant, crac.getAngleCnec("angleCnecId"), Unit.DEGREE), DOUBLE_TOLERANCE);
        assertEquals(Set.of("pstSetpointRaId", "complexNetworkActionId"), raoResultWithAngleMonitoring.getActivatedNetworkActionsDuringState(crac.getState("contingency1Id", curativeInstant)).stream().map(Identifiable::getId).collect(Collectors.toSet()));
        assertTrue(raoResultWithAngleMonitoring.isActivatedDuringState(crac.getState("contingency1Id", curativeInstant), crac.getNetworkAction("complexNetworkActionId")));
        assertEquals(ComputationStatus.DEFAULT, raoResultWithAngleMonitoring.getComputationStatus());

        AngleMonitoringResult angleMonitoringResult2 = new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/angle-monitoring-result2.json"), crac);
        RaoResult raoResultWithAngleMonitoring2 = new RaoResultWithAngleMonitoring(raoResult, angleMonitoringResult2);
        assertEquals(ComputationStatus.FAILURE, raoResultWithAngleMonitoring2.getComputationStatus());
    }

    @Test
    void testRaoResultWithNullAngleMonitoring() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new RaoResultWithAngleMonitoring(raoResult, null));
        assertEquals("AngleMonitoringResult must not be null", exception.getMessage());
    }
}
