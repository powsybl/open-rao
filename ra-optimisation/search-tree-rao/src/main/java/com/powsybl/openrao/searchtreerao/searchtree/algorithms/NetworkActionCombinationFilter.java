/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;

import java.util.Map;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface NetworkActionCombinationFilter {
    Map<NetworkActionCombination, Boolean> filter(Map<NetworkActionCombination, Boolean> naCombinations, Leaf fromLeaf);
}
