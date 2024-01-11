/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.flowbasedcomputation;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.commons.Versionable;
import com.powsybl.commons.config.PlatformConfigNamedProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.concurrent.CompletableFuture;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public interface FlowbasedComputationProvider extends Versionable, PlatformConfigNamedProvider {
    CompletableFuture<FlowbasedComputationResult> run(Network network, Crac crac, RaoResult raoResult, ZonalData<SensitivityVariableSet> glsk, FlowbasedComputationParameters parameters);
}
