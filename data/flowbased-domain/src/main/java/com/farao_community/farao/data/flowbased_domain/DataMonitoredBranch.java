/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flowbased_domain;

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
    private double fmax;
    @NotNull(message = "dataMonitoredBranch.fref.empty")
    private double fref;

    @NotNull(message = "dataMonitoredBranch.ptdfList.empty")
    private List<DataPtdfPerCountry> ptdfList;

    @ConstructorProperties({"id", "name", "instantId", "branchId", "fmax", "fref", "ptdfList"})
    public DataMonitoredBranch(final String id, final String name, final String instantId, final String branchId, final double fmax, final double fref, final List<DataPtdfPerCountry> ptdfList) {
        this.id = id;
        this.name = name;
        this.instantId = instantId;
        this.branchId = branchId;
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

    public boolean correspondsTo(DataMonitoredBranch other) {
        return this.getId().equals(other.getId())
            && this.getName().equals(other.getName())
            && this.getInstantId().equals(other.getInstantId())
            && this.getBranchId().equals(other.getBranchId());
    }

    public void updateDataMonitoredBranch(DataMonitoredBranch other) {
        this.fmax = other.fmax;
        this.fref = other.fref;
        this.ptdfList = other.ptdfList;
    }
}
