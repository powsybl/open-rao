/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.flowbaseddomain.json;

import com.powsybl.openrao.data.flowbaseddomain.DataGlskFactors;
import com.powsybl.openrao.data.flowbaseddomain.DataMonitoredBranch;
import com.powsybl.openrao.data.flowbaseddomain.DataPostContingency;
import com.powsybl.openrao.data.flowbaseddomain.DataDomain;
import com.powsybl.commons.test.AbstractSerDeTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class JsonFlowbasedDomainTest extends AbstractSerDeTest {

    private static final double EPSILON = 1e-3;

    private static DataDomain create() {
        return JsonFlowbasedDomain.read(JsonFlowbasedDomainTest.class.getResourceAsStream("/dataDomain.json"));
    }

    private static DataDomain read(Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (InputStream is = Files.newInputStream(jsonFile)) {
            return JsonFlowbasedDomain.read(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(DataDomain results, Path jsonFile) {
        Objects.requireNonNull(results);
        Objects.requireNonNull(jsonFile);

        try (OutputStream os = Files.newOutputStream(jsonFile)) {
            JsonFlowbasedDomain.write(results, os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void roundTripTest() throws IOException {
        roundTripTest(create(), JsonFlowbasedDomainTest::write, JsonFlowbasedDomainTest::read, "/dataDomain.json");
    }

    @Test
    void testUtilityMethods() {
        DataDomain flowbasedDomain = JsonFlowbasedDomainTest.create();

        assertNotNull(flowbasedDomain.getDataPreContingency().findMonitoredBranchById("FLOWBASED_DATA_DOMAIN_BRANCH_1"));
        assertNull(flowbasedDomain.getDataPreContingency().findMonitoredBranchById("FLOWBASED_DATA_DOMAIN_BRANCH_2"));

        assertNotNull(flowbasedDomain.getDataPreContingency().findMonitoredBranchById("FLOWBASED_DATA_DOMAIN_BRANCH_1").findPtdfByCountry("France"));
        assertNull(flowbasedDomain.getDataPreContingency().findMonitoredBranchById("FLOWBASED_DATA_DOMAIN_BRANCH_1").findPtdfByCountry("Austria"));
    }

    @Test
    void testExceptionCases() {
        InputStream resource = getClass().getResourceAsStream("/notExistingFile.json");
        assertThrows(IllegalArgumentException.class, () -> JsonFlowbasedDomain.read(resource));
    }

    @Test
    void testGetters() {
        DataDomain flowbasedDomain = JsonFlowbasedDomainTest.create();
        assertEquals("FLOWBASED_DATA_DOMAIN_ID", flowbasedDomain.getId());
        assertEquals("This is an example of Flow-based data domain inputs for Open RAO", flowbasedDomain.getName());
        assertEquals("JSON", flowbasedDomain.getSourceFormat());
        assertEquals("This is an example of Flow-based inputs for Open RAO", flowbasedDomain.getDescription());

        assertNotNull(flowbasedDomain.getDataPreContingency());
        assertEquals(1, flowbasedDomain.getDataPreContingency().getDataMonitoredBranches().size());
        DataMonitoredBranch preventiveBranch = flowbasedDomain.getDataPreContingency().getDataMonitoredBranches().get(0);
        assertEquals("FLOWBASED_DATA_DOMAIN_BRANCH_1", preventiveBranch.getId());
        assertEquals("France-Germany interconnector", preventiveBranch.getName());
        assertEquals("FFR2AA1  DDE3AA1  1", preventiveBranch.getBranchId());
        testDataMonitoredBranch(preventiveBranch, -2200., 2300., 123456., 5);

        assertEquals(1, flowbasedDomain.getDataPostContingency().size());
        DataPostContingency postContingency = flowbasedDomain.getDataPostContingency().get(0);
        assertEquals("CONTINGENCY", postContingency.getContingencyId());
        assertEquals(1, postContingency.getDataMonitoredBranches().size());
        DataMonitoredBranch curativeBranch = postContingency.getDataMonitoredBranches().get(0);
        assertEquals("FLOWBASED_DATA_DOMAIN_BRANCH_N_1_1", curativeBranch.getId());
        assertEquals("France-Germany interconnector", curativeBranch.getName());
        assertEquals("FFR2AA1  DDE3AA1  1", curativeBranch.getBranchId());
        testDataMonitoredBranch(curativeBranch, -2200., 2300., 1234567., 5);

        assertEquals(3, flowbasedDomain.getGlskData().size());
        DataGlskFactors dataGlskFactors = flowbasedDomain.getGlskData().get(0);
        assertEquals("France", dataGlskFactors.getAreaId());
        assertEquals(4, dataGlskFactors.getGlskFactors().size());
        Map<String, Float> glskMap = dataGlskFactors.getGlskFactors();
        assertEquals(0.2, glskMap.get("FR1 _generator"), 0.001);
    }

    private void testDataMonitoredBranch(DataMonitoredBranch dataMonitoredBranch, double fmin, double fmax, double fref, int nbPtdf) {
        assertEquals(fmin, dataMonitoredBranch.getFmin(), EPSILON);
        assertEquals(fmax, dataMonitoredBranch.getFmax(), EPSILON);
        assertEquals(fref, dataMonitoredBranch.getFref(), EPSILON);
        assertEquals(nbPtdf, dataMonitoredBranch.getPtdfList().size());
    }
}
