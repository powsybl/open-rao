/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracioapi;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;

import javax.annotation.Nonnull;
import java.io.InputStream;

/**
 * Interface for CRAC object import
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

public interface CracImporter {

    Crac importCrac(InputStream inputStream, @Nonnull CracFactory cracFactory, Network network);

    Crac importCrac(InputStream inputStream, Network network);

    boolean exists(String fileName, InputStream inputStream);
}
