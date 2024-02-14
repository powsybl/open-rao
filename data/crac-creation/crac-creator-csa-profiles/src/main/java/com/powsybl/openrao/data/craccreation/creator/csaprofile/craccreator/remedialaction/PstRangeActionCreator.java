/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.range.TapRangeAdder;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.openrao.data.craccreation.util.iidm.IidmPstHelper;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.*;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class PstRangeActionCreator {
    private final Crac crac;
    private final Network network;

    public PstRangeActionCreator(Crac crac, Network network) {
        this.crac = crac;
        this.network = network;
    }

    public PstRangeActionAdder getPstRangeActionAdder(Map<String, Set<PropertyBag>> linkedTapPositionActions, Map<String, Set<PropertyBag>> linkedStaticPropertyRanges, String remedialActionId, String elementaryActionsAggregatorId) {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction().withId(remedialActionId);
        if (linkedTapPositionActions.containsKey(elementaryActionsAggregatorId)) {
            for (PropertyBag tapPositionActionPropertyBag : linkedTapPositionActions.get(elementaryActionsAggregatorId)) {
                Set<PropertyBag> linkedStaticPropertyRangesToTapPositionAction = new HashSet<>();
                if (linkedStaticPropertyRanges.containsKey(tapPositionActionPropertyBag.getId(MRID))) {
                    linkedStaticPropertyRangesToTapPositionAction = linkedStaticPropertyRanges.get(tapPositionActionPropertyBag.getId(MRID));
                }
                addTapPositionElementaryAction(linkedStaticPropertyRangesToTapPositionAction, remedialActionId, elementaryActionsAggregatorId, pstRangeActionAdder, tapPositionActionPropertyBag);
            }
        }
        return pstRangeActionAdder;
    }

    private void addTapPositionElementaryAction(Set<PropertyBag> linkedStaticPropertyRangesToTapPositionAction, String remedialActionId, String elementaryActionsAggregatorId, PstRangeActionAdder pstRangeActionAdder, PropertyBag tapPositionActionPropertyBag) {
        CsaProfileCracUtils.checkNormalEnabled(tapPositionActionPropertyBag, remedialActionId, TAP_POSITION_ACTION);
        CsaProfileCracUtils.checkPropertyReference(tapPositionActionPropertyBag, remedialActionId, TAP_POSITION_ACTION, PropertyReference.TAP_CHANGER.toString());
        String rawId = tapPositionActionPropertyBag.get(TAP_CHANGER);
        String tapChangerId = rawId.substring(rawId.lastIndexOf("#_") + 2).replace("+", " ");
        IidmPstHelper iidmPstHelper = new IidmPstHelper(tapChangerId, network);
        if (!iidmPstHelper.isValid()) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action " + remedialActionId + " will not be imported because " + iidmPstHelper.getInvalidReason());
        }

        pstRangeActionAdder
            .withNetworkElement(tapChangerId)
            .withInitialTap(iidmPstHelper.getInitialTap())
            .withTapToAngleConversionMap(iidmPstHelper.getTapToAngleConversionMap());

        if (!linkedStaticPropertyRangesToTapPositionAction.isEmpty()) {
            Optional<Integer> normalValueUp = Optional.empty();
            Optional<Integer> normalValueDown = Optional.empty();
            for (PropertyBag staticPropertyRangePropertyBag : linkedStaticPropertyRangesToTapPositionAction) {
                CsaProfileCracUtils.checkPropertyReference(staticPropertyRangePropertyBag, remedialActionId, STATIC_PROPERTY_RANGE, PropertyReference.TAP_CHANGER.toString());
                String valueKind = staticPropertyRangePropertyBag.get(STATIC_PROPERTY_RANGE_VALUE_KIND);

                if (!valueKind.equals(ValueOffsetKind.ABSOLUTE.toString())) {
                    throw new OpenRaoImportException(ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, "Remedial action " + remedialActionId + " will not be imported because StaticPropertyRange has wrong value of valueKind, the only allowed value is 'absolute'");
                } else {
                    String direction = staticPropertyRangePropertyBag.get(STATIC_PROPERTY_RANGE_DIRECTION);
                    int normalValue = (int) Float.parseFloat(staticPropertyRangePropertyBag.get(NORMAL_VALUE));
                    if (direction.equals(RelativeDirectionKind.DOWN.toString())) {
                        normalValueDown = Optional.of(normalValue);
                    } else if (direction.equals(RelativeDirectionKind.UP.toString())) {
                        normalValueUp = Optional.of(normalValue);
                    } else {
                        throw new OpenRaoImportException(ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, "Remedial action " + remedialActionId + " will not be imported because StaticPropertyRange has wrong value of direction, the only allowed values are RelativeDirectionKind.up and RelativeDirectionKind.down");
                    }
                }
            }
            TapRangeAdder tapRangeAdder = pstRangeActionAdder.newTapRange().withRangeType(RangeType.ABSOLUTE);
            normalValueDown.ifPresent(tapRangeAdder::withMinTap);
            normalValueUp.ifPresent(tapRangeAdder::withMaxTap);
            tapRangeAdder.add();
        }
    }

}
