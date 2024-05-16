package com.powsybl.openrao.data.cracimpl;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

public final class CracImplReports {
    private CracImplReports() {
        // utility class
    }

    public static ReportNode reportNewRaUsageLimitsAtInstant(ReportNode reportNode, String instantName) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportNewRaUsageLimits", "New RA usage limit at instant \"${instantName}\".")
            .withUntypedValue("instantName", instantName)
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }
}
