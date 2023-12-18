/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.rao_api.parameters.extensions;

import com.powsybl.open_rao.rao_api.parameters.ParametersUtil;
import com.powsybl.open_rao.rao_api.parameters.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;

import java.util.ArrayList;
import java.util.Objects;

import static com.powsybl.open_rao.rao_api.RaoParametersCommons.*;
/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class LoopFlowParametersConfigLoader implements RaoParameters.ConfigLoader<LoopFlowParametersExtension> {

    @Override
    public LoopFlowParametersExtension load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        LoopFlowParametersExtension parameters = new LoopFlowParametersExtension();
        platformConfig.getOptionalModuleConfig(LOOP_FLOW_PARAMETERS_SECTION)
                .ifPresent(config -> {
                    parameters.setAcceptableIncrease(config.getDoubleProperty(ACCEPTABLE_INCREASE, LoopFlowParametersExtension.DEFAULT_ACCEPTABLE_INCREASE));
                    parameters.setPtdfApproximation(config.getEnumProperty(PTDF_APPROXIMATION, PtdfApproximation.class, LoopFlowParametersExtension.DEFAULT_PTDF_APPROXIMATION));
                    parameters.setConstraintAdjustmentCoefficient(config.getDoubleProperty(CONSTRAINT_ADJUSTMENT_COEFFICIENT, LoopFlowParametersExtension.DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT));
                    parameters.setViolationCost(config.getDoubleProperty(VIOLATION_COST, LoopFlowParametersExtension.DEFAULT_VIOLATION_COST));
                    parameters.setCountries(ParametersUtil.convertToCountrySet(config.getStringListProperty(COUNTRIES, new ArrayList<>())));
                });
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return LOOP_FLOW_PARAMETERS_SECTION;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super LoopFlowParametersExtension> getExtensionClass() {
        return LoopFlowParametersExtension.class;
    }
}
