/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.rao_mock.AnotherRaoProviderMock;
import com.farao_community.farao.rao_api.rao_mock.RaoProviderMock;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.FileSystem;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Baptiste Seguinot <baptiste.seguinot at rte-france.com>
 */
class RaoTest {

    private FileSystem fileSystem;
    private InMemoryPlatformConfig platformConfig;
    private RaoInput raoInput;

    @BeforeEach
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        Network network = Mockito.mock(Network.class);
        Crac crac = Mockito.mock(Crac.class);
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        Mockito.when(network.getVariantManager()).thenReturn(variantManager);
        Mockito.when(variantManager.getWorkingVariantId()).thenReturn("v");
        raoInput = RaoInput.build(network, crac).withNetworkVariantId("variant-id").build();
    }

    @AfterEach
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    void testDefaultOneProvider() {
        // case with only one provider, no need for config
        // find rao
        Rao.Runner defaultRao = Rao.find(null, ImmutableList.of(new RaoProviderMock()), platformConfig);
        assertEquals("RandomRAO", defaultRao.getName());
        assertEquals("1.0", defaultRao.getVersion());

        // run rao
        RaoResult result = defaultRao.run(raoInput, new RaoParameters());
        assertNotNull(result);
        //todo : assertEquals(RaoResultImpl.Status.DEFAULT, result.getStatus());
        RaoResult resultAsync = defaultRao.runAsync(raoInput, new RaoParameters()).join();
        assertNotNull(resultAsync);
        // todo: assertEquals(RaoResultImpl.Status.DEFAULT, resultAsync.getStatus());
    }

    @Test
    void testDefaultTwoProviders() {
        // case with two providers : should throw as no config defines which provider must be selected
        List<RaoProvider> raoProviders = ImmutableList.of(new RaoProviderMock(), new AnotherRaoProviderMock());
        FaraoException exception = assertThrows(FaraoException.class, () -> Rao.find(null, raoProviders, platformConfig));
        assertEquals("", exception.getMessage());
    }

    @Test
    void testDefinedAmongTwoProviders() {
        // case with two providers where one the two RAOs is specifically selected
        Rao.Runner definedRao = Rao.find("GlobalRAOptimizer", ImmutableList.of(new RaoProviderMock(), new AnotherRaoProviderMock()), platformConfig);
        assertEquals("GlobalRAOptimizer", definedRao.getName());
        assertEquals("2.3", definedRao.getVersion());
    }

    @Test
    void testDefaultNoProvider() {
        // case with no provider
        List<RaoProvider> raoProviders = ImmutableList.of();
        FaraoException exception = assertThrows(FaraoException.class, () -> Rao.find(null, raoProviders, platformConfig));
        assertEquals("", exception.getMessage());
    }

    @Test
    void testDefaultTwoProvidersPlatformConfig() {
        // case with 2 providers without any config but specifying which one to use in platform config
        platformConfig.createModuleConfig("rao").setStringProperty("default", "GlobalRAOptimizer");
        Rao.Runner globalRaOptimizer = Rao.find(null, ImmutableList.of(new RaoProviderMock(), new AnotherRaoProviderMock()), platformConfig);
        assertEquals("GlobalRAOptimizer", globalRaOptimizer.getName());
        assertEquals("2.3", globalRaOptimizer.getVersion());
    }

    @Test
    void testOneProviderAndMistakeInPlatformConfig() {
        // case with 1 provider with config but with a name that is not the one of provider.
        platformConfig.createModuleConfig("rao").setStringProperty("default", "UnknownRao");
        List<RaoProvider> raoProviders = ImmutableList.of(new RaoProviderMock());
        FaraoException exception = assertThrows(FaraoException.class, () -> Rao.find(null, raoProviders, platformConfig));
        assertEquals("", exception.getMessage());
    }
}
