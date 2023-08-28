/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import java.util.List;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public final class CsaProfileConstants {

    private CsaProfileConstants() {
    }

    /**
     * constants to read rdf files
     */

    public static final String EXTENSION_FILE_CSA_PROFILE = "zip";

    public static final String RDF_BASE_URL = "http://entsoe.eu";

    public static final String TRIPLESTORE_RDF4J_NAME = "rdf4j";

    /**
     * constants to access triplestore data
     */

    public static final String SPARQL_FILE_CSA_PROFILE = "csa_profile.sparql";

    /**
     * constants to request file headers
     */

    public static final String REQUEST_HEADER_START_DATE = "startDate";

    public static final String REQUEST_HEADER_END_DATE = "endDate";

    public static final String REQUEST_HEADER_KEYWORD = "keyword";

    /**
     * requests for contingencies
     */

    public static final String REQUEST_CONTINGENCY = "contingency";

    public static final String REQUEST_ORDINARY_CONTINGENCY = "ordinaryContingency";

    public static final String REQUEST_EXCEPTIONAL_CONTINGENCY = "exceptionalContingency";

    public static final String REQUEST_OUT_OF_RANGE_CONTINGENCY = "outOfRangeContingency";

    public static final String REQUEST_CONTINGENCY_EQUIPMENT = "contingencyEquipment";

    public static final String REQUEST_CONTINGENCIES_NAME = "name";

    public static final String REQUEST_CONTINGENCIES_EQUIPMENT_OPERATOR = "idEquipmentOperator";

    public static final String REQUEST_CONTINGENCIES_EQUIPMENT_ID = "contingencyEquipmentId";

    public static final String REQUEST_CONTINGENCIES_MUST_STUDY = "normalMustStudy";

    public static final String REQUEST_CONTINGENCIES_CONTINGENT_STATUS = "contingentStatus";

    public static final String IMPORTED_CONTINGENT_STATUS = "http://iec.ch/TC57/CIM100#ContingencyEquipmentStatusKind.outOfService";

    public static final String CONTINGENCY_FILE_KEYWORD = "CO";

    /**
     * remedial actions
     */

    public static final String REMEDIAL_ACTION_NAME = "name";
    public static final String REMEDIAL_ACTION_FILE_KEYWORD = "RA";
    public static final String GRID_STATE_ALTERATION_REMEDIAL_ACTION = "gridStateAlterationRemedialAction";
    public static final String TOPOLOGY_ACTION = "topologyAction";
    public static final String ROTATING_MACHINE_ACTION = "rotatingMachineAction";
    public static final String TAP_POSITION_ACTION = "tapPositionAction";
    public static final String STATIC_PROPERTY_RANGE = "staticPropertyRange";
    public static final String CONTINGENCY_WITH_REMEDIAL_ACTION = "contingencyWithRemedialAction";
    public static final String MRID = "mRID";
    public static final String TIME_TO_IMPLEMENT = "timeToImplement";
    public static final String TSO = "tso";
    public static final String NORMAL_AVAILABLE = "normalAvailable";
    public static final String RA_KIND = "kind";
    public static final String COMBINATION_CONSTRAINT_KIND = "combinationConstraintKind";
    public static final String SWITCH = "switchId";
    public static final String NORMAL_ENABLED = "normalEnabled";
    public static final String GRID_ALTERATION_PROPERTY_REFERENCE = "propertyReference";
    public static final String PROPERTY_REFERENCE_SWITCH_OPEN = "http://energy.referencedata.eu/PropertyReference/Switch.open";
    public static final String PROPERTY_REFERENCE_ROTATING_MACHINE = "http://energy.referencedata.eu/PropertyReference/RotatingMachine.p";
    public static final String PROPERTY_REFERENCE_TAP_POSITION = "http://energy.referencedata.eu/PropertyReference/TapChanger.step";

    public static final String ROTATING_MACHINE = "rotatingMachineId";

    public static final String TAP_CHANGER = "tapChangerId";

    public static final String NORMAL_VALUE = "normalValue";

    public static final String STATIC_PROPERTY_RANGE_VALUE_KIND = "valueKind";
    public static final String STATIC_PROPERTY_RANGE_DIRECTION = "direction";
    public static final String DIRECTION_NONE = "http://entsoe.eu/ns/nc#RelativeDirectionKind.none";
    public static final String DIRECTION_DOWN = "http://entsoe.eu/ns/nc#RelativeDirectionKind.down";
    public static final String DIRECTION_UP = "http://entsoe.eu/ns/nc#RelativeDirectionKind.up";
    public static final String VALUE_KIND_ABSOLUTE = "http://entsoe.eu/ns/nc#ValueOffsetKind.absolute";

    public enum RemedialActionKind {
        CURATIVE("curative"),
        PREVENTIVE("preventive");

        RemedialActionKind(String name) {
            this.name = name;
        }

        private final String name;
        private static final String URL = ENTSOE_NS_NC_URL + "#RemedialActionKind.";

        @Override
        public String toString() {
            return RemedialActionKind.URL + this.name;
        }
    }

    /**
     * requests for flow cnec
     */

    public static final String REQUEST_ASSESSED_ELEMENT = "assessedElement";

    public static final String REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY = "assessedElementWithContingency";

    public static final String REQUEST_ASSESSED_ELEMENT_IN_BASE_CASE = "inBaseCase";

    public static final String REQUEST_ASSESSED_ELEMENT_NAME = "name";

    public static final String REQUEST_ASSESSED_ELEMENT_OPERATOR = "assessedSystemOperator";

    public static final String REQUEST_ASSESSED_ELEMENT_OPERATIONAL_LIMIT = "operationalLimit";

    public static final String REQUEST_ASSESSED_ELEMENT_IS_CRITICAL = "isCritical";

    public static final String REQUEST_ASSESSED_ELEMENT_NORMAL_ENABLED = "normalEnabled";

    public static final String REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_CONTINGENCY = "isCombinableWithContingency";

    public static final String REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY_COMBINATION_CONSTRAINT_KIND = "combinationConstraintKind";

    public static final String REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY_NORMAL_ENABLED = "normalEnabled";

    public static final String REQUEST_CURRENT_LIMIT = "currentLimit";

    public static final String REQUEST_CURRENT_LIMIT_NORMAL_VALUE = "normalValue";

    public static final String REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_SET = "operationalLimitSet";

    public static final String REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_TYPE = "operationalLimitType";

    public static final String REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_TERMINAL = "terminal";

    public static final String REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_KIND = "kind";

    public static final String REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_DIRECTION = "direction";

    public static final String REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_ACCEPTABLE_DURATION = "acceptableDuration";

    public static final String ASSESSED_ELEMENT_FILE_KEYWORD = "AE";

    public static final String ENTSOE_NS_NC_URL = "http://entsoe.eu/ns/nc";

    public static final List<String> CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_LEFT = List.of("CGMES.Terminal1", "CGMES.Terminal_Boundary_1");

    public static final List<String> CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_RIGHT = List.of("CGMES.Terminal2", "CGMES.Terminal_Boundary_2");

    public static final String IEC_URL = "http://iec.ch/TC57/";

    public enum ElementCombinationConstraintKind {
        CONSIDERED("considered"),
        INCLUDED("included"),
        EXCLUDED("excluded");

        ElementCombinationConstraintKind(String name) {
            this.name = name;
        }

        private final String name;
        private static final String URL = ENTSOE_NS_NC_URL + "#ElementCombinationConstraintKind.";

        @Override
        public String toString() {
            return ElementCombinationConstraintKind.URL + this.name;
        }
    }

    public enum LimitKind {
        PATL("patl"),
        TATL("tatl");

        LimitKind(String name) {
            this.name = name;
        }

        private final String name;
        private static final String URL = IEC_URL + "CIM100-European#LimitKind.";

        @Override
        public String toString() {
            return LimitKind.URL + this.name;
        }
    }

    public enum LimitDirectionKind {
        ABSOLUTE("absoluteValue"),
        HIGH("high"),
        LOW("low");

        LimitDirectionKind(String direction) {
            this.direction = direction;
        }

        private final String direction;
        private static final String URL = IEC_URL + "CIM100#OperationalLimitDirectionKind.";

        @Override
        public String toString() {
            return LimitDirectionKind.URL + this.direction;
        }
    }
}
