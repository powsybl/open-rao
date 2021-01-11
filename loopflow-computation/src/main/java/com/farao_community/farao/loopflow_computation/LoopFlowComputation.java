/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;

import com.farao_community.farao.data.refprog.reference_program.ReferenceExchangeData;

import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgramArea;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.farao_community.farao.util.EICode;
import com.powsybl.iidm.network.Network;

import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowComputation {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoopFlowComputation.class);

    private ZonalData<LinearGlsk> glsk;
    private ReferenceProgram referenceProgram;

    public LoopFlowComputation(ZonalData<LinearGlsk> glsk, ReferenceProgram referenceProgram) {
        this.glsk = requireNonNull(glsk, "glskProvider should not be null");
        this.referenceProgram = requireNonNull(referenceProgram, "referenceProgram should not be null");
    }

    public LoopFlowResult calculateLoopFlows(Network network, SensitivityAnalysisParameters sensitivityAnalysisParameters, Set<BranchCnec> cnecs) {
        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder()
            .withDefaultParameters(sensitivityAnalysisParameters)
            .withPtdfSensitivities(glsk, cnecs, Collections.singleton(Unit.MEGAWATT))
            .build();

        SystematicSensitivityResult ptdfsAndRefFlows = systematicSensitivityInterface.run(network);

        return buildLoopFlowsFromReferenceFlowAndPtdf(network, ptdfsAndRefFlows, cnecs);
    }

    public LoopFlowResult buildLoopFlowsFromReferenceFlowAndPtdf(Network network, SystematicSensitivityResult alreadyCalculatedPtdfAndFlows, Set<BranchCnec> cnecs) {

        List<LinearGlsk> glsks = getValidGlsks(network);
        LoopFlowResult results = new LoopFlowResult();

        for (BranchCnec cnec : cnecs) {
            double refFlow = alreadyCalculatedPtdfAndFlows.getReferenceFlow(cnec);
            double commercialFLow = glsks.stream()
                .mapToDouble(glskElement -> alreadyCalculatedPtdfAndFlows.getSensitivityOnFlow(glskElement, cnec) * referenceProgram.getGlobalNetPosition(gslkToReferenceProgramArea(glskElement)))
                .sum();
            results.addCnecResult(cnec, refFlow - commercialFLow, commercialFLow, refFlow);
        }
        return results;
    }

    private ReferenceProgramArea gslkToReferenceProgramArea(LinearGlsk glsk) {
        try {
            EICode eiCode = new EICode(glsk.getId().substring(0, EICode.LENGTH));
            return new ReferenceProgramArea(eiCode.getCountry());
        } catch (IllegalArgumentException e) {
            return new ReferenceProgramArea(glsk.getName());
        }
    }

    private List<LinearGlsk> getValidGlsks(Network network) {
        List<LinearGlsk> linearGlsksFromRealCountry = getRealReferenceProgramAreasGlsks();
        List<LinearGlsk> linearGlsksFromVirtualHubs = getVirtualHubGlsks(network);
        return Stream.concat(linearGlsksFromRealCountry.stream(), linearGlsksFromVirtualHubs.stream()).collect(Collectors.toList());
    }

    private List<LinearGlsk> getVirtualHubGlsks(Network network) {
        List<LinearGlsk> virtualHubGlsks = new ArrayList<>();
        // Extract from the referenceExchangeDataList the ones that are described in the virtualhubs
        List<ReferenceExchangeData> referenceExchangesFromVirtualHubs = referenceProgram.getReferenceExchangeDataList().stream().filter(ReferenceExchangeData::involvesVirtualHub).collect(Collectors.toList());
        referenceExchangesFromVirtualHubs.forEach(referenceExchangeData -> virtualHubGlsks.add(getVirtualHubGlsk(network, referenceExchangeData)));
        return new ArrayList<>();
        // return virtualHubGlsks;
    }

    private LinearGlsk getVirtualHubGlsk(Network network, ReferenceExchangeData referenceExchangeData) {
        return null; //TODO: implement this functio
    }

    private List<LinearGlsk> getRealReferenceProgramAreasGlsks() {
        return glsk.getDataPerZone().values().stream().filter(linearGlsk -> {
            ReferenceProgramArea referenceProgramArea = gslkToReferenceProgramArea(linearGlsk);
            if (!referenceProgram.getListOfAreas().contains(referenceProgramArea)) {
                LOGGER.warn(String.format("Glsk [%s] is ignored as no corresponding referenceProgramArea was found in the ReferenceProgram", linearGlsk.getId()));
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

}


