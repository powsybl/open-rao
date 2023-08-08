/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.commons.TsoEICode;
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

    public static Map<String, ArrayList<PropertyBag>> getMappedPropertyBags(PropertyBags propertyBags, String property) {
        Map<String, ArrayList<PropertyBag>> mappedPropertyBags = new HashMap<>();
        for (PropertyBag propertyBag : propertyBags) {
            String propValue = propertyBag.getId(property);
            ArrayList<PropertyBag> propPropertyBags = mappedPropertyBags.get(propValue);
            if (propPropertyBags == null) {
                propPropertyBags = new ArrayList<>();
                mappedPropertyBags.put(propValue, propPropertyBags);
            }
            propPropertyBags.add(propertyBag);
        }
        return mappedPropertyBags;
    }

    public static Map<String, Set<PropertyBag>> getMappedPropertyBagsSet(PropertyBags propertyBags, String property) {
        Map<String, Set<PropertyBag>> mappedPropertyBags = new HashMap<>();
        for (PropertyBag propertyBag : propertyBags) {
            String propValue = propertyBag.getId(property);
            Set<PropertyBag> propPropertyBags = mappedPropertyBags.get(propValue);
            if (propPropertyBags == null) {
                propPropertyBags = new HashSet<>();
                mappedPropertyBags.put(propValue, propPropertyBags);
            }
            propPropertyBags.add(propertyBag);
        }
        return mappedPropertyBags;
    }

    public static String getUniqueName(String idWithEicCode, String elementId) {
        return TsoEICode.fromEICode(idWithEicCode.substring(idWithEicCode.lastIndexOf('/') + 1)).getDisplayName().concat("_").concat(elementId);
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

        Pattern pattern = Pattern.compile("P(?:\\d+D)?T(\\d+H)?(\\d+M)?(\\d+S)?");
        Matcher matcher = pattern.matcher(duration);

        if (!matcher.matches()) {
            return -1;
        }

        int seconds = 0;

        for (int i = 1; i <= 3; i++) {
            String group = matcher.group(i);
            if (group != null) {
                seconds += Integer.parseInt(group, 0, group.length() - 1, 10) * durationFactors.get(group.charAt(group.length() - 1));
            }
        }
        return seconds;
    }
}
