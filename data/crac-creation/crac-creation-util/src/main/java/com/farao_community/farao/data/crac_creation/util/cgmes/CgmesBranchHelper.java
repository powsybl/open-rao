/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.util.cgmes;

import com.farao_community.farao.data.crac_creation.util.ElementHelper;
import com.powsybl.iidm.network.*;

import java.util.Objects;

/**
 * @author Philippe Edwards{@literal <philippe.edwards at rte-france.com>}
 */
public class CgmesBranchHelper implements ElementHelper {

    private final String mrId;

    private Branch.Side tieLineSide = null;
    private boolean isHalfLine = false;

    private Branch<?> branch = null;

    /**
     * Constructor.
     *
     * @param mrId,                 CGMES-id of the branch
     * @param network,              powsybl iidm network object
     *
     */
    public CgmesBranchHelper(String mrId, Network network) {
        this.mrId = mrId;
        interpret(network);
    }

    public boolean isHalfLine() {
        return isHalfLine;
    }

    public Branch.Side getTieLineSide() {
        return tieLineSide;
    }

    public Branch<?> getBranch() {
        return branch;
    }

    protected void interpret(Network network) {
        branch = network.getBranch(mrId);
        if (Objects.isNull(branch)) {
            // check if it's a half line
            for (Line line : network.getLines()) {
                if (line.isTieLine()) {
                    TieLine tieLine = (TieLine) line;
                    if (tieLine.getHalf1().getId().equals(mrId)) {
                        isHalfLine = true;
                        tieLineSide = Branch.Side.ONE;
                        branch = line;
                        return;
                    } else if (tieLine.getHalf2().getId().equals(mrId)) {
                        isHalfLine = true;
                        tieLineSide = Branch.Side.TWO;
                        branch = line;
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean isValid() {
        return Objects.nonNull(branch);
    }

    @Override
    public String getInvalidReason() {
        return String.format("Branch with id %s was not found in network.", mrId);
    }

    @Override
    public String getIdInNetwork() {
        return branch.getId();
    }
}
