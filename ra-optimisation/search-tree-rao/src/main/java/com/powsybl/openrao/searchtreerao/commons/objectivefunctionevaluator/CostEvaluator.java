/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface CostEvaluator {

    String getName();

    /**
     * It evaluates the cost of RaoData containing a Network, a Crac and a SystematicSensitivityResult on
     * the current RaoData variant.
     *
     * @return Double value of the RaoData cost.
     * @param flowResult : the flow computation result
     * @param rangeActionActivationResult
     * @param sensitivityStatus : the sensitivity computation status
     */
    default Pair<Double, List<FlowCnec>> computeCostAndLimitingElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus) {
        return computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, new HashSet<>());
    }

    Pair<Double, List<FlowCnec>> computeCostAndLimitingElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus, Set<String> contingenciesToExclude);

    Unit getUnit();

    Set<FlowCnec> getFlowCnecs();
}
