/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;

import java.util.Map;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowResultFromMapImpl implements FlowResult {
    protected final SystematicSensitivityResult systematicSensitivityResult;
    private final Map<FlowCnec, Map<TwoSides, Double>> commercialFlows;
    private final Map<FlowCnec, Map<TwoSides, Double>> ptdfZonalSums;

    public FlowResultFromMapImpl(SystematicSensitivityResult systematicSensitivityResult,
                                 Map<FlowCnec, Map<TwoSides, Double>> commercialFlows,
                                 Map<FlowCnec, Map<TwoSides, Double>> ptdfZonalSums) {
        this.systematicSensitivityResult = systematicSensitivityResult;
        this.commercialFlows = commercialFlows;
        this.ptdfZonalSums = ptdfZonalSums;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getReferenceFlow(flowCnec, side);
        } else if (unit == Unit.AMPERE) {
            double intensity = systematicSensitivityResult.getReferenceIntensity(flowCnec, side);
            if (Double.isNaN(intensity) || Math.abs(intensity) <= 1e-6) {
                return systematicSensitivityResult.getReferenceFlow(flowCnec, side) * RaoUtil.getFlowUnitMultiplier(flowCnec, side, Unit.MEGAWATT, Unit.AMPERE);
            } else {
                return intensity;
            }
        } else {
            throw new OpenRaoException("Unknown unit for flow.");
        }
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        if (unit != Unit.MEGAWATT) {
            throw new OpenRaoException("Commercial flows only in MW.");
        }
        if (!commercialFlows.containsKey(flowCnec) || !commercialFlows.get(flowCnec).containsKey(side)) {
            throw new OpenRaoException(format("No commercial flow on the CNEC %s on side %s", flowCnec.getName(), side));
        }
        return commercialFlows.get(flowCnec).get(side);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
        if (!ptdfZonalSums.containsKey(flowCnec) || !ptdfZonalSums.get(flowCnec).containsKey(side)) {
            throw new OpenRaoException(format("No PTDF computed on the CNEC %s on side %s", flowCnec.getName(), side));
        }
        return ptdfZonalSums.get(flowCnec).get(side);
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
        return ptdfZonalSums;
    }
}
