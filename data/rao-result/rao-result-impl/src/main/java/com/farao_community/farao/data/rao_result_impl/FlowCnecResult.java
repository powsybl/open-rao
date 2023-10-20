/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class FlowCnecResult {

    private static final ElementaryFlowCnecResult DEFAULT_RESULT = new ElementaryFlowCnecResult();
    private final Map<String, ElementaryFlowCnecResult> results;

    FlowCnecResult() {
        results = new HashMap<>();
    }

    public ElementaryFlowCnecResult getResult(String optimizedInstantId) {
        return results.getOrDefault(optimizedInstantId, DEFAULT_RESULT);
    }

    public ElementaryFlowCnecResult getAndCreateIfAbsentResultForOptimizationState(String optimizedInstantId) {
        results.putIfAbsent(optimizedInstantId, new ElementaryFlowCnecResult());
        return results.get(optimizedInstantId);
    }
}
