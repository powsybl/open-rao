/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.range.TapRange;
import com.farao_community.farao.data.crac_api.range.RangeType;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class TapRangeImpl extends AbstractRange implements TapRange {

    private final int minTap;
    private final int maxTap;

    TapRangeImpl(int minTap, int maxTap, RangeType rangeType) {
        super(rangeType, Unit.TAP);
        this.minTap = minTap;
        this.maxTap = maxTap;
    }

    @Override
    public int getMinTap() {
        return minTap;
    }

    @Override
    public int getMaxTap() {
        return maxTap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TapRangeImpl otherRange = (TapRangeImpl) o;
        return super.equals(otherRange)
            && maxTap == otherRange.getMaxTap()
            && minTap == otherRange.getMinTap();
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + super.hashCode();
        result = 31 * result + minTap;
        result = 31 * result + maxTap;
        return result;
    }

}
