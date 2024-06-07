/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.RandomizedString;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityAnalysisResult;
import com.powsybl.sensitivity.SensitivityFactor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
final class SystematicSensitivityAdapter {
    private SystematicSensitivityAdapter() {
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters,
                                                      String sensitivityProvider,
                                                      Instant outageInstant,
                                                      ReportNode reportNode) {
        ReportNode sensitivityAnalysisReportNode = SensitivityAnalysisReports.reportSystematicSensitivityAnalysisStart(reportNode);
        SensitivityAnalysisResult result;
        try {
            result = SensitivityAnalysis.find(sensitivityProvider).run(network,
                    network.getVariantManager().getWorkingVariantId(),
                    cnecSensitivityProvider.getAllFactors(network),
                    cnecSensitivityProvider.getContingencies(network),
                    cnecSensitivityProvider.getVariableSets(),
                    sensitivityComputationParameters,
                    LocalComputationManager.getDefault(), // TODO check if this is ok ?
                    sensitivityAnalysisReportNode);
        } catch (Exception e) {
            SensitivityAnalysisReports.reportSystematicSensitivityAnalysisFailed(sensitivityAnalysisReportNode, e.getMessage());
            return new SystematicSensitivityResult(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE);
        }
        SensitivityAnalysisReports.reportSystematicSensitivityAnalysisEnd(sensitivityAnalysisReportNode);
        return new SystematicSensitivityResult().completeData(result, outageInstant.getOrder()).postTreatIntensities().postTreatHvdcs(network, cnecSensitivityProvider.getHvdcs());
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      AppliedRemedialActions appliedRemedialActions,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters,
                                                      String sensitivityProvider,
                                                      Instant outageInstant,
                                                      ReportNode reportNode) {
        if (appliedRemedialActions == null || appliedRemedialActions.isEmpty(network)) {
            return runSensitivity(network, cnecSensitivityProvider, sensitivityComputationParameters, sensitivityProvider, outageInstant, reportNode);
        }

        ReportNode reportNodeStart = SensitivityAnalysisReports.reportSystematicSensitivityAnalysisWithAppliedRAStart(reportNode);
        // Information : for contingencies with auto RA but no curative RA, SystematicSensitivityResult::getCnecStateResult will
        // retrieve sensi information for curative state from auto state to take into account auto RAs.
        // (When auto AND curative RAs are applied, they will both be included in statesWithRa and both sensis
        // are computed.)
        Set<State> statesWithRa = appliedRemedialActions.getStatesWithRa(network);
        Set<State> statesWithoutRa = cnecSensitivityProvider.getFlowCnecs().stream().map(Cnec::getState).collect(Collectors.toSet());
        statesWithoutRa.removeAll(statesWithRa);

        // systematic analysis for states without RA
        ReportNode reportNodeWithoutRa = SensitivityAnalysisReports.reportSystematicSensitivityAnalysisForStatesWithoutRA(reportNodeStart, statesWithRa.size() + 1, statesWithoutRa.size());

        List<Contingency> contingenciesWithoutRa = statesWithoutRa.stream()
            .filter(state -> state.getContingency().isPresent())
            .map(state -> state.getContingency().get())
            .distinct()
            .toList();

        SystematicSensitivityResult result = new SystematicSensitivityResult();
        List<SensitivityFactor> allFactorsWithoutRa = cnecSensitivityProvider.getBasecaseFactors(network);
        allFactorsWithoutRa.addAll(cnecSensitivityProvider.getContingencyFactors(network, contingenciesWithoutRa));
        result.completeData(SensitivityAnalysis.find(sensitivityProvider).run(network,
            network.getVariantManager().getWorkingVariantId(),
            allFactorsWithoutRa,
            contingenciesWithoutRa,
            cnecSensitivityProvider.getVariableSets(),
            sensitivityComputationParameters,
            LocalComputationManager.getDefault(),
            reportNodeWithoutRa), outageInstant.getOrder());

        // systematic analyses for states with RA
        cnecSensitivityProvider.disableFactorsForBaseCaseSituation();
        String workingVariantId = network.getVariantManager().getWorkingVariantId();
        int counterForLogs = 2;

        String variantForState = RandomizedString.getRandomizedString();
        boolean shouldRemoveVariant = false;
        for (State state : statesWithRa) {

            Optional<Contingency> optContingency = state.getContingency();

            if (optContingency.isEmpty()) {
                throw new OpenRaoException("Sensitivity analysis with applied RA does not handle preventive RA.");
            }

            ReportNode reportNodeWithRa = SensitivityAnalysisReports.reportSystematicSensitivityAnalysisForStatesWithRA(reportNodeStart, counterForLogs, statesWithRa.size() + 1, state.getId());

            //TODO: We can save a bit of time by unapplying previous remedial actions here if we find a clean way to do it
            network.getVariantManager().cloneVariant(workingVariantId, variantForState, true);
            shouldRemoveVariant = true;
            network.getVariantManager().setWorkingVariant(variantForState);

            appliedRemedialActions.applyOnNetwork(state, network);

            List<Contingency> contingencyList = Collections.singletonList(optContingency.get());

            result.completeData(SensitivityAnalysis.find(sensitivityProvider).run(network,
                network.getVariantManager().getWorkingVariantId(),
                cnecSensitivityProvider.getContingencyFactors(network, contingencyList),
                contingencyList,
                cnecSensitivityProvider.getVariableSets(),
                sensitivityComputationParameters,
                LocalComputationManager.getDefault(),
                reportNodeWithRa), state.getInstant().getOrder());
            counterForLogs++;
        }

        if (shouldRemoveVariant) {
            network.getVariantManager().removeVariant(variantForState);
        }

        SensitivityAnalysisReports.reportSystematicSensitivityAnalysisWithAppliedRAEnd(reportNodeStart);

        network.getVariantManager().setWorkingVariant(workingVariantId);
        return result.postTreatIntensities().postTreatHvdcs(network, cnecSensitivityProvider.getHvdcs());
    }
}
