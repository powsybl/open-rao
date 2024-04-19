/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.*;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.*;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCnecCreator {
    private final Crac crac;
    private final Network network;
    private final PropertyBags assessedElementsPropertyBags;
    private final Map<String, Set<PropertyBag>> assessedElementsWithContingenciesPropertyBags;
    private final Map<String, Set<PropertyBag>> currentLimitsPropertyBags;
    private final Map<String, Set<PropertyBag>> voltageLimitsPropertyBags;
    private final Map<String, Set<PropertyBag>> angleLimitsPropertyBags;
    private Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts;
    private final CsaProfileCracCreationContext cracCreationContext;
    private final Set<Side> defaultMonitoredSides;
    private final String regionEic;

    public CsaProfileCnecCreator(Crac crac, Network network, PropertyBags assessedElementsPropertyBags, PropertyBags assessedElementsWithContingenciesPropertyBags, PropertyBags currentLimitsPropertyBags, PropertyBags voltageLimitsPropertyBags, PropertyBags angleLimitsPropertyBags, CsaProfileCracCreationContext cracCreationContext, Set<Side> defaultMonitoredSides, String regionEic) {
        this.crac = crac;
        this.network = network;
        this.assessedElementsPropertyBags = assessedElementsPropertyBags;
        this.assessedElementsWithContingenciesPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(assessedElementsWithContingenciesPropertyBags, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT);
        this.currentLimitsPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(currentLimitsPropertyBags, CsaProfileConstants.REQUEST_CURRENT_LIMIT);
        this.voltageLimitsPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(voltageLimitsPropertyBags, CsaProfileConstants.REQUEST_VOLTAGE_LIMIT);
        this.angleLimitsPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(angleLimitsPropertyBags, CsaProfileConstants.REQUEST_VOLTAGE_ANGLE_LIMIT);
        this.cracCreationContext = cracCreationContext;
        this.defaultMonitoredSides = defaultMonitoredSides;
        this.regionEic = regionEic;
        this.createAndAddCnecs();
    }

    private void createAndAddCnecs() {
        csaProfileCnecCreationContexts = new HashSet<>();

        for (PropertyBag assessedElementPropertyBag : assessedElementsPropertyBags) {
            String assessedElementId = assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT);
            try {
                addCnec(assessedElementPropertyBag);
            } catch (OpenRaoImportException exception) {
                csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, exception.getImportStatus(), exception.getMessage()));
            }
        }
        cracCreationContext.setCnecCreationContexts(csaProfileCnecCreationContexts);
    }

    private void addCnec(PropertyBag assessedElementPropertyBag) {
        String rejectedLinksAssessedElementContingency = "";
        String assessedElementId = assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT);
        checkNormalEnabled(assessedElementPropertyBag);

        String inBaseCaseStr = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IN_BASE_CASE);
        boolean inBaseCase = Boolean.parseBoolean(inBaseCaseStr);

        Set<PropertyBag> assessedElementsWithContingencies = this.assessedElementsWithContingenciesPropertyBags.get(assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT));

        String isCombinableWithContingencyStr = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_CONTINGENCY);
        boolean isCombinableWithContingency = Boolean.parseBoolean(isCombinableWithContingencyStr);

        if (!inBaseCase && !isCombinableWithContingency && assessedElementsWithContingencies == null) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement %s ignored because the assessed element is not in base case and not combinable with contingencies, but no explicit link to a contingency was found".formatted(assessedElementId));
        }

        Set<Contingency> combinableContingencies = isCombinableWithContingency ? cracCreationContext.getCrac().getContingencies() : new HashSet<>();

        String nativeAssessedElementName = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_NAME);
        String assessedSystemOperator = assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATOR);

        if (assessedElementsWithContingencies != null) {
            for (PropertyBag assessedElementWithContingencies : assessedElementsWithContingencies) {
                if (!checkAndProcessCombinableContingencyFromExplicitAssociation(assessedElementId, assessedElementWithContingencies, combinableContingencies)) {
                    rejectedLinksAssessedElementContingency = rejectedLinksAssessedElementContingency.concat(assessedElementWithContingencies.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY) + " ");
                }
            }
        }

        // We check whether the AssessedElement is defined using an OperationalLimit
        CsaProfileConstants.LimitType limitType = getLimit(assessedElementPropertyBag);
        String conductingEquipment = assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_CONDUCTING_EQUIPMENT);

        checkAeScannedSecuredCoherence(assessedElementId, assessedElementPropertyBag);

        boolean aeSecuredForRegion = isAeSecuredForRegion(assessedElementPropertyBag);
        boolean aeScannedForRegion = isAeScannedForRegion(assessedElementPropertyBag);

        // If not, we check if it is defined with a ConductingEquipment instead, otherwise we ignore
        if (limitType == null) {
            new FlowCnecCreator(crac, network, assessedElementId, nativeAssessedElementName, assessedSystemOperator, inBaseCase, null, conductingEquipment, combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, defaultMonitoredSides, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion).addFlowCnecs();
            return;
        }

        if (CsaProfileConstants.LimitType.CURRENT.equals(limitType)) {
            new FlowCnecCreator(crac, network, assessedElementId, nativeAssessedElementName, assessedSystemOperator, inBaseCase, getOperationalLimitPropertyBag(currentLimitsPropertyBags, assessedElementPropertyBag), conductingEquipment, combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, defaultMonitoredSides, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion).addFlowCnecs();
        } else if (CsaProfileConstants.LimitType.VOLTAGE.equals(limitType)) {
            new VoltageCnecCreator(crac, network, assessedElementId, nativeAssessedElementName, assessedSystemOperator, inBaseCase, getOperationalLimitPropertyBag(voltageLimitsPropertyBags, assessedElementPropertyBag), combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion).addVoltageCnecs();
        } else {
            new AngleCnecCreator(crac, network, assessedElementId, nativeAssessedElementName, assessedSystemOperator, inBaseCase, getOperationalLimitPropertyBag(angleLimitsPropertyBags, assessedElementPropertyBag), combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion).addAngleCnecs();
        }
    }

    private void checkAeScannedSecuredCoherence(String assessedElementId, PropertyBag assessedElementPropertyBag) {
        String rawIdSecured = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_SECURED_FOR_REGION);
        String rawIdScanned = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_SCANNED_FOR_REGION);
        if (rawIdSecured != null && rawIdSecured.equals(rawIdScanned)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement " + assessedElementId + " ignored because an AssessedElement cannot be optimized and monitored at the same time");
        }
    }

    private boolean isAeSecuredForRegion(PropertyBag assessedElementPropertyBag) {
        return isAeSecuredOrScannedForRegion(assessedElementPropertyBag, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_SECURED_FOR_REGION);
    }

    private boolean isAeScannedForRegion(PropertyBag assessedElementPropertyBag) {
        return isAeSecuredOrScannedForRegion(assessedElementPropertyBag, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_SCANNED_FOR_REGION);
    }

    private boolean isAeSecuredOrScannedForRegion(PropertyBag assessedElementPropertyBag, String propertyName) {
        String rawRegionId = assessedElementPropertyBag.get(propertyName);
        String region = rawRegionId == null ? null : CsaProfileCracUtils.getEicFromUrl(assessedElementPropertyBag.get(propertyName));
        return region != null && region.equals(regionEic);
    }

    private PropertyBag getOperationalLimitPropertyBag(Map<String, Set<PropertyBag>> operationalLimitPropertyBags, PropertyBag assessedElementPropertyBag) {
        return operationalLimitPropertyBags.get(assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATIONAL_LIMIT)).stream().toList().get(0);
    }

    private void checkNormalEnabled(PropertyBag assessedElementPropertyBag) {
        String normalEnabled = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_NORMAL_ENABLED);
        if (normalEnabled != null && !Boolean.parseBoolean(normalEnabled)) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, "AssessedElement %s ignored because it is not enabled".formatted(assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT)));
        }
    }

    private CsaProfileConstants.LimitType getLimit(PropertyBag assessedElementPropertyBag) {
        if (checkLimit(this.currentLimitsPropertyBags, "current", assessedElementPropertyBag)) {
            return CsaProfileConstants.LimitType.CURRENT;
        }
        if (checkLimit(this.voltageLimitsPropertyBags, "voltage", assessedElementPropertyBag)) {
            return CsaProfileConstants.LimitType.VOLTAGE;
        }
        if (checkLimit(this.angleLimitsPropertyBags, "angle", assessedElementPropertyBag)) {
            return CsaProfileConstants.LimitType.ANGLE;
        }

        return null;
    }

    private boolean checkLimit(Map<String, Set<PropertyBag>> limitPropertyBags, String limitType, PropertyBag assessedElementPropertyBag) {
        Set<PropertyBag> limits = limitPropertyBags.get(assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATIONAL_LIMIT));
        if (limits != null) {
            if (limits.size() != 1) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement %s ignored because more than one %s limit linked with the assessed element".formatted(assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT), limitType));
            }
            return true;
        }
        return false;
    }

    private boolean checkAndProcessCombinableContingencyFromExplicitAssociation(String assessedElementId, PropertyBag assessedElementWithContingencies, Set<Contingency> combinableContingenciesSet) {
        String normalEnabledWithContingency = assessedElementWithContingencies.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY_NORMAL_ENABLED);
        String contingencyId = assessedElementWithContingencies.getId(CsaProfileConstants.REQUEST_CONTINGENCY);
        Contingency contingencyToLink = crac.getContingency(contingencyId);

        // Unknown contingency
        if (contingencyToLink == null) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "The contingency " + contingencyId + " linked to the assessed element does not exist in the CRAC"));
            return false;
        }

        // Illegal element combination constraint kind
        if (!CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString().equals(assessedElementWithContingencies.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY_COMBINATION_CONSTRAINT_KIND))) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "The contingency " + contingencyId + " is linked to the assessed element with an illegal elementCombinationConstraint kind"));
            combinableContingenciesSet.remove(contingencyToLink);
            return false;
        }

        // Disabled link to contingency
        if (normalEnabledWithContingency != null && !Boolean.parseBoolean(normalEnabledWithContingency)) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, "The link between contingency " + contingencyId + " and the assessed element is disabled"));
            combinableContingenciesSet.remove(contingencyToLink);
            return false;
        }

        combinableContingenciesSet.add(contingencyToLink);
        return true;
    }
}
