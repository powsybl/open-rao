package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.powsybl.sensitivity.SensitivityComputationResults;

public final class LinearRaoOptimizedSituation extends LinearRaoSituation {

    boolean initial;

    SensitivityComputationResults sensitivityComputationResults;
    double cost;
    String situationId;

    ComputationStatus lpStatus;

    LinearRaoOptimizedSituation(Crac crac) {super(crac);}

    void evaluateCost() {
    }

    boolean compareRaResults(LinearRaoSituation otherLinearRaoSituation) {
        return true;
    }

    void runLp(LinearRaoProblem linearRaoProblem) {

    }

    void completeResults(Crac crac) {
    }

    void delete() {
    }

}
