/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json;

import com.powsybl.openrao.commons.OpenRaoException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.powsybl.openrao.data.cracio.json.JsonSchemaProvider.validateJsonCrac;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class JsonSchemaProviderTest {
    @Test
    void missingVersion() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> validateJsonCrac("/whatever/the/file/is", 0, 0));
        assertEquals("No JSON Schema found for CRAC v0.0.", exception.getMessage());
    }

    @Test
    void validateCrac1Point6() throws IOException {
        assertTrue(validateJsonCrac("/retrocompatibility/v1/crac-v1.6.json", 1, 6));
    }

    @Test
    void validateCrac1Point7() throws IOException {
        assertTrue(validateJsonCrac("/retrocompatibility/v1/crac-v1.7.json", 1, 7));
    }

    @Test
    void validateCrac1Point8() throws IOException {
        assertTrue(validateJsonCrac("/retrocompatibility/v1/crac-v1.8.json", 1, 8));
    }

    @Test
    void validateCrac1Point9() throws IOException {
        assertTrue(validateJsonCrac("/retrocompatibility/v1/crac-v1.9.json", 1, 9));
    }

    @Test
    void validateCrac2Point0() throws IOException {
        assertTrue(validateJsonCrac("/retrocompatibility/v2/crac-v2.0.json", 2, 0));
    }

    @Test
    void validateCrac2Point1() throws IOException {
        assertTrue(validateJsonCrac("/retrocompatibility/v2/crac-v2.1.json", 2, 1));
    }

    @Test
    void validateCrac2Point2() throws IOException {
        assertTrue(validateJsonCrac("/retrocompatibility/v2/crac-v2.2.json", 2, 2));
    }

    @Test
    void validateCrac2Point3() throws IOException {
        assertTrue(validateJsonCrac("/retrocompatibility/v2/crac-v2.3.json", 2, 3));
    }

    @Test
    void validateCrac2Point4() throws IOException {
        assertTrue(validateJsonCrac("/retrocompatibility/v2/crac-v2.4.json", 2, 4));
    }

    @Test
    void validateCrac2Point5() throws IOException {
        assertTrue(validateJsonCrac("/retrocompatibility/v2/crac-v2.5.json", 2, 5));
    }
}
