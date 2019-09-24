/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.data.crac_file.*;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.*;
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test case is a 4 nodes network, with 4 countries.
 *
 *       FR   (+100 MW)       BE  (0 MW)
 *          + ------------ +
 *          |              |
 *          |              |
 *          |              |
 *          + ------------ +
 *       DE   (0 MW)          NL  (-100 MW)
 *
 * All lines have same impedance and are monitored.
 * One contingency is simulated, the loss of FR-BE interconnection line.
 * Each Country GLSK is a simple one node GLSK.
 * Compensation is considered as equally shared on each country, and there are no losses.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class ExampleGenerator {

    private ExampleGenerator() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    public static <K, U> Collector<Map.Entry<K, U>, ?, Map<K, U>> entriesToMap() {
        return Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue());
    }

    static Network network() {
        Network network = Network.create("Test", "code");
        Substation substationFr = network.newSubstation()
            .setId("Substation FR")
            .setName("Substation FR")
            .setCountry(Country.FR)
            .add();
        VoltageLevel voltageLevelFr = substationFr.newVoltageLevel()
            .setId("Voltage level FR")
            .setName("Voltage level FR")
            .setNominalV(400)
            .setTopologyKind(TopologyKind.BUS_BREAKER)
            .setLowVoltageLimit(300)
            .setHighVoltageLimit(500)
            .add();
        voltageLevelFr.getBusBreakerView()
            .newBus()
            .setId("Bus FR")
            .setName("Bus FR")
            .add();
        voltageLevelFr.newGenerator()
            .setId("Generator FR")
            .setName("Generator FR")
            .setBus("Bus FR")
            .setEnergySource(EnergySource.OTHER)
            .setMinP(1000)
            .setMaxP(2000)
            .setRatedS(100)
            .setTargetP(1600)
            .setTargetV(400)
            .setVoltageRegulatorOn(true)
            .add();
        voltageLevelFr.newLoad()
            .setId("Load FR")
            .setName("Load FR")
            .setBus("Bus FR")
            .setLoadType(LoadType.UNDEFINED)
            .setP0(1500)
            .setQ0(0)
            .add();

        Substation substationBe = network.newSubstation()
            .setId("Substation BE")
            .setName("Substation BE")
            .setCountry(Country.BE)
            .add();
        VoltageLevel voltageLevelBe = substationBe.newVoltageLevel()
            .setId("Voltage level BE")
            .setName("Voltage level BE")
            .setNominalV(400)
            .setTopologyKind(TopologyKind.BUS_BREAKER)
            .setLowVoltageLimit(300)
            .setHighVoltageLimit(500)
            .add();
        voltageLevelBe.getBusBreakerView()
            .newBus()
            .setId("Bus BE")
            .setName("Bus BE")
            .add();
        voltageLevelBe.newGenerator()
            .setId("Generator BE")
            .setName("Generator BE")
            .setBus("Bus BE")
            .setEnergySource(EnergySource.OTHER)
            .setMinP(1000)
            .setMaxP(2000)
            .setRatedS(100)
            .setTargetP(1500)
            .setTargetV(400)
            .setVoltageRegulatorOn(true)
            .add();
        voltageLevelBe.newLoad()
            .setId("Load BE")
            .setName("Load BE")
            .setBus("Bus BE")
            .setLoadType(LoadType.UNDEFINED)
            .setP0(1500)
            .setQ0(0)
            .add();

        Substation substationDe = network.newSubstation()
            .setId("Substation DE")
            .setName("Substation DE")
            .setCountry(Country.DE)
            .add();
        VoltageLevel voltageLevelDe = substationDe.newVoltageLevel()
            .setId("Voltage level DE")
            .setName("Voltage level DE")
            .setNominalV(400)
            .setTopologyKind(TopologyKind.BUS_BREAKER)
            .setLowVoltageLimit(300)
            .setHighVoltageLimit(500)
            .add();
        voltageLevelDe.getBusBreakerView()
            .newBus()
            .setId("Bus DE")
            .setName("Bus DE")
            .add();
        voltageLevelDe.newGenerator()
            .setId("Generator DE")
            .setName("Generator DE")
            .setBus("Bus DE")
            .setEnergySource(EnergySource.OTHER)
            .setMinP(1000)
            .setMaxP(2000)
            .setRatedS(100)
            .setTargetP(1500)
            .setTargetV(400)
            .setVoltageRegulatorOn(true)
            .add();
        voltageLevelDe.newLoad()
            .setId("Load DE")
            .setName("Load DE")
            .setBus("Bus DE")
            .setLoadType(LoadType.UNDEFINED)
            .setP0(1500)
            .setQ0(0)
            .add();

        Substation substationNl = network.newSubstation()
            .setId("Substation NL")
            .setName("Substation NL")
            .setCountry(Country.NL)
            .add();
        VoltageLevel voltageLevelNl = substationNl.newVoltageLevel()
            .setId("Voltage level NL")
            .setName("Voltage level NL")
            .setNominalV(400)
            .setTopologyKind(TopologyKind.BUS_BREAKER)
            .setLowVoltageLimit(300)
            .setHighVoltageLimit(500)
            .add();
        voltageLevelNl.getBusBreakerView()
            .newBus()
            .setId("Bus NL")
            .setName("Bus NL")
            .add();
        voltageLevelNl.newGenerator()
            .setId("Generator NL")
            .setName("Generator NL")
            .setBus("Bus NL")
            .setEnergySource(EnergySource.OTHER)
            .setMinP(1000)
            .setMaxP(2000)
            .setRatedS(100)
            .setTargetP(1500)
            .setTargetV(400)
            .setVoltageRegulatorOn(true)
            .add();
        voltageLevelNl.newLoad()
            .setId("Load NL")
            .setName("Load NL")
            .setBus("Bus NL")
            .setLoadType(LoadType.UNDEFINED)
            .setP0(1600)
            .setQ0(0)
            .add();

        network.newLine()
            .setId("FR-BE")
            .setName("FR-BE")
            .setVoltageLevel1("Voltage level FR")
            .setVoltageLevel2("Voltage level BE")
            .setBus1("Bus FR")
            .setBus2("Bus BE")
            .setR(0)
            .setX(5)
            .setB1(0)
            .setB2(0)
            .setG1(0)
            .setG2(0)
            .add();
        network.newLine()
            .setId("FR-DE")
            .setName("FR-DE")
            .setVoltageLevel1("Voltage level FR")
            .setVoltageLevel2("Voltage level DE")
            .setBus1("Bus FR")
            .setBus2("Bus DE")
            .setR(0)
            .setX(5)
            .setB1(0)
            .setB2(0)
            .setG1(0)
            .setG2(0)
            .add();
        network.newLine()
            .setId("BE-NL")
            .setName("BE-NL")
            .setVoltageLevel1("Voltage level BE")
            .setVoltageLevel2("Voltage level NL")
            .setBus1("Bus BE")
            .setBus2("Bus NL")
            .setR(0)
            .setX(5)
            .setB1(0)
            .setB2(0)
            .setG1(0)
            .setG2(0)
            .add();
        network.newLine()
            .setId("DE-NL")
            .setName("DE-NL")
            .setVoltageLevel1("Voltage level DE")
            .setVoltageLevel2("Voltage level NL")
            .setBus1("Bus DE")
            .setBus2("Bus NL")
            .setR(0)
            .setX(5)
            .setB1(0)
            .setB2(0)
            .setG1(0)
            .setG2(0)
            .add();
        return network;
    }

    static CracFile cracFile() {
        return CracFile.builder()
            .id("Test")
            .name("Test")
            .sourceFormat("code")
            .preContingency(
                PreContingency.builder()
                    .monitoredBranches(
                        Arrays.asList(
                            MonitoredBranch.builder()
                                .id("FR-BE")
                                .name("FR-BE")
                                .branchId("FR-BE")
                                .fmax(100)
                                .build(),
                            MonitoredBranch.builder()
                                .id("FR-DE")
                                .name("FR-DE")
                                .branchId("FR-DE")
                                .fmax(100)
                                .build(),
                            MonitoredBranch.builder()
                                .id("BE-NL")
                                .name("BE-NL")
                                .branchId("BE-NL")
                                .fmax(100)
                                .build(),
                            MonitoredBranch.builder()
                                .id("DE-NL")
                                .name("DE-NL")
                                .branchId("DE-NL")
                                .fmax(100)
                                .build()
                        )
                    )
                .build()
        )
        .contingencies(
            Collections.singletonList(
                Contingency.builder()
                    .id("N-1 FR-BE")
                    .name("N-1 FR-BE")
                    .contingencyElements(
                        Collections.singletonList(
                            ContingencyElement.builder()
                                .name("N-1 FR-BE")
                                .elementId("FR-BE")
                                .build()
                        )
                    )
                    .monitoredBranches(
                        Arrays.asList(
                            MonitoredBranch.builder()
                                .id("N-1 FR-BE / FR-BE")
                                .name("N-1 FR-BE / FR-BE")
                                .branchId("FR-BE")
                                .fmax(100)
                                .build(),
                            MonitoredBranch.builder()
                                .id("N-1 FR-BE / FR-DE")
                                .name("N-1 FR-BE / FR-DE")
                                .branchId("FR-DE")
                                .fmax(100)
                                .build(),
                            MonitoredBranch.builder()
                                .id("N-1 FR-BE / BE-NL")
                                .name("N-1 FR-BE / BE-NL")
                                .branchId("BE-NL")
                                .fmax(100)
                                .build(),
                            MonitoredBranch.builder()
                                .id("N-1 FR-BE / DE-NL")
                                .name("N-1 FR-BE / DE-NL")
                                .branchId("DE-NL")
                                .fmax(100)
                                .build()
                        )
                    )
                    .build()
            )
        )
        .build();
    }

    static GlskProvider glskProvider() {
        Map<String, LinearGlsk> glsks = new HashMap<>();
        glsks.put("FR GLSK", new LinearGlsk("FR GLSK", "FR GLSK", Collections.singletonMap("Generator FR", 1.f)));
        glsks.put("BE GLSK", new LinearGlsk("BE GLSK", "BE GLSK", Collections.singletonMap("Generator BE", 1.f)));
        glsks.put("DE GLSK", new LinearGlsk("DE GLSK", "DE GLSK", Collections.singletonMap("Generator DE", 1.f)));
        glsks.put("NL GLSK", new LinearGlsk("NL GLSK", "NL GLSK", Collections.singletonMap("Generator NL", 1.f)));
        return new GlskProvider() {
            @Override
            public Map<String, LinearGlsk> getAllGlsk(Network network) {
                return glsks;
            }

            @Override
            public LinearGlsk getGlsk(Network network, String area) {
                return glsks.get(area);
            }
        };
    }

    static LoadFlowFactory loadFlowFactory() {
        return new LoadFlowFactoryMock();
    }

    static SensitivityComputationFactory sensitivityComputationFactory() {
        return new SensitivityComputationFactoryMock();
    }

    /**
     * Load flow provider dedicated for this test case.
     * It ** ONLY ** works in basecase and in N-1 FR-BE.
     */
    @AutoService(LoadFlowFactory.class)
    public static class LoadFlowFactoryMock implements LoadFlowFactory {
        private final Map<String, Double> expectedFref;

        public LoadFlowFactoryMock() {
            expectedFref = getExpectedFref();
        }

        @Override
        public LoadFlow create(Network network, ComputationManager computationManager, int i) {
            return new LoadFlow() {
                @Override
                public CompletableFuture<LoadFlowResult> run(String workingVariantId, LoadFlowParameters loadFlowParameters) {
                    String initialVariantId = network.getVariantManager().getWorkingVariantId();
                    network.getVariantManager().setWorkingVariant(workingVariantId);
                    if (network.getLine("FR-BE").getTerminal1().isConnected() && network.getLine("FR-BE").getTerminal2().isConnected()) {
                        fillPreContingencyResult(network);
                    } else {
                        fillPostContingencyResult(network);
                    }
                    network.getVariantManager().setWorkingVariant(initialVariantId);
                    return CompletableFuture.completedFuture(new LoadFlowResultImpl(true, Collections.emptyMap(), null));
                }

                @Override
                public String getName() {
                    return "MockLoadflow";
                }

                @Override
                public String getVersion() {
                    return "1.0.0";
                }
            };
        }

        private void fillPreContingencyResult(Network network) {
            network.getLineStream().forEach(line -> {
                double fref = expectedFref.get(line.getId());
                line.getTerminal1().setP(fref);
                line.getTerminal2().setP(-fref);
            });
        }

        private void fillPostContingencyResult(Network network) {
            network.getLineStream().forEach(line -> {
                double fref = expectedFref.get("N-1 FR-BE / " + line.getId());
                line.getTerminal1().setP(fref);
                line.getTerminal2().setP(-fref);
            });
        }

        private Map<String, Double> getExpectedFref() {
            Map<String, Double> expectedFrefByBranch = new HashMap<>();
            expectedFrefByBranch.put("FR-BE", 50.);
            expectedFrefByBranch.put("FR-DE", 50.);
            expectedFrefByBranch.put("BE-NL", 50.);
            expectedFrefByBranch.put("DE-NL", 50.);
            expectedFrefByBranch.put("N-1 FR-BE / FR-BE", 0.);
            expectedFrefByBranch.put("N-1 FR-BE / FR-DE", 100.);
            expectedFrefByBranch.put("N-1 FR-BE / BE-NL", 0.);
            expectedFrefByBranch.put("N-1 FR-BE / DE-NL", 100.);
            return expectedFrefByBranch;
        }
    }

    @AutoService(SensitivityComputationFactory.class)
    public static class SensitivityComputationFactoryMock implements SensitivityComputationFactory {
        private final Map<String, Map<String, Double>> expectedPtdf;

        public SensitivityComputationFactoryMock() {
            expectedPtdf = getExpectedPtdf();
        }

        public static <K, V> Map.Entry<K, V> entry(K key, V value) {
            return new AbstractMap.SimpleEntry<>(key, value);
        }

        public static <K, U> Collector<Map.Entry<K, U>, ?, Map<K, U>> entriesToMap() {
            return Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue());
        }

        private Map<String, Map<String, Double>> getExpectedPtdf() {
            Map<String, Map<String, Double>> expectedPtdfByBranch = new HashMap<>();
            expectedPtdfByBranch.put("FR-BE", Collections.unmodifiableMap(
                    Stream.of(
                            entry("FR GLSK", 0.375),
                            entry("BE GLSK", -0.375),
                            entry("DE GLSK", 0.125),
                            entry("NL GLSK", -0.125)
                    )
                            .collect(entriesToMap())
            ));
            expectedPtdfByBranch.put("FR-DE", Collections.unmodifiableMap(
                    Stream.of(
                            entry("FR GLSK", 0.375),
                            entry("BE GLSK", 0.125),
                            entry("DE GLSK", -0.375),
                            entry("NL GLSK", -0.125)
                    )
                            .collect(entriesToMap())
            ));
            expectedPtdfByBranch.put("BE-NL", Collections.unmodifiableMap(
                    Stream.of(
                            entry("FR GLSK", 0.125),
                            entry("BE GLSK", 0.375),
                            entry("DE GLSK", -0.125),
                            entry("NL GLSK", -0.375)
                    )
                            .collect(entriesToMap())
            ));
            expectedPtdfByBranch.put("DE-NL", Collections.unmodifiableMap(
                    Stream.of(
                            entry("FR GLSK", 0.125),
                            entry("BE GLSK", -0.125),
                            entry("DE GLSK", 0.375),
                            entry("NL GLSK", -0.375)
                    )
                            .collect(entriesToMap())
            ));
            expectedPtdfByBranch.put("N-1 FR-BE / FR-BE", Collections.unmodifiableMap(
                    Stream.of(
                            entry("FR GLSK", 0.),
                            entry("BE GLSK", 0.),
                            entry("DE GLSK", 0.),
                            entry("NL GLSK", 0.)
                    )
                            .collect(entriesToMap())
            ));
            expectedPtdfByBranch.put("N-1 FR-BE / FR-DE", Collections.unmodifiableMap(
                    Stream.of(
                            entry("FR GLSK", 0.75),
                            entry("BE GLSK", -0.25),
                            entry("DE GLSK", -0.25),
                            entry("NL GLSK", -0.25)
                    )
                            .collect(entriesToMap())
            ));
            expectedPtdfByBranch.put("N-1 FR-BE / BE-NL", Collections.unmodifiableMap(
                    Stream.of(
                            entry("FR GLSK", -0.25),
                            entry("BE GLSK", 0.75),
                            entry("DE GLSK", -0.25),
                            entry("NL GLSK", -0.25)
                    )
                            .collect(entriesToMap())
            ));
            expectedPtdfByBranch.put("N-1 FR-BE / DE-NL", Collections.unmodifiableMap(
                    Stream.of(
                            entry("FR GLSK", 0.5),
                            entry("BE GLSK", -0.5),
                            entry("DE GLSK", 0.5),
                            entry("NL GLSK", -0.5)
                    )
                            .collect(entriesToMap())
            ));
            return expectedPtdfByBranch;
        }

        @Override
        public SensitivityComputation create(Network network, ComputationManager computationManager, int i) {
            return new SensitivityComputation() {

                @Override
                public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider sensitivityFactorsProvider, String s, SensitivityComputationParameters sensitivityComputationParameters) {
                    List<SensitivityValue> sensitivityValues = sensitivityFactorsProvider.getFactors(network).stream()
                            .map(factor -> new SensitivityValue(factor, expectedPtdf.get(factor.getFunction().getId()).get(factor.getVariable().getId()), Double.NaN, Double.NaN))
                            .collect(Collectors.toList());
                    return CompletableFuture.completedFuture(new SensitivityComputationResults(true, Collections.emptyMap(), "", sensitivityValues));
                }

                @Override
                public String getName() {
                    return "MockSensitivity";
                }

                @Override
                public String getVersion() {
                    return "1.0.0";
                }
            };
        }
    }
}
