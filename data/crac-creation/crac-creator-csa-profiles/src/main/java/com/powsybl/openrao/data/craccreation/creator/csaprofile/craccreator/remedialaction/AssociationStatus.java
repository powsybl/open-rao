/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.constants.ElementCombinationConstraintKind;

/**
 * @author Thomas Bouquet <thomas.bouquet at rte-france.com>
 */
public record AssociationStatus(boolean isValid,
                                ElementCombinationConstraintKind elementCombinationConstraintKind,
                                String statusDetails) {
}
