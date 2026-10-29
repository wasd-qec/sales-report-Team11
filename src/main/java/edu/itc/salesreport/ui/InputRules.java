package edu.itc.salesreport.ui;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Set;

/** Pure input rules: no UI toolkit, I/O, or implicit system time. */
public final class InputRules {
    private static final Set<String> BRANCHES =
            Set.of(SalesModel.ALL, "PNH", "REP", "BTB");

    private InputRules() {}

    public static Validation<YearMonth> month(String text, Clock clock) {
        if (text == null || text.isBlank()) {
            return new Validation.Invalid<>("Enter a month as yyyy-MM, e.g. 2026-09");
        }
        String input = text.strip();
        final YearMonth month;
        try {
            month = YearMonth.parse(input);
        } catch (DateTimeException e) {
            return new Validation.Invalid<>(
                    "'%s' is not a month in the form yyyy-MM".formatted(input));
        }
        if (month.isAfter(YearMonth.now(clock))) {
            return new Validation.Invalid<>(month + " is in the future");
        }
        return new Validation.Valid<>(month);
    }

    public static Validation<String> branch(String text) {
        String branch = text == null ? "" : text.strip().toUpperCase(Locale.ROOT);
        if (!BRANCHES.contains(branch)) {
            return new Validation.Invalid<>(
                    "Unknown branch '%s': use ALL, PNH, REP or BTB".formatted(branch));
        }
        return new Validation.Valid<>(branch);
    }
}
