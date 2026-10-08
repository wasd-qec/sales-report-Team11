package edu.itc.salesreport;

import edu.itc.salesreport.ingest.ConsoleProgress;
import edu.itc.salesreport.ingest.MonthLoader;
import edu.itc.salesreport.model.MonthlyReport;
import edu.itc.salesreport.render.CsvReportRenderer;
import edu.itc.salesreport.render.HtmlReportRenderer;
import edu.itc.salesreport.render.ReportRenderer;
import edu.itc.salesreport.render.TextReportRenderer;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;

/**
 * Main application entry point for running the Monthly Sales Report pipeline.
 * Usage:
 *   java -cp ... edu.itc.salesreport.App [month: YYYY-MM] [format: text|html|csv|all] [dataDir]
 */
public class App {

    public static void main(String[] args) {
        YearMonth month = args.length > 0 ? YearMonth.parse(args[0]) : YearMonth.parse("2026-09");
        String format = args.length > 1 ? args[1].toLowerCase() : "text";
        Path dataDir = args.length > 2 ? Path.of(args[2]) : Path.of("data", month.toString());

        System.out.println("===================================================================");
        System.out.printf(" Starting Sales Ingestion & Report Generator for %s%n", month);
        System.out.printf(" Data source directory: %s%n", dataDir.toAbsolutePath());
        System.out.println("===================================================================");

        if (!Files.exists(dataDir)) {
            System.err.printf("Error: Data directory does not exist: %s%n", dataDir);
            System.err.println("Generate sample data first using: java tools/SampleData.java");
            System.exit(1);
        }

        MonthLoader loader = new MonthLoader();
        loader.addListener(new ConsoleProgress());

        try {
            MonthlyReport report = loader.loadMonth(month, dataDir);

            switch (format) {
                case "html" -> {
                    Path htmlOut = Path.of("monthly-report-" + month + ".html");
                    try (var writer = new FileWriter(htmlOut.toFile())) {
                        new HtmlReportRenderer().render(report, writer);
                    }
                    System.out.printf("HTML report written to: %s%n", htmlOut.toAbsolutePath());
                }
                case "csv" -> {
                    Path csvOut = Path.of("monthly-report-" + month + ".csv");
                    try (var writer = new FileWriter(csvOut.toFile())) {
                        new CsvReportRenderer().render(report, writer);
                    }
                    System.out.printf("CSV summary report written to: %s%n", csvOut.toAbsolutePath());
                }
                case "all" -> {
                    // Render Text to console
                    new TextReportRenderer().render(report, new java.io.PrintWriter(System.out));

                    Path htmlOut = Path.of("monthly-report-" + month + ".html");
                    try (var writer = new FileWriter(htmlOut.toFile())) {
                        new HtmlReportRenderer().render(report, writer);
                    }
                    Path csvOut = Path.of("monthly-report-" + month + ".csv");
                    try (var writer = new FileWriter(csvOut.toFile())) {
                        new CsvReportRenderer().render(report, writer);
                    }
                    System.out.printf("All reports generated: %s and %s%n", htmlOut, csvOut);
                }
                case "text" -> {
                    ReportRenderer renderer = new TextReportRenderer();
                    renderer.render(report, new java.io.PrintWriter(System.out));
                }
                default -> {
                    System.err.printf("Unknown format '%s', defaulting to text.%n", format);
                    new TextReportRenderer().render(report, new java.io.PrintWriter(System.out));
                }
            }
        } catch (IOException e) {
            System.err.printf("Failed to generate report: %s%n", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
