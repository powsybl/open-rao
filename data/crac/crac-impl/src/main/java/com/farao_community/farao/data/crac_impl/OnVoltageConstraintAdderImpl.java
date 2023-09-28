/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.usage_rule.OnVoltageConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnVoltageConstraintAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;

import java.util.Objects;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
public class OnVoltageConstraintAdderImpl<T extends AbstractRemedialActionAdder<T>> implements OnVoltageConstraintAdder<T> {

    private final T owner;
    private Instant instant;
    private String voltageCnecId;
    private UsageMethod usageMethod;

    OnVoltageConstraintAdderImpl(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public OnVoltageConstraintAdder<T> withInstant(Instant instant) {
        this.instant = instant;
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
        assertAttributeNotNull(instant, "OnInstant", "instant", "withInstant()");
        assertAttributeNotNull(voltageCnecId, "OnVoltageConstraint", "voltage cnec", "withVoltageCnec()");

        if (Objects.isNull(usageMethod)) {
            throw new FaraoException("Since CRAC version 1.7, the field usageMethod is required for OnVoltageConstraint usage rules");
        }
        if (instant.equals(Instant.OUTAGE)) {
            throw new FaraoException("OnVoltageConstraint usage rules are not allowed for OUTAGE instant.");
        }
        if (instant.equals(Instant.PREVENTIVE)) {
            owner.getCrac().addPreventiveState();
        }

        VoltageCnec voltageCnec = owner.getCrac().getVoltageCnec(voltageCnecId);
        if (Objects.isNull(voltageCnec)) {
            throw new FaraoException(String.format("VoltageCnec %s does not exist in crac. Consider adding it first.", voltageCnecId));
        }

        AbstractRemedialActionAdder.checkOnConstraintUsageRules(instant, voltageCnec);

        OnVoltageConstraint onVoltageConstraint = new OnVoltageConstraintImpl(usageMethod, instant, voltageCnec);
        owner.addUsageRule(onVoltageConstraint);
        return owner;
    }
}
