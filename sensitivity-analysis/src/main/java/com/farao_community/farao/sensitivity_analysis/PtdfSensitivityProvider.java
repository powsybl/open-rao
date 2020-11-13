/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.glsk.import_.glsk_provider.GlskProvider;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.factors.BranchFlowPerLinearGlsk;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class PtdfSensitivityProvider extends AbstractSimpleSensitivityProvider {
    private final GlskProvider glskProvider;

    PtdfSensitivityProvider(GlskProvider glskProvider) {
        super();
        this.glskProvider = Objects.requireNonNull(glskProvider);
    }

    @Override
    public List<SensitivityFactor> getFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();
        Map<String, LinearGlsk> mapCountryLinearGlsk = glskProvider.getAllGlsk(network);

        cnecs.stream().map(Cnec::getNetworkElement)
            .distinct()
            .forEach(ne -> mapCountryLinearGlsk.values().stream()
                .map(linearGlsk -> new BranchFlowPerLinearGlsk(new BranchFlow(ne.getId(), ne.getName(), ne.getId()), linearGlsk))
                .forEach(factors::add));

        return factors;
    }

    @Override
    public List<SensitivityFactor> getFactors(Network network, String contingencyId) {
        List<SensitivityFactor> factors = new ArrayList<>();
        Map<String, LinearGlsk> mapCountryLinearGlsk = glskProvider.getAllGlsk(network);

        if (Objects.isNull(contingencyId)) {
            cnecs.stream()
                .filter(cnec -> cnec.getState().getContingency().isEmpty())
                .map(Cnec::getNetworkElement)
                .distinct()
                .forEach(ne -> mapCountryLinearGlsk.values().stream()
                    .map(linearGlsk -> new BranchFlowPerLinearGlsk(new BranchFlow(ne.getId(), ne.getName(), ne.getId()), linearGlsk))
                    .forEach(factors::add));
        } else {
            cnecs.stream()
                .filter(cnec -> !cnec.getState().getContingency().isEmpty() && cnec.getState().getContingency().get().getId().equals(contingencyId))
                .map(Cnec::getNetworkElement)
                .distinct()
                .forEach(ne -> mapCountryLinearGlsk.values().stream()
                    .map(linearGlsk -> new BranchFlowPerLinearGlsk(new BranchFlow(ne.getId(), ne.getName(), ne.getId()), linearGlsk))
                    .forEach(factors::add));
        }
        return factors;
    }

}
