/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.adapter;

import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface BranchResultAdapter {

    FlowResult getResult(SystematicSensitivityResult systematicSensitivityResult, Network network);
}
