/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnecAdder;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.constants.OperationalLimitDirectionKind;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElement;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.VoltageAngleLimit;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;

import java.util.List;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AngleCnecCreator extends AbstractCnecCreator {

    private final VoltageAngleLimit nativeVoltageAngleLimit;

    public AngleCnecCreator(Crac crac, Network network, AssessedElement nativeAssessedElement, VoltageAngleLimit nativeVoltageAngleLimit, List<Contingency> linkedContingencies, Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts, CsaProfileCracCreationContext cracCreationContext, String rejectedLinksAssessedElementContingency, boolean aeSecuredForRegion, boolean aeScannedForRegion) {
        super(crac, network, nativeAssessedElement, linkedContingencies, csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion);
        this.nativeVoltageAngleLimit = nativeVoltageAngleLimit;
    }

    public void addAngleCnecs() {
        if (nativeAssessedElement.inBaseCase()) {
            addAngleCnec(crac.getPreventiveInstant().getId(), null);
        }
        for (Contingency contingency : linkedContingencies) {
            addAngleCnec(crac.getInstant(InstantKind.CURATIVE).getId(), contingency);
        }
    }

    private void addAngleCnec(String instantId, Contingency contingency) {
        AngleCnecAdder angleCnecAdder = initAngleCnec();
        addAngleLimit(angleCnecAdder);
        addCnecBaseInformation(angleCnecAdder, contingency, instantId);
        angleCnecAdder.add();
        markCnecAsImportedAndHandleRejectedContingencies(getCnecName(instantId, contingency));
    }

    private AngleCnecAdder initAngleCnec() {
        return crac.newAngleCnec();
    }

    private void addAngleLimit(AngleCnecAdder angleCnecAdder) {
        String networkElement1Id = checkAngleNetworkElementAndGetId(nativeVoltageAngleLimit.terminal1());
        String networkElement2Id = checkAngleNetworkElementAndGetId(nativeVoltageAngleLimit.terminal2());

        addAngleCnecElements(angleCnecAdder, networkElement1Id, networkElement2Id);
        addAngleLimitThreshold(angleCnecAdder);
    }

    private String checkAngleNetworkElementAndGetId(String terminalId) {
        Identifiable<?> networkElement = this.getNetworkElementInNetwork(terminalId);
        if (networkElement == null) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, writeAssessedElementIgnoredReasonMessage("the angle limit equipment " + terminalId + " is missing in network"));
        }
        if (!networkElement.getType().equals(IdentifiableType.BUS)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("the network element " + networkElement.getId() + " is not a bus bar section"));
        }
        return networkElement.getId();
    }

    private void addAngleLimitThreshold(AngleCnecAdder angleCnecAdder) {
        if (nativeVoltageAngleLimit.normalValue() < 0) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("the angle limit's normal value is negative"));
        }
        if (OperationalLimitDirectionKind.HIGH.toString().equals(nativeVoltageAngleLimit.direction())) {
            handleMissingIsFlowToRefTerminalForNotAbsoluteDirection(OperationalLimitDirectionKind.HIGH);
            angleCnecAdder.newThreshold()
                .withUnit(Unit.DEGREE)
                .withMax(nativeVoltageAngleLimit.normalValue()).add();
        } else if (OperationalLimitDirectionKind.LOW.toString().equals(nativeVoltageAngleLimit.direction())) {
            handleMissingIsFlowToRefTerminalForNotAbsoluteDirection(OperationalLimitDirectionKind.LOW);
            angleCnecAdder.newThreshold()
                .withUnit(Unit.DEGREE)
                .withMin(-nativeVoltageAngleLimit.normalValue()).add();
        } else if (OperationalLimitDirectionKind.ABSOLUTE.toString().equals(nativeVoltageAngleLimit.direction())) {
            angleCnecAdder.newThreshold()
                .withUnit(Unit.DEGREE)
                .withMin(-nativeVoltageAngleLimit.normalValue())
                .withMax(nativeVoltageAngleLimit.normalValue()).add();
        }
    }

    private void handleMissingIsFlowToRefTerminalForNotAbsoluteDirection(OperationalLimitDirectionKind direction) {
        if (nativeVoltageAngleLimit.isFlowToRefTerminal() == null) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("of an ambiguous angle limit direction definition from an undefined VoltageAngleLimit.isFlowToRefTerminal and an OperationalLimit.OperationalLimitType: " + direction));
        }
    }

    private void addAngleCnecElements(AngleCnecAdder angleCnecAdder, String networkElement1Id, String networkElement2Id) {
        if (networkElement1Id.equals(networkElement2Id)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("AngleCNEC's importing and exporting equipments are the same: " + networkElement1Id));
        }
        String importingElement = nativeVoltageAngleLimit.isFlowToRefTerminal() == null || nativeVoltageAngleLimit.isFlowToRefTerminal() ? networkElement1Id : networkElement2Id;
        String exportingElement = nativeVoltageAngleLimit.isFlowToRefTerminal() == null || nativeVoltageAngleLimit.isFlowToRefTerminal() ? networkElement2Id : networkElement1Id;
        angleCnecAdder.withImportingNetworkElement(importingElement).withExportingNetworkElement(exportingElement);
    }
}
