package edu.itc.salesreport.ui.fx;

import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ui.ReportController;
import edu.itc.salesreport.ui.SalesModel;
import java.nio.file.Path;
import java.time.Clock;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class ReportApp extends Application {
    @Override
    public void start(Stage stage) {
        var model = new SalesModel(new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
        var view = new ReportView(model);
        var controller = new ReportController(
                model, Path.of("data"), Clock.systemDefaultZone(), view);
        view.bind(controller);
        stage.setTitle("Angkor Mart sales report");
        stage.setScene(new Scene(view.root(), 1100, 650));
        stage.show();
        controller.monthEntered("2026-09");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
