/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cim.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.AbstractAlignedRaCracCreationParameters;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CimCracCreationParameters extends AbstractAlignedRaCracCreationParameters {
    private Set<String> timeseriesMrids = new HashSet<>();
    private Set<RangeActionSpeed> speedSet = new HashSet<>();
    private VoltageCnecsCreationParameters voltageCnecsCreationParameters = null;

    @Override
    public String getName() {
        return "CimCracCreatorParameters";
    }

    public Set<String> getTimeseriesMrids() {
        return timeseriesMrids;
    }

    public void setTimeseriesMrids(Set<String> timeseriesMrids) {
        this.timeseriesMrids = new HashSet<>(timeseriesMrids);
    }

    public void setRemedialActionSpeed(Set<RangeActionSpeed> rangeActionSpeedSet) {
        this.speedSet = rangeActionSpeedSet;
        checkRangeActionSpeedSet();
    }

    public Set<RangeActionSpeed> getRangeActionSpeedSet() {
        return new HashSet<>(speedSet);
    }

    public RangeActionSpeed getRangeActionSpeed(String remedialActionId) {
        for (RangeActionSpeed raSpeed : speedSet) {
            if (raSpeed.getRangeActionId().equals(remedialActionId)) {
                return raSpeed;
            }
        }
        return null;
    }

    public void checkRangeActionSpeedSet() {
        speedSet.stream().sorted(Comparator.comparing(RangeActionSpeed::hashCode)).forEach(
            raSpeed1 -> speedSet.stream().sorted(Comparator.comparing(RangeActionSpeed::hashCode)).forEach(
                raSpeed2 -> ckechRangeActionSpeed(raSpeed1, raSpeed2)
            ));
    }

    private void ckechRangeActionSpeed(RangeActionSpeed raSpeed1, RangeActionSpeed raSpeed2) {
        // Aligned RAs :
        if (areRasAligned(raSpeed1.getRangeActionId(), raSpeed2.getRangeActionId())) {
            if (!raSpeed1.getSpeed().equals(raSpeed2.getSpeed())) {
                throw new FaraoException(String.format("Range actions %s and %s are aligned but have different speeds (%s and %s)", raSpeed1.getRangeActionId(), raSpeed2.getRangeActionId(), raSpeed1.getSpeed().toString(), raSpeed2.getSpeed().toString()));
            }
        } else if (raSpeed1.getSpeed().equals(raSpeed2.getSpeed()) && !raSpeed1.getRangeActionId().equals(raSpeed2.getRangeActionId())) {
            throw new FaraoException(String.format("Range action %s has a speed %s already defined", raSpeed1.getRangeActionId(), raSpeed1.getSpeed().toString()));
        }
    }

    public VoltageCnecsCreationParameters getVoltageCnecsCreationParameters() {
        return voltageCnecsCreationParameters;
    }

    public void setVoltageCnecsCreationParameters(VoltageCnecsCreationParameters voltageCnecsCreationParameters) {
        this.voltageCnecsCreationParameters = voltageCnecsCreationParameters;
    }
}
