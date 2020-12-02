/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.usage_rule;

import com.farao_community.farao.data.crac_api.State;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The UsageRule define conditions under which a RemedialAction can be used.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface UsageRule {

    UsageMethod getUsageMethod();

    UsageMethod getUsageMethod(State state);
}
