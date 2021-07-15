/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business object of a group of elementary remedial actions (range or network action).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public abstract class AbstractRemedialAction<I extends RemedialAction<I>> extends AbstractIdentifiable<I> implements RemedialAction<I> {
    protected String operator;
    protected List<UsageRule> usageRules;

    protected AbstractRemedialAction(String id, String name, String operator, List<UsageRule> usageRules) {
        super(id, name);
        this.operator = operator;
        this.usageRules = usageRules;
    }

    @Override
    public String getOperator() {
        return operator;
    }

    @Override
    public final List<UsageRule> getUsageRules() {
        return usageRules;
    }

    @Override
    public UsageMethod getUsageMethod(State state) {
        Set<UsageMethod> usageMethods = usageRules.stream()
            .map(usageRule -> usageRule.getUsageMethod(state))
            .collect(Collectors.toSet());

        if (usageMethods.contains(UsageMethod.UNAVAILABLE)) {
            return UsageMethod.UNAVAILABLE;
        } else if (usageMethods.contains(UsageMethod.FORCED)) {
            return UsageMethod.FORCED;
        } else if (usageMethods.contains(UsageMethod.AVAILABLE)) {
            return UsageMethod.AVAILABLE;
        } else if (usageMethods.contains(UsageMethod.TO_BE_EVALUATED)) {
            return UsageMethod.TO_BE_EVALUATED;
        } else {
            return UsageMethod.UNAVAILABLE;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractRemedialAction<?> remedialAction = (AbstractRemedialAction<?>) o;
        return super.equals(remedialAction) && new HashSet<>(usageRules).equals(new HashSet<>(remedialAction.getUsageRules())) && operator.equals(remedialAction.operator);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
