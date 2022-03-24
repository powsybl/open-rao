/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.powsybl.glsk.commons.ZonalData;
import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.virtual_hubs.network_extension.AssignedVirtualHub;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class XnodeGlskHandler {

    /*
    This class enables to filter LinearGlsk who acts on a virtual hub which has already been disconnected
    by a Contingency.

    It has initially been developed to fix an issue related to CORE Alegro hubs
    - Alegro Hubs are in the RefProg file (as virtual hubs), with a GLSK in one Xnode
    - Alegro Hubs are disconnected in one contingency, the N-1 on the HVDC Alegro

    For the Cnec monitored in states after this contingency, the net position of the Alegro virtual hubs should
    not be shifted to the amount defined in the refProg, as the N-1 on the HVDC already implicitly shifts to zero
    the net positions on these hubs.

    Warn:
    - this class only works for UCTE data !

    todo: fix this issue at its root: in the PTDF computation

    In the example above, the PTDF of the GLSK of Alegro should be nul after the contingency on Alegro
    This is actually currently not the case as the generator of the GLSK is located in the "real" node
    connected to Alegro.
    */

    private static final int N_CHARACTERS_IN_UCTE_NODE = 8;

    private Map<Contingency, List<String>> invalidGlskPerContingency;
    private ZonalData<SensitivityVariableSet> glskZonalData;
    private Set<Contingency> contingencies;
    private Network network;

    public Network getNetwork() {
        return network;
    }

    public XnodeGlskHandler(ZonalData<SensitivityVariableSet> glskZonalData, Set<Contingency> contingencies, Network network) {
        this.glskZonalData = glskZonalData;
        this.contingencies = contingencies;
        this.network = network;
        this.invalidGlskPerContingency = buildInvalidGlskPerContingency();
    }

    public boolean isLinearGlskValidForCnec(FlowCnec cnec, SensitivityVariableSet linearGlsk) {

        Optional<Contingency> optContingency = cnec.getState().getContingency();
        if (optContingency.isEmpty()) {
            return true;
        }
        return !invalidGlskPerContingency.get(optContingency.get()).contains(linearGlsk.getId());
    }

    private Map<Contingency, List<String>> buildInvalidGlskPerContingency() {

        Map<Contingency, List<String>> outputMap = new HashMap<>();

        contingencies.forEach(contingency -> outputMap.put(contingency, getInvalidGlsksForContingency(contingency)));

        return outputMap;
    }

    private List<String> getInvalidGlsksForContingency(Contingency contingency) {
        List<String> xNodesInContingency = getXNodeInContingency(contingency);
        List<String> invalidGlsk = new ArrayList<>();

        glskZonalData.getDataPerZone().forEach((k, linearGlsk) -> {
            if (!isGlskValid(linearGlsk, xNodesInContingency)) {
                FaraoLoggerProvider.TECHNICAL_LOGS.info("PTDF of zone {} will be replaced by 0 after contingency {}, as it acts on a Xnode which has been disconnected by the contingency", linearGlsk.getId(), contingency.getId());
                invalidGlsk.add(linearGlsk.getId());
            }
        });

        return invalidGlsk;
    }

    private boolean isGlskValid(SensitivityVariableSet linearGlsk, List<String> xNodesInContingency) {

        // if the linearGlsk is not related to only one, the linearGlsk is considered valid
        if (linearGlsk.getVariables().size() > 1) {
            return true;
        }

        // if the linearGlsk is on a Xnode present in the contingency, the linearGlsk is invalid
        String glskInjectionId = linearGlsk.getVariablesById().keySet().iterator().next();

        if (network.getIdentifiable(glskInjectionId) instanceof Injection<?>) {
            Injection<?> injection = (Injection) network.getIdentifiable(glskInjectionId);
            AssignedVirtualHub virtualHub = injection.getExtension(AssignedVirtualHub.class);

            // if the injection contains a virtual hub extension which is tagging a xnode disconnected by the
            // contingency, it is invalid
            if (virtualHub != null && xNodesInContingency.contains(virtualHub.getNodeName())) {
                return false;
            }

            // if the injection's id starts with a xnode disconnected by the contingency, it is invalid
            String ucteNode = injection.getId().substring(0, N_CHARACTERS_IN_UCTE_NODE);
            if (ucteNode.startsWith("X") && xNodesInContingency.contains(ucteNode)) {
                return false;
            }
        }
        return true;
    }

    private List<String> getXNodeInContingency(Contingency contingency) {
        // Warn: only works with UCTE data
        List<String> xNodeInContingency = new ArrayList<>();
        contingency.getNetworkElements().forEach(ne -> xNodeInContingency.addAll(getXnodeInId(ne.getId())));
        return xNodeInContingency;
    }

    private List<String> getXnodeInId(String id) {
        List<String> output = new ArrayList<>();
        Pattern xNodeBeginningOfString = Pattern.compile("^X.......");
        Pattern xNodeInString = Pattern.compile(" X.......");
        xNodeBeginningOfString.matcher(id).results().forEach(re -> output.add(re.group()));
        xNodeInString.matcher(id).results().forEach(re -> output.add(re.group().substring(1)));
        return output;
    }
}
