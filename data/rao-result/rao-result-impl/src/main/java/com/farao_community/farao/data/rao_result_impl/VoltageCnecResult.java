/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class VoltageCnecResult {

    private static final ElementaryVoltageCnecResult DEFAULT_RESULT = new ElementaryVoltageCnecResult();
    private final Map<String, ElementaryVoltageCnecResult> results;

    VoltageCnecResult() {
        results = new HashMap<>();
    }

    public ElementaryVoltageCnecResult getResult(String optimizedInstantId) {
        return results.getOrDefault(optimizedInstantId, DEFAULT_RESULT);
    }

    public ElementaryVoltageCnecResult getAndCreateIfAbsentResultForOptimizationState(String optimizedInstantId) {
        results.putIfAbsent(optimizedInstantId, new ElementaryVoltageCnecResult());
        return results.get(optimizedInstantId);
    }
}
