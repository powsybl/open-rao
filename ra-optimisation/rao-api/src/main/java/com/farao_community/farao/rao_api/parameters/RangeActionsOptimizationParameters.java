/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters;

import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * Range actions optimization parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 *
 */
public class RangeActionsOptimizationParameters {

    // Default values
    private static final int DEFAULT_MAX_MIP_ITERATIONS = 10;
    private static final double DEFAULT_PST_PENALTY_COST = 0.01;
    private static final double DEFAULT_PST_SENSITIVITY_THRESHOLD = 0.0;
    private static final PstModel DEFAULT_PST_MODEL = PstModel.CONTINUOUS;
    private static final double DEFAULT_HVDC_PENALTY_COST = 0.001;
    private static final double DEFAULT_HVDC_SENSITIVITY_THRESHOLD = 0.0;
    private static final double DEFAULT_INJECTION_RA_PENALTY_COST = 0.001;
    private static final double DEFAULT_INJECTION_RA_SENSITIVITY_THRESHOLD = 0.0;
    // Attributes
    private int maxMipIterations = DEFAULT_MAX_MIP_ITERATIONS;
    private double pstPenaltyCost = DEFAULT_PST_PENALTY_COST;
    private double pstSensitivityThreshold = DEFAULT_PST_SENSITIVITY_THRESHOLD;
    private PstModel pstModel = DEFAULT_PST_MODEL;
    private double hvdcPenaltyCost = DEFAULT_HVDC_PENALTY_COST;
    private double hvdcSensitivityThreshold = DEFAULT_HVDC_SENSITIVITY_THRESHOLD;
    private double injectionRaPenaltyCost = DEFAULT_INJECTION_RA_PENALTY_COST;
    private double injectionRaSensitivityThreshold = DEFAULT_INJECTION_RA_SENSITIVITY_THRESHOLD;
    private LinearOptimizationSolver linearOptimizationSolver = new LinearOptimizationSolver();

    // Enum
    public enum PstModel {
        CONTINUOUS,
        APPROXIMATED_INTEGERS
    }

    public static class LinearOptimizationSolver {
        private static final Solver DEFAULT_SOLVER = Solver.CBC;
        public static final double DEFAULT_RELATIVE_MIP_GAP = 0.0001;
        public static final String DEFAULT_SOLVER_SPECIFIC_PARAMETERS = null;
        private Solver solver = DEFAULT_SOLVER;
        private double relativeMipGap = DEFAULT_RELATIVE_MIP_GAP;
        private String solverSpecificParameters = DEFAULT_SOLVER_SPECIFIC_PARAMETERS;

        public Solver getSolver() {
            return solver;
        }

        public void setSolver(Solver solver) {
            this.solver = solver;
        }

        public double getRelativeMipGap() {
            return relativeMipGap;
        }

        public void setRelativeMipGap(double relativeMipGap) {
            this.relativeMipGap = relativeMipGap;
        }

        public String getSolverSpecificParameters() {
            return solverSpecificParameters;
        }

        public void setSolverSpecificParameters(String solverSpecificParameters) {
            this.solverSpecificParameters = solverSpecificParameters;
        }

        public static LinearOptimizationSolver load(PlatformConfig platformConfig) {
            Objects.requireNonNull(platformConfig);
            LinearOptimizationSolver parameters = new LinearOptimizationSolver();
            platformConfig.getOptionalModuleConfig(LINEAR_OPTIMIZATION_SOLVER_SECTION)
                    .ifPresent(config -> {
                        parameters.setSolver(config.getEnumProperty(SOLVER, Solver.class, DEFAULT_SOLVER));
                        parameters.setRelativeMipGap(config.getDoubleProperty(RELATIVE_MIP_GAP, DEFAULT_RELATIVE_MIP_GAP));
                        parameters.setSolverSpecificParameters(config.getStringProperty(SOLVER_SPECIFIC_PARAMETERS, DEFAULT_SOLVER_SPECIFIC_PARAMETERS));
                    });
            return parameters;
        }
    }

    public enum Solver {
        CBC,
        SCIP,
        XPRESS
    }

    // Getters and setters
    public int getMaxMipIterations() {
        return maxMipIterations;
    }

