/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.cnec.VoltageCnec;
import com.powsybl.open_rao.data.crac_api.usage_rule.OnVoltageConstraint;
import com.powsybl.open_rao.data.crac_api.usage_rule.OnVoltageConstraintAdder;
import com.powsybl.open_rao.data.crac_api.usage_rule.UsageMethod;

import java.util.Objects;

import static com.powsybl.open_rao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
public class OnVoltageConstraintAdderImpl<T extends AbstractRemedialActionAdder<T>> implements OnVoltageConstraintAdder<T> {

    public static final String ON_VOLTAGE_CONSTRAINT = "OnVoltageConstraint";
    private final T owner;
    private String instantId;
    private String voltageCnecId;
    private UsageMethod usageMethod;

    OnVoltageConstraintAdderImpl(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public OnVoltageConstraintAdder<T> withInstant(String instantId) {
        this.instantId = instantId;
        return this;
    }

    @Override
    public OnVoltageConstraintAdder<T> withUsageMethod(UsageMethod usageMethod) {
        this.usageMethod = usageMethod;
        return this;
    }

    @Override
    public OnVoltageConstraintAdder<T> withVoltageCnec(String voltageCnecId) {
        this.voltageCnecId = voltageCnecId;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(instantId, ON_VOLTAGE_CONSTRAINT, "instant", "withInstant()");
        assertAttributeNotNull(voltageCnecId, ON_VOLTAGE_CONSTRAINT, "voltage cnec", "withVoltageCnec()");
        assertAttributeNotNull(usageMethod, ON_VOLTAGE_CONSTRAINT, "usage method", "withUsageMethod()");

        Instant instant = owner.getCrac().getInstant(instantId);
        if (instant.isOutage()) {
            throw new OpenRaoException("OnVoltageConstraint usage rules are not allowed for OUTAGE instant.");
        }
        if (instant.isPreventive()) {
            owner.getCrac().addPreventiveState();
        }

        VoltageCnec voltageCnec = owner.getCrac().getVoltageCnec(voltageCnecId);
        if (Objects.isNull(voltageCnec)) {
            throw new OpenRaoException(String.format("VoltageCnec %s does not exist in crac. Consider adding it first.", voltageCnecId));
        }

        AbstractRemedialActionAdder.checkOnConstraintUsageRules(instant, voltageCnec);

        OnVoltageConstraint onVoltageConstraint = new OnVoltageConstraintImpl(usageMethod, instant, voltageCnec);
        owner.addUsageRule(onVoltageConstraint);
        return owner;
    }
}
