/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.range;

import com.farao_community.farao.data.crac_api.range_action.StandardRangeActionAdder;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface StandardRangeAdder<T extends StandardRangeActionAdder<T>> {

    StandardRangeAdder<T> withMin(double minSetpoint);

    StandardRangeAdder<T> withMax(double maxSetpoint);

    T add();
}
