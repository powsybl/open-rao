package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.results.PrePerimeterResult;
import com.farao_community.farao.rao_commons.linear_optimisation.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunction;
import com.powsybl.iidm.network.Network;

import java.util.Set;

public class SearchTreeInput {
    private Network network;
    private Set<FlowCnec> flowCnecs;
    private Set<NetworkAction> networkActions;
    private Set<RangeAction> rangeActions;

    private ObjectiveFunction objectiveFunction;
    private IteratingLinearOptimizer iteratingLinearOptimizer;
    private SearchTreeBloomer searchTreeBloomer;
    private SearchTreeProblem searchTreeProblem;
    private SearchTreeComputer searchTreeComputer;

    private PrePerimeterResult prePerimeterOutput;

    public SearchTreeBloomer getSearchTreeBloomer() {
        return searchTreeBloomer;
    }

    public void setSearchTreeBloomer(SearchTreeBloomer searchTreeBloomer) {
        this.searchTreeBloomer = searchTreeBloomer;
    }

    public SearchTreeComputer getSearchTreeComputer() {
        return searchTreeComputer;
    }

    public void setSearchTreeComputer(SearchTreeComputer searchTreeComputer) {
        this.searchTreeComputer = searchTreeComputer;
    }

    public Set<NetworkAction> getNetworkActions() {
        return networkActions;
    }

    public Network getNetwork() {
        return network;
    }

    public Set<RangeAction> getRangeActions() {
        return rangeActions;
    }

    public Set<FlowCnec> getFlowCnecs() {
        return flowCnecs;
    }

    public void setNetworkActions(Set<NetworkAction> networkActions) {
        this.networkActions = networkActions;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public void setRangeActions(Set<RangeAction> rangeActions) {
        this.rangeActions = rangeActions;
    }

    public void setFlowCnecs(Set<FlowCnec> flowCnecs) {
        this.flowCnecs = flowCnecs;
    }

    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public void setObjectiveFunction(ObjectiveFunction objectiveFunction) {
        this.objectiveFunction = objectiveFunction;
    }

    public IteratingLinearOptimizer getIteratingLinearOptimizer() {
        return iteratingLinearOptimizer;
    }

    public void setIteratingLinearOptimizer(IteratingLinearOptimizer iteratingLinearOptimizer) {
        this.iteratingLinearOptimizer = iteratingLinearOptimizer;
    }

    public SearchTreeProblem getSearchTreeProblem() {
        return searchTreeProblem;
    }

    public void setSearchTreeProblem(SearchTreeProblem searchTreeProblem) {
        this.searchTreeProblem = searchTreeProblem;
    }

    public PrePerimeterResult getPrePerimeterOutput() {
        return prePerimeterOutput;
    }

    public void setPrePerimeterOutput(PrePerimeterResult prePerimeterOutput) {
        this.prePerimeterOutput = prePerimeterOutput;
    }
}
