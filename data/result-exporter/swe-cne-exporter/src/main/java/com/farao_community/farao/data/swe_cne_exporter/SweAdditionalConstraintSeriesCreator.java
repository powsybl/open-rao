/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.AngleCnecCreationContext;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.swe_cne_exporter.xsd.AdditionalConstraintSeries;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;

/**
 * Generates AdditionalConstraintSeries for SWE CNE format
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class SweAdditionalConstraintSeriesCreator {
    public static final String ANGLE_CNEC_BUSINESS_TYPE = "B87";
    private final SweCneHelper sweCneHelper;
    private final CimCracCreationContext cracCreationContext;

    public SweAdditionalConstraintSeriesCreator(SweCneHelper sweCneHelper, CimCracCreationContext cracCreationContext) {
        this.sweCneHelper = sweCneHelper;
        this.cracCreationContext = cracCreationContext;
    }

    public List<AdditionalConstraintSeries> generateAdditionalConstraintSeries(Contingency contingency) {
        List<AngleCnecCreationContext> sortedAngleCnecs = cracCreationContext.getAngleCnecCreationContexts().stream()
            .filter(AngleCnecCreationContext::isImported)
            .sorted(Comparator.comparing(AngleCnecCreationContext::getCreatedCnecId))
            .toList();
        if (contingency == null) {
            sortedAngleCnecs.stream().filter(angleCnecCreationContext -> Objects.isNull(angleCnecCreationContext.getContingencyId()))
                .forEach(angleCnecCreationContext ->
                    BUSINESS_WARNS.warn("Preventive angle cnec {} will not be added to CNE file", angleCnecCreationContext.getNativeId()));
            return Collections.emptyList();
        } else {
            return sortedAngleCnecs.stream()
                .filter(angleCnecCreationContext -> angleCnecCreationContext.getContingencyId().equals(contingency.getId()))
                .map(this::generateAdditionalConstraintSeries)
                .filter(Objects::nonNull)
                .toList();
        }
    }

    private AdditionalConstraintSeries generateAdditionalConstraintSeries(AngleCnecCreationContext angleCnecCreationContext) {
        Crac crac = sweCneHelper.getCrac();
        AngleCnec angleCnec = crac.getAngleCnec(angleCnecCreationContext.getCreatedCnecId());
        if (!angleCnec.getState().getInstant().isCurative()) {
            BUSINESS_WARNS.warn("{} angle cnec {} will not be added to CNE file", angleCnec.getState().getInstant(), angleCnecCreationContext.getNativeId());
            return null;
        }
        AdditionalConstraintSeries additionalConstraintSeries = new AdditionalConstraintSeries();
        additionalConstraintSeries.setMRID(angleCnecCreationContext.getCreatedCnecId());
        additionalConstraintSeries.setBusinessType(ANGLE_CNEC_BUSINESS_TYPE);
        additionalConstraintSeries.setName(angleCnec.getName());
        if (!sweCneHelper.getRaoResult().getComputationStatus().equals(ComputationStatus.FAILURE)) {
            additionalConstraintSeries.setQuantityQuantity(BigDecimal.valueOf(sweCneHelper.getRaoResult().getAngle(crac.getInstant(InstantKind.CURATIVE), angleCnec, Unit.DEGREE)).setScale(1, RoundingMode.HALF_UP));
        }
        return additionalConstraintSeries;
    }
}
