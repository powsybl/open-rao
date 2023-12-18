/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.castor.algorithm;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.data.crac_api.State;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This class represents the functional preventive scenario
 * It contains the basecase state, as well as post-contingency states that should be optimized
 * during preventive RAO (ie outage states and curative states that have no RAs)
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class BasecaseScenario {
    private final State basecaseState;
    private final Set<State> otherStates;

    /**
     * Construct a basecase scenario
     * @param basecaseState the basecase state (required)
     * @param otherStates the other states to optimize in preventive (can be empty or null)
     */
    public BasecaseScenario(State basecaseState, Set<State> otherStates) {
        Objects.requireNonNull(basecaseState);
        if (!basecaseState.getInstant().isPreventive()) {
            throw new OpenRaoException(String.format("Basecase state `%s` is not preventive", basecaseState));
        }
        if (otherStates != null && otherStates.stream().anyMatch(state -> state.getInstant().isPreventive())) {
            throw new OpenRaoException("OtherStates should not be preventive");
        }
        this.basecaseState = basecaseState;
        this.otherStates = otherStates == null ? new HashSet<>() : otherStates;
    }

    public State getBasecaseState() {
        return basecaseState;
    }

    public Set<State> getOtherStates() {
        return otherStates;
    }

    public Set<State> getAllStates() {
        Set<State> states = new HashSet<>(otherStates);
        states.add(basecaseState);
        return states;
    }

    void addOtherState(State state) {
        if (state.getInstant().isPreventive()) {
            throw new OpenRaoException("OtherStates should not be preventive");
        }
        otherStates.add(state);
    }
}
