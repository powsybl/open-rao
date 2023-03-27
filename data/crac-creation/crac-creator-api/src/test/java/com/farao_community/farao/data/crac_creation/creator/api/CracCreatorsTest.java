/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.api;

import com.farao_community.farao.data.crac_creation.creator.api.mock.CracCreatorMock;
import com.farao_community.farao.data.crac_creation.creator.api.mock.NativeCracMock;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Paths;
import java.time.OffsetDateTime;

import static com.farao_community.farao.data.crac_creation.creator.api.CracCreators.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class CracCreatorsTest {

    private Network network;
    private OffsetDateTime offsetDateTime;

    @BeforeEach
    public void setUp() {
        network = Mockito.mock(Network.class);
        offsetDateTime = OffsetDateTime.parse("2020-01-01T01:00:00Z");
    }

    @Test
    void testFindCreatorKnownFormat() {
        CracCreator cracCreator = findCreator("MockedNativeCracFormat");
        assertNotNull(cracCreator);
        assertTrue(cracCreator instanceof CracCreatorMock);
    }

    @Test
    void testFindCreatorTestFormat() {
        CracCreator cracCreator = findCreator("UnknownFormat");
        assertNull(cracCreator);
    }

    @Test
    void testCreateCrac() {
        CracCreationContext cracCreationContext = createCrac(new NativeCracMock(true), network, offsetDateTime);
        assertTrue(cracCreationContext.isCreationSuccessful());

        cracCreationContext = createCrac(new NativeCracMock(false), network, offsetDateTime);
        assertFalse(cracCreationContext.isCreationSuccessful());
    }

    @Test
    void testCreateCracWithFactory() {
        CracCreationContext cracCreationContext = createCrac(new NativeCracMock(true), network, offsetDateTime, new CracCreationParameters());
        assertTrue(cracCreationContext.isCreationSuccessful());
    }

    @Test
    void testCreateAndImportCracFromInputStream() {
        CracCreationContext cracCreationContext = CracCreators.importAndCreateCrac("empty.txt", getClass().getResourceAsStream("/empty.txt"), network, offsetDateTime);
        assertTrue(cracCreationContext.isCreationSuccessful());
    }

    @Test
    void testCreateAndImportCracFromPath() {
        CracCreationContext cracCreationContext = CracCreators.importAndCreateCrac(Paths.get(new File(getClass().getResource("/empty.txt").getFile()).getAbsolutePath()), network, offsetDateTime);
        assertTrue(cracCreationContext.isCreationSuccessful());
    }
}
