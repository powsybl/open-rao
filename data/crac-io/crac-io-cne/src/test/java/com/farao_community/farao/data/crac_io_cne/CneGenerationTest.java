/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;
import static org.junit.Assert.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CneGenerationTest {

    @Test
    public void testExport1() {

        Crac crac = CracImporters.importCrac("US2-3-crac1-standard.json", getClass().getResourceAsStream("/US2-3-crac1-standard.json"));
        Network network = Importers.loadNetwork("US2-3-case1-standard.uct", getClass().getResourceAsStream("/US2-3-case1-standard.uct"));

        // build object
        Cne cne = new Cne(crac, network);
        cne.generate();
        CriticalNetworkElementMarketDocument marketDocument = cne.getMarketDocument();
        Point point = marketDocument.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

        assertEquals(5, point.getConstraintSeries().size());

        assertEquals(1, point.getConstraintSeries().stream().filter(constraintSeries -> constraintSeries.getBusinessType().equals(B54_BUSINESS_TYPE)).count());
        assertEquals(1, point.getConstraintSeries().stream().filter(constraintSeries -> constraintSeries.getBusinessType().equals(B57_BUSINESS_TYPE)).count());
        assertEquals(1, point.getConstraintSeries().stream().filter(constraintSeries -> constraintSeries.getBusinessType().equals(B88_BUSINESS_TYPE)).count());
        List<ConstraintSeries> b56constraintSeriesList = point.getConstraintSeries().stream().filter(constraintSeries -> constraintSeries.getBusinessType().equals(B56_BUSINESS_TYPE)).collect(Collectors.toList());
        assertEquals(2, b56constraintSeriesList.size());
        Optional<ConstraintSeries> preventiveB56 = b56constraintSeriesList.stream().filter(constraintSeries -> !constraintSeries.getMRID().contains("SelectTapPST43")).findFirst();
        if (preventiveB56.isPresent()) {
            assertEquals(2, preventiveB56.get().getRemedialActionSeries().size());
        } else {
            fail();
        }

        Optional<ConstraintSeries> constraintSeriesB54 = point.getConstraintSeries().stream().filter(constraintSeries ->
            constraintSeries.getMRID().contains("FFR1AA1  FFR2AA1  1 - N - preventive") && constraintSeries.getBusinessType().equals(B54_BUSINESS_TYPE)).findFirst();
        Optional<ConstraintSeries> constraintSeriesB57 = point.getConstraintSeries().stream().filter(constraintSeries ->
            constraintSeries.getMRID().contains("FFR1AA1  FFR2AA1  1 - N - preventive") && constraintSeries.getBusinessType().equals(B57_BUSINESS_TYPE)).findFirst();
        Optional<ConstraintSeries> constraintSeriesB88 = point.getConstraintSeries().stream().filter(constraintSeries ->
            constraintSeries.getMRID().contains("FFR1AA1  FFR2AA1  1 - N - preventive") && constraintSeries.getBusinessType().equals(B88_BUSINESS_TYPE)).findFirst();
        Optional<ConstraintSeries> constraintSeriesB56 = point.getConstraintSeries().stream().filter(constraintSeries ->
            constraintSeries.getMRID().contains("SelectTapPST43") && constraintSeries.getBusinessType().equals(B56_BUSINESS_TYPE)).findFirst();

        if (constraintSeriesB54.isPresent() && constraintSeriesB57.isPresent() && constraintSeriesB88.isPresent() && constraintSeriesB56.isPresent()) {
            // Constraint series B54
            assertEquals(1, constraintSeriesB54.get().getMonitoredSeries().size());
            assertEquals(2, constraintSeriesB54.get().getRemedialActionSeries().size());
            assertEquals(8, constraintSeriesB54.get().getMonitoredSeries().get(0).getRegisteredResource().get(0).getMeasurements().size());
            assertNotSame(constraintSeriesB54.get().getRemedialActionSeries().get(0).getMRID(), constraintSeriesB54.get().getRemedialActionSeries().get(1).getMRID());
            // Constraint series B57
            assertEquals(1, constraintSeriesB57.get().getMonitoredSeries().size());
            assertEquals(2, constraintSeriesB57.get().getRemedialActionSeries().size());
            assertEquals(4, constraintSeriesB57.get().getMonitoredSeries().get(0).getRegisteredResource().get(0).getMeasurements().size());
            // Constraint series B88
            assertEquals("10YFR-RTE------C", constraintSeriesB88.get().getPartyMarketParticipant().get(0).getMRID().getValue());
            assertEquals(0, constraintSeriesB88.get().getContingencySeries().size());
            MonitoredRegisteredResource monitoredRegisteredResource = constraintSeriesB88.get().getMonitoredSeries().get(0).getRegisteredResource().get(0);
            assertEquals(11, monitoredRegisteredResource.getMeasurements().size());
            assertEquals("FFR1AA1  FFR2AA1  1 - N - preventive", monitoredRegisteredResource.getMRID().getValue().substring(0, 36));
            assertEquals("Threshold12", monitoredRegisteredResource.getName());
            assertEquals("FFR1AA1 ", monitoredRegisteredResource.getInAggregateNodeMRID().getValue());
            assertEquals("FFR2AA1 ", monitoredRegisteredResource.getOutAggregateNodeMRID().getValue());

            // Test measurements with correct order
            List<Analog> measurements = monitoredRegisteredResource.getMeasurements();
            int iMeasure = 0;
            // Measurement A01 - AMP
            assertEquals(FLOW_MEASUREMENT_TYPE, measurements.get(iMeasure).getMeasurementType());
            assertEquals(AMP_UNIT_SYMBOL, measurements.get(iMeasure).getUnitSymbol());
            assertEquals(DIRECT_POSITIVE_FLOW_IN, measurements.get(iMeasure).getPositiveFlowIn());
            assertEquals(16540, measurements.get(iMeasure).getAnalogValuesValue(), 0);
            // Measurement A01 - MAW
            iMeasure++;
            assertEquals(FLOW_MEASUREMENT_TYPE, measurements.get(iMeasure).getMeasurementType());
            assertEquals(MAW_UNIT_SYMBOL, measurements.get(iMeasure).getUnitSymbol());
            assertEquals(DIRECT_POSITIVE_FLOW_IN, measurements.get(iMeasure).getPositiveFlowIn());
            assertEquals(2853, measurements.get(iMeasure).getAnalogValuesValue(), 0);
            // Measurement A02 - AMP
            iMeasure++;
            assertEquals(PATL_MEASUREMENT_TYPE, measurements.get(iMeasure).getMeasurementType());
            assertEquals(AMP_UNIT_SYMBOL, measurements.get(iMeasure).getUnitSymbol());
            assertEquals(DIRECT_POSITIVE_FLOW_IN, measurements.get(iMeasure).getPositiveFlowIn());
            assertEquals(3325, measurements.get(iMeasure).getAnalogValuesValue(), 0);
            // Measurement A02 - MAW
            iMeasure++;
            assertEquals(PATL_MEASUREMENT_TYPE, measurements.get(iMeasure).getMeasurementType());
            assertEquals(MAW_UNIT_SYMBOL, measurements.get(iMeasure).getUnitSymbol());
            assertEquals(DIRECT_POSITIVE_FLOW_IN, measurements.get(iMeasure).getPositiveFlowIn());
            assertEquals(2304, measurements.get(iMeasure).getAnalogValuesValue(), 0);
            // Measurement A03 - MAW
            iMeasure++;
            assertEquals(FRM_MEASUREMENT_TYPE, measurements.get(iMeasure).getMeasurementType());
            assertEquals(MAW_UNIT_SYMBOL, measurements.get(iMeasure).getUnitSymbol());
            assertEquals(DIRECT_POSITIVE_FLOW_IN, measurements.get(iMeasure).getPositiveFlowIn());
            assertEquals(0, measurements.get(iMeasure).getAnalogValuesValue(), 0);
            // Measurement Z12 - AMP
            iMeasure++;
            assertEquals(ABS_MARG_PATL_MEASUREMENT_TYPE, measurements.get(iMeasure).getMeasurementType());
            assertEquals(AMP_UNIT_SYMBOL, measurements.get(iMeasure).getUnitSymbol());
            assertEquals(OPPOSITE_POSITIVE_FLOW_IN, measurements.get(iMeasure).getPositiveFlowIn());
            assertEquals(13215, measurements.get(iMeasure).getAnalogValuesValue(), 0);
            // Measurement Z12 - MAW
            iMeasure++;
            assertEquals(ABS_MARG_PATL_MEASUREMENT_TYPE, measurements.get(iMeasure).getMeasurementType());
            assertEquals(MAW_UNIT_SYMBOL, measurements.get(iMeasure).getUnitSymbol());
            assertEquals(OPPOSITE_POSITIVE_FLOW_IN, measurements.get(iMeasure).getPositiveFlowIn());
            assertEquals(550, measurements.get(iMeasure).getAnalogValuesValue(), 0);
            // Measurement Z13 - AMP
            iMeasure++;
            assertEquals(OBJ_FUNC_PATL_MEASUREMENT_TYPE, measurements.get(iMeasure).getMeasurementType());
            assertEquals(AMP_UNIT_SYMBOL, measurements.get(iMeasure).getUnitSymbol());
            assertEquals(DIRECT_POSITIVE_FLOW_IN, measurements.get(iMeasure).getPositiveFlowIn());
            assertEquals(13215, measurements.get(iMeasure).getAnalogValuesValue(), 0);
            // Measurement Z13 - MAW
            iMeasure++;
            assertEquals(OBJ_FUNC_PATL_MEASUREMENT_TYPE, measurements.get(iMeasure).getMeasurementType());
            assertEquals(MAW_UNIT_SYMBOL, measurements.get(iMeasure).getUnitSymbol());
            assertEquals(DIRECT_POSITIVE_FLOW_IN, measurements.get(iMeasure).getPositiveFlowIn());
            assertEquals(550, measurements.get(iMeasure).getAnalogValuesValue(), 0);
            // Measurement Z16 - MAW
            iMeasure++;
            assertEquals(LOOPFLOW_MEASUREMENT_TYPE, measurements.get(iMeasure).getMeasurementType());
            assertEquals(MAW_UNIT_SYMBOL, measurements.get(iMeasure).getUnitSymbol());
            assertEquals(OPPOSITE_POSITIVE_FLOW_IN, measurements.get(iMeasure).getPositiveFlowIn());
            assertEquals(13, measurements.get(iMeasure).getAnalogValuesValue(), 0);
            // Measurement Z17 - MAW
            iMeasure++;
            assertEquals(MAX_LOOPFLOW_MEASUREMENT_TYPE, measurements.get(iMeasure).getMeasurementType());
            assertEquals(MAW_UNIT_SYMBOL, measurements.get(iMeasure).getUnitSymbol());
            assertEquals(OPPOSITE_POSITIVE_FLOW_IN, measurements.get(iMeasure).getPositiveFlowIn());
            assertEquals(19, measurements.get(iMeasure).getAnalogValuesValue(), 0);

            // Remedial action
            Optional<RemedialActionSeries> remedialActionSeriesPST = constraintSeriesB54.get().getRemedialActionSeries().stream().filter(remedialActionSeries -> remedialActionSeries.getMRID().contains("SelectTapPST43@-16@")).findFirst();
            if (remedialActionSeriesPST.isPresent()) {
                assertTrue(remedialActionSeriesPST.get().getRegisteredResource().isEmpty());
                assertEquals("SelectTapPST43", remedialActionSeriesPST.get().getName());
                assertEquals(PREVENTIVE_MARKET_OBJECT_STATUS, remedialActionSeriesPST.get().getApplicationModeMarketObjectStatusStatus());
            } else {
                fail();
            }
            RemedialActionRegisteredResource remedialActionRegisteredResource = constraintSeriesB56.get().getRemedialActionSeries().get(0).getRegisteredResource().get(0);
            assertEquals("FFR4AA1  FFR3AA1  1", remedialActionRegisteredResource.getMRID().getValue());
            assertEquals("FFR4AA1  FFR3AA1  1", remedialActionRegisteredResource.getName());
            assertEquals(PST_RANGE_PSR_TYPE, remedialActionRegisteredResource.getPSRTypePsrType());
            assertEquals(ABSOLUTE_MARKET_OBJECT_STATUS, remedialActionRegisteredResource.getMarketObjectStatusStatus());
            assertEquals(BigDecimal.valueOf(0), remedialActionRegisteredResource.getResourceCapacityDefaultCapacity());
            assertEquals(WITHOUT_UNIT_SYMBOL, remedialActionRegisteredResource.getResourceCapacityUnitSymbol());
        } else {
            fail();
        }
    }

    private void testCnecConstraintSeries(ConstraintSeries constraintSeries, String expectedCnecId, String expectedBusinessType) {
        assertEquals(expectedCnecId, constraintSeries.getMonitoredSeries().get(0).getRegisteredResource().get(0).getMRID().getValue());
        assertEquals(expectedBusinessType, constraintSeries.getBusinessType());
    }

    private void testRemedialActionConstraintSeries(ConstraintSeries constraintSeries, String expectedRemedialActionId) {
        assertEquals(expectedRemedialActionId, constraintSeries.getRemedialActionSeries().get(0).getName());
        assertEquals(B56_BUSINESS_TYPE, constraintSeries.getBusinessType());
    }

    @Test
    public void testExport2() {

        Crac crac = CracImporters.importCrac("US3-2-pst-direct.json", getClass().getResourceAsStream("/US3-2-pst-direct.json"));
        Network network = NetworkImportsUtil.import12NodesNetwork();

        // build object
        Cne cne = new Cne(crac, network);
        cne.generate();
        CriticalNetworkElementMarketDocument marketDocument = cne.getMarketDocument();
        Point point = marketDocument.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

        assertEquals(20, point.getConstraintSeries().size());

        // Test order of Constraint_Series
        testCnecConstraintSeries(point.getConstraintSeries().get(0), "DDE1AA1  DDE2AA1  1 - Curatif - Contingency FR1 FR3", B88_BUSINESS_TYPE);
        testCnecConstraintSeries(point.getConstraintSeries().get(1), "DDE1AA1  DDE2AA1  1 - Curatif - Contingency FR1 FR3", B57_BUSINESS_TYPE);
        testCnecConstraintSeries(point.getConstraintSeries().get(2), "DDE1AA1  DDE2AA1  1 - Curatif - Contingency FR1 FR3", B54_BUSINESS_TYPE);

        testCnecConstraintSeries(point.getConstraintSeries().get(3), "DDE1AA1  DDE2AA1  1 - Défaut - Contingency FR1 FR3", B88_BUSINESS_TYPE);
        testCnecConstraintSeries(point.getConstraintSeries().get(4), "DDE1AA1  DDE2AA1  1 - Défaut - Contingency FR1 FR3", B57_BUSINESS_TYPE);
        testCnecConstraintSeries(point.getConstraintSeries().get(5), "DDE1AA1  DDE2AA1  1 - Défaut - Contingency FR1 FR3", B54_BUSINESS_TYPE);

        testCnecConstraintSeries(point.getConstraintSeries().get(6), "DDE1AA1  DDE2AA1  1 - N - preventive", B88_BUSINESS_TYPE);
        testCnecConstraintSeries(point.getConstraintSeries().get(7), "DDE1AA1  DDE2AA1  1 - N - preventive", B57_BUSINESS_TYPE);
        testCnecConstraintSeries(point.getConstraintSeries().get(8), "DDE1AA1  DDE2AA1  1 - N - preventive", B54_BUSINESS_TYPE);

        testCnecConstraintSeries(point.getConstraintSeries().get(9), "FFR2AA1  DDE3AA1  1 - Curatif - Contingency FR1 FR3", B88_BUSINESS_TYPE);
        testCnecConstraintSeries(point.getConstraintSeries().get(10), "FFR2AA1  DDE3AA1  1 - Curatif - Contingency FR1 FR3", B57_BUSINESS_TYPE);
        testCnecConstraintSeries(point.getConstraintSeries().get(11), "FFR2AA1  DDE3AA1  1 - Curatif - Contingency FR1 FR3", B54_BUSINESS_TYPE);
        // test countries order
        assertEquals("10YCB-GERMANY--8", point.getConstraintSeries().get(11).getPartyMarketParticipant().get(0).getMRID().getValue());
        assertEquals("10YFR-RTE------C", point.getConstraintSeries().get(11).getPartyMarketParticipant().get(1).getMRID().getValue());

        testCnecConstraintSeries(point.getConstraintSeries().get(12), "FFR2AA1  DDE3AA1  1 - Défaut - Contingency FR1 FR3", B88_BUSINESS_TYPE);
        testCnecConstraintSeries(point.getConstraintSeries().get(13), "FFR2AA1  DDE3AA1  1 - Défaut - Contingency FR1 FR3", B57_BUSINESS_TYPE);
        testCnecConstraintSeries(point.getConstraintSeries().get(14), "FFR2AA1  DDE3AA1  1 - Défaut - Contingency FR1 FR3", B54_BUSINESS_TYPE);

        testCnecConstraintSeries(point.getConstraintSeries().get(15), "FFR2AA1  DDE3AA1  1 - N - preventive", B88_BUSINESS_TYPE);
        testCnecConstraintSeries(point.getConstraintSeries().get(16), "FFR2AA1  DDE3AA1  1 - N - preventive", B57_BUSINESS_TYPE);
        testCnecConstraintSeries(point.getConstraintSeries().get(17), "FFR2AA1  DDE3AA1  1 - N - preventive", B54_BUSINESS_TYPE);

        List<ConstraintSeries> constraintSeriesList1 = point.getConstraintSeries().stream().filter(constraintSeries ->
            constraintSeries.getMRID().contains("FFR2AA1  DDE3AA1  1 - Curatif - Contingency FR1 FR3")).collect(Collectors.toList());
        assertEquals(3, constraintSeriesList1.size());
        List<ConstraintSeries> constraintSeriesList2 = point.getConstraintSeries().stream().filter(constraintSeries ->
            constraintSeries.getBusinessType().equals(B54_BUSINESS_TYPE)).collect(Collectors.toList());
        assertEquals(6, constraintSeriesList2.size());
        List<ConstraintSeries> constraintSeriesList3 = point.getConstraintSeries().stream().filter(constraintSeries ->
            constraintSeries.getBusinessType().equals(B57_BUSINESS_TYPE)).collect(Collectors.toList());
        assertEquals(6, constraintSeriesList3.size());
        List<ConstraintSeries> constraintSeriesList4 = point.getConstraintSeries().stream().filter(constraintSeries ->
            constraintSeries.getBusinessType().equals(B88_BUSINESS_TYPE)).collect(Collectors.toList());
        assertEquals(6, constraintSeriesList4.size());

        Optional<ConstraintSeries> constraintSeriesB54 = point.getConstraintSeries().stream().filter(constraintSeries ->
            constraintSeries.getMRID().contains("FFR2AA1  DDE3AA1  1 - Curatif - Contingency FR1 FR3") && constraintSeries.getBusinessType().equals(B54_BUSINESS_TYPE)).findFirst();
        Optional<ConstraintSeries> constraintSeriesB57 = point.getConstraintSeries().stream().filter(constraintSeries ->
            constraintSeries.getMRID().contains("DDE1AA1  DDE2AA1  1 - N - preventive") && constraintSeries.getBusinessType().equals(B57_BUSINESS_TYPE)).findFirst();
        Optional<ConstraintSeries> constraintSeriesB88 = point.getConstraintSeries().stream().filter(constraintSeries ->
            constraintSeries.getMRID().contains("DDE1AA1  DDE2AA1  1 - Défaut - Contingency FR1 FR3") && constraintSeries.getBusinessType().equals(B88_BUSINESS_TYPE)).findFirst();
        Optional<ConstraintSeries> constraintSeriesB88prev = point.getConstraintSeries().stream().filter(constraintSeries ->
            constraintSeries.getMRID().contains("FFR2AA1  DDE3AA1  1 - N - preventive") && constraintSeries.getBusinessType().equals(B88_BUSINESS_TYPE)).findFirst();

        if (constraintSeriesB54.isPresent() && constraintSeriesB57.isPresent() && constraintSeriesB88.isPresent() && constraintSeriesB88prev.isPresent()) {
            assertEquals(1, constraintSeriesB54.get().getContingencySeries().size());
            assertEquals("Contingency FR1 FR3", constraintSeriesB54.get().getContingencySeries().get(0).getMRID().substring(0, 19));
            assertEquals("Contingency FR1 FR3", constraintSeriesB54.get().getContingencySeries().get(0).getName());
            assertEquals(2, constraintSeriesB54.get().getPartyMarketParticipant().size());

            MonitoredRegisteredResource monitoredRegisteredResourceB54 = constraintSeriesB54.get().getMonitoredSeries().get(0).getRegisteredResource().get(0);
            MonitoredRegisteredResource monitoredRegisteredResourceB57 = constraintSeriesB57.get().getMonitoredSeries().get(0).getRegisteredResource().get(0);
            MonitoredRegisteredResource monitoredRegisteredResourceB88 = constraintSeriesB88.get().getMonitoredSeries().get(0).getRegisteredResource().get(0);
            MonitoredRegisteredResource monitoredRegisteredResourceB88prev = constraintSeriesB88prev.get().getMonitoredSeries().get(0).getRegisteredResource().get(0);
            assertEquals(2, monitoredRegisteredResourceB54.getMeasurements().size());
            assertEquals(2, monitoredRegisteredResourceB57.getMeasurements().size());
            assertEquals(9, monitoredRegisteredResourceB88.getMeasurements().size());
            assertEquals(9, monitoredRegisteredResourceB88prev.getMeasurements().size());

            // Measurement A07
            Optional<Analog> measurementA07 = monitoredRegisteredResourceB88.getMeasurements().stream().filter(measurement -> measurement.getMeasurementType().equals(TATL_MEASUREMENT_TYPE) && measurement.getUnitSymbol().equals(AMP_UNIT_SYMBOL)).findFirst();
            if (measurementA07.isPresent()) {
                assertEquals(OPPOSITE_POSITIVE_FLOW_IN, measurementA07.get().getPositiveFlowIn());
                assertEquals(1500, measurementA07.get().getAnalogValuesValue(), 0);
            } else {
                fail();
            }
            // Measurement Z12
            Optional<Analog> measurementZ12 = monitoredRegisteredResourceB88prev.getMeasurements().stream().filter(measurement -> measurement.getMeasurementType().equals(ABS_MARG_PATL_MEASUREMENT_TYPE) && measurement.getUnitSymbol().equals(AMP_UNIT_SYMBOL)).findFirst();
            if (measurementZ12.isPresent()) {
                assertEquals(OPPOSITE_POSITIVE_FLOW_IN, measurementZ12.get().getPositiveFlowIn());
                assertEquals(667, measurementZ12.get().getAnalogValuesValue(), 0);
            } else {
                fail();
            }
            // Measurement Z13
            Optional<Analog> measurementZ13 = monitoredRegisteredResourceB88prev.getMeasurements().stream().filter(measurement -> measurement.getMeasurementType().equals(OBJ_FUNC_PATL_MEASUREMENT_TYPE) && measurement.getUnitSymbol().equals(MAW_UNIT_SYMBOL)).findFirst();
            if (measurementZ13.isPresent()) {
                assertEquals(DIRECT_POSITIVE_FLOW_IN, measurementZ13.get().getPositiveFlowIn());
                assertEquals(461, measurementZ13.get().getAnalogValuesValue(), 0);
            } else {
                fail();
            }
        } else {
            fail();
        }
    }
}
