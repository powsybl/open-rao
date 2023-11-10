/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.rao_result_json.deserializers.StateDeserializer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class StateDeserializerTest {
    @Test
    void testGetState() {
        Crac crac = Mockito.mock(Crac.class);
        State preventiveState = Mockito.mock(State.class);
        State curativeState = Mockito.mock(State.class);
        State outageState = Mockito.mock(State.class);
        String contingencyId = "contingency";
        Mockito.when(crac.getPreventiveState()).thenReturn(preventiveState);
        Mockito.when(crac.getState(contingencyId, "curative")).thenReturn(curativeState);
        Mockito.when(crac.getState(contingencyId, "outage")).thenReturn(outageState);
        Instant prevInstant = Mockito.mock(Instant.class);
        Mockito.when(prevInstant.getInstantKind()).thenReturn(InstantKind.PREVENTIVE);
        Mockito.when(crac.getInstant("preventive")).thenReturn(prevInstant);
        Instant outageInstant = Mockito.mock(Instant.class);
        Mockito.when(outageInstant.getInstantKind()).thenReturn(InstantKind.OUTAGE);
        Mockito.when(crac.getInstant("outage")).thenReturn(outageInstant);
        Instant curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.getInstantKind()).thenReturn(InstantKind.CURATIVE);
        Mockito.when(crac.getInstant("curative")).thenReturn(curativeInstant);

        FaraoException exception = assertThrows(FaraoException.class, () -> StateDeserializer.getState(null, contingencyId, crac, "type"));
        assertEquals("Cannot deserialize RaoResult: no instant defined in activated states of type", exception.getMessage());
        assertEquals(preventiveState, StateDeserializer.getState("preventive", null, crac, null));
        exception = assertThrows(FaraoException.class, () -> StateDeserializer.getState("outage", null, crac, "type"));
        assertEquals("Cannot deserialize RaoResult: no contingency defined in N-k activated states of type", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> StateDeserializer.getState("outage", "wrongContingencyId", crac, "type"));
        assertEquals("Cannot deserialize RaoResult: State at instant outage with contingency wrongContingencyId not found in Crac", exception.getMessage());
        assertEquals(outageState, StateDeserializer.getState("outage", contingencyId, crac, "type"));
        assertEquals(curativeState, StateDeserializer.getState("curative", contingencyId, crac, "type"));
    }
}
