/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.*;

import java.util.*;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
public class RaUsageLimitsAdderImpl implements RaUsageLimitsAdder {
    CracImpl owner;
    private final Instant instant;
    private final RaUsageLimits raUsageLimits = new RaUsageLimits();
    private final ReportNode reportNode;

    RaUsageLimitsAdderImpl(CracImpl owner, String instantName, ReportNode reportNode) {
        Objects.requireNonNull(owner);
        this.owner = owner;
        List<Instant> instants = this.owner.getSortedInstants().stream().filter(cracInstant -> cracInstant.getId().equals(instantName)).toList();
        if (instants.isEmpty()) {
            throw new OpenRaoException(String.format("The instant %s does not exist in the crac.", instantName));
        }
        this.instant = instants.get(0);
        this.reportNode = CracImplReports.reportNewRaUsageLimitsAtInstant(reportNode, instantName);
    }

    @Override
    public RaUsageLimitsAdder withMaxRa(int maxRa) {
        raUsageLimits.setMaxRa(maxRa, reportNode);
        return this;
    }

    @Override
    public RaUsageLimitsAdder withMaxTso(int maxTso) {
        raUsageLimits.setMaxTso(maxTso, reportNode);
        return this;
    }

    @Override
    public RaUsageLimitsAdder withMaxTopoPerTso(Map<String, Integer> maxTopoPerTso) {
        raUsageLimits.setMaxTopoPerTso(maxTopoPerTso, reportNode);
        return this;
    }

    @Override
    public RaUsageLimitsAdder withMaxPstPerTso(Map<String, Integer> maxPstPerTso) {
        raUsageLimits.setMaxPstPerTso(maxPstPerTso, reportNode);
        return this;
    }

    @Override
    public RaUsageLimitsAdder withMaxRaPerTso(Map<String, Integer> maxRaPerTso) {
        raUsageLimits.setMaxRaPerTso(maxRaPerTso, reportNode);
        return this;
    }

    @Override
    public RaUsageLimits add() {
        owner.addRaUsageLimits(instant, raUsageLimits);
        return raUsageLimits;
    }
}
