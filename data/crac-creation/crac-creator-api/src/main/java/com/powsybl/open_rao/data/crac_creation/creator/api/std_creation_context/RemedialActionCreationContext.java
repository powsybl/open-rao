/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_creation.creator.api.std_creation_context;

import com.powsybl.open_rao.data.crac_creation.creator.api.ElementaryCreationContext;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface RemedialActionCreationContext extends ElementaryCreationContext {
    /**
     * Get the id of the created remedial action
     * Underlying assumption: one native RA has been converted into one created RA
     */
    String getCreatedRAId();
}
