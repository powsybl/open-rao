/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreationReport;
import com.farao_community.farao.data.crac_creation.creator.api.ElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec.CsaProfileCnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.contingency.CsaProfileContingencyCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action.CsaProfileRemedialActionCreationContext;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCracCreationContext implements CracCreationContext {

    private Crac crac;

    private boolean isCreationSuccessful;

    private Set<CsaProfileContingencyCreationContext> contingencyCreationContexts;
    private Set<CsaProfileRemedialActionCreationContext> remedialActionCreationContext;
    private Set<CsaProfileCnecCreationContext> flowCnecCreationContexts;

    private CracCreationReport creationReport;

    private final OffsetDateTime timeStamp;

    private final String networkName;

    CsaProfileCracCreationContext(Crac crac, OffsetDateTime timeStamp, String networkName) {
        this.crac = crac;
        this.creationReport = new CracCreationReport();
        this.timeStamp = timeStamp;
        this.networkName = networkName;
    }

    protected CsaProfileCracCreationContext(CsaProfileCracCreationContext toCopy) {
        this.crac = toCopy.crac;
        this.creationReport = toCopy.creationReport;
        this.timeStamp = toCopy.timeStamp;
        this.networkName = toCopy.networkName;
        this.isCreationSuccessful = toCopy.isCreationSuccessful;
        this.contingencyCreationContexts = new HashSet<>(toCopy.contingencyCreationContexts);
        this.remedialActionCreationContext = new HashSet<>(toCopy.remedialActionCreationContext);
        this.flowCnecCreationContexts = new HashSet<>(toCopy.flowCnecCreationContexts);
    }

    @Override
    public boolean isCreationSuccessful() {
        return this.isCreationSuccessful;
    }

    @Override
    public Crac getCrac() {
        return this.crac;
    }

    @Override
    public OffsetDateTime getTimeStamp() {
        return this.timeStamp;
    }

    @Override
    public String getNetworkName() {
        return this.networkName;
    }

    public void setContingencyCreationContexts(Set<CsaProfileContingencyCreationContext> contingencyCreationContexts) {
        this.contingencyCreationContexts = contingencyCreationContexts.stream().collect(Collectors.toSet());
    }

    public Set<CsaProfileContingencyCreationContext> getContingencyCreationContexts() {
        return this.contingencyCreationContexts.stream().collect(Collectors.toSet());
    }

    public Set<CsaProfileRemedialActionCreationContext> getRemedialActionCreationContext() {
        return remedialActionCreationContext;
    }

    public void setRemedialActionCreationContext(Set<CsaProfileRemedialActionCreationContext> remedialActionCreationContext) {
        this.remedialActionCreationContext = remedialActionCreationContext;
    }

    public void setFlowCnecCreationContexts(Set<CsaProfileCnecCreationContext> flowCnecCreationContexts) {
        this.flowCnecCreationContexts = flowCnecCreationContexts.stream().collect(Collectors.toSet());
    }

    public Set<CsaProfileCnecCreationContext> getFlowCnecCreationContexts() {
        return this.flowCnecCreationContexts.stream().collect(Collectors.toSet());
    }

    @Override
    public CracCreationReport getCreationReport() {
        return this.creationReport;
    }

    CsaProfileCracCreationContext creationFailure() {
        this.isCreationSuccessful = false;
        this.crac = null;
        return this;
    }

    CsaProfileCracCreationContext creationSuccess(Crac crac) {
        this.isCreationSuccessful = true;
        this.crac = crac;
        return this;
    }

    public void buildCreationReport() {
        creationReport = new CracCreationReport();
        addToReport(contingencyCreationContexts, "Contingencies");
        addToReport(remedialActionCreationContext, "RemedialActions");
        addToReport(flowCnecCreationContexts, "FlowCnecs");
    }

    private void addToReport(Collection<? extends ElementaryCreationContext> contexts, String nativeTypeIdentifier) {
        contexts.stream().filter(ElementaryCreationContext::isAltered).forEach(context ->
            creationReport.altered(String.format("%s \"%s\" was modified: %s. ", nativeTypeIdentifier, context.getNativeId(), context.getImportStatusDetail()))
        );
        contexts.stream().filter(context -> !context.isImported()).forEach(context ->
            creationReport.removed(String.format("%s \"%s\" was not imported: %s. %s.", nativeTypeIdentifier, context.getNativeId(), context.getImportStatus(), context.getImportStatusDetail()))
        );
    }
}
