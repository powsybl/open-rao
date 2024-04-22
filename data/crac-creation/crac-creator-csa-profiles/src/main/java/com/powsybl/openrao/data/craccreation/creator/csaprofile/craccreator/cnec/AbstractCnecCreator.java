package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.CnecAdder;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec.nc.AssessedElement;
import com.powsybl.openrao.data.craccreation.util.cgmes.CgmesBranchHelper;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractCnecCreator {
    protected final Crac crac;
    protected final Network network;
    protected final AssessedElement nativeAssessedElement;
    protected final List<Contingency> linkedContingencies;
    protected final PropertyBag operationalLimitPropertyBag;
    protected Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts;
    protected final CsaProfileCracCreationContext cracCreationContext;
    protected final String rejectedLinksAssessedElementContingency;
    protected final boolean aeSecuredForRegion;
    protected final boolean aeScannedForRegion;

    protected AbstractCnecCreator(Crac crac, Network network, AssessedElement nativeAssessedElement, PropertyBag operationalLimitPropertyBag, List<Contingency> linkedContingencies, Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts, CsaProfileCracCreationContext cracCreationContext, String rejectedLinksAssessedElementContingency, boolean aeSecuredForRegion, boolean aeScannedForRegion) {
        this.crac = crac;
        this.network = network;
        this.nativeAssessedElement = nativeAssessedElement;
        this.operationalLimitPropertyBag = operationalLimitPropertyBag;
        this.linkedContingencies = linkedContingencies;
        this.csaProfileCnecCreationContexts = csaProfileCnecCreationContexts;
        this.cracCreationContext = cracCreationContext;
        this.rejectedLinksAssessedElementContingency = rejectedLinksAssessedElementContingency;
        this.aeSecuredForRegion = aeSecuredForRegion;
        this.aeScannedForRegion = aeScannedForRegion;
    }

    protected Identifiable<?> getNetworkElementInNetwork(String networkElementId) {
        Identifiable<?> networkElement = network.getIdentifiable(networkElementId);
        if (networkElement == null) {
            CgmesBranchHelper cgmesBranchHelper = new CgmesBranchHelper(networkElementId, network);
            if (cgmesBranchHelper.isValid()) {
                networkElement = cgmesBranchHelper.getBranch();
            }
        }

        if (networkElement instanceof DanglingLine danglingLine) {
            Optional<TieLine> optionalTieLine = danglingLine.getTieLine();
            if (optionalTieLine.isPresent()) {
                networkElement = optionalTieLine.get();
            }
        }
        return networkElement;
    }

    protected String writeAssessedElementIgnoredReasonMessage(String reason) {
        return "AssessedElement " + nativeAssessedElement.identifier() + " ignored because " + reason;
    }

    protected String getCnecName(String instantId, Contingency contingency) {
        // Need to include the mRID in the name in case the AssessedElement's name is not unique
        return "%s (%s) - %s%s".formatted(nativeAssessedElement.getUniqueName(), nativeAssessedElement.identifier(), contingency == null ? "" : contingency.getName().orElse(contingency.getId()) + " - ", instantId);
    }

    protected String getCnecName(String instantId, Contingency contingency, int tatlDuration) {
        // Add TATL duration in case to CNECs of the same instant are created with different TATLs
        return "%s - TATL %s".formatted(getCnecName(instantId, contingency), tatlDuration);
    }

    protected void addCnecBaseInformation(CnecAdder<?> cnecAdder, Contingency contingency, String instantId) {
        String cnecName = getCnecName(instantId, contingency);
        initCnecAdder(cnecAdder, contingency, instantId, cnecName);
    }

    protected void addCnecBaseInformation(CnecAdder<?> cnecAdder, Contingency contingency, String instantId, int tatlDuration) {
        String cnecName = getCnecName(instantId, contingency, tatlDuration);
        initCnecAdder(cnecAdder, contingency, instantId, cnecName);
    }

    private void initCnecAdder(CnecAdder<?> cnecAdder, Contingency contingency, String instantId, String cnecName) {
        cnecAdder.withContingency(contingency == null ? null : contingency.getId())
            .withId(cnecName)
            .withName(cnecName)
            .withInstant(instantId)
            .withOperator(CsaProfileCracUtils.getTsoNameFromUrl(nativeAssessedElement.operator()))
            .withOptimized(aeSecuredForRegion)
            .withMonitored(aeScannedForRegion);
    }

    protected void markCnecAsImportedAndHandleRejectedContingencies(String cnecName) {
        if (rejectedLinksAssessedElementContingency.isEmpty()) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.imported(nativeAssessedElement.identifier(), cnecName, cnecName, "", false));
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.imported(nativeAssessedElement.identifier(), cnecName, cnecName, "some cnec for the same assessed element are not imported because of incorrect data for assessed elements for contingencies : " + rejectedLinksAssessedElementContingency, true));
        }
    }
}
