package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

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
import com.powsybl.triplestore.api.PropertyBag;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class NetworkActionCreator {
    private final Crac crac;
    private final Network network;

    public NetworkActionCreator(Crac crac, Network network) {
        this.crac = crac;
        this.network = network;
    }

    public NetworkActionAdder getNetworkActionAdder(Map<String, Set<PropertyBag>> linkedTopologyActions, Map<String, Set<PropertyBag>> linkedRotatingMachineActions, Map<String, Set<PropertyBag>> linkedStaticPropertyRanges, String remedialActionId) {
        NetworkActionAdder networkActionAdder = crac.newNetworkAction().withId(remedialActionId);
        if (linkedTopologyActions.containsKey(remedialActionId)) {
            for (PropertyBag topologyActionPropertyBag : linkedTopologyActions.get(remedialActionId)) {
                addTopologicalElementaryAction(networkActionAdder, topologyActionPropertyBag, remedialActionId);
            }
        }

        if (linkedRotatingMachineActions.containsKey(remedialActionId) && linkedStaticPropertyRanges.containsKey(remedialActionId)) {
            for (PropertyBag rotatingMachineActionPropertyBag : linkedRotatingMachineActions.get(remedialActionId)) {
                addInjectionSetPointElementaryAction(linkedStaticPropertyRanges, remedialActionId, networkActionAdder, rotatingMachineActionPropertyBag);
            }
        }
        return networkActionAdder;
    }

    private void addInjectionSetPointElementaryAction(Map<String, Set<PropertyBag>> linkedStaticPropertyRanges, String remedialActionId, NetworkActionAdder networkActionAdder, PropertyBag rotatingMachineActionPropertyBag) {
        CsaProfileCracUtils.checkNormalEnabled(rotatingMachineActionPropertyBag, remedialActionId, "RotatingMachineAction");
        CsaProfileCracUtils.checkPropertyReference(rotatingMachineActionPropertyBag, remedialActionId, "RotatingMachineAction", CsaProfileConstants.PROPERTY_REFERENCE_ROTATING_MACHINE);
        String rawId = rotatingMachineActionPropertyBag.get(CsaProfileConstants.ROTATING_MACHINE);
        String rotatingMachineId = rawId.substring(rawId.lastIndexOf("_") + 1);
        Optional<Generator> optionalGenerator = network.getGeneratorStream().filter(gen -> gen.getId().equals(rotatingMachineId)).findAny();
        Optional<Load> optionalLoad = findLoad(rotatingMachineId);
        if (optionalGenerator.isEmpty() && optionalLoad.isEmpty()) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial Action: " + remedialActionId + " will not be imported because Network model does not contain a generator, neither a load with id of RotatingMachine: " + rotatingMachineId);
        }

        linkedStaticPropertyRanges.get(remedialActionId).stream().findAny().ifPresent(staticPropertyRangePropertyBag -> {
            CsaProfileCracUtils.checkPropertyReference(staticPropertyRangePropertyBag, remedialActionId, "StaticPropertyRange", CsaProfileConstants.PROPERTY_REFERENCE_ROTATING_MACHINE);
            float normalValue = Float.parseFloat(staticPropertyRangePropertyBag.get(CsaProfileConstants.NORMAL_VALUE));
            String valueKind = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_VALUE_KIND);
            String direction = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_DIRECTION);
            if (!(valueKind.equals(CsaProfileConstants.VALUE_KIND_ABSOLUTE) && direction.equals(CsaProfileConstants.DIRECTION_NONE))) {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial Action: " + remedialActionId + " will not be imported because StaticPropertyRange has wrong values of valueKind and direction, the only allowed combination is absolute + none");
            }
            networkActionAdder.newInjectionSetPoint()
                    .withSetpoint(normalValue)
                    .withNetworkElement(rotatingMachineId)
                    .add();
        });
    }

    private Optional<Load> findLoad(String rotatingMachineId) {
        return network.getLoadStream().filter(load -> load.getId().equals(rotatingMachineId)).findAny();
    }

    private void addTopologicalElementaryAction(NetworkActionAdder networkActionAdder, PropertyBag
            topologyActionPropertyBag, String remedialActionId) {
        String switchId = topologyActionPropertyBag.getId(CsaProfileConstants.SWITCH);
        if (network.getSwitch(switchId) == null) {
            throw new FaraoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial Action: " + remedialActionId + " will not be imported because network model does not contain a switch with id: " + switchId);
        }
        CsaProfileCracUtils.checkPropertyReference(topologyActionPropertyBag, remedialActionId, "TopologyAction", CsaProfileConstants.PROPERTY_REFERENCE_SWITCH_OPEN);
        networkActionAdder.newTopologicalAction()
                .withNetworkElement(switchId)
                // todo this is a temporary behaviour closing switch will be implemented in a later version
                .withActionType(ActionType.OPEN).add();
    }
}
