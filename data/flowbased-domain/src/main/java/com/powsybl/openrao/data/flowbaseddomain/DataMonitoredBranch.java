/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.flowbaseddomain;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;
import java.util.Collections;
import java.util.List;

/**
 * Business Object of the FlowBased DataMonitoredBranch
 *
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
@Builder
@Data
public class DataMonitoredBranch {
    @NotNull(message = "monitoredBranch.id.empty")
    private final String id;
    @NotNull(message = "monitoredBranch.name.empty")
    private final String name;
    @NotNull(message = "instant.id.empty")
    private String instantId;
    @NotNull(message = "monitoredBranch.branchId.empty")
    private final String branchId;
    private double fmin;
    private double fmax;
    @NotNull(message = "dataMonitoredBranch.fref.empty")
    private double fref;

    @NotNull(message = "dataMonitoredBranch.ptdfList.empty")
    private List<DataPtdfPerCountry> ptdfList;

    @ConstructorProperties({"id", "name", "instantId", "branchId", "fmin", "fmax", "fref", "ptdfList"})
    public DataMonitoredBranch(final String id, final String name, final String instantId, final String branchId, final double fmin, final double fmax, final double fref, final List<DataPtdfPerCountry> ptdfList) {
        this.id = id;
        this.name = name;
        this.instantId = instantId;
        this.branchId = branchId;
        this.fmin = fmin;
        this.fmax = fmax;
        this.fref = fref;
        this.ptdfList = Collections.unmodifiableList(ptdfList);
    }

    public DataPtdfPerCountry findPtdfByCountry(String country) {
        return ptdfList.stream()
                .filter(ptdf -> ptdf.getCountry().equals(country))
                .findAny()
                .orElse(null);
    }
}
