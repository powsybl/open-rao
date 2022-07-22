/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.AngleCnecAdder;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimConstants;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.AdditionalConstraintRegisteredResource;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.AdditionalConstraintSeries;
import com.powsybl.iidm.network.Network;

import java.util.Objects;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class AdditionalConstraintSeriesCreator {
    private final Crac crac;
    private final Network network;
    private AdditionalConstraintSeries additionalConstraintSerie;
    private CimCracCreationContext cracCreationContext;
    private String contingencyId;
    private String cimSerieId;

    public AdditionalConstraintSeriesCreator(Crac crac, Network network, AdditionalConstraintSeries additionalConstraintSerie, String contingencyId, String cimSerieId, CimCracCreationContext cracCreationContext) {
        this.crac = crac;
        this.network = network;
        this.additionalConstraintSerie = additionalConstraintSerie;
        this.contingencyId = contingencyId;
        this.cimSerieId = cimSerieId;
        this.cracCreationContext = cracCreationContext;
    }

    public AngleCnec createAndAddAdditionalConstraintSeries() {
        String additionalConstraintSerieId = additionalConstraintSerie.getMRID();

        if (!checkAdditionalConstraintRegisteredResource()) {
            return null;
        }

        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
                .withId(additionalConstraintSerieId)
                .withName(additionalConstraintSerie.getName())
                .withMonitored()
                .withOptimized(false)
                .withReliabilityMargin(0.)
                .newThreshold().withUnit(Unit.DEGREE).withMax(additionalConstraintSerie.getQuantityQuantity().doubleValue()).add()
                .withInstant(Instant.CURATIVE)
                .withContingency(contingencyId);

        for (AdditionalConstraintRegisteredResource rr : additionalConstraintSerie.getRegisteredResource()) {
            String networkElement = rr.getMRID().getValue();
            if (Objects.isNull(network.getVoltageLevel(networkElement))) {
                this.cracCreationContext.addAngleCnecCreationContext(AngleCnecCreationContext.notImported(additionalConstraintSerieId, contingencyId, cimSerieId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s is not a Voltage Level", networkElement)));
                return null;
            }
            String marketObjectStatus = rr.getMarketObjectStatusStatus();
            if (marketObjectStatus.equals(CimConstants.IMPORTING_ELEMENT)) {
                angleCnecAdder.withImportingNetworkElement(networkElement);
            } else if (marketObjectStatus.equals(CimConstants.EXPORTING_ELEMENT)) {
                angleCnecAdder.withExportingNetworkElement(networkElement);
            } else {
                this.cracCreationContext.addAngleCnecCreationContext(AngleCnecCreationContext.notImported(additionalConstraintSerieId, contingencyId, cimSerieId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong market object status : %s", marketObjectStatus)));
                return null;
            }
        }

        this.cracCreationContext.addAngleCnecCreationContext(AngleCnecCreationContext.imported(additionalConstraintSerieId, contingencyId, cimSerieId, ""));
        return angleCnecAdder.add();
    }

    private boolean checkAdditionalConstraintRegisteredResource() {
        String additionalConstraintSerieId = additionalConstraintSerie.getMRID();
        // Read business type
        if (!additionalConstraintSerie.getBusinessType().equals(CimConstants.PHASE_SHIFT_ANGLE)) {
            this.cracCreationContext.addAngleCnecCreationContext(AngleCnecCreationContext.notImported(additionalConstraintSerieId, contingencyId, cimSerieId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong businessType: %s", additionalConstraintSerie.getBusinessType())));
            return false;
        }
        // Check measurement unit
        if (!additionalConstraintSerie.getMeasurementUnitName().equals(CimConstants.DEGREE)) {
            this.cracCreationContext.addAngleCnecCreationContext(AngleCnecCreationContext.notImported(additionalConstraintSerieId, contingencyId, cimSerieId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong measurement unit : %s", additionalConstraintSerie.getMeasurementUnitName())));
            return false;
        }
        // Check number of registered resources
        if (additionalConstraintSerie.getRegisteredResource().size() != 2) {
            this.cracCreationContext.addAngleCnecCreationContext(AngleCnecCreationContext.notImported(additionalConstraintSerieId, contingencyId, cimSerieId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong number of registered resources : %s instead of 2", additionalConstraintSerie.getRegisteredResource().size())));
            return false;
        }
        // Check that quantity is defined
        if (Objects.isNull(additionalConstraintSerie.getQuantityQuantity())) {
            this.cracCreationContext.addAngleCnecCreationContext(AngleCnecCreationContext.notImported(additionalConstraintSerieId, contingencyId, cimSerieId, ImportStatus.INCOMPLETE_DATA, "Missing quantity"));
            return false;
        }
        return true;
    }
}
