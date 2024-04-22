package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec.nc;

import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.triplestore.api.PropertyBag;

public record AssessedElement(String identifier, boolean inBaseCase, String name, String operator, String conductingEquipment, String operationalLimit, boolean isCombinableWithContingency, boolean isCombinableWithRemedialAction, boolean normalEnabled, String securedForRegion, String scannedForRegion) {

    public String getUniqueName() {
        return CsaProfileCracUtils.createElementName(name, operator).orElse(identifier);
    }

    public static AssessedElement fromPropertyBag(PropertyBag propertyBag) {
        return new AssessedElement(
            propertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT),
            Boolean.parseBoolean(propertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IN_BASE_CASE)),
            propertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_NAME),
            propertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATOR),
            propertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_CONDUCTING_EQUIPMENT),
            propertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATIONAL_LIMIT),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_CONTINGENCY, "false")),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_REMEDIAL_ACTION, "false")),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_NORMAL_ENABLED, "true")),
            propertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_SECURED_FOR_REGION),
            propertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_SCANNED_FOR_REGION)
        );
    }
}
