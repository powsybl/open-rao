/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_api.network_action;

import com.powsybl.open_rao.data.crac_api.NetworkElement;

/***
 * A SwitchPair action is an Elementary Action which consists in changing
 * closing one switch and opening the other.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface SwitchPair extends ElementaryAction {
    /**
     * Get the switch that should be opened by the elementary action
     */
    public NetworkElement getSwitchToOpen();

    /**
     * Get the switch that should be closed by the elementary action
     */
    public NetworkElement getSwitchToClose();
}
