/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Crac;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class CracResultUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(CracResultUtil.class);

    private CracResultUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Apply preventive remedial actions saved in CRAC result extension on current working variant of given network.
     *
     * @param network Network on which remedial actions should be applied
     * @param crac CRAC that should contain result extension
     * @param cracVariantId CRAC variant to get active remedial actions from
     */
    public static void applyPreventiveRemedialActions(Network network, Crac crac, String cracVariantId) {
        String preventiveStateId = crac.getPreventiveState().getId();
        crac.getNetworkActions().forEach(na -> {
            NetworkActionResult networkActionResult = na.getExtension(NetworkActionResultExtension.class).getVariant(cracVariantId);
            if (networkActionResult != null) {
                if (networkActionResult.isActivated(preventiveStateId)) {
                    na.apply(network);
                }
            } else {
                LOGGER.error(String.format("Could not find results for variant %s on network action %s", cracVariantId, na.getId()));
            }
        });
        crac.getRangeActions().forEach(ra -> {
            RangeActionResult rangeActionResult = ra.getExtension(RangeActionResultExtension.class).getVariant(cracVariantId);
            if (rangeActionResult != null) {
                ra.apply(network, rangeActionResult.getSetPoint(preventiveStateId));
            } else {
                LOGGER.error(String.format("Could not find results for variant %s on range action %s", cracVariantId, ra.getId()));
            }
        });
    }
}
