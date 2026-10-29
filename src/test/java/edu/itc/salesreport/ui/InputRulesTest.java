package edu.itc.salesreport.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class InputRulesTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-27T03:00:00Z"), ZoneId.of("Asia/Phnom_Penh"));

    @Test
    void acceptsTrimmedMonth() {
        assertEquals(new Validation.Valid<>(YearMonth.of(2026, 9)),
                InputRules.month(" 2026-09 ", CLOCK));
    }

    @Test
    void rejectsBlankMonth() {
        assertEquals(new Validation.Invalid<>("Enter a month as yyyy-MM, e.g. 2026-09"),
                InputRules.month("", CLOCK));
        assertEquals(new Validation.Invalid<>("Enter a month as yyyy-MM, e.g. 2026-09"),
                InputRules.month(" ", CLOCK));
    }

    @Test
    void rejectsMalformedMonths() {
        assertEquals(new Validation.Invalid<>("'2026/09' is not a month in the form yyyy-MM"),
                InputRules.month("2026/09", CLOCK));
        assertEquals(new Validation.Invalid<>("'2026-9' is not a month in the form yyyy-MM"),
                InputRules.month("2026-9", CLOCK));
        assertEquals(new Validation.Invalid<>("'Sept' is not a month in the form yyyy-MM"),
                InputRules.month("Sept", CLOCK));
        assertEquals(new Validation.Invalid<>("2026-10 is in the future"),
                InputRules.month("2026-10", CLOCK));
    }

    @Test
    void rejectsFutureMonthWithExactMessage() {
        assertEquals(new Validation.Invalid<>("2099-01 is in the future"),
                InputRules.month("2099-01", CLOCK));
    }

    @Test
    void normalisesKnownBranches() {
        assertEquals(new Validation.Valid<>("REP"), InputRules.branch(" rep "));
        assertEquals(new Validation.Valid<>("ALL"), InputRules.branch("all"));
    }

    @Test
    void rejectsUnknownBranchWithExactMessage() {
        var result = assertInstanceOf(Validation.Invalid.class, InputRules.branch(" xx "));
        assertEquals("Unknown branch 'XX': use ALL, PNH, REP or BTB", result.message());
    }
}
