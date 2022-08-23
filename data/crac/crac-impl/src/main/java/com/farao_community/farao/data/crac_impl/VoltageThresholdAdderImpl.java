/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnecAdder;
import com.farao_community.farao.data.crac_api.threshold.VoltageThresholdAdder;

import java.util.Objects;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class VoltageThresholdAdderImpl extends AbstractThresholdAdderImpl<VoltageThresholdAdder> implements VoltageThresholdAdder {

    private final VoltageCnecAdderImpl ownerAdder;

    VoltageThresholdAdderImpl(VoltageCnecAdder ownerAdder) {
        Objects.requireNonNull(ownerAdder);
        this.ownerAdder = (VoltageCnecAdderImpl) ownerAdder;
    }

    @Override
    public VoltageThresholdAdderImpl withUnit(Unit unit) {
        unit.checkPhysicalParameter(PhysicalParameter.VOLTAGE);
        this.unit = unit;
        return this;
    }

    @Override
    public VoltageCnecAdder add() {
        super.checkThreshold();

        ownerAdder.addThreshold(new ThresholdImpl(unit, min, max));
        return ownerAdder;
    }
}
