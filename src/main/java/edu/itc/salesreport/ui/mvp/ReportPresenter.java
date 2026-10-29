package edu.itc.salesreport.ui.mvp;

import edu.itc.salesreport.model.SaleTransaction;
import edu.itc.salesreport.ui.InputRules;
import edu.itc.salesreport.ui.SalesEvent;
import edu.itc.salesreport.ui.SalesModel;
import edu.itc.salesreport.ui.SalesModelListener;
import edu.itc.salesreport.ui.Validation;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Locale;

/** MVP presenter: handles input and pushes formatted data. */
public final class ReportPresenter implements SalesModelListener {
    private final SalesModel model;
    private final ReportScreen screen;
    private final Path dataDir;
    private final Clock clock;

    public ReportPresenter(SalesModel model, ReportScreen screen, Path dataDir, Clock clock) {
        this.model = model;
        this.screen = screen;
        this.dataDir = dataDir;
        this.clock = clock;
        model.addListener(this);
    }

    public void monthEntered(String text) {
        switch (InputRules.month(text, clock)) {
            case Validation.Valid(var month) -> {
                try {
                    model.load(dataDir, month);
                    screen.showError("");
                } catch (NoSuchFileException e) {
                    screen.showError("No sales data for " + month);
                } catch (IOException e) {
                    screen.showError("Could not read the sales files: " + e);
                }
            }
            case Validation.Invalid(var message) -> screen.showError(message);
        }
    }

    public void branchChosen(String text) {
        switch (InputRules.branch(text)) {
            case Validation.Valid(var branch) -> {
                model.selectBranch(branch);
                screen.showError("");
            }
            case Validation.Invalid(var message) -> screen.showError(message);
        }
    }

    @Override
    public void modelChanged(SalesEvent event) {
        var rows = model.visibleTransactions();
        var summary = model.summary();
        screen.showRows(rows.stream().map(ReportPresenter::toRow).toList());
        screen.showDiscounts(rows.stream().map(t -> money(t.discount())).toList());
        screen.showCategoryRevenue(summary.revenueByCategory());
        screen.showStatus("%s · %d receipts · revenue USD %s · basket USD %s"
                .formatted(model.branch(), summary.receipts(), money(summary.revenue()),
                        money(summary.averageBasket())));
        screen.showError("");
    }

    private static RowItem toRow(SaleTransaction transaction) {
        return new RowItem(transaction.date().toString(), transaction.receiptNo(),
                transaction.productName(), transaction.category(),
                Integer.toString(transaction.quantity()), money(transaction.revenue()));
    }

    static String money(BigDecimal amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }
}
