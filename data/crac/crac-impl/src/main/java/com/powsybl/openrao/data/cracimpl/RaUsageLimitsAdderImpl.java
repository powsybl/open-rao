/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.*;

import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.data.cracimpl.AdderUtils.assertAttributeNotNull;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
public class RaUsageLimitsAdderImpl implements RaUsageLimitsAdder {
    public static final String RA_USAGE_LIMITS = "RaUsageLimits";
    CracImpl owner;
    private Instant instant = null;
    private final RaUsageLimits raUsageLimits = new RaUsageLimits();

    RaUsageLimitsAdderImpl(CracImpl owner, String instantName) {
        Objects.requireNonNull(owner);
        this.owner = owner;
        List<Instant> instants = this.owner.getSortedInstants().stream().filter(cracInstant -> cracInstant.getId().equals(instantName)).toList();
        if (instants.isEmpty()) {
            BUSINESS_WARNS.warn("The instant {} registered in the crac creation parameters does not exist in the crac. Its remedial action limitations will be ignored.", instantName);
        } else {
            this.instant = instants.get(0);
        }
    }

    @Override
    public RaUsageLimitsAdder withMaxRa(int maxRa) {
        raUsageLimits.setMaxRa(maxRa);
        return this;
    }

    @Override
    public RaUsageLimitsAdder withMaxTso(int maxTso) {
        raUsageLimits.setMaxTso(maxTso);
        return this;
    }

    @Override
    public RaUsageLimitsAdder withMaxTopoPerTso(Map<String, Integer> maxTopoPerTso) {
        raUsageLimits.setMaxTopoPerTso(maxTopoPerTso);
        return this;
    }

    @Override
    public RaUsageLimitsAdder withMaxPstPerTso(Map<String, Integer> maxPstPerTso) {
        raUsageLimits.setMaxPstPerTso(maxPstPerTso);
        return this;
    }

    @Override
    public RaUsageLimitsAdder withMaxRaPerTso(Map<String, Integer> maxRaPerTso) {
        raUsageLimits.setMaxRaPerTso(maxRaPerTso);
        return this;
    }

    @Override
    public RaUsageLimits add() {
        assertAttributeNotNull(instant, RA_USAGE_LIMITS, "instant", "newRaUsageLimits(String instant)");
        owner.addRaUsageLimits(instant, raUsageLimits);
        return raUsageLimits;
    }
}
