/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;

/**
 * Injection range remedial action: choose the optimal value for a load or generator setpoint.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public final class InjectionRange extends AbstractNetworkElementRangeAction {

    @JsonCreator
    public InjectionRange(@JsonProperty("networkElement") NetworkElement networkElement) {
        super(networkElement);
    }

    @Override
    public void apply(Network network, double setpoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NetworkElement> getNetworkElements() {
        List<NetworkElement> networkElements = new ArrayList<>();
        networkElements.add(this.networkElement);
        return networkElements;
    }
}
