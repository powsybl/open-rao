/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.refprog.reference_program;

import com.farao_community.farao.util.LoadFlowService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class ReferenceProgramBuilder {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ReferenceProgramBuilder.class);

    private ReferenceProgramBuilder() {

    }

    private static void computeRefFlowOnCurrentNetwork(Network network) {
        String errorMsg = "LoadFlow could not be computed. The ReferenceProgram will be built without a prior LoadFlow computation";
        try {
            // we need this separate load flow to get reference flow on cnec.
            // because reference flow from sensi is not yet fully implemented in powsybl
            String initialVariantId = network.getVariantManager().getWorkingVariantId();
            LoadFlowResult loadFlowResult = LoadFlowService.runLoadFlow(network, initialVariantId);
            if (!loadFlowResult.isOk()) {
                LOGGER.warn(errorMsg);
            }
        } catch (PowsyblException e) {
            LOGGER.warn(errorMsg);
        }
    }

    public static ReferenceProgram buildReferenceProgram(Network network) {
        computeRefFlowOnCurrentNetwork(network);
        Map<Country, Double> netPositions = (new CountryNetPositionComputation(network)).getNetPositions();
        List<ReferenceExchangeData> referenceExchangeDataList = new ArrayList<>();
        netPositions.forEach((country, flow) ->
            referenceExchangeDataList.add(new ReferenceExchangeData(country, null, flow)));
        return new ReferenceProgram(referenceExchangeDataList);
    }
}
