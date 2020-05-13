/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class CracContingenciesProvider implements ContingenciesProvider {
    private final Crac crac;

    CracContingenciesProvider(Crac crac) {
        this.crac = Objects.requireNonNull(crac);
    }

    private Contingency convertCracContingencyToPowsybl(com.farao_community.farao.data.crac_api.Contingency cracContingency, Network network) {
        String id = cracContingency.getId();
        List<ContingencyElement> contingencyElements = cracContingency.getNetworkElements().stream()
                .map(element -> convertCracContingencyElementToPowsybl(element, network))
                .collect(Collectors.toList());
        return new Contingency(id, contingencyElements);
    }

    private ContingencyElement convertCracContingencyElementToPowsybl(NetworkElement cracContingencyElement, Network network) {
        String elementId = cracContingencyElement.getId();
        Identifiable networkIdentifiable = network.getIdentifiable(elementId);
        if (networkIdentifiable instanceof Branch<?>) {
            return new BranchContingency(elementId);
        } else {
            throw new FaraoException("Unable to apply contingency element " + elementId);
        }
    }

    @Override
    public List<Contingency> getContingencies(Network network) {
        return crac.getContingencies().stream()
                .map(contingency -> convertCracContingencyToPowsybl(contingency, network))
                .collect(Collectors.toList());
    }
}
