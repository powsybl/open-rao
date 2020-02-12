/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.mocks;

import com.farao_community.farao.data.crac_api.*;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationResults;

import java.util.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionMock implements RangeAction {
    private String id;
    private NetworkElement networkElement;
    private double currentValue;
    private double minValue;
    private double maxValue;
    private Map<Cnec, Double> sensitivityValues;

    public RangeActionMock(String id, String networkElementId, double currentValue, double minValue, double maxValue, Map<Cnec, Double> sensitivityValues) {
        this.id = id;
        networkElement = new NetworkElement(networkElementId);
        this.currentValue = currentValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.sensitivityValues = sensitivityValues;
    }

    @Override
    public void apply(Network network, double setpoint) {

    }

    @Override
    public void synchronize(Network network) {

    }

    @Override
    public void desynchronize() {
        //
    }

    @Override
    public String getOperator() {
        return null;
    }

    @Override
    public UsageMethod getUsageMethod(Network network, State state) {
        return null;
    }

    @Override
    public List<UsageRule> getUsageRules() {
        return null;
    }

    @Override
    public void addUsageRule(UsageRule usageRule) {

    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        HashSet<NetworkElement> networkElements = new HashSet<>();
        networkElements.add(networkElement);
        return networkElements;
    }

    @Override
    public double getMinValue(Network network) {
        return minValue;
    }

    @Override
    public double getMaxValue(Network network) {
        return maxValue;
    }

    @Override
    public double getMaxNegativeVariation(Network network) {
        return Math.abs(currentValue - minValue);
    }

    @Override
    public double getMaxPositiveVariation(Network network) {
        return Math.abs(currentValue - maxValue);
    }

    @Override
    public double getSensitivityValue(SensitivityComputationResults sensitivityComputationResults, Cnec cnec) {
        return sensitivityValues.get(cnec);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return id;
    }

    @Override
    public void addExtension(Class aClass, Extension extension) {

    }

    @Override
    public Extension getExtension(Class aClass) {
        return null;
    }

    @Override
    public Extension getExtensionByName(String s) {
        return null;
    }

    @Override
    public boolean removeExtension(Class aClass) {
        return false;
    }

    @Override
    public Collection getExtensions() {
        return null;
    }
}
