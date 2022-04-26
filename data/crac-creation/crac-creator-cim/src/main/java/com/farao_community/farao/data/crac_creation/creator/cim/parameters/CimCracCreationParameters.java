/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cim.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.RangeActionGroup;
import com.powsybl.commons.extensions.AbstractExtension;

import java.util.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CimCracCreationParameters extends AbstractExtension<CracCreationParameters> {

    static final List<String> DEFAULT_RA_GROUPS_AS_STRING = new ArrayList<>();

    private List<String> raGroupsAsString = DEFAULT_RA_GROUPS_AS_STRING;
    private List<RangeActionGroup> raGroups = new ArrayList<>();
    private List<String> failedParseMessages = new ArrayList<>();

    @Override
    public String getName() {
        return "CimCracCreatorParameters";
    }

    public List<String> getRangeActionGroupsAsString() {
        return raGroupsAsString;
    }

    public List<RangeActionGroup> getRangeActionGroups() {
        return raGroups;
    }

    public void setRangeActionGroupsAsString(List<String> raGroupsAsString) {
        this.raGroupsAsString = new ArrayList<>();
        raGroups = new ArrayList<>();
        failedParseMessages = new ArrayList<>();
        raGroupsAsString.forEach(concatenatedId -> {
            try {
                raGroups.add(new RangeActionGroup(RangeActionGroup.parse(concatenatedId)));
                this.raGroupsAsString.add(concatenatedId);
            } catch (FaraoException e) {
                this.failedParseMessages.add(e.getMessage());
            }
        });
    }

    public List<String> getFailedParseWarnings() {
        return failedParseMessages;
    }
}
