/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.sensitivity_analysis.ra_sensi_handler;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_api.range_action.HvdcRangeAction;
import com.powsybl.open_rao.data.crac_api.range_action.InjectionRangeAction;
import com.powsybl.open_rao.data.crac_api.range_action.PstRangeAction;
import com.powsybl.open_rao.data.crac_api.range_action.RangeAction;
import com.powsybl.open_rao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface RangeActionSensiHandler {

    double getSensitivityOnFlow(FlowCnec cnec, Side side, SystematicSensitivityResult sensitivityResult);

    void checkConsistency(Network network);

    static RangeActionSensiHandler get(RangeAction<?> rangeAction) {
        if (rangeAction instanceof PstRangeAction pstRangeAction) {
            return new PstRangeActionSensiHandler(pstRangeAction);
        } else if (rangeAction instanceof HvdcRangeAction hvdcRangeAction) {
            return new HvdcRangeActionSensiHandler(hvdcRangeAction);
        } else if (rangeAction instanceof InjectionRangeAction injectionRangeAction) {
            return new InjectionRangeActionSensiHandler(injectionRangeAction);
        } else {
            throw new OpenRaoException(String.format("RangeAction implementation %s not handled by sensitivity analysis", rangeAction.getClass()));
        }
    }
}
