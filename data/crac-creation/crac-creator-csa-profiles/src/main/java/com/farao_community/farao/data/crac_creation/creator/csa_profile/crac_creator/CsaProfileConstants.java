/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

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
     * constants to request contingencies
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

}
