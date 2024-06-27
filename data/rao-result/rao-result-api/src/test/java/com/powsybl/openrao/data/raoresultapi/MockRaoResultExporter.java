/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresultapi;

import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.raoresultapi.io.Exporter;

import java.io.OutputStream;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(Exporter.class)
public class MockRaoResultExporter implements Exporter {
    @Override
    public String getFormat() {
        return "Mock";
    }

    @Override
    public void exportData(RaoResult raoResult, Crac crac, Set<Unit> flowUnits, OutputStream outputStream) {
        if (raoResult instanceof MockRaoResult mockRaoResult) {
            mockRaoResult.setExportSuccessful();
        }
    }
}
