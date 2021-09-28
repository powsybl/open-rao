/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFunction;
import com.powsybl.sensitivity.SensitivityVariable;
import com.powsybl.sensitivity.factors.*;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.functions.BranchIntensity;
import com.powsybl.sensitivity.factors.variables.HvdcSetpointIncrease;
import com.powsybl.sensitivity.factors.variables.InjectionIncrease;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;

import java.util.*;
import java.util.stream.Collectors;

/**
 * To run a systematic sensitivity analysis and evaluate the flows in all states at once,
 * hades requires sensitivities. We therefore extend RangeActionSensitivityProvider to use
 * some of its conversion methods, and use a random PST from the network to create "dummy"
 * sensitivities for each studied cnec.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class LoadflowProvider extends AbstractSimpleSensitivityProvider {

    LoadflowProvider(Set<FlowCnec> cnecs, Set<Unit> units) {
        super(cnecs, units);
    }

    @Override
    public List<SensitivityFactor> getCommonFactors(Network network) {
        return new ArrayList<>();
    }

    @Override
    public List<SensitivityFactor> getAdditionalFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();
        if (afterContingencyOnly) {
            return factors;
        }
        SensitivityVariable defaultSensitivityVariable = defaultSensitivityVariable(network);
        getSensitivityFunctions(network, null).forEach(fun -> factors.add(sensitivityFactorMapping(fun, defaultSensitivityVariable)));
        return factors;
    }

    @Override
    public List<SensitivityFactor> getAdditionalFactors(Network network, String contingencyId) {
        List<SensitivityFactor> factors = new ArrayList<>();
        SensitivityVariable defaultSensitivityVariable = defaultSensitivityVariable(network);
        getSensitivityFunctions(network, contingencyId).forEach(fun -> factors.add(sensitivityFactorMapping(fun, defaultSensitivityVariable)));
        return factors;
    }

    private boolean willBeKeptInSensi(TwoWindingsTransformer twoWindingsTransformer) {
        return twoWindingsTransformer.getTerminal1().isConnected() && twoWindingsTransformer.getTerminal1().getBusBreakerView().getBus().isInMainSynchronousComponent() &&
            twoWindingsTransformer.getTerminal2().isConnected() && twoWindingsTransformer.getTerminal2().getBusBreakerView().getBus().isInMainSynchronousComponent() &&
            twoWindingsTransformer.getPhaseTapChanger() != null;
    }

    private boolean willBeKeptInSensi(Generator gen) {
        return gen.getTerminal().isConnected() && gen.getTerminal().getBusBreakerView().getBus().isInMainSynchronousComponent();
    }

    SensitivityVariable defaultSensitivityVariable(Network network) {
        // First try to get a PST angle
        Optional<TwoWindingsTransformer> optionalPst = network.getTwoWindingsTransformerStream()
            .filter(this::willBeKeptInSensi)
            .findAny();

        if (optionalPst.isPresent()) {
            TwoWindingsTransformer pst = optionalPst.get();
            return new PhaseTapChangerAngle(pst.getId(), pst.getNameOrId(), pst.getId());
        }

        // If no one found, pick a Generator injection
        Optional<Generator> optionalGen = network.getGeneratorStream()
            .filter(this::willBeKeptInSensi)
            .findAny();

        if (optionalGen.isPresent()) {
            Generator gen = optionalGen.get();
            return new InjectionIncrease(gen.getId(), gen.getNameOrId(), gen.getId());
        }
        throw new FaraoException(String.format("Unable to create sensitivity factors. Did not find any varying element in network '%s'.", network.getId()));
    }

    SensitivityFactor sensitivityFactorMapping(SensitivityFunction function, SensitivityVariable variable) {
        if (function instanceof BranchFlow) {
            if (variable instanceof PhaseTapChangerAngle) {
                return new BranchFlowPerPSTAngle((BranchFlow) function, (PhaseTapChangerAngle) variable);
            } else if (variable instanceof InjectionIncrease) {
                return new BranchFlowPerInjectionIncrease((BranchFlow) function, (InjectionIncrease) variable);
            } else if (variable instanceof HvdcSetpointIncrease) {
                return new BranchFlowPerHvdcSetpointIncrease((BranchFlow) function, (HvdcSetpointIncrease) variable);
            } else {
                throw new FaraoException(faraoExceptionSensitivityString(function, variable));
            }
        } else if (function instanceof BranchIntensity) {
            if (variable instanceof PhaseTapChangerAngle) {
                return new BranchIntensityPerPSTAngle((BranchIntensity) function, (PhaseTapChangerAngle) variable);
            } else if (variable instanceof HvdcSetpointIncrease) {
                return new BranchIntensityPerHvdcSetpointIncrease((BranchIntensity) function, (HvdcSetpointIncrease) variable);
            } else {
                throw new FaraoException(faraoExceptionSensitivityString(function, variable));
            }
        } else {
            throw new FaraoException(faraoExceptionSensitivityString(function, variable));
        }
    }

    private String  faraoExceptionSensitivityString(SensitivityFunction function, SensitivityVariable variable) {
        return "Unable to create sensitivity factor for function of type " + function.getClass().getTypeName() + " and variable of type " + variable.getClass().getTypeName();
    }

    List<SensitivityFunction> getSensitivityFunctions(Network network, String contingencyId) {
        Set<NetworkElement> networkElements;
        if (Objects.isNull(contingencyId)) {
            networkElements = cnecs.stream()
                .filter(cnec -> cnec.getState().getContingency().isEmpty())
                .map(Cnec::getNetworkElement)
                .collect(Collectors.toSet());
        } else {
            networkElements = cnecs.stream()
                .filter(cnec -> cnec.getState().getContingency().isPresent() && cnec.getState().getContingency().get().getId().equals(contingencyId))
                .map(Cnec::getNetworkElement)
                .collect(Collectors.toSet());
        }
        return networkElements.stream()
            .map(networkElement -> cnecToSensitivityFunctions(network, networkElement))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private List<SensitivityFunction> cnecToSensitivityFunctions(Network network, NetworkElement networkElement) {
        String id = networkElement.getId();
        String name = networkElement.getName();
        Identifiable<?> networkIdentifiable = network.getIdentifiable(id);

        if (networkIdentifiable instanceof Branch) {
            List<SensitivityFunction> sensitivityFunctions = new ArrayList<>();
            if (factorsInMegawatt) {
                sensitivityFunctions.add(new BranchFlow(id, name, id));
            }
            if (factorsInAmpere) {
                sensitivityFunctions.add(new BranchIntensity(id, name, id));
            }
            return sensitivityFunctions;
        } else {
            throw new FaraoException("Unable to create sensitivity function for " + id);
        }
    }

}
