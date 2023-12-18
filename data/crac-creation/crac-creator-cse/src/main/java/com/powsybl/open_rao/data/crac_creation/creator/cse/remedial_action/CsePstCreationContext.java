/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_creation.creator.cse.remedial_action;

import com.powsybl.open_rao.data.crac_creation.creator.api.ImportStatus;
import com.powsybl.open_rao.data.crac_creation.creator.api.std_creation_context.PstRangeActionCreationContext;
import com.powsybl.open_rao.data.crac_creation.creator.cse.xsd.TRemedialAction;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class CsePstCreationContext extends CseRemedialActionCreationContext implements PstRangeActionCreationContext {
    private final String nativeNetworkElementId;
    private final boolean isInverted;

    private CsePstCreationContext(TRemedialAction tRemedialAction, String nativeNetworkElementId, String createdRaId, ImportStatus importStatus, boolean isInverted, String inversionDetail) {
        super(tRemedialAction, createdRaId, importStatus, false, inversionDetail);
        this.nativeNetworkElementId = nativeNetworkElementId;
        this.isInverted = isInverted;
    }

    public static CsePstCreationContext imported(TRemedialAction tRemedialAction, String nativeNetworkElementId, String createdRAId, boolean isInverted, String inversionDetail) {
        return new CsePstCreationContext(tRemedialAction, nativeNetworkElementId, createdRAId, ImportStatus.IMPORTED, isInverted, inversionDetail);
    }

    public static CsePstCreationContext notImported(TRemedialAction tRemedialAction, String nativeNetworkElementId, ImportStatus importStatus, String importStatusDetail) {
        return new CsePstCreationContext(tRemedialAction, nativeNetworkElementId, null, importStatus, false, importStatusDetail);
    }

    @Override
    public boolean isInverted() {
        return isInverted;
    }

    @Override
    public String getNativeNetworkElementId() {
        return nativeNetworkElementId;
    }
}
