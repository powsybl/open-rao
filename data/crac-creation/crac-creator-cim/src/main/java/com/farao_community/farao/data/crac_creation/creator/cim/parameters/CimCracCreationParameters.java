/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cim.parameters;

import com.farao_community.farao.data.crac_creation.creator.api.parameters.AbstractAlignedRaCracCreationParameters;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CimCracCreationParameters extends AbstractAlignedRaCracCreationParameters {

    @Override
    public String getName() {
        return "CimCracCreatorParameters";
    }

}
