/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.ActionType;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

/**
 * Topological remedial action: open or close a network element.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("topology")
public final class Topology extends AbstractElementaryNetworkAction<Topology> {

    private ActionType actionType;

    @JsonCreator
    public Topology(@JsonProperty("id") String id,
                    @JsonProperty("name") String name,
                    @JsonProperty("operator") String operator,
                    @JsonProperty("usageRules") List<UsageRule> usageRules,
                    @JsonProperty("networkElement") NetworkElement networkElement,
                    @JsonProperty("actionType") ActionType actionType) {
        super(id, name, operator, usageRules, networkElement);
        this.actionType = actionType;
    }

    public Topology(String id, String name, String operator, NetworkElement networkElement, ActionType actionType) {
        super(id, name, operator, networkElement);
        this.actionType = actionType;
    }

    public Topology(String id, NetworkElement networkElement, ActionType actionType) {
        super(id, networkElement);
        this.actionType = actionType;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    @Override
    public void apply(Network network) {
        Identifiable element = network.getIdentifiable(getNetworkElement().getId());
        if (element instanceof Branch) {
            Branch branch = (Branch) element;
            if (actionType == ActionType.OPEN) {
                branch.getTerminal1().disconnect();
                branch.getTerminal2().disconnect();
            } else {
                branch.getTerminal1().connect();
                branch.getTerminal2().connect();
            }
        } else {
            throw new NotImplementedException("Topological actions are only on branches for now");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Topology topology = (Topology) o;
        return super.equals(o) && actionType == topology.getActionType();
    }

    @Override
    public int hashCode() {
        return String.format("%s%s", getId(), getActionType().toString()).hashCode();
    }
}
