package com.back.domain.report.report.handler;

import com.back.domain.report.report.common.ReportType;

public interface ReportValidator {
    boolean validateTargetId(ReportType reportType, Long targetId);
}
