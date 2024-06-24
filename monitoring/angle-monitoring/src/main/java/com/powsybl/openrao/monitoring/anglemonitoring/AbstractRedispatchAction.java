/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.powsybl.openrao.monitoring.anglemonitoring;

import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * This abstract implementation uses a Scalable to apply redispatching
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public abstract class AbstractRedispatchAction implements RedispatchAction {
    protected void apply(Network network, double powerToRedispatch, Scalable scalable) {
        double redispatchedPower = scalable.scale(network, powerToRedispatch);
        if (Math.abs(redispatchedPower - powerToRedispatch) > 1) {
            BUSINESS_WARNS.warn("Redispatching failed: asked={} MW, applied={} MW", powerToRedispatch, redispatchedPower);
        }
    }
}
