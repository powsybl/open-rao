/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.result;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;

import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class BranchResultImpl implements BranchResult {
    protected final SystematicSensitivityResult systematicSensitivityResult;
    private final BranchResult fixedCommercialFlows;
    private final BranchResult fixedPtdfs;

    public BranchResultImpl(SystematicSensitivityResult systematicSensitivityResult,
                               BranchResult fixedCommercialFlows,
                               BranchResult fixedPtdfs) {
        this.systematicSensitivityResult = systematicSensitivityResult;
        this.fixedCommercialFlows = fixedCommercialFlows;
        this.fixedPtdfs = fixedPtdfs;
    }

    @Override
    public double getFlow(BranchCnec branchCnec, Unit unit) {
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getReferenceFlow(branchCnec);
        } else if (unit == Unit.AMPERE) {
            double intensity = systematicSensitivityResult.getReferenceIntensity(branchCnec);
            if (Double.isNaN(intensity)) {
                return systematicSensitivityResult.getReferenceFlow(branchCnec) * RaoUtil.getBranchFlowUnitMultiplier(branchCnec, Side.LEFT, Unit.MEGAWATT, Unit.AMPERE);
            } else {
                return intensity;
            }
        } else {
            throw new FaraoException("Unknown unit for flow.");
        }
    }

    @Override
    public double getCommercialFlow(BranchCnec branchCnec, Unit unit) {
        if (unit == Unit.MEGAWATT) {
            return fixedCommercialFlows.getCommercialFlow(branchCnec, unit);
        } else {
            throw new FaraoException("Commercial flows only in MW.");
        }
    }

    @Override
    public double getPtdfZonalSum(BranchCnec branchCnec) {
        return fixedPtdfs.getPtdfZonalSum(branchCnec);
    }

    @Override
    public Map<BranchCnec, Double> getPtdfZonalSums() {
        return fixedPtdfs.getPtdfZonalSums();
    }
}
