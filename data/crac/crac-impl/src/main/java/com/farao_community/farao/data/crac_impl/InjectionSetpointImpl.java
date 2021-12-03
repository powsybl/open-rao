/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.network_action.InjectionSetpoint;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.powsybl.iidm.network.*;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Collections;
import java.util.Set;

/**
 * Injection setpoint remedial action: set a load or generator at a given value.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class InjectionSetpointImpl implements InjectionSetpoint {

    private NetworkElement networkElement;
    private double setpoint;

    InjectionSetpointImpl(NetworkElement networkElement, double setpoint) {
        this.networkElement = networkElement;
        this.setpoint = setpoint;
    }

    @Override
    public double getSetpoint() {
        return setpoint;
    }

    @Override
    public boolean canBeApplied(Network network) {
        // TODO : we can return false if the network element is already at the target setpoint
        return true;
    }

    @Override
    public void apply(Network network) {
        Identifiable<?> identifiable = network.getIdentifiable(networkElement.getId());
        if (identifiable instanceof Generator) {
            Generator generator = (Generator) identifiable;
            generator.setTargetP(setpoint);
        } else if (identifiable instanceof Load) {
            Load load = (Load) identifiable;
            load.setP0(setpoint);
        } else if (identifiable instanceof DanglingLine) {
            DanglingLine danglingLine = (DanglingLine) identifiable;
            danglingLine.setP0(setpoint);
        } else {
            throw new NotImplementedException("Injection setpoint only handled for generators, loads or dangling lines");
        }
    }

    @Override
    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InjectionSetpointImpl oInjectionSetPoint =  (InjectionSetpointImpl) o;
        return oInjectionSetPoint.getNetworkElement().equals(this.networkElement)
            && oInjectionSetPoint.getSetpoint() == this.setpoint;
    }

    @Override
    public int hashCode() {
        return networkElement.hashCode() + 37 * Double.valueOf(setpoint).hashCode();
    }
}
