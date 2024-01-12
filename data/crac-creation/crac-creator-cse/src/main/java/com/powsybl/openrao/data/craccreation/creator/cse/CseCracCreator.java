/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.cse;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreator;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cse.criticalbranch.TCriticalBranchesAdder;
import com.powsybl.openrao.data.craccreation.creator.cse.criticalbranch.TMonitoredElementsAdder;
import com.powsybl.openrao.data.craccreation.creator.cse.outage.TOutageAdder;
import com.powsybl.openrao.data.craccreation.creator.cse.parameters.CseCracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cse.remedialaction.TRemedialActionAdder;
import com.powsybl.openrao.data.craccreation.creator.cse.xsd.CRACDocumentType;
import com.powsybl.openrao.data.craccreation.creator.cse.xsd.TCRACSeries;
import com.powsybl.openrao.data.craccreation.util.ucte.UcteNetworkAnalyzer;
import com.powsybl.openrao.data.craccreation.util.ucte.UcteNetworkAnalyzerProperties;
import com.powsybl.openrao.data.cracutil.CracValidator;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@AutoService(CracCreator.class)
public class CseCracCreator implements CracCreator<CseCrac, CseCracCreationContext> {
    CseCracCreationContext creationContext;

    @Override
    public String getNativeCracFormat() {
        return "CseCrac";
    }

    @Override
    public CseCracCreationContext createCrac(CseCrac cseCrac, Network network, OffsetDateTime offsetDateTime, CracCreationParameters cracCreationParameters) {
        // Set attributes
        Crac crac = cracCreationParameters.getCracFactory().create(cseCrac.getCracDocument().getDocumentIdentification().getV());
        addCseInstants(crac);
        this.creationContext = new CseCracCreationContext(crac, offsetDateTime, network.getNameOrId());

        // Check timestamp field
        if (offsetDateTime != null) {
            creationContext.getCreationReport().warn("Timestamp filtering is not implemented for cse crac creator. The timestamp will be ignored.");
        }

        // Get warning messages from parameters parsing
        CseCracCreationParameters cseCracCreationParameters = cracCreationParameters.getExtension(CseCracCreationParameters.class);
        if (cseCracCreationParameters != null) {
            cseCracCreationParameters.getFailedParseWarnings().forEach(message -> creationContext.getCreationReport().warn(message));
        }

        // Check for UCTE compatibility
        if (!network.getSourceFormat().equals("UCTE")) {
            creationContext.getCreationReport().error("CSE CRAC creation is only possible with a UCTE network");
            return creationContext.creationFailure();
        }

        // Import elements from the CRAC
        try {
            UcteNetworkAnalyzer ucteNetworkAnalyzer = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));

            TCRACSeries tcracSeries = getCracSeries(cseCrac.getCracDocument());
            // Add outages
            new TOutageAdder(tcracSeries, crac, ucteNetworkAnalyzer, creationContext).add();
            // Add critical branches
            TCriticalBranchesAdder tCriticalBranchesAdder = new TCriticalBranchesAdder(tcracSeries, crac, ucteNetworkAnalyzer, creationContext, cracCreationParameters.getDefaultMonitoredSides());
            tCriticalBranchesAdder.add();
            // Add remedial actions
            new TRemedialActionAdder(tcracSeries, crac, network, ucteNetworkAnalyzer, tCriticalBranchesAdder.getRemedialActionsForCnecsMap(), creationContext, cseCracCreationParameters).add();
            // Add monitored elements
            TMonitoredElementsAdder tMonitoredElementsAdder = new TMonitoredElementsAdder(tcracSeries, crac, ucteNetworkAnalyzer, creationContext, cracCreationParameters.getDefaultMonitoredSides());
            tMonitoredElementsAdder.add();

            creationContext.buildCreationReport();
            CracValidator.validateCrac(crac, network).forEach(creationContext.getCreationReport()::added);
            // TODO : add unit test for CracValidator.validateCrac step when auto RAs are handled
            return creationContext.creationSuccess(crac);
        } catch (OpenRaoException e) {
            creationContext.getCreationReport().error(String.format("CRAC could not be created: %s", e.getMessage()));
            return creationContext.creationFailure();
        }
    }

    private static void addCseInstants(Crac crac) {
        crac.newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative", InstantKind.CURATIVE);
    }

    public static TCRACSeries getCracSeries(CRACDocumentType cracDocumentType) {
        // Check that there is only one CRACSeries in the file, which defines the CRAC
        // XSD enables several CRACSeries but without any further specification it doesn't make sense.
        List<TCRACSeries> tcracSeriesList = cracDocumentType.getCRACSeries();
        if (tcracSeriesList.size() != 1) {
            throw new OpenRaoException("CRAC file contains no or more than one <CRACSeries> tag which is not handled.");
        }
        return tcracSeriesList.get(0);
    }

}
