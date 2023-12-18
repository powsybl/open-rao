/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.flowbased_computation;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.rao_result_api.RaoResult;
import com.powsybl.commons.Versionable;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.config.PlatformConfigNamedProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * FlowBased main API. It is a utility class (so with only static methods) used as an entry point for running
 * a flowbased computation allowing to choose either a specific find implementation or just to rely on default one.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class FlowbasedComputation {

    private FlowbasedComputation() {
        throw new AssertionError("Utility class should not been instantiated");
    }

    /**
     * A FlowBased  computation runner is responsible for providing convenient methods on top of {@link FlowbasedComputationProvider}:
     * several variants of synchronous and asynchronous run with default parameters.
     */
    public static class Runner implements Versionable {

        private final FlowbasedComputationProvider provider;

        public Runner(FlowbasedComputationProvider provider) {
            this.provider = Objects.requireNonNull(provider);
        }

        public CompletableFuture<FlowbasedComputationResult> runAsync(Network network, Crac crac, ZonalData<SensitivityVariableSet> glsk, FlowbasedComputationParameters parameters) {
            Objects.requireNonNull(network);
            Objects.requireNonNull(crac);
            Objects.requireNonNull(glsk);
            Objects.requireNonNull(parameters);
            return provider.run(network, crac, null, glsk, parameters);
        }

        public CompletableFuture<FlowbasedComputationResult> runAsync(Network network, Crac crac, ZonalData<SensitivityVariableSet> glsk) {
            return runAsync(network, crac, glsk, FlowbasedComputationParameters.load());
        }

        public FlowbasedComputationResult run(Network network, Crac crac, ZonalData<SensitivityVariableSet> glsk, RaoResult raoResult, FlowbasedComputationParameters parameters) {
            Objects.requireNonNull(network);
            Objects.requireNonNull(crac);
            Objects.requireNonNull(glsk);
            Objects.requireNonNull(parameters);
            return provider.run(network, crac, raoResult, glsk, parameters).join();
        }

        public FlowbasedComputationResult run(Network network, Crac crac, RaoResult raoResult, ZonalData<SensitivityVariableSet> glsk) {
            return run(network, crac, glsk, raoResult, FlowbasedComputationParameters.load());
        }

        @Override
        public String getName() {
            return provider.getName();
        }

        @Override
        public String getVersion() {
            return provider.getVersion();
        }
    }

    /**
     * Get a runner for flowbased implementation named {@code name}. In the case of a null {@code name}, default
     * implementation is used.
     *
     * @param name name of the flowbased implementation, null if we want to use default one
     * @return a runner for flowbased implementation named {@code name}
     */
    public static Runner find(String name) {
        return new Runner(PlatformConfigNamedProvider.Finder.find(name, "flowbased-computation", FlowbasedComputationProvider.class,
                PlatformConfig.defaultConfig()));
    }

    /**
     * Get a runner for default flowbased implementation.
     *
     * @throws OpenRaoException in case we cannot find a default implementation
     * @return a runner for default flowbased implementation
     */
    public static Runner find() {
        return find(null);
    }

    public static CompletableFuture<FlowbasedComputationResult> runAsync(Network network, Crac crac, ZonalData<SensitivityVariableSet> glsk, FlowbasedComputationParameters parameters) {
        return find().runAsync(network, crac, glsk, parameters);
    }

    public static CompletableFuture<FlowbasedComputationResult> runAsync(Network network, Crac crac, ZonalData<SensitivityVariableSet> glsk) {
        return find().runAsync(network, crac, glsk);
    }

    public static FlowbasedComputationResult run(Network network, Crac crac, RaoResult raoResult, ZonalData<SensitivityVariableSet> glsk, FlowbasedComputationParameters parameters) {
        return find().run(network, crac, glsk, raoResult, parameters);
    }

    public static FlowbasedComputationResult run(Network network, Crac crac, ZonalData<SensitivityVariableSet> glsk, RaoResult raoResult) {
        return find().run(network, crac, raoResult, glsk);
    }
}
