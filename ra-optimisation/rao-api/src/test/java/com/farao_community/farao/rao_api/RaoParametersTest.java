/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.google.auto.service.AutoService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Country;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.FileSystem;
import java.util.*;

import static com.farao_community.farao.rao_api.parameters.RaoParameters.PstOptimizationApproximation.APPROXIMATED_INTEGERS;
import static com.farao_community.farao.rao_api.parameters.RaoParameters.Solver.XPRESS;
import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoParametersTest {

    private PlatformConfig config;
    private InMemoryPlatformConfig platformCfg;
    private FileSystem fileSystem;

    @Before
    public void setUp() {
        config = Mockito.mock(PlatformConfig.class);
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformCfg = new InMemoryPlatformConfig(fileSystem);
    }

    @Test
    public void testExtensions() {
        RaoParameters parameters = new RaoParameters();
        DummyExtension dummyExtension = new DummyExtension();
        parameters.addExtension(DummyExtension.class, dummyExtension);

        assertEquals(1, parameters.getExtensions().size());
        assertTrue(parameters.getExtensions().contains(dummyExtension));
        assertTrue(parameters.getExtensionByName("dummyExtension") instanceof DummyExtension);
        assertNotNull(parameters.getExtension(DummyExtension.class));
    }

    @Test
    public void testNoExtensions() {
        RaoParameters parameters = new RaoParameters();

        assertEquals(0, parameters.getExtensions().size());
        assertFalse(parameters.getExtensions().contains(new DummyExtension()));
        assertFalse(parameters.getExtensionByName("dummyExtension") instanceof DummyExtension);
        assertNull(parameters.getExtension(DummyExtension.class));
    }

    @Test
    public void checkConfig() {

        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("rao-with-mnec-limitation", Boolean.toString(true));
        moduleConfig.setStringProperty("rao-with-loop-flow-limitation", Boolean.toString(false));
        moduleConfig.setStringProperty("loop-flow-constraint-adjustment-coefficient", Objects.toString(15.0));
        moduleConfig.setStringProperty("loop-flow-violation-cost", Objects.toString(10.0));
        moduleConfig.setStringProperty("forbid-cost-increase", Boolean.toString(true));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertTrue(parameters.isRaoWithMnecLimitation());
        assertFalse(parameters.isRaoWithLoopFlowLimitation());
        assertEquals(15., parameters.getLoopFlowConstraintAdjustmentCoefficient(), 1e-6);
        assertEquals(10., parameters.getLoopFlowViolationCost(), 1e-6);
        assertTrue(parameters.getForbidCostIncrease());
    }

    @Test
    public void testExtensionFromConfig() {
        RaoParameters parameters = RaoParameters.load(config);

        assertEquals(1, parameters.getExtensions().size());
        assertTrue(parameters.getExtensionByName("dummyExtension") instanceof DummyExtension);
        assertNotNull(parameters.getExtension(DummyExtension.class));
    }

    @Test
    public void checkMnecConfig() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("mnec-acceptable-margin-diminution", Objects.toString(100.0));
        moduleConfig.setStringProperty("mnec-violation-cost", Objects.toString(5.0));
        moduleConfig.setStringProperty("mnec-constraint-adjustment-coefficient", Objects.toString(0.1));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertEquals(100, parameters.getMnecAcceptableMarginDiminution(), 1e-6);
        assertEquals(5, parameters.getMnecViolationCost(), 1e-6);
        assertEquals(0.1, parameters.getMnecConstraintAdjustmentCoefficient(), 1e-6);
    }

    @Test
    public void checkPtdfConfig() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringListProperty("relative-margin-ptdf-boundaries", new ArrayList<>(Arrays.asList("{FR}-{ES}", "{ES}-{PT}")));
        moduleConfig.setStringProperty("ptdf-sum-lower-bound", Objects.toString(5.0));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertEquals(5, parameters.getPtdfSumLowerBound(), 1e-6);
        assertEquals(2, parameters.getRelativeMarginPtdfBoundaries().size());
        assertEquals(2, parameters.getRelativeMarginPtdfBoundaries().size());
        assertEquals(1, parameters.getRelativeMarginPtdfBoundaries().get(0).getWeight(new EICode(Country.FR)), 1e-6);
        assertEquals(-1, parameters.getRelativeMarginPtdfBoundaries().get(0).getWeight(new EICode(Country.ES)), 1e-6);
        assertEquals(1, parameters.getRelativeMarginPtdfBoundaries().get(1).getWeight(new EICode(Country.ES)), 1e-6);
        assertEquals(-1, parameters.getRelativeMarginPtdfBoundaries().get(1).getWeight(new EICode(Country.PT)), 1e-6);
    }

    @Test
    public void checkLoopFlowConfig() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("loop-flow-approximation", "UPDATE_PTDF_WITH_TOPO");
        moduleConfig.setStringProperty("loop-flow-constraint-adjustment-coefficient", Objects.toString(5.0));
        moduleConfig.setStringProperty("loop-flow-violation-cost", Objects.toString(20.6));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertEquals(RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO, parameters.getLoopFlowApproximationLevel());
        assertEquals(5, parameters.getLoopFlowConstraintAdjustmentCoefficient(), 1e-6);
        assertEquals(20.6, parameters.getLoopFlowViolationCost(), 1e-6);
    }

    @Test
    public void checkPerimetersParallelConfig() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("perimeters-in-parallel", Objects.toString(10));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertEquals(10, parameters.getPerimetersInParallel());
    }

    @Test
    public void checkSolverAndMipConfig() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("pst-optimization-approximation", "APPROXIMATED_INTEGERS");
        moduleConfig.setStringProperty("optimization-solver", "XPRESS");
        moduleConfig.setStringProperty("relative-mip-gap", Objects.toString(1e-3));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertEquals(APPROXIMATED_INTEGERS, parameters.getPstOptimizationApproximation());
        assertEquals(XPRESS, parameters.getSolver());
        assertEquals(1e-3, parameters.getRelativeMipGap(), 1e-6);
    }

    @Test
    public void checkInjectionRaParameters() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("injection-ra-penalty-cost", Objects.toString(1.2));
        moduleConfig.setStringProperty("injection-ra-sensitivity-threshold", Objects.toString(0.55));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertEquals(1.2, parameters.getInjectionRaPenaltyCost(), 1e-3);
        assertEquals(0.55, parameters.getInjectionRaSensitivityThreshold(), 1e-3);
    }

    @Test
    public void testUpdatePtdfWithTopo() {
        assertFalse(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF.shouldUpdatePtdfWithTopologicalChange());
        assertTrue(RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO.shouldUpdatePtdfWithTopologicalChange());
        assertTrue(RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO_AND_PST.shouldUpdatePtdfWithTopologicalChange());
    }

    @Test
    public void testUpdatePtdfWithPst() {
        assertFalse(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF.shouldUpdatePtdfWithPstChange());
        assertFalse(RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO.shouldUpdatePtdfWithPstChange());
        assertTrue(RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO_AND_PST.shouldUpdatePtdfWithPstChange());
    }

    @Test
    public void testSetBoundariesFromCountryCodes() {
        RaoParameters parameters = new RaoParameters();
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("{FR}-{ES}", "{ES}-{PT}"));
        parameters.setRelativeMarginPtdfBoundariesFromString(stringBoundaries);
        assertEquals(2, parameters.getRelativeMarginPtdfBoundaries().size());
        assertEquals(1, parameters.getRelativeMarginPtdfBoundaries().get(0).getWeight(new EICode(Country.FR)), 1e-6);
        assertEquals(-1, parameters.getRelativeMarginPtdfBoundaries().get(0).getWeight(new EICode(Country.ES)), 1e-6);
        assertEquals(1, parameters.getRelativeMarginPtdfBoundaries().get(1).getWeight(new EICode(Country.ES)), 1e-6);
        assertEquals(-1, parameters.getRelativeMarginPtdfBoundaries().get(1).getWeight(new EICode(Country.PT)), 1e-6);
    }

    @Test
    public void testSetBoundariesFromEiCodes() {
        RaoParameters parameters = new RaoParameters();
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("{10YBE----------2}-{10YFR-RTE------C}", "{10YBE----------2}-{22Y201903144---9}"));
        parameters.setRelativeMarginPtdfBoundariesFromString(stringBoundaries);
        assertEquals(2, parameters.getRelativeMarginPtdfBoundaries().size());
        assertEquals(2, parameters.getRelativeMarginPtdfBoundaries().size());
        assertEquals(1, parameters.getRelativeMarginPtdfBoundaries().get(0).getWeight(new EICode("10YBE----------2")), 1e-6);
        assertEquals(-1, parameters.getRelativeMarginPtdfBoundaries().get(0).getWeight(new EICode("10YFR-RTE------C")), 1e-6);
        assertEquals(1, parameters.getRelativeMarginPtdfBoundaries().get(1).getWeight(new EICode("10YBE----------2")), 1e-6);
        assertEquals(-1, parameters.getRelativeMarginPtdfBoundaries().get(1).getWeight(new EICode("22Y201903144---9")), 1e-6);
    }

    @Test
    public void testSetBoundariesFromMixOfCodes() {
        RaoParameters parameters = new RaoParameters();
        List<String> stringBoundaries = new ArrayList<>(Collections.singletonList("{BE}-{22Y201903144---9}+{22Y201903145---4}-{DE}"));
        parameters.setRelativeMarginPtdfBoundariesFromString(stringBoundaries);
        assertEquals(1, parameters.getRelativeMarginPtdfBoundaries().size());
        assertEquals(1, parameters.getRelativeMarginPtdfBoundaries().get(0).getWeight(new EICode(Country.BE)), 1e-6);
        assertEquals(-1, parameters.getRelativeMarginPtdfBoundaries().get(0).getWeight(new EICode(Country.DE)), 1e-6);
        assertEquals(1, parameters.getRelativeMarginPtdfBoundaries().get(0).getWeight(new EICode("22Y201903145---4")), 1e-6);
        assertEquals(-1, parameters.getRelativeMarginPtdfBoundaries().get(0).getWeight(new EICode("22Y201903144---9")), 1e-6);
    }

    @Test
    public void testRelativePositiveMargins() {
        assertTrue(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE.relativePositiveMargins());
        assertTrue(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT.relativePositiveMargins());
        assertFalse(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE.relativePositiveMargins());
        assertFalse(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT.relativePositiveMargins());
    }

    private static class DummyExtension extends AbstractExtension<RaoParameters> {

        @Override
        public String getName() {
            return "dummyExtension";
        }
    }

    @AutoService(RaoParameters.ConfigLoader.class)
    public static class DummyLoader implements RaoParameters.ConfigLoader<DummyExtension> {

        @Override
        public DummyExtension load(PlatformConfig platformConfig) {
            return new DummyExtension();
        }

        @Override
        public String getExtensionName() {
            return "dummyExtension";
        }

        @Override
        public String getCategoryName() {
            return "rao-parameters";
        }

        @Override
        public Class<? super DummyExtension> getExtensionClass() {
            return DummyExtension.class;
        }
    }

}
