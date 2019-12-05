/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class SearchTreeRao implements RaoProvider {

    @Override
    public String getName() {
        return "Search Tree Rao";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<RaoComputationResult> run(Network network, Crac crac, String variantId, ComputationManager computationManager, RaoParameters parameters) {
        Leaf optimalLeaf = new Leaf();
        optimalLeaf.evaluate(network, crac, computationManager, parameters);

        while (optimalLeaf.getActionImpact().getCost() < 0) {
            // TODO: create a crac copy
            crac.getNetworkActions().remove(optimalLeaf.getNetworkAction());

            List<NetworkAction> availableNetworkActions = crac.getNetworkActions(network, UsageMethod.AVAILABLE);
            List<Leaf> generatedLeaves = optimalLeaf.bloom(availableNetworkActions);
            generatedLeaves.forEach(leaf -> leaf.evaluate(network, crac, variantId, computationManager, parameters));

            for (Leaf currentLeaf: generatedLeaves) {
                if (currentLeaf.getActionImpact().getCost() < optimalLeaf.getActionImpact().getCost()) {
                    optimalLeaf = currentLeaf;
                }
            }
        }

        return null;
    }
}
