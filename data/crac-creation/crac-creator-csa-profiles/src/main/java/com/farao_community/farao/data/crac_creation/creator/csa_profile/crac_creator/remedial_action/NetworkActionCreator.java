/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils;
import com.farao_community.farao.data.crac_creation.util.FaraoImportException;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class NetworkActionCreator {
    private final Crac crac;
    private final Network network;

    public NetworkActionCreator(Crac crac, Network network) {
        this.crac = crac;
        this.network = network;
    }

    public NetworkActionAdder getNetworkActionAdder(Map<String, Set<PropertyBag>> linkedTopologyActions, Map<String, Set<PropertyBag>> linkedRotatingMachineActions, Map<String, Set<PropertyBag>> linkedShuntCompensatorModifications, Map<String, Set<PropertyBag>> staticPropertyRanges, String gridStateAlterationId, String targetRaId) {
        NetworkActionAdder networkActionAdder = crac.newNetworkAction().withId(targetRaId);
        if (linkedTopologyActions.containsKey(gridStateAlterationId)) {
            for (PropertyBag topologyActionPropertyBag : linkedTopologyActions.get(gridStateAlterationId)) {
                addTopologicalElementaryAction(networkActionAdder, topologyActionPropertyBag, gridStateAlterationId);
            }
        }

        if (linkedRotatingMachineActions.containsKey(gridStateAlterationId)) {
            for (PropertyBag rotatingMachineActionPropertyBag : linkedRotatingMachineActions.get(gridStateAlterationId)) {
                if (staticPropertyRanges.containsKey(rotatingMachineActionPropertyBag.getId(CsaProfileConstants.MRID))) {
                    addInjectionSetPointFromRotatingMachineAction(
                        staticPropertyRanges.get(rotatingMachineActionPropertyBag.getId(CsaProfileConstants.MRID)),
                        gridStateAlterationId, networkActionAdder, rotatingMachineActionPropertyBag);
                } else {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + targetRaId + " will not be imported because there is no StaticPropertyRange linked to that RA");
                }
            }
        }
        if (linkedShuntCompensatorModifications.containsKey(gridStateAlterationId)) {
            for (PropertyBag shuntCompensatorModificationPropertyBag : linkedShuntCompensatorModifications.get(gridStateAlterationId)) {
                if (staticPropertyRanges.containsKey(shuntCompensatorModificationPropertyBag.getId(CsaProfileConstants.MRID))) {
                    addInjectionSetPointFromShuntCompensatorModification(
                        staticPropertyRanges.get(shuntCompensatorModificationPropertyBag.getId(CsaProfileConstants.MRID)),
                        gridStateAlterationId, networkActionAdder, shuntCompensatorModificationPropertyBag);
                } else {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + targetRaId + " will not be imported because there is no StaticPropertyRange linked to that RA");
                }
            }
        }
        return networkActionAdder;
    }

    private void addInjectionSetPointFromRotatingMachineAction(Set<PropertyBag> staticPropertyRangesLinkedToRotatingMachineAction, String remedialActionId, NetworkActionAdder networkActionAdder, PropertyBag rotatingMachineActionPropertyBag) {
        CsaProfileCracUtils.checkNormalEnabled(rotatingMachineActionPropertyBag, remedialActionId, "RotatingMachineAction");
        CsaProfileCracUtils.checkPropertyReference(rotatingMachineActionPropertyBag, remedialActionId, "RotatingMachineAction", CsaProfileConstants.PropertyReference.ROTATING_MACHINE.toString());
        String rawId = rotatingMachineActionPropertyBag.get(CsaProfileConstants.ROTATING_MACHINE);
        String rotatingMachineId = rawId.substring(rawId.lastIndexOf("#_") + 2).replace("+", " ");
        Optional<Generator> optionalGenerator = network.getGeneratorStream().filter(gen -> gen.getId().equals(rotatingMachineId)).findAny();
        Optional<Load> optionalLoad = findLoad(rotatingMachineId);
        if (optionalGenerator.isEmpty() && optionalLoad.isEmpty()) {
            throw new FaraoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because Network model does not contain a generator, neither a load with id of RotatingMachine: " + rotatingMachineId);
        }

        PropertyBag staticPropertyRangePropertyBag = staticPropertyRangesLinkedToRotatingMachineAction.iterator().next(); // get a random one (in theory only one will be present in case of rotating machines)
        CsaProfileCracUtils.checkPropertyReference(staticPropertyRangePropertyBag, remedialActionId, "StaticPropertyRange", CsaProfileConstants.PropertyReference.ROTATING_MACHINE.toString());
        float normalValue;
        try {
            normalValue = Float.parseFloat(staticPropertyRangePropertyBag.get(CsaProfileConstants.NORMAL_VALUE));
        } catch (Exception e) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because StaticPropertyRange has a non float-castable normalValue so no set-point value was retrieved");
        }
        String valueKind = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_VALUE_KIND);
        String direction = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_DIRECTION);
        if (!(valueKind.equals(CsaProfileConstants.ValueOffsetKind.ABSOLUTE.toString()) && direction.equals(CsaProfileConstants.RelativeDirectionKind.NONE.toString()))) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because StaticPropertyRange has wrong values of valueKind and direction, the only allowed combination is absolute + none");
        }
        networkActionAdder.newInjectionSetPoint()
            .withSetpoint(normalValue)
            .withNetworkElement(rotatingMachineId)
            .withUnit(Unit.MEGAWATT)
            .add();
    }

    private void addInjectionSetPointFromShuntCompensatorModification(Set<PropertyBag> staticPropertyRangesLinkedToShuntCompensatorModification, String remedialActionId, NetworkActionAdder networkActionAdder, PropertyBag shuntCompensatorModificationPropertyBag) {
        CsaProfileCracUtils.checkNormalEnabled(shuntCompensatorModificationPropertyBag, remedialActionId, "ShuntCompensatorModification");
        CsaProfileCracUtils.checkPropertyReference(shuntCompensatorModificationPropertyBag, remedialActionId, "ShuntCompensatorModification", CsaProfileConstants.PropertyReference.SHUNT_COMPENSATOR.toString());
        String rawId = shuntCompensatorModificationPropertyBag.get(CsaProfileConstants.SHUNT_COMPENSATOR_ID);
        String shuntCompensatorId = rawId.substring(rawId.lastIndexOf("_") + 1);
        Optional<ShuntCompensator> optionalShuntCompensator = network.getShuntCompensatorStream().filter(sc -> sc.getId().equals(shuntCompensatorId)).findAny();
        if (optionalShuntCompensator.isEmpty()) {
            throw new FaraoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because Network model does not contain a shunt compensator with id of ShuntCompensator: " + shuntCompensatorId);
        }

        PropertyBag staticPropertyRangePropertyBag = staticPropertyRangesLinkedToShuntCompensatorModification.iterator().next(); // get a random one (in theory only one will be present in case of shunt compensators)
        CsaProfileCracUtils.checkPropertyReference(staticPropertyRangePropertyBag, remedialActionId, "StaticPropertyRange", CsaProfileConstants.PropertyReference.SHUNT_COMPENSATOR.toString());
        int normalValue;
        try {
            String normalValueStr = staticPropertyRangePropertyBag.get(CsaProfileConstants.NORMAL_VALUE);
            if (normalValueStr.endsWith(".0")) {
                normalValue = (int) Float.parseFloat(normalValueStr);
            } else {
                normalValue = Integer.parseInt(normalValueStr);
            }
        } catch (Exception e) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because StaticPropertyRange has a non integer-castable normalValue so no set-point value was retrieved");
        }
        if (normalValue < 0) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because StaticPropertyRange has a negative integer normalValue so no set-point value was retrieved");
        }
        String valueKind = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_VALUE_KIND);
        String direction = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_DIRECTION);
        if (!(valueKind.equals(CsaProfileConstants.ValueOffsetKind.ABSOLUTE.toString()) && direction.equals(CsaProfileConstants.RelativeDirectionKind.NONE.toString()))) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because StaticPropertyRange has wrong values of valueKind and direction, the only allowed combination is absolute + none");
        }
        networkActionAdder.newInjectionSetPoint()
            .withSetpoint(normalValue)
            .withNetworkElement(shuntCompensatorId)
            .withUnit(Unit.MEGAWATT)
            .add();
    }

    private Optional<Load> findLoad(String rotatingMachineId) {
        return network.getLoadStream().filter(load -> load.getId().equals(rotatingMachineId)).findAny();
    }

    private void addTopologicalElementaryAction(NetworkActionAdder networkActionAdder, PropertyBag
        topologyActionPropertyBag, String remedialActionId) {
        String switchId = topologyActionPropertyBag.getId(CsaProfileConstants.SWITCH);
        if (network.getSwitch(switchId) == null) {
            throw new FaraoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because network model does not contain a switch with id: " + switchId);
        }
        CsaProfileCracUtils.checkPropertyReference(topologyActionPropertyBag, remedialActionId, "TopologyAction", CsaProfileConstants.PropertyReference.SWITCH.toString());
        networkActionAdder.newTopologicalAction()
            .withNetworkElement(switchId)
            // todo this is a temporary behaviour closing switch will be implemented in a later version
            .withActionType(ActionType.OPEN).add();
    }
}
