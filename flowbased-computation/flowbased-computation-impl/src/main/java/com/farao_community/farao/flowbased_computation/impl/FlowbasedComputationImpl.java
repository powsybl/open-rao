/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_result_extensions.CracResultUtil;
import com.farao_community.farao.data.flowbased_domain.*;
import com.farao_community.farao.flowbased_computation.*;
import com.farao_community.farao.commons.RandomizedString;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Flowbased computation implementation
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(FlowbasedComputationProvider.class)
public class FlowbasedComputationImpl implements FlowbasedComputationProvider {

    static final String INITIAL_STATE_WITH_PRA = "InitialStateWithPra";
    String onOutageInstantId = "";
    String afterCraInstantId = "";
    final Logger logger = LoggerFactory.getLogger(FlowbasedComputationImpl.class);

    @Override
    public String getName() {
        return "SimpleIterativeFlowBased";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<FlowbasedComputationResult> run(Network network, Crac crac, ZonalData<LinearGlsk> glsk, FlowbasedComputationParameters parameters) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(crac);
        Objects.requireNonNull(glsk);
        Objects.requireNonNull(parameters);

        sortInstants(crac.getInstants());

        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder()
                .withDefaultParameters(parameters.getSensitivityAnalysisParameters())
                .withPtdfSensitivities(glsk, crac.getBranchCnecs(), Collections.singleton(Unit.MEGAWATT))
                .build();

        // Preventive perimeter
        String initialNetworkId = network.getVariantManager().getWorkingVariantId();
        network.getVariantManager().cloneVariant(initialNetworkId, INITIAL_STATE_WITH_PRA);
        network.getVariantManager().setWorkingVariant(INITIAL_STATE_WITH_PRA);
        CracResultUtil.applyRemedialActionsForState(network, crac, crac.getPreventiveState());
        SystematicSensitivityResult result = systematicSensitivityInterface.run(network);
        FlowbasedComputationResult flowBasedComputationResult = new FlowbasedComputationResultImpl(FlowbasedComputationResult.Status.SUCCESS, buildFlowbasedDomain(crac, glsk, result));

        // Curative perimeter
        if (!afterCraInstantId.equals("")) {
            crac.getStatesFromInstant(afterCraInstantId).forEach(state -> {
                String variantName = "State" + state.getId();
                network.getVariantManager().cloneVariant(INITIAL_STATE_WITH_PRA, variantName);
                network.getVariantManager().setWorkingVariant(variantName);
                CracResultUtil.applyRemedialActionsForState(network, crac, state);
                SystematicSensitivityResult sensitivityResult = systematicSensitivityInterface.run(network);
                FlowbasedComputationResult newFbRes = new FlowbasedComputationResultImpl(FlowbasedComputationResult.Status.SUCCESS, buildFlowbasedDomain(crac, glsk, sensitivityResult));
                Optional<Contingency> contingencyOptional = state.getContingency();
                String contingencyId = "";
                if (contingencyOptional.isPresent()) {
                    contingencyId = contingencyOptional.get().getId();
                } else {
                    throw new FaraoException("Contingency shouldn't be empty in curative.");
                }
                List<DataMonitoredBranch> updatedMonitoredBranches = newFbRes.getFlowBasedDomain().findContingencyById(contingencyId).getDataMonitoredBranches();
                flowBasedComputationResult.getFlowBasedDomain().findContingencyById(contingencyId).updateCurativeDataMonitoredBranches(updatedMonitoredBranches, afterCraInstantId);
            });
        } else {
            logger.info("No curative computation in flowbased.");
        }

        // Restore initial variant at the end of the computation
        network.getVariantManager().setWorkingVariant(initialNetworkId);

        return CompletableFuture.completedFuture(flowBasedComputationResult);
    }

    private void sortInstants(Set<Instant> instants) {
        Map<Integer, String> instantMap = new HashMap<>();

        for (Instant instant : instants) {
            instantMap.put(instant.getSeconds(), instant.getId());
        }
        List<Integer> seconds = new ArrayList<>(instantMap.keySet());
        Collections.sort(seconds);

        if (instants.size() == 1) {
            logger.info("Only Preventive instant is present for flowbased computation.");
            return;
        } else if (instants.size() == 2) {
            logger.info("Only Preventive and On outage instants are present for flowbased computation.");
        } else if (instants.size() == 3) {
            logger.debug("All instants are defined for flowbased computation.");
            // last instant
            afterCraInstantId = instantMap.get(seconds.get(seconds.size() - 1));
        } else {
            throw new FaraoException(String.format("Incorrect number of instants for flowbased computation. It is %s, but should be 1, 2 or 3.", instants.size()));
        }
        // 2nd instant
        onOutageInstantId = instantMap.get(seconds.get(1));

    }

