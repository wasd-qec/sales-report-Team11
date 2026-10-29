package edu.itc.salesreport.ui.fx;

import edu.itc.salesreport.ui.mvp.ReportPresenter;
import edu.itc.salesreport.ui.mvp.ReportScreen;
import edu.itc.salesreport.ui.mvp.RowItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class FxReportScreen implements ReportScreen {
    private final BorderPane root = new BorderPane();
    private final TextField monthField = new TextField("2026-09");
    private final ComboBox<String> branchBox = new ComboBox<>();
    private final TableView<RowItem> table = new TableView<>();
    private final BarChart<String, Number> chart =
            new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final Label status = new Label("No month loaded");
    private final Label error = new Label();
    private List<String> discounts = List.of();

    public FxReportScreen() {
        branchBox.getItems().setAll("ALL", "PNH", "REP", "BTB");
        branchBox.setValue("ALL");
        table.getColumns().setAll(
                column("Date", RowItem::date), column("Receipt", RowItem::receipt),
                column("Product", RowItem::product), column("Category", RowItem::category),
                column("Qty", RowItem::quantity), column("Revenue", RowItem::revenue),
                column("Discount", this::discount));
        chart.setTitle("Revenue by category (USD)");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        error.setStyle("-fx-text-fill: firebrick;");
        root.setTop(new HBox(8, new Label("Month:"), monthField,
                new Label("Branch:"), branchBox));
        root.setCenter(new VBox(table, chart));
        root.setBottom(new VBox(4, status, error));
    }

    public Parent root() { return root; }
    public TextField monthField() { return monthField; }
    public ComboBox<String> branchBox() { return branchBox; }

    public void attach(ReportPresenter presenter) {
        monthField.setOnAction(event -> presenter.monthEntered(monthField.getText()));
        branchBox.setOnAction(event -> presenter.branchChosen(branchBox.getValue()));
    }

    @Override public void showRows(List<RowItem> rows) { table.getItems().setAll(rows); }
    @Override public void showDiscounts(List<String> values) { discounts = List.copyOf(values); }
    @Override public void showCategoryRevenue(Map<String, BigDecimal> revenue) {
        var series = new XYChart.Series<String, Number>();
        revenue.forEach((category, amount) -> series.getData().add(
                new XYChart.Data<>(category, amount)));
        chart.getData().setAll(List.of(series));
    }
    @Override public void showStatus(String text) { status.setText(text); }
    @Override public void showError(String message) { error.setText(message); }

    private String discount(RowItem row) {
        int index = table.getItems().indexOf(row);
        return index >= 0 && index < discounts.size() ? discounts.get(index) : "";
    }

    private static TableColumn<RowItem, String> column(
            String title, java.util.function.Function<RowItem, String> value) {
        var column = new TableColumn<RowItem, String>(title);
        column.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }
}
