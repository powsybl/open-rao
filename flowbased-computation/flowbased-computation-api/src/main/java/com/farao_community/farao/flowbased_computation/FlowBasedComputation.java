/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.powsybl.iidm.network.Network;

import java.util.concurrent.CompletableFuture;

/**
 * Flowbased computation interface
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public interface FlowBasedComputation {
    /**
     * @param workingStateId working state id of completable future
     * @param parameters flow based computation parameters
     * @return
     */
    CompletableFuture<FlowBasedComputationResult> run(Network network, String workingStateId, FlowBasedComputationParameters parameters);
}
