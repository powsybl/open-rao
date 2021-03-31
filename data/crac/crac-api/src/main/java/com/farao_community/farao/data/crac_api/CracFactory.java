/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.FaraoException;
import com.google.common.base.Suppliers;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.util.ServiceLoaderCache;
import java.util.List;
import java.util.Set;

/**
 * Crac Factory interface.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface CracFactory {

    /**
     * Create a {@code Crac} object.
     *
     * @param id: ID to assign to the created Crac.
     * @param name: Name to assign to the created Crac.
     * @param postContingencyInstants: Set of post-contingency instants that are considered in the CRAC chronology.
     * @return the created {@code Crac} instance.
     */
    Crac create(String id, String name, Set<Instant> postContingencyInstants);

    /**
     * Create a {@code Crac} object. Name will be equal to id.
     *
     * @param id: ID to assign to the created Crac.
     * @param postContingencyInstants: Set of post-contingency instants that are considered in the CRAC chronology.
     * @return the created {@code Crac} instance with given ID, name equal to ID.
     */
    default Crac create(String id, Set<Instant> postContingencyInstants) {
        return create(id, id, postContingencyInstants);
    }

    /**
     * Function that returns the name of the implementation
     *
     * @return The name of the CracFactory implementation.
     */
    String getName();

    /**
     * Find a {@code CracFactory} implementation by its name
     *
     * @param factoryName: The name of the {@code CracFactory} implementation.
     * @return An instance of the {@code CracFactory} implementation.
     * @throws FaraoException if the factory name is not recognized as an existent implementation.
     */
    static CracFactory find(String factoryName) {
        List<CracFactory> providers = Suppliers.memoize(() -> new ServiceLoaderCache<>(CracFactory.class).getServices()).get();
        if (providers.size() == 1 && factoryName == null) {
            return providers.get(0);
        } else if (factoryName != null) {
            for (CracFactory provider : providers) {
                if (provider.getName().equals(factoryName)) {
                    return provider;
                }
            }
            throw new FaraoException("Crac factory '" + factoryName + "' not found");
        } else {
            throw new FaraoException("No CracFactory implementation found, or no default implementation set and multiple implementation found.");
        }
    }

    /**
     * Get an instance of the default {@code CracFactory} implementation
     *
     * @return An instance of the default {@code CracFactory} implementation.
     * @throws FaraoException if no default has been set and multiple {@code CracFactory} implementations exist.
     */
    static CracFactory findDefault() {
        String factoryName = PlatformConfig.defaultConfig().getOptionalModuleConfig("crac")
                .flatMap(mc -> mc.getOptionalStringProperty("default"))
                .orElse(null);
        return find(factoryName);
    }
}
