/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.commons.TsoEICode;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.util.FaraoImportException;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public final class CsaProfileCracUtils {

    private CsaProfileCracUtils() {

    }

    public static Map<String, UsageMethod> getConstraintToUsageMethodMap() {
        Map<String, UsageMethod> constraintToUsageMethodMap = new HashMap<>();
        constraintToUsageMethodMap.put(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString(), UsageMethod.FORCED);
        constraintToUsageMethodMap.put(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString(), UsageMethod.AVAILABLE);
        constraintToUsageMethodMap.put(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString(), UsageMethod.UNAVAILABLE);
        return constraintToUsageMethodMap;
    }

    public static Map<String, Set<PropertyBag>> getMappedPropertyBagsSet(PropertyBags propertyBags, String property) {
        Map<String, Set<PropertyBag>> mappedPropertyBags = new HashMap<>();
        for (PropertyBag propertyBag : propertyBags) {
            String propValue = propertyBag.getId(property);
            Set<PropertyBag> propPropertyBags = mappedPropertyBags.computeIfAbsent(propValue, k -> new HashSet<>());
            propPropertyBags.add(propertyBag);
        }
        return mappedPropertyBags;
    }

    public static String getUniqueName(String prefixUrl, String suffix) {
        return TsoEICode.fromEICode(prefixUrl.substring(prefixUrl.lastIndexOf('/') + 1)).getDisplayName().concat("_").concat(suffix);
    }

    public static Optional<String> createRemedialActionName(String nativeRemedialActionName, String tsoNameUrl) {
        if (nativeRemedialActionName != null) {
            if (tsoNameUrl != null) {
                return Optional.of(getUniqueName(tsoNameUrl, nativeRemedialActionName));
            }
            return Optional.of(nativeRemedialActionName);
        } else {
            return Optional.empty();
        }
    }

    public static boolean isValidInterval(OffsetDateTime dateTime, String startTime, String endTime) {
        OffsetDateTime startDateTime = OffsetDateTime.parse(startTime);
        OffsetDateTime endDateTime = OffsetDateTime.parse(endTime);
        return !dateTime.isBefore(startDateTime) && !dateTime.isAfter(endDateTime);
    }

    public static int convertDurationToSeconds(String duration) {
        Map<Character, Integer> durationFactors = new HashMap<>();
        durationFactors.put('D', 86400);
        durationFactors.put('H', 3600);
        durationFactors.put('M', 60);
        durationFactors.put('S', 1);

        Pattern pattern = Pattern.compile("P(?:\\d+D)?(?:T(?:\\d+H)?(?:\\d+M)?(?:\\d+S)?)?");
        Matcher matcher = pattern.matcher(duration);

        if (!matcher.matches()) {
            throw new RuntimeException("Error occurred while converting time to implement to seconds, unknown pattern: " + duration);
        }

        int seconds = 0;

        for (Map.Entry<Character, Integer> entry : durationFactors.entrySet()) {
            Pattern unitPattern = Pattern.compile("(\\d+)" + entry.getKey());
            Matcher unitMatcher = unitPattern.matcher(duration);

            if (unitMatcher.find()) {
                int value = Integer.parseInt(unitMatcher.group(1));
                seconds += value * entry.getValue();
            }
        }

        return seconds;
    }

    public static void checkPropertyReference(PropertyBag propertyBag, String remedialActionId, String propertyBagKind, String expectedPropertyReference) {
        String actualPropertyReference = propertyBag.get(CsaProfileConstants.GRID_ALTERATION_PROPERTY_REFERENCE);
        if (!actualPropertyReference.equals(expectedPropertyReference)) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action '%s' will not be imported because '%s' must have a property reference with '%s' value, but it was: '%s'", remedialActionId, propertyBagKind, expectedPropertyReference, actualPropertyReference));
        }
    }

    public static void checkNormalEnabled(PropertyBag propertyBag, String remedialActionId, String propertyBagKind) {
        Optional<String> normalEnabledOpt = Optional.ofNullable(propertyBag.get(CsaProfileConstants.NORMAL_ENABLED));
        if (normalEnabledOpt.isPresent() && !Boolean.parseBoolean(normalEnabledOpt.get())) {
            throw new FaraoImportException(ImportStatus.NOT_FOR_RAO, String.format("Remedial action '%s' will not be imported because field 'normalEnabled' in '%s' must be true or empty", remedialActionId, propertyBagKind));
        }
    }

    public static CsaProfileConstants.HeaderValidity checkProfileHeader(PropertyBag propertyBag, CsaProfileConstants.CsaProfile csaProfileKeyword, OffsetDateTime importTimestamp) {
        if (!checkProfileKeyword(propertyBag, csaProfileKeyword)) {
            return CsaProfileConstants.HeaderValidity.INVALID_KEYWORD;
        }
        if (!checkProfileValidityInterval(propertyBag, importTimestamp)) {
            return CsaProfileConstants.HeaderValidity.INVALID_INTERVAL;
        }
        return CsaProfileConstants.HeaderValidity.OK;
    }

    private static boolean checkProfileValidityInterval(PropertyBag propertyBag, OffsetDateTime importTimestamp) {
        String startTime = propertyBag.get(CsaProfileConstants.REQUEST_HEADER_START_DATE);
        String endTime = propertyBag.get(CsaProfileConstants.REQUEST_HEADER_END_DATE);
        return isValidInterval(importTimestamp, startTime, endTime);
    }

    private static boolean checkProfileKeyword(PropertyBag propertyBag, CsaProfileConstants.CsaProfile csaProfileKeyword) {
        String keyword = propertyBag.get(CsaProfileConstants.REQUEST_HEADER_KEYWORD);
        return csaProfileKeyword.getKeyword().equals(keyword);
    }

}

