/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.google.auto.service.AutoService;

import java.util.List;

/**
 * Crac Factory implementation.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(CracFactory.class)
public class CracImplFactory implements CracFactory {

    private static final String NAME = "CracImplFactory";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Crac create(String id, String name) {
        Crac crac = new CracImpl(id, name);
        // TODO : move this
        crac.setInstants(
            List.of(
                Instant.preventive(),
                Instant.outage(),
                Instant.auto(2, "auto"),
                Instant.curative(3, "curative")
            )
        );
        return crac;
    }

    @Override
    public Crac create(String id) {
        Crac crac = new CracImpl(id);
        // TODO : move this
        crac.setInstants(
            List.of(
                Instant.preventive(),
                Instant.outage(),
                Instant.auto(2, "auto"),
                Instant.curative(3, "curative")
            )
        );
        return crac;
    }
}
