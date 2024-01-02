/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.cnec.Cnec;

import java.util.Objects;

/**
 * Class representing the instants at which a {@link Cnec} can be monitored and
 * a {@link RemedialAction} applied.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class InstantImpl extends AbstractIdentifiable<Instant> implements Instant {

    private final InstantKind instantKind;
    private final Instant previous;
    private final int order;

    InstantImpl(String id, InstantKind instantKind, Instant previous) {
        super(id);
        if (Objects.equals(id, "initial")) {
            throw new FaraoException("Instant with id 'initial' can't be defined");
        }
        this.previous = previous;
        this.instantKind = instantKind;
        if (previous == null) {
            this.order = 0;
        } else {
            this.order = previous.getOrder() + 1;
        }
    }

    public int getOrder() {
        return order;
    }

    public InstantKind getKind() {
        return instantKind;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean comesBefore(Instant otherInstant) {
        return this.order < otherInstant.getOrder();
    }

    @Override
    public boolean isPreventive() {
        return instantKind == InstantKind.PREVENTIVE;
    }

    @Override
    public boolean isOutage() {
        return instantKind == InstantKind.OUTAGE;
    }

    @Override
    public boolean isAuto() {
        return instantKind == InstantKind.AUTO;
    }

    @Override
    public boolean isCurative() {
        return instantKind == InstantKind.CURATIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        InstantImpl instant = (InstantImpl) o;
        return order == instant.order && instantKind == instant.instantKind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), instantKind, order);
    }

    Instant getInstantBefore() {
        return previous;
    }
}
