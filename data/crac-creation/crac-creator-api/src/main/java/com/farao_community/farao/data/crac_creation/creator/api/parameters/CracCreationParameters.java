/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.api.parameters;

import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.google.common.base.Suppliers;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionConfigLoader;
import com.powsybl.commons.extensions.ExtensionProviders;

import java.util.*;
import java.util.function.Supplier;

/**
 * Parameters related to the creation of a CRAC.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracCreationParameters extends AbstractExtendable<CracCreationParameters> {

    static final String MODULE_NAME = "crac-creation-parameters";
    private static final String DEFAULT_CRAC_FACTORY_NAME = CracFactory.findDefault().getName();
    static final MonitoredLineSide DEFAULT_DEFAULT_MONITORED_LINE_SIDE = MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES;

    public interface ConfigLoader<E extends Extension<CracCreationParameters>> extends ExtensionConfigLoader<CracCreationParameters, E> { }

    private static final Supplier<ExtensionProviders<ConfigLoader>> PARAMETERS_EXTENSIONS_SUPPLIER =
        Suppliers.memoize(() -> ExtensionProviders.createProvider(ConfigLoader.class, MODULE_NAME));

    private String cracFactoryName = DEFAULT_CRAC_FACTORY_NAME;

    public enum MonitoredLineSide {
        MONITOR_LINES_ON_LEFT_SIDE(Set.of(Side.LEFT)),
        MONITOR_LINES_ON_RIGHT_SIDE(Set.of(Side.RIGHT)),
        MONITOR_LINES_ON_BOTH_SIDES(Set.of(Side.LEFT, Side.RIGHT));

        private final Set<Side> monitoredSides;
        MonitoredLineSide(Set<Side> monitoredSides) {
            this.monitoredSides = monitoredSides;
        }

        Set<Side> getMonitoredSides() {
            return monitoredSides;
        }
    }

    private MonitoredLineSide defaultMonitoredLineSide = DEFAULT_DEFAULT_MONITORED_LINE_SIDE;

    public String getCracFactoryName() {
        return cracFactoryName;
    }

    public void setCracFactoryName(String cracFactoryName) {
        this.cracFactoryName = cracFactoryName;
    }

    public CracFactory getCracFactory() {
        return CracFactory.find(cracFactoryName);
    }

    public Set<Side> getDefaultMonitoredSides() {
        return defaultMonitoredLineSide.getMonitoredSides();
    }

    MonitoredLineSide getDefaultMonitoredLineSide() {
        return defaultMonitoredLineSide;
    }

    public void setDefaultMonitoredLineSide(MonitoredLineSide defaultMonitoredLineSide) {
        this.defaultMonitoredLineSide = defaultMonitoredLineSide;
    }

    public static CracCreationParameters load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static CracCreationParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        CracCreationParameters parameters = new CracCreationParameters();

        platformConfig.getOptionalModuleConfig(MODULE_NAME)
            .ifPresent(config -> parameters.setCracFactoryName(config.getStringProperty("crac-factory", DEFAULT_CRAC_FACTORY_NAME)));

        parameters.readExtensions(platformConfig);
        return parameters;
    }

    private void readExtensions(PlatformConfig platformConfig) {
        for (ExtensionConfigLoader provider : PARAMETERS_EXTENSIONS_SUPPLIER.get().getProviders()) {
            addExtension(provider.getExtensionClass(), provider.load(platformConfig));
        }
    }
}
