/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.commons.AbstractToolTest;
import com.farao_community.farao.flowbased_computation.tools.FlowbasedComputationTool;
import com.powsybl.tools.Tool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

/**
 * FlowBased Computation Tool Test
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
class FlowbasedComputationToolTest extends AbstractToolTest {

    //how to use itools to test flowbased-computation:
    //command line example, from git farao root directory:
    // ~/farao/bin/itools flowbased-computation --case-file flowbased-computation/flowbased-computation-api/src/test/resources/testCase.xiidm --crac-file flowbased-computation/flowbased-computation-api/src/test/resources/fakeCrac.json --glsk-file flowbased-computation/flowbased-computation-api/src/test/resources/GlskCountry.xml --instant 2018-08-28T22:00:00Z --output-file /tmp/outputflowbased

    // ~/farao/bin/itools flowbased-computation
    // --case-file flowbased-computation/flowbased-computation-api/src/test/resources/testCase.xiidm
    // --crac-file flowbased-computation/flowbased-computation-api/src/test/resources/fakeCrac.json
    // --glsk-file flowbased-computation/flowbased-computation-api/src/test/resources/GlskCountry.xml
    // --define-aliases
    // --instant 2018-08-28T22:00:00Z
    // --output-file /tmp/outputflowbased

    private static final String COMMAND_NAME = "flowbased-computation";
    private final FlowbasedComputationTool tool = new FlowbasedComputationTool();

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        createFile("testCase.xiidm", "");
        createFile("fakeCrac.json", "");
        createFile("GlskCountry.xml", "");
        createFile("outputflowbased", "");
    }

    protected Iterable<Tool> getTools() {
        return Collections.singleton(tool);
    }

    @Override
    public void assertCommand() {
        assertCommand(tool.getCommand(), COMMAND_NAME, 7, 4);
        assertOption(tool.getCommand().getOptions(), "case-file", true, true);
        assertOption(tool.getCommand().getOptions(), "crac-file", true, true);
        assertOption(tool.getCommand().getOptions(), "glsk-file", true, true);
        assertOption(tool.getCommand().getOptions(), "parameters-file", false, true);
        assertOption(tool.getCommand().getOptions(), "define-aliases", false, false);
        assertOption(tool.getCommand().getOptions(), "instant", true, true);
        assertOption(tool.getCommand().getOptions(), "output-file", false, true);

        Assertions.assertEquals("Computation", tool.getCommand().getTheme());
        Assertions.assertEquals("Run modular FlowBased computation", tool.getCommand().getDescription());
    }

    @Test
    void checkCommandFail() throws IOException {
        assertCommand(new String[] {COMMAND_NAME, "--case-file", "testCase.xiidm"}, 2, "", "");
    }

    @Test
    void checkCommandFailBis() throws IOException {
        assertCommand(new String[] {
            COMMAND_NAME,
            "--case-file", "testCase.xiidm",
            "--crac-file", "fakeCrac.json",
            "--glsk-file", "GlskCountry.xml"
        }, 2, "", "");
    }

    @Test
    void checkCommandOK() throws IOException {
        assertCommand(new String[] {
            COMMAND_NAME,
            "--case-file", "testCase.xiidm",
            "--crac-file", "fakeCrac.json",
            "--glsk-file", "GlskCountry.xml",
            "--instant", "2018-08-28T22:00:00Z",
            "--output-file", "outputflowbased"
        }, 3, "", "");
    }
}
