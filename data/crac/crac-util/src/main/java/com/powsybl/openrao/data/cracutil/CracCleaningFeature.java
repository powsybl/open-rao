/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracutil;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public enum CracCleaningFeature {
    CHECK_CNEC_MNEC(true),
    REMOVE_UNHANDLED_CONTINGENCIES(false);

    boolean enabled;

    CracCleaningFeature(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }
}
