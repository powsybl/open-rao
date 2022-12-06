/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;

import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowResultImpl implements FlowResult {
    protected final SystematicSensitivityResult systematicSensitivityResult;
    private final FlowResult fixedCommercialFlows;
    private final FlowResult fixedPtdfs;

    public FlowResultImpl(SystematicSensitivityResult systematicSensitivityResult,
                          FlowResult fixedCommercialFlows,
                          FlowResult fixedPtdfs) {
        this.systematicSensitivityResult = systematicSensitivityResult;
        this.fixedCommercialFlows = fixedCommercialFlows;
        this.fixedPtdfs = fixedPtdfs;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, Side side, Unit unit) {
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
            throw new FaraoException("Unknown unit for flow.");
        }
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Side side, Unit unit) {
        if (unit == Unit.MEGAWATT) {
            return fixedCommercialFlows.getCommercialFlow(flowCnec, side, unit);
        } else {
            throw new FaraoException("Commercial flows only in MW.");
        }
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, Side side) {
        return fixedPtdfs.getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public Map<FlowCnec, Map<Side, Double>> getPtdfZonalSums() {
        return fixedPtdfs.getPtdfZonalSums();
    }

}
