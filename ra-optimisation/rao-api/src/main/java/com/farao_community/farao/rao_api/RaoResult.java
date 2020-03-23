/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * RAO result API. This class will contain information about the RAO computation (computation status, logs, etc).
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */

@JsonIgnoreProperties(value = { "ok" })
public class RaoResult {

    public enum Status {
        FAILURE,
        SUCCESS
    }

    private Status status;

    private String preOptimVariantId;

    private String postOptimVariantId;

    @JsonCreator
    public RaoResult(@JsonProperty("status") Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isOk() {
        return status == Status.SUCCESS;
    }

    public void setPreOptimVariantId(String preOptimVariantId) {
        this.preOptimVariantId = preOptimVariantId;
    }

    public String getPreOptimVariantId() {
        return preOptimVariantId;
    }

    public void setPostOptimVariantId(String postOptimVariantId) {
        this.postOptimVariantId = postOptimVariantId;
    }

    public String getPostOptimVariantId() {
        return postOptimVariantId;
    }
}
