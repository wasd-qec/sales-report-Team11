package edu.itc.salesreport.ui;

import java.time.YearMonth;

/** Commands accepted by the report controller. */
public sealed interface ReportCommand
        permits ReportCommand.LoadMonth, ReportCommand.SelectBranch {
    record LoadMonth(YearMonth month) implements ReportCommand {}

    record SelectBranch(String branch) implements ReportCommand {}
}
