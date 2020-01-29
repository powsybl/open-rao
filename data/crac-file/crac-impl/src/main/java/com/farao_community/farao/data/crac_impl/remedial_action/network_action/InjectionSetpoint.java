/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * Injection setpoint remedial action: set a load or generator at a given value.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public final class InjectionSetpoint extends AbstractNetworkElementAction {

    private double setpoint;

    @JsonCreator
    public InjectionSetpoint(@JsonProperty("id") String id,
                             @JsonProperty("name") String name,
                             @JsonProperty("operator") String operator,
                             @JsonProperty("usageRules") List<UsageRule> usageRules,
                             @JsonProperty("networkElement") NetworkElement networkElement,
                             @JsonProperty("setpoint")  double setpoint) {
        super(id, name, operator, usageRules, networkElement);
        this.setpoint = setpoint;
    }

    public InjectionSetpoint(String id,
                             NetworkElement networkElement,
                             double setpoint) {
        super(id, networkElement);
        this.setpoint = setpoint;
    }

    public double getSetpoint() {
        return setpoint;
    }

    public void setSetpoint(double setpoint) {
        this.setpoint = setpoint;
    }

    @Override
    public void apply(Network network) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InjectionSetpoint otherInjectionSetpoint = (InjectionSetpoint) o;

        return super.equals(o) && setpoint == otherInjectionSetpoint.getSetpoint();
    }

    @Override
    public int hashCode() {
        return String.format("%s%f", getId(), setpoint).hashCode();
    }
}
