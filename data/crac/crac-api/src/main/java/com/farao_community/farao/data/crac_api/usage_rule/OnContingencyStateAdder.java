/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_api.usage_rule;

import com.farao_community.farao.data.crac_api.RemedialActionAdder;

/**
 * Adds a OnContingencyState usage rule to a RemedialActionAdder
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface OnContingencyStateAdder<T extends RemedialActionAdder<T>> {

    OnContingencyStateAdder<T> withContingency(String contingencyId);

    OnContingencyStateAdder<T> withInstantId(String instantId);

    OnContingencyStateAdder<T> withUsageMethod(UsageMethod usageMethod);

    T add();
}