    public void setMaxMipIterations(int maxMipIterations) {
        this.maxMipIterations = maxMipIterations;
    }

    public double getPstPenaltyCost() {
        return pstPenaltyCost;
    }

    public void setPstPenaltyCost(double pstPenaltyCost) {
        this.pstPenaltyCost = pstPenaltyCost;
    }

    public double getPstSensitivityThreshold() {
        return pstSensitivityThreshold;
    }

    public void setPstSensitivityThreshold(double pstSensitivityThreshold) {
        this.pstSensitivityThreshold = pstSensitivityThreshold;
    }

    public double getHvdcPenaltyCost() {
        return hvdcPenaltyCost;
    }

    public void setHvdcPenaltyCost(double hvdcPenaltyCost) {
        this.hvdcPenaltyCost = hvdcPenaltyCost;
    }

    public double getHvdcSensitivityThreshold() {
        return hvdcSensitivityThreshold;
    }

    public void setHvdcSensitivityThreshold(double hvdcSensitivityThreshold) {
        this.hvdcSensitivityThreshold = hvdcSensitivityThreshold;
    }

    public double getInjectionRaPenaltyCost() {
        return injectionRaPenaltyCost;
    }

    public void setInjectionRaPenaltyCost(double injectionRaPenaltyCost) {
        this.injectionRaPenaltyCost = injectionRaPenaltyCost;
    }

    public double getInjectionRaSensitivityThreshold() {
        return injectionRaSensitivityThreshold;
    }

    public void setInjectionRaSensitivityThreshold(double injectionRaSensitivityThreshold) {
        this.injectionRaSensitivityThreshold = injectionRaSensitivityThreshold;
    }

    public LinearOptimizationSolver getLinearOptimizationSolver() {
        return linearOptimizationSolver;
    }

    public void setPstModel(PstModel pstModel) {
        this.pstModel = pstModel;
    }

    public PstModel getPstModel() {
        return pstModel;
    }

    public void setLinearOptimizationSolver(LinearOptimizationSolver linearOptimizationSolver) {
        this.linearOptimizationSolver = linearOptimizationSolver;
    }

    public static RangeActionsOptimizationParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        RangeActionsOptimizationParameters parameters = new RangeActionsOptimizationParameters();
        platformConfig.getOptionalModuleConfig(RANGE_ACTIONS_OPTIMIZATION_SECTION)
                .ifPresent(config -> {
                    parameters.setMaxMipIterations(config.getIntProperty(MAX_MIP_ITERATIONS, DEFAULT_MAX_MIP_ITERATIONS));
                    parameters.setPstPenaltyCost(config.getDoubleProperty(PST_PENALTY_COST, DEFAULT_PST_PENALTY_COST));
                    parameters.setPstSensitivityThreshold(config.getDoubleProperty(PST_SENSITIVITY_THRESHOLD, DEFAULT_PST_SENSITIVITY_THRESHOLD));
                    parameters.setPstModel(config.getEnumProperty(PST_MODEL, PstModel.class, DEFAULT_PST_MODEL));
                    parameters.setHvdcPenaltyCost(config.getDoubleProperty(HVDC_PENALTY_COST, DEFAULT_HVDC_PENALTY_COST));
                    parameters.setHvdcSensitivityThreshold(config.getDoubleProperty(HVDC_SENSITIVITY_THRESHOLD, DEFAULT_HVDC_SENSITIVITY_THRESHOLD));
                    parameters.setInjectionRaPenaltyCost(config.getDoubleProperty(INJECTION_RA_PENALTY_COST, DEFAULT_INJECTION_RA_PENALTY_COST));
                    parameters.setInjectionRaSensitivityThreshold(config.getDoubleProperty(INJECTION_RA_SENSITIVITY_THRESHOLD, DEFAULT_INJECTION_RA_SENSITIVITY_THRESHOLD));
                });
        parameters.setLinearOptimizationSolver(LinearOptimizationSolver.load(platformConfig));
        return parameters;
    }

    public static RangeActionsOptimizationParameters buildFromRaoParameters(RaoParameters raoParameters) {
        return raoParameters.getRangeActionsOptimizationParameters();
    }
}
