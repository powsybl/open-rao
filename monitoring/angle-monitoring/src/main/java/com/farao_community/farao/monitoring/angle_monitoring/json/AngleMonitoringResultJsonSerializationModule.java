/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring.json;

import com.farao_community.farao.monitoring.angle_monitoring.AngleMonitoringResult;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class AngleMonitoringResultJsonSerializationModule extends SimpleModule {

    public AngleMonitoringResultJsonSerializationModule() {
        super();
        this.addSerializer(AngleMonitoringResult.class, new AngleMonitoringResultSerializer());
    }
}
