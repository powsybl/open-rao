/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.csaprofiles.nc;

import com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.constants.CsaProfileConstants;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record TapPositionAction(String mrid, String tapChangerId, String propertyReference, boolean normalEnabled, String gridStateAlterationRemedialAction, String gridStateAlterationCollection) implements GridStateAlteration {
    public static TapPositionAction fromPropertyBag(PropertyBag propertyBag) {
        return new TapPositionAction(
            propertyBag.getId(CsaProfileConstants.TAP_POSITION_ACTION),
            propertyBag.getId(CsaProfileConstants.TAP_CHANGER_ID),
            propertyBag.get(CsaProfileConstants.GRID_ALTERATION_PROPERTY_REFERENCE),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.NORMAL_ENABLED, "true")),
            propertyBag.getId(CsaProfileConstants.REQUEST_GRID_STATE_ALTERATION_REMEDIAL_ACTION),
            propertyBag.getId(CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION)
        );
    }
}
