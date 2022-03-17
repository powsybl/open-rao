/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.parameters;

import com.farao_community.farao.rao_api.parameters.RaoParameters;

public class MnecParameters {
    private final double mnecAcceptableMarginDiminution;
    private final double mnecViolationCost;
    private final double mnecConstraintAdjustmentCoefficient;

    public MnecParameters(double mnecAcceptableMarginDiminution, double mnecViolationCost, double mnecConstraintAdjustmentCoefficient) {
        this.mnecAcceptableMarginDiminution = mnecAcceptableMarginDiminution;
        this.mnecViolationCost = mnecViolationCost;
        this.mnecConstraintAdjustmentCoefficient = mnecConstraintAdjustmentCoefficient;
    }

    public double getMnecAcceptableMarginDiminution() {
        return mnecAcceptableMarginDiminution;
    }

    public double getMnecViolationCost() {
        return mnecViolationCost;
    }

    public double getMnecConstraintAdjustmentCoefficient() {
        return mnecConstraintAdjustmentCoefficient;
    }

    public static MnecParameters buildFromRaoParameters(RaoParameters raoParameters) {

        /*
        for now, values of MnecParameters are constant over all the SearchTreeRao
        they can therefore be instantiated directly from a RaoParameters
         */

        return new MnecParameters(raoParameters.getMnecAcceptableMarginDiminution(),
            raoParameters.getMnecViolationCost(),
            raoParameters.getMnecConstraintAdjustmentCoefficient());
    }
}
