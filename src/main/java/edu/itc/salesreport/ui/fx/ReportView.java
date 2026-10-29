package edu.itc.salesreport.ui.fx;

import edu.itc.salesreport.model.SaleTransaction;
import edu.itc.salesreport.ui.ErrorDisplay;
import edu.itc.salesreport.ui.SalesEvent;
import edu.itc.salesreport.ui.SalesModel;
import edu.itc.salesreport.ui.SalesModelListener;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** JavaFX adapter that renders the toolkit-free sales model. */
public final class ReportView implements SalesModelListener, ErrorDisplay {
    private final SalesModel model;
    private final BorderPane root = new BorderPane();
    private final TextField monthField = new TextField("2026-09");
    private final ComboBox<String> branchBox = new ComboBox<>();
    private final TableView<SaleTransaction> table = new TableView<>();
    private final BarChart<String, Number> chart =
            new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final Label status = new Label("No month loaded");
    private final Label error = new Label();

    public ReportView(SalesModel model) {
        this.model = model;
        branchBox.getItems().setAll(SalesModel.ALL, "PNH", "REP", "BTB");
        branchBox.setValue(SalesModel.ALL);
        table.getColumns().setAll(List.of(
                column("Date", t -> t.date().toString()),
                column("Receipt", SaleTransaction::receiptNo),
                column("Product", SaleTransaction::productName),
                column("Category", SaleTransaction::category),
                column("Qty", t -> Integer.toString(t.quantity())),
                column("Revenue", t -> t.revenue().toString())));
        chart.setTitle("Revenue by category (USD)");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        error.setStyle("-fx-text-fill: firebrick;");
        root.setTop(new HBox(8, new Label("Month:"), monthField,
                new Label("Branch:"), branchBox));
        root.setCenter(new SplitPane(table, chart));
        root.setBottom(new VBox(4, status, error));
        model.addListener(this);
    }

    public Parent root() {
        return root;
    }

    /** Exposes the month input for the controller binding introduced in Task 3. */
    public TextField monthField() {
        return monthField;
    }

    /** Exposes the branch input for the controller binding introduced in Task 3. */
    public ComboBox<String> branchBox() {
        return branchBox;
    }

    @Override
    public void modelChanged(SalesEvent event) {
        branchBox.setValue(model.branch());
        table.getItems().setAll(model.visibleTransactions());
        var series = new XYChart.Series<String, Number>();
        model.summary().revenueByCategory().forEach((category, revenue) ->
                series.getData().add(new XYChart.Data<>(category, revenue)));
        chart.getData().setAll(List.of(series));
        status.setText(switch (event) {
            case SalesEvent.MonthLoaded(var month, var rows, var rejected) ->
                    "%s: %d rows, %d rejected · revenue USD %s · receipts %d"
                            .formatted(month, rows, rejected,
                                    model.summary().revenue(), model.summary().receipts());
            case SalesEvent.BranchSelected(var branch) ->
                    "%s: revenue USD %s · receipts %d"
                            .formatted(branch, model.summary().revenue(),
                                    model.summary().receipts());
        });
    }

    @Override
    public void showError(String message) {
        error.setText(message);
    }

    @Override
    public void clearError() {
        error.setText("");
    }

    private static TableColumn<SaleTransaction, String> column(
            String title, Function<SaleTransaction, String> value) {
        var column = new TableColumn<SaleTransaction, String>(title);
        column.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }
}
