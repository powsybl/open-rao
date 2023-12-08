/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreator;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec.CsaProfileCnecCreator;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.contingency.CsaProfileContingencyCreator;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action.CsaProfileRemedialActionsCreator;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action.OnConstraintUsageRuleHelper;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBags;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
@AutoService(CracCreator.class)
public class CsaProfileCracCreator implements CracCreator<CsaProfileCrac, CsaProfileCracCreationContext> {

    private Crac crac;
    private Network network;
    CsaProfileCracCreationContext creationContext;

    @Override
    public String getNativeCracFormat() {
        return "CsaProfileCrac";
    }

    public CsaProfileCracCreationContext createCrac(CsaProfileCrac nativeCrac, Network network, OffsetDateTime offsetDateTime, CracCreationParameters cracCreationParameters) {
        this.crac = cracCreationParameters.getCracFactory().create(nativeCrac.toString());
        this.network = network;
        this.creationContext = new CsaProfileCracCreationContext(crac, offsetDateTime, network.getNameOrId());

        Map<String, String> overridingData = nativeCrac.getOverridingCracData(offsetDateTime);
        PropertyBags contingencies = CsaProfileCracUtils.overrideData(nativeCrac.getContingencies(), overridingData, CsaProfileConstants.OverridingObjectsFields.CONTINGENCY);
        PropertyBags assessedElements = CsaProfileCracUtils.overrideData(nativeCrac.getAssessedElements(), overridingData, CsaProfileConstants.OverridingObjectsFields.ASSESSED_ELEMENT);
        PropertyBags assessedElementsWithContingencies = CsaProfileCracUtils.overrideData(nativeCrac.getAssessedElementsWithContingencies(), overridingData, CsaProfileConstants.OverridingObjectsFields.ASSESSED_ELEMENT_WITH_CONTINGENCY);
        PropertyBags currentLimits = CsaProfileCracUtils.overrideData(nativeCrac.getCurrentLimits(), overridingData, CsaProfileConstants.OverridingObjectsFields.CURRENT_LIMIT);
        PropertyBags voltageLimits = CsaProfileCracUtils.overrideData(nativeCrac.getVoltageLimits(), overridingData, CsaProfileConstants.OverridingObjectsFields.VOLTAGE_LIMIT);
        PropertyBags angleLimits = CsaProfileCracUtils.overrideData(nativeCrac.getAngleLimits(), overridingData, CsaProfileConstants.OverridingObjectsFields.VOLTAGE_ANGLE_LIMIT);
        PropertyBags assessedElementsWithRemedialAction = CsaProfileCracUtils.overrideData(nativeCrac.getAssessedElementsWithRemedialAction(), overridingData, CsaProfileConstants.OverridingObjectsFields.ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION);
        PropertyBags contingenciesWithRemedialAction = CsaProfileCracUtils.overrideData(nativeCrac.getContingencyWithRemedialAction(), overridingData, CsaProfileConstants.OverridingObjectsFields.CONTINGENCY_WITH_REMEDIAL_ACTION);
        PropertyBags remedialActions = CsaProfileCracUtils.overrideData(nativeCrac.getRemedialActions(), overridingData, CsaProfileConstants.OverridingObjectsFields.REMEDIAL_ACTION);
        PropertyBags remedialActionSchemes = CsaProfileCracUtils.overrideData(nativeCrac.getRemedialActionScheme(), overridingData, CsaProfileConstants.OverridingObjectsFields.REMEDIAL_ACTION_SCHEME);
        PropertyBags gridStateAlterations = CsaProfileCracUtils.overrideData(nativeCrac.getGridStateAlterationCollection(), overridingData, CsaProfileConstants.OverridingObjectsFields.GRID_STATE_ALTERATION);
        PropertyBags staticPropertyRanges = CsaProfileCracUtils.overrideData(nativeCrac.getStaticPropertyRanges(), overridingData, CsaProfileConstants.OverridingObjectsFields.RANGE_CONSTRAINT);

        createContingencies(contingencies, nativeCrac.getContingencyEquipments());
        createCnecs(assessedElements, assessedElementsWithContingencies, currentLimits, voltageLimits, angleLimits, cracCreationParameters.getDefaultMonitoredSides());
        OnConstraintUsageRuleHelper onConstraintUsageRuleAdder = new OnConstraintUsageRuleHelper(creationContext.getCnecCreationContexts(), assessedElements, assessedElementsWithRemedialAction);
        createRemedialActions(remedialActions, nativeCrac.getTopologyAction(), nativeCrac.getRotatingMachineAction(), nativeCrac.getShuntCompensatorModifications(), nativeCrac.getTapPositionAction(), staticPropertyRanges, contingenciesWithRemedialAction, onConstraintUsageRuleAdder,
            nativeCrac.getRemedialActionsSchedule(), nativeCrac.getSchemeRemedialActions(), remedialActionSchemes, nativeCrac.getStage(), gridStateAlterations, nativeCrac.getTopologyActionAuto(), nativeCrac.getRotatingMachineActionAuto(), nativeCrac.getShuntCompensatorModificationAuto(),
            nativeCrac.getTapPositionActionAuto());
        creationContext.buildCreationReport();
        return creationContext.creationSuccess(crac);
    }

