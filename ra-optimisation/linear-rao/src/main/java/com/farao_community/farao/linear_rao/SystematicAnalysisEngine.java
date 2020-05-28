package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Unit;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.util.SensitivityComputationException;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A computation engine dedicated to the systematic sensitivity analyses performed
 * in the scope of the LinearRao.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class SystematicAnalysisEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicAnalysisEngine.class);

    /**
     * LinearRao configurations, containing the default and fallback configurations
     * of the sensitivity computation
     */
    private LinearRaoParameters linearRaoParameters;

    /**
     * A boolean indicating whether or not the fallback mode of the sensitivity computation
     * engine is active.
     */
    private boolean fallbackMode;

    /**
     * Computation Manager
     */
    private ComputationManager computationManager;

    private boolean runLoopflow;
    private boolean loopflowViolation;

    /**
     * Constructor
     */
    SystematicAnalysisEngine(LinearRaoParameters linearRaoParameters, ComputationManager computationManager) {
        this.linearRaoParameters = linearRaoParameters;
        this.computationManager = computationManager;
        this.fallbackMode = false;
        this.runLoopflow = linearRaoParameters.getExtendable().isRaoWithLoopFlowLimitation();
        this.loopflowViolation = false;
    }

    boolean isFallback() {
        return fallbackMode;
    }

    /**
     * Run the systematic sensitivity analysis on one Situation, and evaluate the value of the
     * objective function on this Situation.
     *
     * Throw a SensitivityComputationException if the computation fails.
     */
    void run(LinearRaoData linearRaoData) {

        SensitivityComputationParameters sensiConfig = fallbackMode ? linearRaoParameters.getFallbackSensiParameters() : linearRaoParameters.getSensitivityComputationParameters();

        try {
            runWithConfig(linearRaoData, sensiConfig);
        } catch (SensitivityComputationException e) {
            if (!fallbackMode && linearRaoParameters.getFallbackSensiParameters() != null) { // default mode fails, retry in fallback mode
                LOGGER.warn("Error while running the sensitivity computation with default parameters, fallback sensitivity parameters are now used.");
                fallbackMode = true;
                run(linearRaoData);
            } else if (!fallbackMode) { // no fallback mode available, throw an exception
                throw new SensitivityComputationException("Sensitivity computation failed with default parameters. No fallback parameters available.", e);
            } else { // fallback mode fails, throw an exception
                throw new SensitivityComputationException("Sensitivity computation failed with all available sensitivity parameters.", e);
            }
        }
    }

    /**
     * Run the systematic sensitivity analysis with given SensitivityComputationParameters, throw a
     * SensitivityComputationException is the computation fails.
     */
    private void runWithConfig(LinearRaoData linearRaoData, SensitivityComputationParameters sensitivityComputationParameters) {

        try {
            SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult = SystematicSensitivityAnalysisService
                .runAnalysis(linearRaoData.getNetwork(), linearRaoData.getCrac(), computationManager, sensitivityComputationParameters);

            if (!systematicSensitivityAnalysisResult.isSuccess()) {
                throw new SensitivityComputationException("Some output data of the sensitivity computation are missing.");
            }

            Map<String, Double> loopflows = new HashMap<>();
            if (this.runLoopflow) {
                loopflows = computeLoopflowAndCheckLoopflowConstraint(linearRaoData);
            }

            setResults(linearRaoData, systematicSensitivityAnalysisResult, loopflows);

        } catch (Exception e) {
            throw new SensitivityComputationException("Sensitivity computation fails.", e);
        }
    }

    private Map<String, Double> computeLoopflowAndCheckLoopflowConstraint(LinearRaoData linearRaoData) {
        Map<String, Double> loopflows;
        if (!Objects.isNull(linearRaoParameters.getExtendable()) && linearRaoParameters.getExtendable().isLoopflowApproximation()) { //re-compute ptdf
            loopflows = new LoopFlowComputation(linearRaoData.getCrac()).calculateLoopFlowsApproximation(linearRaoData.getNetwork());
        } else {
            loopflows = new LoopFlowComputation(linearRaoData.getCrac()).calculateLoopFlows(linearRaoData.getNetwork());
        }
        for (Cnec cnec : linearRaoData.getCrac().getCnecs(linearRaoData.getCrac().getPreventiveState())) {
            if (!Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class))
                    && Math.abs(loopflows.get(cnec.getId())) > Math.abs(cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraint())) {
                setLoopflowViolation(true);
                LOGGER.warn("Loopflow constraint violation.");
                break;
            }
        }
        return loopflows;
    }

    /**
     * add results of the systematic analysis (flows and objective function value) in the
     * Crac result variant of the situation.
     */
    private void setResults(LinearRaoData linearRaoData, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult, Map<String, Double> loopflows) {
        linearRaoData.setSystematicSensitivityAnalysisResult(systematicSensitivityAnalysisResult);
        if (isLoopflowViolation()) {
//            linearRaoData.getCracResult().setCost(Double.POSITIVE_INFINITY); //todo: set a high cost if loopflow constraint violation => use "virtual cost" in the future
        }
        linearRaoData.getCracResult().setCost(-getMinMargin(linearRaoData, systematicSensitivityAnalysisResult));
        updateCnecExtensions(linearRaoData, systematicSensitivityAnalysisResult, loopflows);
    }

    /**
     * Compute the objective function, the minimal margin.
     */
    private double getMinMargin(LinearRaoData linearRaoData, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {

        double minMargin = Double.POSITIVE_INFINITY;
        for (Cnec cnec : linearRaoData.getCrac().getCnecs()) {
            double flow = systematicSensitivityAnalysisResult.getReferenceFlow(cnec);
            double margin = cnec.computeMargin(flow, Unit.MEGAWATT);
            if (Double.isNaN(margin)) {
                throw new SensitivityComputationException(String.format("Cnec %s is not present in the sensitivity analysis results. Bad behaviour.", cnec.getId()));
            }
            minMargin = Math.min(minMargin, margin);
        }
        return minMargin;
    }

    private void updateCnecExtensions(LinearRaoData linearRaoData, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult, Map<String, Double> loopflows) {
        linearRaoData.getCrac().getCnecs().forEach(cnec -> {
            CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(linearRaoData.getWorkingVariantId());
            cnecResult.setFlowInMW(systematicSensitivityAnalysisResult.getReferenceFlow(cnec));
            cnecResult.setFlowInA(systematicSensitivityAnalysisResult.getReferenceIntensity(cnec));
            cnecResult.setThresholds(cnec);
            if (!Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class)) && loopflows.containsKey(cnec.getId())) {
                cnecResult.setLoopflowInMW(loopflows.get(cnec.getId()));
                cnecResult.setLoopflowThresholdInMW(cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraint());
            }
        });
    }

    public boolean isLoopflowViolation() {
        return loopflowViolation;
    }

    public void setLoopflowViolation(boolean loopflowViolation) {
        this.loopflowViolation = loopflowViolation;
    }
}
