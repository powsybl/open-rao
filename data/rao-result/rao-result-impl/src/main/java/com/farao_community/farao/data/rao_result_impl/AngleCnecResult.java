/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.data.crac_api.Instant;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class AngleCnecResult {

    private static final ElementaryAngleCnecResult DEFAULT_RESULT = new ElementaryAngleCnecResult();
    private final Map<Instant, ElementaryAngleCnecResult> resultPerOptimizedInstant;

    AngleCnecResult() {
        resultPerOptimizedInstant = new HashMap<>();
    }

    public ElementaryAngleCnecResult getResult(Instant optimizedInstant) {
        return resultPerOptimizedInstant.getOrDefault(optimizedInstant, DEFAULT_RESULT);
    }

    public ElementaryAngleCnecResult getAndCreateIfAbsentResultForOptimizationState(Instant optimizedInstant) {
        resultPerOptimizedInstant.putIfAbsent(optimizedInstant, new ElementaryAngleCnecResult());
        return resultPerOptimizedInstant.get(optimizedInstant);
    }
}
