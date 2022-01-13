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
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.factors.BranchFlowPerLinearGlsk;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class PtdfSensitivityProvider extends AbstractSimpleSensitivityProvider {
    private final ZonalData<LinearGlsk> glsk;

    PtdfSensitivityProvider(ZonalData<LinearGlsk> glsk, Set<FlowCnec> cnecs, Set<Unit> units) {
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
    public List<SensitivityFactor> getCommonFactors(Network network) {
        return new ArrayList<>();
    }

    @Override
    public List<SensitivityFactor> getAdditionalFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();

        if (afterContingencyOnly) {
            return factors;
        }

        Map<String, LinearGlsk> mapCountryLinearGlsk = glsk.getDataPerZone();
        cnecs.stream()
            .filter(cnec -> cnec.getState().getContingency().isEmpty())
            .map(Cnec::getNetworkElement)
            .distinct()
            .forEach(ne -> mapCountryLinearGlsk.values().stream()
                .map(linearGlsk -> new BranchFlowPerLinearGlsk(new BranchFlow(ne.getId(), ne.getName(), ne.getId()), linearGlsk))
                .forEach(factors::add));
        return factors;
    }

    @Override
    public List<SensitivityFactor> getAdditionalFactors(Network network, String contingencyId) {
        List<SensitivityFactor> factors = new ArrayList<>();
        Map<String, LinearGlsk> mapCountryLinearGlsk = glsk.getDataPerZone();

        cnecs.stream()
            .filter(cnec -> cnec.getState().getContingency().isPresent() && cnec.getState().getContingency().get().getId().equals(contingencyId))
            .map(Cnec::getNetworkElement)
            .distinct()
            .forEach(ne -> mapCountryLinearGlsk.values().stream()
                .map(linearGlsk -> new BranchFlowPerLinearGlsk(new BranchFlow(ne.getId(), ne.getName(), ne.getId()), linearGlsk))
                .forEach(factors::add));
        return factors;
    }
}