    private void createRemedialActions(PropertyBags remedialActionsPropertyBags, PropertyBags topologyActionsPropertyBags, PropertyBags rotatingMachineActionPropertyBags, PropertyBags shuntCompensatorModificationPropertyBags, PropertyBags tapPositionPropertyBags, PropertyBags staticPropertyRanges, PropertyBags contingencyWithRemedialActionsPropertyBags, OnConstraintUsageRuleHelper onConstraintUsageRuleAdder,
                                       PropertyBags remedialActionsSchedulePropertyBags, PropertyBags schemeRemedialActionsPropertyBags, PropertyBags remedialActionSchemePropertyBags, PropertyBags stagePropertyBags, PropertyBags gridStateAlterationCollectionPropertyBags, PropertyBags topologyActionAuto, PropertyBags rotatingMachineActionAuto, PropertyBags shuntCompensatorModificationAuto,
                                       PropertyBags tapPositionActionAuto) {
        new CsaProfileRemedialActionsCreator(crac, network, creationContext, remedialActionsPropertyBags, contingencyWithRemedialActionsPropertyBags, topologyActionsPropertyBags, rotatingMachineActionPropertyBags, shuntCompensatorModificationPropertyBags, tapPositionPropertyBags, staticPropertyRanges, onConstraintUsageRuleAdder,
            remedialActionsSchedulePropertyBags, schemeRemedialActionsPropertyBags, remedialActionSchemePropertyBags, stagePropertyBags, gridStateAlterationCollectionPropertyBags, topologyActionAuto, rotatingMachineActionAuto, shuntCompensatorModificationAuto, tapPositionActionAuto);
    }

    private void createContingencies(PropertyBags contingenciesPropertyBags, PropertyBags contingencyEquipmentsPropertyBags) {
        new CsaProfileContingencyCreator(crac, network, contingenciesPropertyBags, contingencyEquipmentsPropertyBags, creationContext);
    }

    private void createCnecs(PropertyBags assessedElementsPropertyBags, PropertyBags assessedElementsWithContingenciesPropertyBags, PropertyBags currentLimitsPropertyBags, PropertyBags voltageLimitsPropertyBags, PropertyBags angleLimitsPropertyBags, Set<Side> defaultMonitoredSides) {
        new CsaProfileCnecCreator(crac, network, assessedElementsPropertyBags, assessedElementsWithContingenciesPropertyBags, currentLimitsPropertyBags, voltageLimitsPropertyBags, angleLimitsPropertyBags, creationContext, defaultMonitoredSides);
    }
}
