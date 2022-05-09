/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.util.ucte;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class UcteCnecElementHelperTest {

    private static final double DOUBLE_TOLERANCE = 1e-3;
    private UcteNetworkAnalyzer networkHelper;
    private UcteNetworkAnalyzer alternativeNetworkHelper;

    private void setUp(String networkFile) {
        Network network = Importers.loadNetwork(networkFile, getClass().getResourceAsStream("/" + networkFile));
        networkHelper = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
        alternativeNetworkHelper = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.REPLACE_8TH_CHARACTER_WITH_WILDCARD));
    }

    @Test
    public void testValidInternalBranch() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");
        // internal branch with order code, from/to same as network
        UcteCnecElementHelper branchHelper = new UcteCnecElementHelper("BBE1AA1 ", "BBE2AA1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA1  BBE2AA1  1", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(5000., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(5000., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteCnecElementHelper("FFR1AA1 ", "FFR3AA1 ", "2", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("FFR1AA1  FFR3AA1  2", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());

        // internal branch with element name, from/to same as network
        branchHelper = new UcteCnecElementHelper("BBE1AA1 ", "BBE3AA1 ", null, "BR BE1BE3", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());

        branchHelper = new UcteCnecElementHelper("FFR1AA1 ", "FFR3AA1 ", null, "BR FR1FR3", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("FFR1AA1  FFR3AA1  2", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());

        // internal branch with order code, from/to different from network
        branchHelper = new UcteCnecElementHelper("BBE2AA2 ", "BBE1AA2 ", "1", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA2  BBE2AA2  1", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1000., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1000., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteCnecElementHelper("FFR3AA1 ", "FFR1AA1 ", "2", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("FFR1AA1  FFR3AA1  2", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());

        // internal branch with element name, from/to different from network
        branchHelper = new UcteCnecElementHelper("BBE3AA1 ", "BBE1AA1 ", null, "BR BE1BE3", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());

        branchHelper = new UcteCnecElementHelper("FFR3AA1 ", "FFR1AA1 ", null, "BR FR1FR3", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("FFR1AA1  FFR3AA1  2", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
    }

    @Test
    public void testValidInternalBranchReplace8th() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");
        // internal branch with order code, from/to same as network
        UcteCnecElementHelper branchHelper = new UcteCnecElementHelper("BBE1AA18", "BBE2AA18", "1", null, alternativeNetworkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA1  BBE2AA1  1", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(5000., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(5000., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteCnecElementHelper("FFR1AA18", "FFR3AA18", "2", null, alternativeNetworkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("FFR1AA1  FFR3AA1  2", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());

        // internal branch with element name, from/to same as network
        branchHelper = new UcteCnecElementHelper("BBE1AA18", "BBE3AA18", null, "BR BE1BE3", alternativeNetworkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());

        branchHelper = new UcteCnecElementHelper("FFR1AA1 ", "FFR3AA1 ", null, "BR FR1FR3", alternativeNetworkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("FFR1AA1  FFR3AA1  2", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());

        // internal branch with order code, from/to different from network
        branchHelper = new UcteCnecElementHelper("BBE2AA28", "BBE1AA28", "1", null, alternativeNetworkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA2  BBE2AA2  1", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1000., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1000., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteCnecElementHelper("FFR3AA18", "FFR1AA18", "2", null, alternativeNetworkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("FFR1AA1  FFR3AA1  2", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());

        // internal branch with element name, from/to different from network
        branchHelper = new UcteCnecElementHelper("BBE3AA18", "BBE1AA18", null, "BR BE1BE3", alternativeNetworkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());

        branchHelper = new UcteCnecElementHelper("FFR3AA18", "FFR1AA18", null, "BR FR1FR3", alternativeNetworkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("FFR1AA1  FFR3AA1  2", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
    }

    @Test
    public void testValidInternalBranchReplace8thWithSeveralMatches() {
        setUp("TestCase_severalVoltageLevels_Xnodes_8characters.uct");

        // if exact branch is found, we do not replace the 8th character
        UcteCnecElementHelper branchHelper = new UcteCnecElementHelper("DDE1AA12", "DDE2AA11", "1", null, alternativeNetworkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("DDE1AA12 DDE2AA11 1", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());

        branchHelper = new UcteCnecElementHelper("DDE2AA11", "DDE1AA12", "1", null, alternativeNetworkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("DDE1AA12 DDE2AA11 1", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());

        // if exact branch is not found, 8th character is replaced, and case became ambiguous as several solutions are possible
        branchHelper = new UcteCnecElementHelper("DDE1AA13", "DDE2AA11", "1", null, alternativeNetworkHelper);
        assertFalse(branchHelper.isValid());
        assertTrue(branchHelper.getInvalidReason().contains("several branches were found"));

        branchHelper = new UcteCnecElementHelper("DDE2AA11", "DDE1AA13", "1", null, alternativeNetworkHelper);
        assertFalse(branchHelper.isValid());
        assertTrue(branchHelper.getInvalidReason().contains("several branches were found"));
    }

    @Test
    public void testInvalidInternalBranch() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // unknown from
        assertFalse(new UcteCnecElementHelper("UNKNOW1 ", "BBE1AA1", "1", null, networkHelper).isValid());

        // unknown to
        assertFalse(new UcteCnecElementHelper("BBE3AA1 ", "UNKNOW1 ", "1", null, networkHelper).isValid());

        // branch exists but not with this order code
        assertFalse(new UcteCnecElementHelper("BBE1AA1 ", "BBE2AA1 ", "4", null, networkHelper).isValid());

        // branch exists but not with this element name
        assertFalse(new UcteCnecElementHelper("BBE1AA1 ", "BBE3AA1 ", null, "COUCOU", networkHelper).isValid());

    }

    @Test
    public void testValidTransformer() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        /* note that transformers from/to node are systematically inverted by PowSyBl UCTE importer
        For instance, the transformer with id "UCTNODE1 UCTNODE2 1" have :
            - terminal1 = UCTNODE2
            - terminal2 = UCTNODE1
        That's why the branch is inverted when from/to is aligned with what is defined in the UCTE file, and vice versa.
        This is note the case for the other type of Branch, where terminal 1 and 2 match the id.
         */

        // transformer with order code, from/to same as network
        UcteCnecElementHelper branchHelper = new UcteCnecElementHelper("BBE2AA1 ", "BBE3AA1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE2AA1  BBE3AA1  1", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteCnecElementHelper("FFR1AA2 ", "FFR1AA1 ", "5", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("FFR1AA2  FFR1AA1  5", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1500. * 220 / 380, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        // transformer with element name, from/to same as network
        branchHelper = new UcteCnecElementHelper("BBE2AA1 ", "BBE3AA1 ", null, "PST BE", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE2AA1  BBE3AA1  1", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteCnecElementHelper("BBE1AA1 ", "BBE1AA2 ", null, "TR BE1", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA1  BBE1AA2  1", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1000. * 380 / 220, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1000., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        // transformer with order code, from/to different from network
        branchHelper = new UcteCnecElementHelper("BBE3AA1 ", "BBE2AA1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE2AA1  BBE3AA1  1", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteCnecElementHelper("BBE2AA1 ", "BBE2AA2 ", "2", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE2AA2  BBE2AA1  2", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1200. * 220 / 380, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1200., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        // transformer with element name, from/to different from network
        branchHelper = new UcteCnecElementHelper("BBE3AA1 ", "BBE2AA1 ", null, "PST BE", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE2AA1  BBE3AA1  1", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteCnecElementHelper("FFR1AA1 ", "FFR1AA2 ", null, "TR FR1", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("FFR1AA2  FFR1AA1  5", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1500. * 220 / 380, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);
    }

    @Test
    public void testInvalidTransformer() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // transformer exists but not with this order code
        assertFalse(new UcteCnecElementHelper("BBE2AA1 ", "BBE3AA1 ", "2", null, networkHelper).isValid());

        // transformer exists but not with this element name
        assertFalse(new UcteCnecElementHelper("FFR1AA2 ", "FFR1AA1 ", null, "COUCOU", networkHelper).isValid());
    }

    @Test
    public void testValidTieLine() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // tie-line with order code
        UcteCnecElementHelper branchHelper = new UcteCnecElementHelper("XFRDE11 ", "DDE3AA1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("XFRDE11  DDE3AA1  1 + XFRDE11  FFR2AA1  1", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isHalfLine());
        assertEquals(Branch.Side.ONE, branchHelper.getHalfLineSide());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4800., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4400., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteCnecElementHelper("DDE3AA1 ", "XFRDE11 ", "1", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("XFRDE11  DDE3AA1  1 + XFRDE11  FFR2AA1  1", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isHalfLine());
        assertEquals(Branch.Side.ONE, branchHelper.getHalfLineSide());

        branchHelper = new UcteCnecElementHelper("XFRDE11 ", "FFR2AA1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("XFRDE11  DDE3AA1  1 + XFRDE11  FFR2AA1  1", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isHalfLine());
        assertEquals(Branch.Side.TWO, branchHelper.getHalfLineSide());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4800., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4400., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteCnecElementHelper("FFR2AA1 ", "XFRDE11 ", "1", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("XFRDE11  DDE3AA1  1 + XFRDE11  FFR2AA1  1", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isHalfLine());
        assertEquals(Branch.Side.TWO, branchHelper.getHalfLineSide());

        // tie-line with element name
        branchHelper = new UcteCnecElementHelper("NNL2AA1 ", "XNLBE11 ", null, "TL NL2X", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("NNL2AA1  XNLBE11  1 + XNLBE11  BBE3AA1  1", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isHalfLine());
        assertEquals(Branch.Side.ONE, branchHelper.getHalfLineSide());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(3200., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(2800., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteCnecElementHelper("XNLBE11 ", "NNL2AA1 ", null, "TL NL2X", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("NNL2AA1  XNLBE11  1 + XNLBE11  BBE3AA1  1", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isHalfLine());
        assertEquals(Branch.Side.ONE, branchHelper.getHalfLineSide());

        branchHelper = new UcteCnecElementHelper("XNLBE11 ", "BBE3AA1 ", null, "TL BE3X", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("NNL2AA1  XNLBE11  1 + XNLBE11  BBE3AA1  1", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isHalfLine());
        assertEquals(Branch.Side.TWO, branchHelper.getHalfLineSide());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(3200., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(2800., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteCnecElementHelper("BBE3AA1 ", "XNLBE11 ", null, "TL BE3X", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("NNL2AA1  XNLBE11  1 + XNLBE11  BBE3AA1  1", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isHalfLine());
        assertEquals(Branch.Side.TWO, branchHelper.getHalfLineSide());
    }

    @Test
    public void testInvalidTieLine() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // tie-line exists but not with this order code
        assertFalse(new UcteCnecElementHelper("XFRDE11 ", "FFR2AA1 ", "7", null, networkHelper).isValid());

        // tie-line exists but not with this element name
        assertFalse(new UcteCnecElementHelper("NNL2AA1 ", "XNLBE11 ", null, "COUCOU", networkHelper).isValid());
    }

    @Test
    public void testValidDanglingLine() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // dangling-line with order code
        UcteCnecElementHelper branchHelper = new UcteCnecElementHelper("BBE2AA1 ", "XBE2AL1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE2AA1  XBE2AL1  1", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1250, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1250, branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteCnecElementHelper("XBE2AL1 ", "BBE2AA1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE2AA1  XBE2AL1  1", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());

        // dangling-line with element name
        branchHelper = new UcteCnecElementHelper("XDE2AL1 ", "DDE2AA1 ", null, "DL AL", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("XDE2AL1  DDE2AA1  1", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1245, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1245, branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteCnecElementHelper("DDE2AA1 ", "XDE2AL1 ", null, "DL AL", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("XDE2AL1  DDE2AA1  1", branchHelper.getIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
    }

    @Test
    public void testInvalidDanglingLine() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // dangling-line exists but not with this order code
        assertFalse(new UcteCnecElementHelper("XBE2AL1 ", "BBE2AA1 ", "2", null, networkHelper).isValid());

        // dangling-line exists but not with this element name
        assertFalse(new UcteCnecElementHelper("DDE2AA1 ", "XDE2AL1 ", null, "COUCOU", networkHelper).isValid());
    }

    @Test
    public void testWithSevenCharacters() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        /*
        if the from/to node contains less that 8 characters, the missing characters will be
        replaced by blank spaces at the end of the missing UCTE node id
         */

        // internal branch with order code, 7 characters in from and to
        UcteCnecElementHelper branchReader = new UcteCnecElementHelper("BBE1AA1", "BBE2AA1", "1", null, networkHelper);
        assertTrue(branchReader.isValid());
        assertEquals("BBE1AA1  BBE2AA1  1", branchReader.getIdInNetwork());

        // tie-line with element name, 7 characters in to
        branchReader = new UcteCnecElementHelper("NNL2AA1 ", "XNLBE11", null, "TL NL2X", networkHelper);
        assertTrue(branchReader.isValid());
        assertEquals("NNL2AA1  XNLBE11  1 + XNLBE11  BBE3AA1  1", branchReader.getIdInNetwork());

        // transformer with order code, 7 characters in from
        branchReader = new UcteCnecElementHelper("FFR1AA2", "FFR1AA1 ", "5", null, networkHelper);
        assertTrue(branchReader.isValid());
        assertEquals("FFR1AA2  FFR1AA1  5", branchReader.getIdInNetwork());

        // dangling line with element name, 7 characters in from and to
        branchReader = new UcteCnecElementHelper("DDE2AA1", "XDE2AL1", null, "DL AL", networkHelper);
        assertTrue(branchReader.isValid());
        assertEquals("XDE2AL1  DDE2AA1  1", branchReader.getIdInNetwork());
    }

    @Test
    public void testOtherConstructors() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // element name
        UcteCnecElementHelper branchHelper = new UcteCnecElementHelper("BBE1AA1 ", "BBE3AA1 ", null, "BR BE1BE3", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getIdInNetwork());

        // order code
        branchHelper = new UcteCnecElementHelper("BBE1AA1 ", "BBE3AA1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getIdInNetwork());

        // suffix
        branchHelper = new UcteCnecElementHelper("BBE1AA1 ", "BBE3AA1 ", "BR BE1BE3", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getIdInNetwork());

        branchHelper = new UcteCnecElementHelper("BBE1AA1 ", "BBE3AA1 ", "1", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getIdInNetwork());

        // id
        branchHelper = new UcteCnecElementHelper("BBE1AA1  BBE3AA1  BR BE1BE3", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getIdInNetwork());
        assertEquals("BBE1AA1 ", branchHelper.getOriginalFrom());
        assertEquals("BBE3AA1 ", branchHelper.getOriginalTo());
        assertEquals("BR BE1BE3", branchHelper.getSuffix());

        branchHelper = new UcteCnecElementHelper("BBE1AA1  BBE3AA1  1", networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getIdInNetwork());
        assertEquals("BBE1AA1 ", branchHelper.getOriginalFrom());
        assertEquals("BBE3AA1 ", branchHelper.getOriginalTo());
        assertEquals("1", branchHelper.getSuffix());
    }

    @Test
    public void testInvalidConstructor() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // no from
        assertFalse(new UcteCnecElementHelper(null, "BBE3AA1 ", "1", networkHelper).isValid());
        assertFalse(new UcteCnecElementHelper(null, "BBE3AA1 ", "1", null, networkHelper).isValid());

        // no to
        assertFalse(new UcteCnecElementHelper("BBE1AA1 ", null, "1", networkHelper).isValid());
        assertFalse(new UcteCnecElementHelper("BBE1AA1 ", null, "1", null, networkHelper).isValid());

        // no order code, no element name
        assertFalse(new UcteCnecElementHelper("BBE1AA1 ", "BBE3AA1 ", "", "", networkHelper).isValid());
        assertFalse(new UcteCnecElementHelper("BBE1AA1 ", "BBE3AA1 ", null, null, networkHelper).isValid());
        assertFalse(new UcteCnecElementHelper("BBE1AA1 ", "BBE3AA1 ", null, networkHelper).isValid());

        //no id
        assertFalse(new UcteCnecElementHelper(null, networkHelper).isValid());
    }

    @Test
    public void testInvalidBranchIdConstructor() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        /*
         The presence of 'NODE1ID_ NODE2_ID SUFFIX' in the invalid reason message is checked, as the messages
         related to problems in ids should contain id
         */

        // wrong size of node id or suffix
        UcteCnecElementHelper branchHelper = new UcteCnecElementHelper("7_CHARA 7_CHARA E_NAME", networkHelper);
        assertFalse(branchHelper.isValid());
        assertTrue(branchHelper.getInvalidReason().contains("NODE1ID_ NODE2_ID SUFFIX"));

        branchHelper = new UcteCnecElementHelper("9_CHARACT 9_CHARACT E_NAME", networkHelper);
        assertFalse(branchHelper.isValid());
        assertTrue(branchHelper.getInvalidReason().contains("NODE1ID_ NODE2_ID SUFFIX"));

        branchHelper = new UcteCnecElementHelper("8_CHARAC 8_CHARAC ELEMENT_NAME_WITH_MORE_THAN_12_CHARAC", networkHelper);
        assertFalse(branchHelper.isValid());
        assertTrue(branchHelper.getInvalidReason().contains("NODE1ID_ NODE2_ID SUFFIX"));

        // no suffix
        branchHelper = new UcteCnecElementHelper("8_CHARAC 8_CHARAC", networkHelper);
        assertFalse(branchHelper.isValid());
        assertTrue(branchHelper.getInvalidReason().contains("NODE1ID_ NODE2_ID SUFFIX"));

        // no blank space between ids and suffix
        branchHelper = new UcteCnecElementHelper("8_CHARAC_8_CHARAC_1", networkHelper);
        assertFalse(branchHelper.isValid());
        assertTrue(branchHelper.getInvalidReason().contains("NODE1ID_ NODE2_ID SUFFIX"));
    }

    @Test
    public void testValidBranchesWithWildCard() {
        setUp("TestCase_severalVoltageLevels_Xnodes_8characters.uct");

        // internal branch with order code, from/to same as network
        UcteCnecElementHelper branchHelper = new UcteCnecElementHelper("BBE1AA1*", "BBE2AA1*", "1", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("BBE1AA11 BBE2AA11 1", branchHelper.getIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isHalfLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(5000., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(5000., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteCnecElementHelper("FFR3AA1*", "XBEFR11*", "1", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("FFR3AA11 XBEFR112 1 + XBEFR112 BBE2AA11 1", branchHelper.getIdInNetwork());

        branchHelper = new UcteCnecElementHelper("XDENL11*", "DDE2AA1*", "1", null, networkHelper);
        assertTrue(branchHelper.isValid());
        assertEquals("DDE2AA11 XDENL111 1 + NNL3AA11 XDENL111 1", branchHelper.getIdInNetwork());
    }

    @Test
    public void testInvalidBranchesWithWildCard() {
        setUp("TestCase_severalVoltageLevels_Xnodes_8characters.uct");

        // multiple matches
        UcteCnecElementHelper branchHelper = new UcteCnecElementHelper("DDE1AA1*", "DDE2AA1*", "1", null, networkHelper);
        assertFalse(branchHelper.isValid());
    }

    @Test
    public void testValidTransformerWithWildcardAmbiguity() {
        // In this case, from->to and to->from match, because of the ambiguity introduced by the wildcard:
        // "BBE2AA1*" -> "BBE2AA1*" "1" can match BBE2AA13 BBE2AA11 1 or BBE2AA11 BBE2AA13 1
        // Priority should be given to first match (as written in the network), although Powsybl has the opposite convention
        // PST should then be considered inverted, like nominal case
        // Same with order code 2
        setUp("TestCase_PstAmbiguity.uct");

        UcteCnecElementHelper pstHelper = new UcteCnecElementHelper("BBE2AA1*", "BBE2AA1*", "1", null, networkHelper);
        assertTrue(pstHelper.isValid());
        assertEquals("BBE2AA1* BBE2AA1* 1", pstHelper.getUcteId());
        assertEquals("BBE2AA13 BBE2AA11 1", pstHelper.getIdInNetwork());
        assertTrue(pstHelper.isInvertedInNetwork());

        pstHelper = new UcteCnecElementHelper("BBE2AA1", "BBE2AA1", "2", null, networkHelper);
        assertTrue(pstHelper.isValid());
        assertEquals("BBE2AA1  BBE2AA1  2", pstHelper.getUcteId());
        assertEquals("BBE2AA12 BBE2AA11 2", pstHelper.getIdInNetwork());
        assertTrue(pstHelper.isInvertedInNetwork());
    }
}
