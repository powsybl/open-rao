package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdAdder;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileElementaryCreationContext;
import com.powsybl.iidm.network.*;
import com.powsybl.triplestore.api.PropertyBag;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class FlowCnecCreator extends AbstractCnecCreator {

    private enum FlowCnecDefinitionMode {
        CONDUCTING_EQUIPMENT,
        OPERATIONAL_LIMIT,
        WRONG_DEFINITION;
    }

    private final String conductingEquipment;
    private final Set<Side> defaultMonitoredSides;

    public FlowCnecCreator(Crac crac, Network network, String assessedElementId, String nativeAssessedElementName, String assessedElementOperator, boolean inBaseCase, PropertyBag currentLimitPropertyBag, String conductingEquipment, List<Contingency> linkedContingencies, Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts, CsaProfileCracCreationContext cracCreationContext, Set<Side> defaultMonitoredSides, String rejectedLinksAssessedElementContingency) {
        super(crac, network, assessedElementId, nativeAssessedElementName, assessedElementOperator, inBaseCase, currentLimitPropertyBag, linkedContingencies, csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency);
        this.conductingEquipment = conductingEquipment;
        this.defaultMonitoredSides = defaultMonitoredSides;
    }

    public void addFlowCnecs() {
        FlowCnecDefinitionMode definitionMode = getCnecDefinitionMode();
        if (definitionMode == FlowCnecDefinitionMode.WRONG_DEFINITION) {
            return;
        }

        String networkElementId = definitionMode == FlowCnecDefinitionMode.CONDUCTING_EQUIPMENT ? conductingEquipment : operationalLimitPropertyBag.getId(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_TERMINAL);
        Identifiable<?> branch = getFlowCnecBranch(networkElementId);
        if (branch == null) {
            return;
        }

        Pair<EnumMap<Branch.Side, Double>, Map<Integer, EnumMap<Branch.Side, Double>>> thresholds = definitionMode == FlowCnecDefinitionMode.CONDUCTING_EQUIPMENT ? getPermanentAndTemporaryLimitsOfBranch((Branch<?>) branch) : getPermanentAndTemporaryLimitsOfOperationalLimit(branch, networkElementId);
        EnumMap<Branch.Side, Double> patlThresholds = thresholds.getLeft();
        Map<Integer, EnumMap<Branch.Side, Double>> tatlThresholds = thresholds.getRight();

        if (patlThresholds.isEmpty() && tatlThresholds.isEmpty()) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCOMPLETE_DATA, writeAssessedElementIgnoredReasonMessage("no PATL or TATLs could be retrieved for the branch " + branch.getId())));
            return;
        }

        // If the AssessedElement is defined with a conducting equipment, we use both max and min thresholds.
        boolean useMaxAndMinThresholds = true;
        if (definitionMode == FlowCnecDefinitionMode.OPERATIONAL_LIMIT) {
            String direction = operationalLimitPropertyBag.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_DIRECTION);
            if (CsaProfileConstants.OperationalLimitDirectionKind.HIGH.toString().equals(direction)) {
                useMaxAndMinThresholds = false;
            } else if (!CsaProfileConstants.OperationalLimitDirectionKind.ABSOLUTE.toString().equals(direction)) {
                csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, writeAssessedElementIgnoredReasonMessage("OperationalLimitType.direction is neither 'absoluteValue' nor 'high'")));
                return;
            }
        }

        addAllFlowCnecs((Branch<?>) branch, patlThresholds, tatlThresholds, useMaxAndMinThresholds);
    }

    private FlowCnecAdder initFlowCnec() {
        return crac.newFlowCnec()
                .withMonitored(false)
                .withOptimized(true)
                .withReliabilityMargin(0);
    }

    private Identifiable<?> getFlowCnecBranch(String networkElementId) {
        Identifiable<?> networkElement = getNetworkElementInNetwork(networkElementId);
        if (networkElement == null) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, writeAssessedElementIgnoredReasonMessage("the following is element missing from the network : " + networkElementId)));
            return null;
        }
        if (!(networkElement instanceof Branch)) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("the network element " + networkElement.getId() + " is not a branch")));
            return null;
        }
        return networkElement;
    }

    private FlowCnecDefinitionMode getCnecDefinitionMode() {
        if (conductingEquipment == null && operationalLimitPropertyBag == null) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCOMPLETE_DATA, writeAssessedElementIgnoredReasonMessage("no ConductingEquipment or OperationalLimit was provided")));
            return FlowCnecDefinitionMode.WRONG_DEFINITION;
        }
        if (conductingEquipment != null && operationalLimitPropertyBag != null) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("an assessed element must be defined using either a ConductingEquipment or an OperationalLimit, not both")));
            return FlowCnecDefinitionMode.WRONG_DEFINITION;
        }
        return conductingEquipment != null ? FlowCnecDefinitionMode.CONDUCTING_EQUIPMENT : FlowCnecDefinitionMode.OPERATIONAL_LIMIT;
    }

    private Side getSideFromNetworkElement(Identifiable<?> networkElement, String terminalId) {
        if (networkElement instanceof TieLine) {
            for (String key : CsaProfileConstants.CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_TIE_LINE) {
                TieLine tieLine = (TieLine) networkElement;
                Optional<String> oAlias = tieLine.getDanglingLine1().getAliasFromType(key);
                if (oAlias.isPresent() && oAlias.get().equals(terminalId)) {
                    return Side.LEFT;
                }
                oAlias = tieLine.getDanglingLine2().getAliasFromType(key);
                if (oAlias.isPresent() && oAlias.get().equals(terminalId)) {
                    return Side.RIGHT;
                }

            }
        } else {
            for (String key : CsaProfileConstants.CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_LEFT) {
                Optional<String> oAlias = networkElement.getAliasFromType(key);
                if (oAlias.isPresent() && oAlias.get().equals(terminalId)) {
                    return Side.LEFT;
                }
            }

            for (String key : CsaProfileConstants.CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_RIGHT) {
                Optional<String> oAlias = networkElement.getAliasFromType(key);
                if (oAlias.isPresent() && oAlias.get().equals(terminalId)) {
                    return Side.RIGHT;
                }
            }
        }
        return null;
    }

    private void addFlowCnecThreshold(FlowCnecAdder flowCnecAdder, Side side, double threshold, boolean useMaxAndMinThresholds) {
        BranchThresholdAdder adder = flowCnecAdder.newThreshold().withSide(side)
                .withUnit(Unit.AMPERE)
                .withMax(threshold);
        if (useMaxAndMinThresholds) {
            adder.withMin(-threshold);
        }
        adder.add();
    }

    private boolean setNominalVoltage(FlowCnecAdder flowCnecAdder, Branch<?> branch) {
        double voltageLevelLeft = branch.getTerminal1().getVoltageLevel().getNominalV();
        double voltageLevelRight = branch.getTerminal2().getVoltageLevel().getNominalV();
        if (voltageLevelLeft > 1e-6 && voltageLevelRight > 1e-6) {
            flowCnecAdder.withNominalVoltage(voltageLevelLeft, Side.LEFT);
            flowCnecAdder.withNominalVoltage(voltageLevelRight, Side.RIGHT);
            return true;
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "Voltage level for branch " + branch.getId() + " is 0 in network"));
            return false;
        }
    }

    private boolean setCurrentLimitsFromBranch(FlowCnecAdder flowCnecAdder, Branch<?> branch) {
        Double currentLimitLeft = getCurrentLimitFromBranch(branch, Branch.Side.ONE);
        Double currentLimitRight = getCurrentLimitFromBranch(branch, Branch.Side.TWO);
        if (Objects.nonNull(currentLimitLeft) && Objects.nonNull(currentLimitRight)) {
            flowCnecAdder.withIMax(currentLimitLeft, Side.LEFT);
            flowCnecAdder.withIMax(currentLimitRight, Side.RIGHT);
            return true;
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "Unable to get branch current limits from network for branch " + branch.getId()));
            return false;
        }
    }

    private Double getCurrentLimitFromBranch(Branch<?> branch, Branch.Side side) {

        if (branch.getCurrentLimits(side).isPresent()) {
            return branch.getCurrentLimits(side).orElseThrow().getPermanentLimit();
        }

        if (side == Branch.Side.ONE && branch.getCurrentLimits(Branch.Side.TWO).isPresent()) {
            return branch.getCurrentLimits(Branch.Side.TWO).orElseThrow().getPermanentLimit() * branch.getTerminal1().getVoltageLevel().getNominalV() / branch.getTerminal2().getVoltageLevel().getNominalV();
        }

        if (side == Branch.Side.TWO && branch.getCurrentLimits(Branch.Side.ONE).isPresent()) {
            return branch.getCurrentLimits(Branch.Side.ONE).orElseThrow().getPermanentLimit() * branch.getTerminal2().getVoltageLevel().getNominalV() / branch.getTerminal1().getVoltageLevel().getNominalV();
        }

        return null;
    }

    private Instant getCnecInstant(int acceptableDuration) {
        if (acceptableDuration < 0) {
            return null;
        }
        if (0 < acceptableDuration && acceptableDuration <= 60) {
            return Instant.OUTAGE;
        }
        if (60 < acceptableDuration && acceptableDuration <= 900) {
            return Instant.AUTO;
        }
        return Instant.CURATIVE;
    }

    private void addFlowCnec(Branch<?> networkElement, Contingency contingency, Instant instant, EnumMap<Branch.Side, Double> thresholds, boolean useMaxAndMinThresholds, boolean hasNoPatl) {
        if (thresholds.isEmpty()) {
            return;
        }
        FlowCnecAdder cnecAdder = initFlowCnec();
        for (Branch.Side side : thresholds.keySet()) {
            double threshold = thresholds.get(side);
            addFlowCnecThreshold(cnecAdder, side == Branch.Side.ONE ? Side.LEFT : Side.RIGHT, threshold, useMaxAndMinThresholds);
        }
        addFlowCnecData(cnecAdder, networkElement, contingency, instant);
        if (!setNominalVoltage(cnecAdder, networkElement) || !setCurrentLimitsFromBranch(cnecAdder, networkElement)) {
            return;
        }
        cnecAdder.add();
        if (hasNoPatl) {
            String cnecName = getCnecName(instant, null);
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.imported(assessedElementId, cnecName, cnecName, "the AssessedElement was pointing to a TATL and used inBaseCase. For the preventive instant, this TATL was also used as a PATL to create the CNEC", true));
            return;
        }
        markCnecAsImportedAndHandleRejectedContingencies(instant, contingency);
    }

    private void addFlowCnecData(FlowCnecAdder cnecAdder, Branch<?> networkElement, Contingency contingency, Instant instant) {
        cnecAdder.withNetworkElement(networkElement.getId());
        addCnecData(cnecAdder, contingency, instant);
    }

    private void addAllFlowCnecs(Branch<?> networkElement, EnumMap<Branch.Side, Double> patlThresholds, Map<Integer, EnumMap<Branch.Side, Double>> tatlThresholds, boolean useMaxAndMinThresholds) {
        if (inBaseCase) {
            // If no PATL, we use the lowest TATL instead (as in PowSyBl).
            // Only happens when the AssessedElement is defined with an OperationalLimit
            if (!patlThresholds.isEmpty()) {
                addFlowCnec(networkElement, null, Instant.PREVENTIVE, patlThresholds, useMaxAndMinThresholds, false);
            } else {
                Optional<Integer> longestAcceptableDuration = tatlThresholds.keySet().stream().max(Integer::compareTo);
                longestAcceptableDuration.ifPresent(integer -> addFlowCnec(networkElement, null, Instant.PREVENTIVE, tatlThresholds.get(integer), useMaxAndMinThresholds, true));
            }
        }

        for (Contingency contingency : linkedContingencies) {
            // Add PATL
            addFlowCnec(networkElement, contingency, Instant.CURATIVE, patlThresholds, useMaxAndMinThresholds, false);
            // Add TATLs
            for (int acceptableDuration : tatlThresholds.keySet()) {
                Instant instant = getCnecInstant(acceptableDuration);
                if (instant == null) {
                    csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("TATL acceptable duration is negative: " + acceptableDuration)));
                    return;
                }
                addFlowCnec(networkElement, contingency, instant, tatlThresholds.get(acceptableDuration), useMaxAndMinThresholds, false);
            }
        }
    }

    private Pair<EnumMap<Branch.Side, Double>, Map<Integer, EnumMap<Branch.Side, Double>>> getPermanentAndTemporaryLimitsOfOperationalLimit(Identifiable<?> branch, String terminalId) {
        EnumMap<Branch.Side, Double> patlThresholds = new EnumMap<>(Branch.Side.class);
        Map<Integer, EnumMap<Branch.Side, Double>> tatlThresholds = new HashMap<>();

        String limitKind = operationalLimitPropertyBag.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_KIND);
        Side side = getSideFromNetworkElement(branch, terminalId);
        String normalValueStr = operationalLimitPropertyBag.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_NORMAL_VALUE);
        Double normalValue = Double.valueOf(normalValueStr);

        if (side != null) {
            if (CsaProfileConstants.LimitKind.PATL.toString().equals(limitKind)) {
                patlThresholds.put(side.iidmSide(), normalValue);
            } else if (CsaProfileConstants.LimitKind.TATL.toString().equals(limitKind)) {
                String acceptableDurationStr = operationalLimitPropertyBag.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_ACCEPTABLE_DURATION);
                int acceptableDuration = Integer.parseInt(acceptableDurationStr);
                tatlThresholds.put(acceptableDuration, new EnumMap<>(Map.of(side.iidmSide(), normalValue)));
            }
        }

        return Pair.of(patlThresholds, tatlThresholds);
    }

    private Pair<EnumMap<Branch.Side, Double>, Map<Integer, EnumMap<Branch.Side, Double>>> getPermanentAndTemporaryLimitsOfBranch(Branch<?> branch) {
        Set<Side> sidesToCheck = getSidesToCheck(branch);

        EnumMap<Branch.Side, Double> patlThresholds = new EnumMap<>(Branch.Side.class);
        Map<Integer, EnumMap<Branch.Side, Double>> tatlThresholds = new HashMap<>();

        for (Side side : sidesToCheck) {
            Optional<CurrentLimits> currentLimits = branch.getCurrentLimits(side.iidmSide());
            if (currentLimits.isPresent()) {
                // Retrieve PATL
                Double threshold = currentLimits.get().getPermanentLimit();
                patlThresholds.put(side.iidmSide(), threshold);
                // Retrieve TATLs
                List<LoadingLimits.TemporaryLimit> temporaryLimits = currentLimits.get().getTemporaryLimits().stream().toList();
                for (LoadingLimits.TemporaryLimit temporaryLimit : temporaryLimits) {
                    int acceptableDuration = temporaryLimit.getAcceptableDuration();
                    double temporaryThreshold = temporaryLimit.getValue();
                    if (tatlThresholds.containsKey(acceptableDuration)) {
                        tatlThresholds.get(acceptableDuration).put(side.iidmSide(), temporaryThreshold);
                    } else {
                        tatlThresholds.put(acceptableDuration, new EnumMap<>(Map.of(side.iidmSide(), temporaryThreshold)));
                    }
                }
            }
        }
        return Pair.of(patlThresholds, tatlThresholds);
    }

    private Set<Side> getSidesToCheck(Branch<?> branch) {
        Set<Side> sidesToCheck = new HashSet<>();
        if (defaultMonitoredSides.size() == 2) {
            sidesToCheck.add(Side.LEFT);
            sidesToCheck.add(Side.RIGHT);
        } else {
            // Only one side in the set -> check the default side.
            // If no limit for the default side, check the other side.
            Side defaultSide = defaultMonitoredSides.stream().toList().get(0);
            Side otherSide = defaultSide == Side.LEFT ? Side.RIGHT : Side.LEFT;
            sidesToCheck.add(branch.getCurrentLimits(defaultSide.iidmSide()).isPresent() ? defaultSide : otherSide);
        }
        return sidesToCheck;
    }
}
