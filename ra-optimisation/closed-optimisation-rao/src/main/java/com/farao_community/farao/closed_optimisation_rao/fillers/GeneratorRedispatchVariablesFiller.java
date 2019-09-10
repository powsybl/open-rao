/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.AbstractOptimisationProblemFiller;
import com.farao_community.farao.data.crac_file.*;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.*;
import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoUtil.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class GeneratorRedispatchVariablesFiller extends AbstractOptimisationProblemFiller {

    private Map<Optional<Contingency>, List<RemedialAction>> redispatchingRemedialActions;
    private List<String> generatorList;

    @Override
    public void initFiller(Network network, CracFile cracFile, Map<String, Object> data) {
        super.initFiller(network, cracFile, data);
        this.redispatchingRemedialActions = buildRedispatchRemedialActionMap(cracFile);
        this.generatorList = buildRedispatchGeneratorList(cracFile);
    }

    @Override
    public void fillProblem(MPSolver solver) {
        redispatchingRemedialActions.forEach((contingency, raList)  -> {
            raList.forEach(ra -> {
                RedispatchRemedialActionElement rrae = Objects.requireNonNull(getRedispatchElement(ra));
                double pmin = rrae.getMinimumPower();
                double pmax = rrae.getMaximumPower();
                double pinit = rrae.getTargetPower();
                solver.makeNumVar(pmin - pinit, pmax - pinit, nameRedispatchValueVariable(contingency, ra));
            });
        });
    }

    @Override
    public List<String> variablesProvided() {
        List<String> variables = new ArrayList<>();
        redispatchingRemedialActions.forEach((contingency, raList) -> {
            variables.addAll(raList.stream()
                    .map(ra -> nameRedispatchValueVariable(contingency, ra))
                    .collect(Collectors.toList()));
        });
        return variables;
    }
}
