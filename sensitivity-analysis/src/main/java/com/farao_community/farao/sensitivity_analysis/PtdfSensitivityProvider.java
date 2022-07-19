/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.commons.Unit;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.glsk.commons.ZonalData;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFunctionType;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.SensitivityVariableType;

import java.util.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class PtdfSensitivityProvider extends AbstractSimpleSensitivityProvider {
    private final ZonalData<SensitivityVariableSet> glsk;

    PtdfSensitivityProvider(ZonalData<SensitivityVariableSet> glsk, Set<FlowCnec> cnecs, Set<Unit> units) {
        super(cnecs, units);

        // todo : handle PTDFs in AMPERE
        if (factorsInAmpere || !factorsInMegawatt) {
            FaraoLoggerProvider.TECHNICAL_LOGS.warn("PtdfSensitivity provider currently only handle Megawatt unit");
            factorsInMegawatt = true;
            factorsInAmpere = false;
        }
        this.glsk = Objects.requireNonNull(glsk);
    }

    @Override
    public List<SensitivityFactor> getBasecaseFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();

        if (afterContingencyOnly) {
            return factors;
        }

        Map<String, SensitivityVariableSet> mapCountryLinearGlsk = glsk.getDataPerZone();

        //According to ContingencyContext doc, contingencyId should be null for preContingency context
        ContingencyContext preContingencyContext = new ContingencyContext(null, ContingencyContextType.NONE);

        cnecs.stream()
            .filter(cnec -> cnec.getState().getContingency().isEmpty())
            .map(FlowCnec::getNetworkElement)
            .distinct()
            .forEach(ne -> mapCountryLinearGlsk.values().stream()
                .map(linearGlsk -> new SensitivityFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER, ne.getId(),
                    SensitivityVariableType.INJECTION_ACTIVE_POWER, linearGlsk.getId(),
                    true, preContingencyContext))
                .forEach(factors::add));
        return factors;
    }

    @Override
    public List<SensitivityFactor> getContingencyFactors(Network network, List<Contingency> contingencies) {
        List<SensitivityFactor> factors = new ArrayList<>();
        for (Contingency contingency : contingencies) {
            String contingencyId = contingency.getId();
            Map<String, SensitivityVariableSet> mapCountryLinearGlsk = glsk.getDataPerZone();

            ContingencyContext preContingencyContext = new ContingencyContext(contingencyId, ContingencyContextType.SPECIFIC);

            cnecs.stream()
                .filter(cnec -> cnec.getState().getContingency().isPresent() && cnec.getState().getContingency().get().getId().equals(contingency.getId()))
                .map(FlowCnec::getNetworkElement)
                .distinct()
                .forEach(ne -> mapCountryLinearGlsk.values().stream()
                    .map(linearGlsk -> new SensitivityFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER, ne.getId(),
                        SensitivityVariableType.INJECTION_ACTIVE_POWER, linearGlsk.getId(),
                        true, preContingencyContext))
                    .forEach(factors::add));
        }
        return factors;
    }

    @Override
    public List<SensitivityVariableSet> getVariableSets() {
        return new ArrayList<>(glsk.getDataPerZone().values());
    }
}
