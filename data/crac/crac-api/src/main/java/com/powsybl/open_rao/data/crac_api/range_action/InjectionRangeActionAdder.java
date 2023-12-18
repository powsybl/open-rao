/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_api.range_action;

import com.powsybl.open_rao.data.crac_api.range.StandardRangeAdder;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface InjectionRangeActionAdder extends StandardRangeActionAdder<InjectionRangeActionAdder> {

    InjectionRangeActionAdder withNetworkElementAndKey(double key, String networkElementId);

    InjectionRangeActionAdder withNetworkElementAndKey(double key, String networkElementId, String networkElementName);

    InjectionRangeActionAdder withInitialSetpoint(double initialSetpoint);

    StandardRangeAdder<InjectionRangeActionAdder> newRange();

    InjectionRangeAction add();
}
