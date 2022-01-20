/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * An interface with the engine that computes sensitivities and flows needed in the RAO.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class SystematicSensitivityInterface {
    /**
     * Name of sensitivity analysis provider
     */
    private String sensitivityProvider;

    /**
     * Sensitivity configurations, containing the default and fallback configurations
     * of the sensitivity analysis
     */
    private SensitivityAnalysisParameters defaultParameters;
    private SensitivityAnalysisParameters fallbackParameters;

    /**
     * The sensitivity provider to be used in the sensitivity analysis
     */
    private CnecSensitivityProvider cnecSensitivityProvider;

    /**
     * A boolean indicating whether or not the fallback mode of the sensitivity analysis
     * engine is active.
     */
    private boolean fallbackMode = false;

    /**
     * The remedialActions that are applied in the initial network or after some contingencies
     */
    private AppliedRemedialActions appliedRemedialActions;

    /**
     * Builder
     */
    public static final class SystematicSensitivityInterfaceBuilder {
        private String sensitivityProvider;
        private SensitivityAnalysisParameters defaultParameters;
        private SensitivityAnalysisParameters fallbackParameters;
        private MultipleSensitivityProvider multipleSensitivityProvider = new MultipleSensitivityProvider();
        private AppliedRemedialActions appliedRemedialActions;
        private boolean providerInitialised = false;

        private SystematicSensitivityInterfaceBuilder() {

        }

        public SystematicSensitivityInterfaceBuilder withSensitivityProviderName(String sensitivityProvider) {
            this.sensitivityProvider = sensitivityProvider;
            return this;
        }

        public SystematicSensitivityInterfaceBuilder withFallbackParameters(SensitivityAnalysisParameters fallbackParameters) {
            this.fallbackParameters = fallbackParameters;
            return this;
        }

        public SystematicSensitivityInterfaceBuilder withDefaultParameters(SensitivityAnalysisParameters defaultParameters) {
            this.defaultParameters = defaultParameters;
            return this;
        }

        public SystematicSensitivityInterfaceBuilder withSensitivityProvider(CnecSensitivityProvider cnecSensitivityProvider) {
            this.multipleSensitivityProvider.addProvider(cnecSensitivityProvider);
            providerInitialised = true;
            return this;
        }

        public SystematicSensitivityInterfaceBuilder withPtdfSensitivities(ZonalData<LinearGlsk> glsk, Set<FlowCnec> cnecs, Set<Unit> units) {
            return this.withSensitivityProvider(new PtdfSensitivityProvider(glsk, cnecs, units));
        }

        public SystematicSensitivityInterfaceBuilder withRangeActionSensitivities(Set<RangeAction<?>> rangeActions, Set<FlowCnec> cnecs, Set<Unit> units) {
            return this.withSensitivityProvider(new RangeActionSensitivityProvider(rangeActions, cnecs, units));
        }

        public SystematicSensitivityInterfaceBuilder withLoadflow(Set<FlowCnec> cnecs, Set<Unit> units) {
            return this.withSensitivityProvider(new LoadflowProvider(cnecs, units));
        }

        public SystematicSensitivityInterfaceBuilder withAppliedRemedialActions(AppliedRemedialActions appliedRemedialActions) {
            this.appliedRemedialActions = appliedRemedialActions;
            return this;
        }

        public SystematicSensitivityInterface build() {
            if (Objects.isNull(sensitivityProvider)) {
                throw new SensitivityAnalysisException("You must provide a sensitivity provider implementation name when building a SystematicSensitivityInterface.");
            }
            if (!providerInitialised) {
                throw new SensitivityAnalysisException("Sensitivity provider is mandatory when building a SystematicSensitivityInterface.");
            }
            if (Objects.isNull(defaultParameters)) {
                defaultParameters = new SensitivityAnalysisParameters();
            }
            SystematicSensitivityInterface systematicSensitivityInterface = new SystematicSensitivityInterface();
            systematicSensitivityInterface.sensitivityProvider = sensitivityProvider;
            systematicSensitivityInterface.defaultParameters = defaultParameters;
            systematicSensitivityInterface.fallbackParameters = fallbackParameters;
            systematicSensitivityInterface.cnecSensitivityProvider = multipleSensitivityProvider;
            systematicSensitivityInterface.appliedRemedialActions = appliedRemedialActions;
            return systematicSensitivityInterface;
        }
    }

    public static SystematicSensitivityInterfaceBuilder builder() {
        return new SystematicSensitivityInterfaceBuilder();
    }

    SystematicSensitivityInterface() {

    }

    public boolean isFallback() {
        return fallbackMode;
    }

    /**
     * Run the systematic sensitivity analysis on the given network and crac, and associates the
     * SystematicSensitivityResult to the given network variant.
     *
     * Throw a SensitivityAnalysisException if the computation fails.
     */
    public SystematicSensitivityResult run(Network network) {
        SensitivityAnalysisParameters sensitivityAnalysisParameters = fallbackMode ? fallbackParameters : defaultParameters;
        if (Objects.isNull(cnecSensitivityProvider)) {
            throw new SensitivityAnalysisException("Sensitivity provider was not defined.");
        }

        try {
            SystematicSensitivityResult result = runWithConfig(network, sensitivityAnalysisParameters);
            if (fallbackMode) {
                result.setStatus(SystematicSensitivityResult.SensitivityComputationStatus.FALLBACK);
            }
            return result;

        } catch (SensitivityAnalysisException e) {
            FaraoLoggerProvider.TECHNICAL_LOGS.debug("Exception occurred during sensitivity analysis", e);
            if (!fallbackMode && fallbackParameters != null) { // default mode fails, retry in fallback mode
                FaraoLoggerProvider.BUSINESS_WARNS.warn("Error while running the sensitivity analysis with default parameters, fallback sensitivity parameters are now used.");
                fallbackMode = true;
                refreshRequestedUnits();
                return run(network);
            } else if (!fallbackMode) { // no fallback mode available, throw an exception
                throw new SensitivityAnalysisException("Sensitivity analysis failed with default parameters. No fallback parameters available.", e);
            } else { // fallback mode fails, throw an exception
                throw new SensitivityAnalysisException("Sensitivity analysis failed with all available sensitivity parameters.", e);
            }
        }
    }

    private void refreshRequestedUnits() {
        SensitivityAnalysisParameters sensitivityAnalysisParameters = fallbackMode ? fallbackParameters : defaultParameters;
        Set<Unit> requestedUnits = new HashSet<>();
        requestedUnits.add(Unit.MEGAWATT);
        if (!sensitivityAnalysisParameters.getLoadFlowParameters().isDc()) {
            requestedUnits.add(Unit.AMPERE);
        }
        cnecSensitivityProvider.setRequestedUnits(requestedUnits);
    }

    /**
     * Run the systematic sensitivity analysis with given SensitivityComputationParameters, throw a
     * SensitivityComputationException is the computation fails.
     */
    private SystematicSensitivityResult runWithConfig(Network network, SensitivityAnalysisParameters sensitivityAnalysisParameters) {
        try {
            SystematicSensitivityResult tempSystematicSensitivityAnalysisResult = SystematicSensitivityAdapter
                .runSensitivity(network, cnecSensitivityProvider, appliedRemedialActions, sensitivityAnalysisParameters, sensitivityProvider);

            if (!tempSystematicSensitivityAnalysisResult.isSuccess()) {
                throw new SensitivityAnalysisException("Some output data of the sensitivity analysis are missing.");
            }

            checkSensiResults(tempSystematicSensitivityAnalysisResult);
            return tempSystematicSensitivityAnalysisResult;

        } catch (Exception e) {
            throw new SensitivityAnalysisException("Sensitivity analysis fails.", e);
        }
    }

    private void checkSensiResults(SystematicSensitivityResult systematicSensitivityAnalysisResult) {
        if (!systematicSensitivityAnalysisResult.isSuccess()) {
            throw new SensitivityAnalysisException("Status of the sensitivity result indicates a failure.");
        }
    }
}
