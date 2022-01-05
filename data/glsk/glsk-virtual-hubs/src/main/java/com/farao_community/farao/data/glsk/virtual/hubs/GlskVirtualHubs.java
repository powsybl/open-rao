/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.virtual.hubs;

import com.farao_community.farao.commons.*;
import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.virtual_hubs.network_extension.AssignedVirtualHub;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public final class GlskVirtualHubs {
    private GlskVirtualHubs() {
    }

    /**
     * Build GLSKs of virtual hubs
     *
     * @param network : Network object, which contains AssignedVirtualHub extensions on
     *                Loads which are virtual hubs
     * @param referenceProgram : Reference Program object
     *
     * @return one LinearGlsk for each virtual hub given in the referenceProgram and found
     * in the network
     */
    public static ZonalData<LinearGlsk> getVirtualHubGlsks(Network network, ReferenceProgram referenceProgram) {
        List<String> countryCodes = referenceProgram.getListOfAreas().stream()
            .filter(eiCode -> !eiCode.isCountryCode())
            .map(EICode::getAreaCode)
            .collect(Collectors.toList());
        return getVirtualHubGlsks(network, countryCodes);
    }

    /**
     * Build GLSKs of virtual hubs
     *
     * @param network : Network object, which contains AssignedVirtualHub extensions on
     *                Loads which are virtual hubs
     * @param eiCodes : list of EI Codes of virtual hubs
     *
     * @return one LinearGlsk for each virtual hub given in eiCodes and found in the network
     */
    public static ZonalData<LinearGlsk> getVirtualHubGlsks(Network network, List<String> eiCodes) {
        Map<String, LinearGlsk> glsks = new HashMap<>();
        Map<String, Load> virtualLoads = buildVirtualLoadMap(network);

        eiCodes.forEach(eiCode -> {

            if (!virtualLoads.containsKey(eiCode)) {
                FaraoLoggerProvider.BUSINESS_WARNS.warn("No load found for virtual hub {}", eiCode);
            } else {
                FaraoLoggerProvider.TECHNICAL_LOGS.debug("Load {} found for virtual hub {}", virtualLoads.get(eiCode).getId(), eiCode);
                Optional<LinearGlsk> virtualHubGlsk = createGlskFromVirtualHub(virtualLoads.get(eiCode));
                virtualHubGlsk.ifPresent(linearGlsk -> glsks.put(eiCode, linearGlsk));
            }
        });
        return new ZonalDataImpl<>(glsks);
    }

    private static Map<String, Load> buildVirtualLoadMap(Network network) {
        Map<String, Load> virtualLoads = new HashMap<>();

        network.getLoadStream()
            .filter(load -> load.getExtension(AssignedVirtualHub.class) != null)
            .forEach(load -> virtualLoads.put(load.getExtension(AssignedVirtualHub.class).getEic(), load));

        return virtualLoads;
    }

    private static Optional<LinearGlsk> createGlskFromVirtualHub(Load virtualLoad) {
        Map<String, Float> glskMap = new HashMap<>();
        try {
            glskMap.put(virtualLoad.getId(), 1.0F);
            String eiCode = virtualLoad.getExtension(AssignedVirtualHub.class).getEic();
            return Optional.of(new LinearGlsk(eiCode, eiCode, glskMap));
        } catch (FaraoException e) {
            return Optional.empty();
        }
    }
}
