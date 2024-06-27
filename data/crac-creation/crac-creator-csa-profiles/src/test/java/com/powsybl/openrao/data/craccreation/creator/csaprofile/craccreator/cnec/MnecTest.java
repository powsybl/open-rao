/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class MnecTest {

    @Test
    void importOptimizedAndMonitoredAssessedElements() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/cnecs/SecuredAndScannedAssessedElements.zip", NETWORK);

        assertEquals(7, cracCreationContext.getCrac().getFlowCnecs().size());
        assertTrue(cracCreationContext.getCrac().getFlowCnec("RTE_AE2 (ae-2) - preventive - TWO").isOptimized());
        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE2 (ae-2) - preventive - TWO").isMonitored());

        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE3 (ae-3) - preventive - TWO").isOptimized());
        assertTrue(cracCreationContext.getCrac().getFlowCnec("RTE_AE3 (ae-3) - preventive - TWO").isMonitored());

        assertTrue(cracCreationContext.getCrac().getFlowCnec("RTE_AE5 (ae-5) - preventive - TWO").isOptimized());
        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE5 (ae-5) - preventive - TWO").isMonitored());

        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE6 (ae-6) - preventive - TWO").isOptimized());
        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE6 (ae-6) - preventive - TWO").isMonitored());

        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE7 (ae-7) - preventive - TWO").isOptimized());
        assertTrue(cracCreationContext.getCrac().getFlowCnec("RTE_AE7 (ae-7) - preventive - TWO").isMonitored());

        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE8 (ae-8) - preventive - TWO").isOptimized());
        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE8 (ae-8) - preventive - TWO").isMonitored());

        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE9 (ae-9) - preventive - TWO").isOptimized());
        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE9 (ae-9) - preventive - TWO").isMonitored());

        List<CsaProfileElementaryCreationContext> notImportedCnecCreationContexts = cracCreationContext.getCnecCreationContexts().stream().filter(c -> !c.isImported())
            .sorted(Comparator.comparing(CsaProfileElementaryCreationContext::getNativeId)).toList();
        assertEquals(2, notImportedCnecCreationContexts.size());

        assertEquals("ae-1", notImportedCnecCreationContexts.get(0).getNativeId());
        assertEquals(ImportStatus.INCONSISTENCY_IN_DATA, notImportedCnecCreationContexts.get(0).getImportStatus());
        assertEquals("AssessedElement ae-1 ignored because an AssessedElement cannot be optimized and monitored at the same time", notImportedCnecCreationContexts.get(0).getImportStatusDetail());

        assertEquals("ae-4", notImportedCnecCreationContexts.get(1).getNativeId());
        assertEquals(ImportStatus.INCONSISTENCY_IN_DATA, notImportedCnecCreationContexts.get(1).getImportStatus());
        assertEquals("AssessedElement ae-4 ignored because an AssessedElement cannot be optimized and monitored at the same time", notImportedCnecCreationContexts.get(1).getImportStatusDetail());

    }
}
