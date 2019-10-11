/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.util.LoadFlowService;
import com.farao_community.farao.util.SensitivityComputationService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class MinRamAdjustmentTest {
    private static final double EPSILON = 1e-3;

    private static final double FRM_DEFAULT_ON_FMAX = 0.1;
    private static final double RAMR_DEFAULT = 0.7;
    private static final double RAMR_DEFAULT_BIS = 1.1;

    private GlskProvider glskProviderAll;
    private Network network;
    private CracFile cracFile;
    private GlskProvider glskProviderCore;
    private Map<String, Double> frmById;
    private Map<String, Double> ramrById;
    private Map<String, Double> ramrByIdBis;
    private List<String> countries;

    private ComputationManager computationManager;
    private FlowBasedComputationParameters parameters;

    private MinRamAdjustment minRamAdjustment;
    private MinRamAdjustment minRamAdjustmentBis;

    @Before
    public void setUp() {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
        platformConfig.createModuleConfig("load-flow").setStringProperty("default", "MockLoadflow");

        network = MinRamAdjustmentExampleGenerator.network();
        cracFile = MinRamAdjustmentExampleGenerator.cracFile();
        glskProviderCore = MinRamAdjustmentExampleGenerator.glskProviderCore();
        glskProviderAll = MinRamAdjustmentExampleGenerator.glskProviderAll();
        countries = Arrays.asList("FR", "BE", "DE", "NL");
        computationManager = LocalComputationManager.getDefault();
        parameters = FlowBasedComputationParameters.load(platformConfig);

        frmById = cracFile.getPreContingency().getMonitoredBranches().stream()
                .collect(Collectors.toMap(MonitoredBranch::getBranchId, monitoredBranch -> monitoredBranch.getFmax() * FRM_DEFAULT_ON_FMAX));

        ramrById = cracFile.getPreContingency().getMonitoredBranches().stream()
                .collect(Collectors.toMap(MonitoredBranch::getBranchId, monitoredBranch -> RAMR_DEFAULT));

        ramrByIdBis = cracFile.getPreContingency().getMonitoredBranches().stream()
                .collect(Collectors.toMap(MonitoredBranch::getBranchId, monitoredBranch -> RAMR_DEFAULT_BIS));

        LoadFlowFactory loadFlowFactory = MinRamAdjustmentExampleGenerator.loadFlowFactory();
        SensitivityComputationFactory sensitivityComputationFactory = MinRamAdjustmentExampleGenerator.sensitivityComputationFactory();
        LoadFlowService.init(loadFlowFactory, computationManager);
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);

        minRamAdjustment = new MinRamAdjustment(network, cracFile, glskProviderCore, glskProviderAll, frmById, ramrById, countries, computationManager, parameters);
        minRamAdjustmentBis = new MinRamAdjustment(network, cracFile, glskProviderCore, glskProviderAll, frmById, ramrByIdBis, countries, computationManager, parameters);

    }

    @Test
    public void testRun() {
        Map<String, Double> resultAmr = minRamAdjustment.calculateAMR();
        assertTrue(!resultAmr.isEmpty());
        for (String branch : resultAmr.keySet()) {
            assertEquals(resultAmr.get(branch), 0.0, EPSILON);
        }

        Map<String, Double> resultAmrBis = minRamAdjustmentBis.calculateAMR();
        assertTrue(!resultAmrBis.isEmpty());
        for (String branch : resultAmrBis.keySet()) {
            assertEquals(resultAmrBis.get(branch), 20.0, EPSILON);
        }
    }
}
