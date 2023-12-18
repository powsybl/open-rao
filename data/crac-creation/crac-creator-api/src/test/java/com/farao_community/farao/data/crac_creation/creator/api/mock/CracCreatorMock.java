/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_creation.creator.api.mock;

import com.powsybl.open_rao.data.crac_creation.creator.api.CracCreator;
import com.powsybl.open_rao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;

import java.time.OffsetDateTime;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(CracCreator.class)
public class CracCreatorMock implements CracCreator<NativeCracMock, CracCreationContextMock> {

    @Override
    public String getNativeCracFormat() {
        return "MockedNativeCracFormat";
    }

    @Override
    public CracCreationContextMock createCrac(NativeCracMock nativeCrac, Network network, OffsetDateTime offsetDateTime, CracCreationParameters cracCreationParameters) {
        return new CracCreationContextMock(nativeCrac.isOk());
    }
}
