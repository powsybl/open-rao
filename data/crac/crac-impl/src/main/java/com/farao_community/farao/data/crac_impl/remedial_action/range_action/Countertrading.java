/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.AbstractRemedialAction;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationResults;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Countertrading remedial action.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("countertrading")
public class Countertrading extends AbstractRemedialAction implements RangeAction {

    public static final int TEMP_VALUE = 0;

    @JsonCreator
    public Countertrading(@JsonProperty("id") String id,
                          @JsonProperty("name") String name,
                          @JsonProperty("operator") String operator,
                          @JsonProperty("usageRules") List<UsageRule> usageRules) {
        super(id, name, operator, usageRules);
    }

    public Countertrading(String id, String name, String operator) {
        super(id, name, operator);
    }

    @Override
    public double getMinValue(Network network) {
        return TEMP_VALUE;
    }

    @Override
    public double getMaxValue(Network network) {
        return TEMP_VALUE;
    }

    @Override
    public double getMaxNegativeVariation(Network network) {
        return TEMP_VALUE;
    }

    @Override
    public double getMaxPositiveVariation(Network network) {
        return TEMP_VALUE;
    }

    @Override
    public double getSensitivityValue(SensitivityComputationResults sensitivityComputationResults, Cnec cnec) {
        return TEMP_VALUE;
    }

    @Override
    public void apply(Network network, double setpoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return new HashSet<>();
    }

    @Override
    public void synchronize(Network network) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void desynchronize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSynchronized() {
        throw new UnsupportedOperationException();
    }
}
