/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.usage_rule.OnFlowConstraintInCountry;
import com.powsybl.open_rao.data.crac_api.usage_rule.OnFlowConstraintInCountryAdder;
import com.powsybl.open_rao.data.crac_api.usage_rule.UsageMethod;
import com.powsybl.iidm.network.Country;

import static com.powsybl.open_rao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnFlowConstraintInCountryAdderImpl<T extends AbstractRemedialActionAdder<T>> implements OnFlowConstraintInCountryAdder<T> {

    public static final String ON_FLOW_CONSTRAINT_IN_COUNTRY = "OnFlowConstraintInCountry";
    private T owner;
    private String instantId;
    private Country country;
    private UsageMethod usageMethod;

    OnFlowConstraintInCountryAdderImpl(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public OnFlowConstraintInCountryAdder<T> withInstant(String instantId) {
        this.instantId = instantId;
        return this;
    }

    @Override
    public OnFlowConstraintInCountryAdder<T> withUsageMethod(UsageMethod usageMethod) {
        this.usageMethod = usageMethod;
        return this;
    }

    @Override
    public OnFlowConstraintInCountryAdder<T> withCountry(Country country) {
        this.country = country;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(instantId, ON_FLOW_CONSTRAINT_IN_COUNTRY, "instant", "withInstant()");
        assertAttributeNotNull(country, ON_FLOW_CONSTRAINT_IN_COUNTRY, "country", "withCountry()");
        assertAttributeNotNull(usageMethod, ON_FLOW_CONSTRAINT_IN_COUNTRY, "usage method", "withUsageMethod()");

        Instant instant = owner.getCrac().getInstant(instantId);
        if (instant.isOutage()) {
            throw new OpenRaoException("OnFlowConstraintInCountry usage rules are not allowed for OUTAGE instant.");
        }
        if (instant.isPreventive()) {
            owner.getCrac().addPreventiveState();
        }

        OnFlowConstraintInCountry onFlowConstraint = new OnFlowConstraintInCountryImpl(usageMethod, instant, country);
        owner.addUsageRule(onFlowConstraint);
        return owner;
    }
}
