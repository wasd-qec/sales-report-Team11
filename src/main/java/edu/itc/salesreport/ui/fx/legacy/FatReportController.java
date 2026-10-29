package edu.itc.salesreport.ui.fx.legacy;

import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ui.ErrorDisplay;
import edu.itc.salesreport.ui.ReportController;
import edu.itc.salesreport.ui.SalesModel;
import java.nio.file.Path;
import java.time.Clock;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Compatibility adapter for the pre-MVC screen; all work is delegated to MVC. */
public class FatReportController {
    private final TextField monthField;
    private final ComboBox<String> branchBox;
    private final ReportController controller;

    public FatReportController(TextField monthField,
            ComboBox<String> branchBox, Label status) {
        this(monthField, branchBox, new ReportController(
                new SalesModel(new CsvMonthReader(new CsvTransactionParser()),
                        new DefaultSalesAggregator()),
                Path.of("data"), Clock.systemDefaultZone(),
                new StatusErrors(status)));
    }

    public FatReportController(TextField monthField,
            ComboBox<String> branchBox, ReportController controller) {
        this.monthField = monthField;
        this.branchBox = branchBox;
        this.controller = controller;
    }

    public void onLoad() {
        controller.branchChosen(branchBox.getValue());
        controller.monthEntered(monthField.getText());
    }

    private static final class StatusErrors implements ErrorDisplay {
        private final Label status;

        private StatusErrors(Label status) {
            this.status = status;
        }

        @Override
        public void showError(String message) {
            status.setText(message);
        }

        @Override
        public void clearError() {
            status.setText("");
        }
    }
}
