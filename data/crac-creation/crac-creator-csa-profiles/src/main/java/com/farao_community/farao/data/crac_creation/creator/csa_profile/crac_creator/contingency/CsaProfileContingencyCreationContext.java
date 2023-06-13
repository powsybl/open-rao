/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.contingency;

import com.farao_community.farao.data.crac_creation.creator.api.ElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public final class CsaProfileContingencyCreationContext implements ElementaryCreationContext {
    private String contingencyID;
    private String contingencyName;
    private ImportStatus importStatus;
    private String importStatusDetail;
    private boolean isAltered;

    private CsaProfileContingencyCreationContext(String contingencyID, String contingencyName, ImportStatus importStatus, String importStatusDetail, boolean isAltered) {
        this.contingencyID = contingencyID;
        this.contingencyName = contingencyName;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
        this.isAltered = isAltered;
    }

    public static CsaProfileContingencyCreationContext imported(String contingencyID, String contingencyName, String importStatusDetail, boolean isAltered) {
        return new CsaProfileContingencyCreationContext(contingencyID, contingencyName, ImportStatus.IMPORTED, importStatusDetail, isAltered);
    }

    public static CsaProfileContingencyCreationContext notImported(String contingencyID, String contingencyName, ImportStatus importStatus, String importStatusDetail) {
        return new CsaProfileContingencyCreationContext(contingencyID, contingencyName, importStatus, importStatusDetail, false);
    }

    @Override
    public String getNativeId() {
        return this.contingencyID;
    }

    public String getNativeName() {
        return this.contingencyName;
    }

    @Override
    public boolean isImported() {
        return ImportStatus.IMPORTED.equals(this.importStatus);
    }

    @Override
    public boolean isAltered() {
        return this.isAltered;
    }

    @Override
    public ImportStatus getImportStatus() {
        return this.importStatus;
    }

    @Override
    public String getImportStatusDetail() {
        return this.importStatusDetail;
    }
}
