/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.remedial_action.AbstractRemedialAction;

import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractNetworkAction<I extends AbstractNetworkAction<I>> extends AbstractRemedialAction<I> implements NetworkAction<I> {
    public AbstractNetworkAction(String id, String name, String operator, List<UsageRule> usageRules) {
        super(id, name, operator, usageRules);
    }

    public AbstractNetworkAction(String id, String name, String operator) {
        super(id, name, operator);
    }

    public AbstractNetworkAction(String id, String operator) {
        super(id, operator);
    }

    public AbstractNetworkAction(String id) {
        super(id);
    }
}