    private DataDomain buildFlowbasedDomain(Crac crac, ZonalData<LinearGlsk> glsk, SystematicSensitivityResult result) {
        return DataDomain.builder()
                .id(RandomizedString.getRandomizedString())
                .name("FlowBased results")
                .description("")
                .sourceFormat("code")
                .dataPreContingency(buildDataPreContingency(crac, glsk, result))
                .dataPostContingency(buildDataPostContingencies(crac, glsk, result))
                .glskData(buildDataGlskFactors(glsk))
                .build();
    }

    private List<DataGlskFactors> buildDataGlskFactors(ZonalData<LinearGlsk> glsk) {
        List<DataGlskFactors> glskFactors = new ArrayList<>();
        glsk.getDataPerZone().forEach((s, linearGlsk) -> glskFactors.add(new DataGlskFactors(s, linearGlsk.getGLSKs())));
        return glskFactors;
    }

    private List<DataPostContingency> buildDataPostContingencies(Crac crac, ZonalData<LinearGlsk> glsk, SystematicSensitivityResult result) {
        List<DataPostContingency> postContingencyList = new ArrayList<>();
        crac.getContingencies().forEach(contingency -> postContingencyList.add(buildDataPostContingency(crac, contingency, glsk, result)));
        return postContingencyList;
    }

    private DataPostContingency buildDataPostContingency(Crac crac, Contingency contingency, ZonalData<LinearGlsk> glsk, SystematicSensitivityResult result) {
        return DataPostContingency.builder()
                .contingencyId(contingency.getId())
                .dataMonitoredBranches(buildDataMonitoredBranches(crac, crac.getStates(contingency), glsk, result))
                .build();
    }

    private DataPreContingency buildDataPreContingency(Crac crac, ZonalData<LinearGlsk> glsk, SystematicSensitivityResult result) {
        return DataPreContingency.builder()
                .dataMonitoredBranches(buildDataMonitoredBranches(crac, Set.of(crac.getPreventiveState()), glsk, result))
                .build();
    }

    private List<DataMonitoredBranch> buildDataMonitoredBranches(Crac crac, Set<State> states, ZonalData<LinearGlsk> glsk, SystematicSensitivityResult result) {
        List<DataMonitoredBranch> branchResultList = new ArrayList<>();
        states.forEach(state -> crac.getBranchCnecs(state).forEach(cnec -> branchResultList.add(buildDataMonitoredBranch(cnec, glsk, result))));
        return branchResultList;
    }

    private DataMonitoredBranch buildDataMonitoredBranch(BranchCnec cnec, ZonalData<LinearGlsk> glsk, SystematicSensitivityResult result) {
        double maxThreshold = cnec.getUpperBound(Side.LEFT, Unit.MEGAWATT).orElse(Double.POSITIVE_INFINITY);
        double minThreshold = cnec.getLowerBound(Side.LEFT, Unit.MEGAWATT).orElse(Double.NEGATIVE_INFINITY);
        return new DataMonitoredBranch(
                cnec.getId(),
                cnec.getName(),
                cnec.getState().getInstant().getId(),
                cnec.getNetworkElement().getId(),
                Math.min(maxThreshold, -minThreshold),
                zeroIfNaN(result.getReferenceFlow(cnec)),
                buildDataPtdfPerCountry(cnec, glsk, result)
        );
    }

    private List<DataPtdfPerCountry> buildDataPtdfPerCountry(BranchCnec cnec, ZonalData<LinearGlsk> glskProvider, SystematicSensitivityResult result) {
        Map<String, LinearGlsk> glsks = glskProvider.getDataPerZone();
        return glsks.values().stream()
                .map(glsk ->
                        new DataPtdfPerCountry(
                                glsk.getId(),
                                zeroIfNaN(result.getSensitivityOnFlow(glsk.getId(), cnec))
                        )
                ).collect(Collectors.toList());
    }

    private double zeroIfNaN(double value) {
        return Double.isNaN(value) ? 0. : value;
    }
}
