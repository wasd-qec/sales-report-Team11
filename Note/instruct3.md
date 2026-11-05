Software
 Engineering Concepts · Chapter 03


 

# Lab-03: Model View ViewModel (MVVM) design pattern


 

You refactor the Lab-02 Angkor Mart report viewer from MVC to MVVM. You design a
 `ReportViewModel` with properties, commands and validation, bind a JavaFX view to it
 and delete the controller logic, load months in a background `Task` with progress and
 cancellation (and measure that the UI no longer freezes), and prove the design with at least
 eight ViewModel unit tests that never start JavaFX. The challenge adds a second view, a compact
 dashboard, bound to the same ViewModel.


 
 JavaFX 25.0.4 
 JDK 25 · Maven 3.9 
 JUnit 6.1.3 · JaCoCo 0.8.15 
 ≈ 6 hours · pairs 
 5 tasks + 1 challenge 
 
 

 
 **
 **How to work through this lab.** Do the tasks in order; each builds on the
 previous. Read the *Goal*, follow the *Steps*, compare with the *Expected
 output*, and tick the *Acceptance checklist* (saved in your browser). Open a hint
 only when stuck for more than 10 minutes. Work in pairs and split the work so that **each
 member authors commits** (for example one writes the ViewModel and its tests, the
 other the view and the background loading); the Git history is part of the grade. 
 

 
 
 
 0 
 

## Setup


 30 min 
 
 
 
 
- Verify the tools and create the lab branch from your Lab-02 state:
 

```bash
java -version          # openjdk 25.x
mvn -v                 # Apache Maven 3.9.16
git switch main && git pull
git tag lab-02-final   # skip if your Lab-02 is already tagged
git switch -c lab-03
mkdir -p lab03/screenshots
```


 
 
- Read the case-study brief. It contains every fact this lab needs.
 
 ** Case-study
 brief: Angkor Mart monthly sales report viewer 
 
 
 
- **Company**: Angkor Mart, a retail chain with three
 branches: `PNH` (Phnom Penh), `REP` (Siem Reap),
 `BTB` (Battambang). Head office needs a monthly sales report
 per branch and for the chain. Currency USD.
 
- **Data**: One CSV per branch and day:
 `data/ / - .csv`,
 UTF-8, comma separated, header
 `branch,date,receipt_no,sku,product_name,category,quantity,unit_price,discount,payment_method`.
 Example row:
 `PNH,2026-09-01,PNH-000123,SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,KHQR`.
 Categories `Grocery`, `Beverages`,
 `Household`, `Personal Care`,
 `Electronics`, `Stationery`; payment methods
 `CASH`, `CARD`, `KHQR`.
 
- **Rules (model)**: Line revenue =
 `quantity × unit_price − discount` with
 `BigDecimal`, scale 2, `RoundingMode.HALF_UP`. A
 branch summary has revenue, discounts, number of receipts, average
 basket (revenue / receipts), revenue by category, top 10 products and
 revenue by payment method.
 
- **Java names**: Maven `edu.itc:sales-report`, Java
 25. Packages `edu.itc.salesreport.model`,
 `.ingest`, `.engine`, `.ui`. Records
 `SaleTransaction` (with `revenue()`),
 `ProductTotal`, `BranchSummary`,
 `MonthlyReport`; `enum PaymentMethod`; interfaces
 `TransactionParser`, `ReportEngine`.
 
- **The screen**: Month selector (last 12 months, also typed as
 `2026-08`), one tick box per branch (all ticked at start),
 *Generate* and *Cancel* buttons, a progress bar, a status
 line, a validation line, a load-error line, a table (Branch, Revenue,
 Discounts, Receipts, Avg basket), a product filter with the top-products
 list of the loaded branches, and the chain revenue as
 `USD 12,345.60`.
 
- **Screen rules**: A month is required and must not be after the
 current month ("Month 2026-10 is in the future."). At least one branch
 must be ticked ("Select at least one branch."). *Generate* is
 disabled while the input is invalid or a load runs; *Cancel* is
 enabled only while a load runs. Cancel and load errors keep the
 previously loaded data. The product filter is case-insensitive on name
 and SKU.
 
- **Test facts**: "Today" in tests is **2026-09-27, zone
 Asia/Phnom_Penh**. Measurement data: August 2026, 5,000 rows
 per branch and day, seed 42 (465,000 rows).
 
 
 
 
 
- **Inputs from earlier labs.** If one of them is incomplete, use the
 fallback and carry on; do not stop to repair Lab-02.
 
 

| Input | Where | What you need from it | Fallback if missing |
| --- | --- | --- | --- |
| Maven project `sales-report` | team repository, tag `lab-02-final` | Builds with `mvn -q verify` on JDK 25 | Use the `pom.xml` of step 4. |
| Model classes | Lab-02, `edu.itc.salesreport.model` and your CSV loader | The records and a way to load one branch for one month | Fallback starter A: model records,
 `CsvTransactionParser`, `SimpleReportEngine`. |
| Lab-02 MVC viewer | Lab-02, `edu.itc.salesreport.ui` | The controller whose logic you move into the ViewModel, and the
 baseline for Task 5 | Fallback starter B: `MvcReportViewerApp` with its
 `ReportController`. |
| Sample data | Lab-02, `data/` | One complete month for all three branches | Generate it with `SalesDataGenerator` (starter A),
 command in Task 4. |
| Lab-02 notes | `lab02/README.md` | Commit or tag of the viewer you refactor | Write "fallback starter B" in `lab03/README.md` instead. |


 
 
 
- Update `pom.xml` (versions resolved from Maven Central and built with JDK
 25.0.3 and Maven 3.9.16). `app.main` lets you run other main classes with
 `mvn javafx:run -Dapp.main=…`.
 

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>edu.itc</groupId>
  <artifactId>sales-report</artifactId>
  <version>0.3.0-SNAPSHOT</version>

  <properties>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <javafx.version>25.0.4</javafx.version>
    <app.main>edu.itc.salesreport.ui.ReportViewerApp</app.main>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.junit</groupId>
        <artifactId>junit-bom</artifactId>
        <version>6.1.3</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <dependency>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-controls</artifactId>
      <version>${javafx.version}</version>
    </dependency>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.15.0</version>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.5.6</version>
      </plugin>
      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.15</version>
        <executions>
          <execution><goals><goal>prepare-agent</goal></goals></execution>
          <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-maven-plugin</artifactId>
        <version>0.0.8</version>
        <configuration>
          <mainClass>${app.main}</mainClass>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```


 
 
- Fallback starters (only if your Lab-02 is incomplete). Copy the files into
 `src/main/java/edu/itc/salesreport/…` as their `package` line
 says.
 [** Fallback starter A: model, parser,
 engine, data generator](#fallbackA) 
 
 

```java
package edu.itc.salesreport.model;

public enum PaymentMethod { CASH, CARD, KHQR }
package edu.itc.salesreport.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record SaleTransaction(String branch, LocalDate date, String receiptNo,
        String sku, String productName, String category, int quantity,
        BigDecimal unitPrice, BigDecimal discount,
        PaymentMethod paymentMethod) {

    /** quantity x unit price - discount, scale 2, HALF_UP. */
    public BigDecimal revenue() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity))
                .subtract(discount)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
package edu.itc.salesreport.model;

import java.math.BigDecimal;

public record ProductTotal(String sku, String productName,
        BigDecimal revenue, long quantity) { }
package edu.itc.salesreport.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public record BranchSummary(String branch, BigDecimal revenue,
        BigDecimal discounts, long receipts,
        Map<String, BigDecimal> revenueByCategory,
        List<ProductTotal> topProducts,
        Map<PaymentMethod, BigDecimal> revenueByPayment) {

    /** Revenue per receipt, scale 2; zero when there are no receipts. */
    public BigDecimal averageBasket() {
        return receipts == 0 ? BigDecimal.ZERO.setScale(2)
                : revenue.divide(BigDecimal.valueOf(receipts), 2,
                        RoundingMode.HALF_UP);
    }
}
package edu.itc.salesreport.model;

import java.time.YearMonth;
import java.util.List;

public record MonthlyReport(YearMonth month, List<BranchSummary> branches,
        BranchSummary chain, List<String> anomalies) { }
package edu.itc.salesreport.model;

public class InvalidRowException extends Exception {
    public InvalidRowException(String message) { super(message); }
}
package edu.itc.salesreport.model;

public interface TransactionParser {
    SaleTransaction parse(String line) throws InvalidRowException;
}
package edu.itc.salesreport.model;

import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;

public interface ReportEngine {
    MonthlyReport generate(YearMonth month, Path dataDir) throws IOException;
}
```


 

```java
package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.InvalidRowException;
import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.SaleTransaction;
import edu.itc.salesreport.model.TransactionParser;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Parses one data row of the canonical 10-column branch CSV. */
public final class CsvTransactionParser implements TransactionParser {

    @Override
    public SaleTransaction parse(String line) throws InvalidRowException {
        String[] f = line.split(",", -1);
        if (f.length != 10) {
            throw new InvalidRowException("expected 10 fields: " + line);
        }
        try {
            return new SaleTransaction(f[0], LocalDate.parse(f[1]), f[2],
                    f[3], f[4], f[5], Integer.parseInt(f[6]),
                    new BigDecimal(f[7]), new BigDecimal(f[8]),
                    PaymentMethod.valueOf(f[9]));
        } catch (RuntimeException e) {
            throw new InvalidRowException(e.getMessage() + ": " + line);
        }
    }
}
```


 

```java
package edu.itc.salesreport.ingest;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Seeded generator of canonical branch CSV files (small sample version). */
public final class SalesDataGenerator {

    private record Product(String sku, String name, String category,
            String price) { }

    private static final List<Product> PRODUCTS = List.of(
        new Product("SKU-1001", "Jasmine Rice 5kg", "Grocery", "6.50"),
        new Product("SKU-1002", "Fish Sauce 700ml", "Grocery", "1.80"),
        new Product("SKU-2001", "Angkor Beer 330ml", "Beverages", "0.90"),
        new Product("SKU-2002", "Iced Coffee 250ml", "Beverages", "1.20"),
        new Product("SKU-3001", "Laundry Powder 1kg", "Household", "3.40"),
        new Product("SKU-4001", "Shampoo 400ml", "Personal Care", "4.10"),
        new Product("SKU-5001", "USB-C Cable 1m", "Electronics", "5.00"),
        new Product("SKU-6001", "Notebook A5", "Stationery", "0.75"));
    private static final String[] BRANCHES = { "PNH", "REP", "BTB" };
    private static final String[] PAYMENTS = { "CASH", "CARD", "KHQR" };
    private static final String HEADER = "branch,date,receipt_no,sku,"
        + "product_name,category,quantity,unit_price,discount,payment_method";

    /** Arguments: month rowsPerDay seed dataDir, e.g. 2026-08 5000 42 data */
    public static void main(String[] args) throws IOException {
        new SalesDataGenerator().generate(YearMonth.parse(args[0]),
                Integer.parseInt(args[1]), Long.parseLong(args[2]),
                Path.of(args[3]));
    }

    public void generate(YearMonth month, int rowsPerDay, long seed,
            Path dataDir) throws IOException {
        Random rnd = new Random(seed);
        Path dir = dataDir.resolve(month.toString());
        Files.createDirectories(dir);
        for (String branch : BRANCHES) {
            int receipt = 0;
            for (int d = 1; d <= month.lengthOfMonth(); d++) {
                LocalDate date = month.atDay(d);
                Path file = dir.resolve(branch + "-" + date + ".csv");
                try (var out = new PrintWriter(Files.newBufferedWriter(
                        file, StandardCharsets.UTF_8))) {
                    out.println(HEADER);
                    for (int r = 0; r < rowsPerDay; r++) {
                        if (r % 3 == 0) receipt++;
                        Product p = PRODUCTS.get(rnd.nextInt(PRODUCTS.size()));
                        int qty = 1 + rnd.nextInt(4);
                        String discount =
                                rnd.nextInt(10) == 0 ? "0.50" : "0.00";
                        out.println(String.format(Locale.ROOT,
                            "%s,%s,%s-%06d,%s,%s,%s,%d,%s,%s,%s", branch, date,
                            branch, receipt, p.sku(), p.name(), p.category(),
                            qty, p.price(), discount,
                            PAYMENTS[rnd.nextInt(PAYMENTS.length)]));
                    }
                }
            }
        }
    }
}
```


 

```java
package edu.itc.salesreport.engine;

import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.InvalidRowException;
import edu.itc.salesreport.model.MonthlyReport;
import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.ProductTotal;
import edu.itc.salesreport.model.ReportEngine;
import edu.itc.salesreport.model.SaleTransaction;
import edu.itc.salesreport.model.TransactionParser;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/** Sequential engine: reads one branch or all branches of a month. */
public final class SimpleReportEngine implements ReportEngine {

    public static final List<String> BRANCHES = List.of("PNH", "REP", "BTB");
    private final TransactionParser parser = new CsvTransactionParser();

    @Override
    public MonthlyReport generate(YearMonth month, Path dataDir)
            throws IOException {
        List<BranchSummary> branches = new ArrayList<>();
        List<SaleTransaction> all = new ArrayList<>();
        List<String> anomalies = new ArrayList<>();
        for (String b : BRANCHES) {
            List<SaleTransaction> rows = read(month, b, dataDir, anomalies);
            all.addAll(rows);
            branches.add(summarize(b, rows));
        }
        return new MonthlyReport(month, branches, summarize("ALL", all),
                anomalies);
    }

    /** Summary of one branch for one month. */
    public BranchSummary summarizeBranch(YearMonth month, String branch,
            Path dataDir) throws IOException {
        return summarize(branch,
                read(month, branch, dataDir, new ArrayList<>()));
    }

    private List<SaleTransaction> read(YearMonth month, String branch,
            Path dataDir, List<String> anomalies) throws IOException {
        Path dir = dataDir.resolve(month.toString());
        List<SaleTransaction> rows = new ArrayList<>();
        if (!Files.isDirectory(dir)) return rows;
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.getFileName().toString()
                    .startsWith(branch + "-")).sorted().toList();
        }
        for (Path f : files) {
            List<String> lines = Files.readAllLines(f);
            for (String line : lines.subList(1, lines.size())) {
                if (line.isBlank()) continue;
                try {
                    rows.add(parser.parse(line));
                } catch (InvalidRowException e) {
                    anomalies.add(f.getFileName() + ": " + e.getMessage());
                }
            }
        }
        return rows;
    }

    private static BranchSummary summarize(String branch,
            List<SaleTransaction> rows) {
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        BigDecimal discounts = BigDecimal.ZERO.setScale(2);
        Set<String> receipts = new HashSet<>();
        Map<String, BigDecimal> byCategory = new TreeMap<>();
        Map<PaymentMethod, BigDecimal> byPayment =
                new EnumMap<>(PaymentMethod.class);
        Map<String, ProductTotal> products = new TreeMap<>();
        for (SaleTransaction t : rows) {
            BigDecimal r = t.revenue();
            revenue = revenue.add(r);
            discounts = discounts.add(t.discount());
            receipts.add(t.receiptNo());
            byCategory.merge(t.category(), r, BigDecimal::add);
            byPayment.merge(t.paymentMethod(), r, BigDecimal::add);
            products.merge(t.sku(), new ProductTotal(t.sku(),
                    t.productName(), r, t.quantity()),
                (a, b) -> new ProductTotal(a.sku(), a.productName(),
                    a.revenue().add(b.revenue()),
                    a.quantity() + b.quantity()));
        }
        List<ProductTotal> top = products.values().stream()
                .sorted(Comparator.comparing(ProductTotal::revenue)
                        .reversed())
                .limit(10).toList();
        return new BranchSummary(branch, revenue, discounts,
                receipts.size(), byCategory, top, byPayment);
    }
}
```


 
 [** Fallback starter B: minimal Lab-02
 MVC viewer](#fallbackB) 
 
 

```java
package edu.itc.salesreport.ui.mvc;

import edu.itc.salesreport.engine.SimpleReportEngine;
import edu.itc.salesreport.model.BranchSummary;
import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/** Fallback Lab-02 viewer (MVC): the controller reads and writes controls. */
public class MvcReportViewerApp extends Application {

    @Override
    public void start(Stage stage) {
        ComboBox<YearMonth> month = new ComboBox<>();
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 12; i++) month.getItems().add(now.minusMonths(i));
        List<CheckBox> branches = new ArrayList<>();
        for (String b : SimpleReportEngine.BRANCHES) {
            CheckBox box = new CheckBox(b);
            box.setSelected(true);
            branches.add(box);
        }
        Button generate = new Button("Generate");
        Label status = new Label();
        TableView<BranchSummary> table = new TableView<>();
        TableColumn<BranchSummary, String> branch = new TableColumn<>("Branch");
        branch.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().branch()));
        TableColumn<BranchSummary, String> revenue =
                new TableColumn<>("Revenue");
        revenue.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().revenue().toPlainString()));
        table.getColumns().add(branch);
        table.getColumns().add(revenue);

        var controller = new ReportController(new SimpleReportEngine(),
                Path.of(getParameters().getNamed()
                        .getOrDefault("data", "data")),
                month, branches, status, table);
        generate.setOnAction(e -> controller.onGenerate());

        HBox top = new HBox(8, month, generate, status);
        top.getChildren().addAll(1, branches);
        stage.setScene(new Scene(new BorderPane(table, top, null, null, null),
                800, 500));
        stage.setTitle("Angkor Mart · Monthly report (MVC)");
        stage.show();
    }

    /** Controller: validation, loading and display mixed with controls. */
    static final class ReportController {
        private final SimpleReportEngine engine;
        private final Path dataDir;
        private final ComboBox<YearMonth> month;
        private final List<CheckBox> branches;
        private final Label status;
        private final TableView<BranchSummary> table;

        ReportController(SimpleReportEngine engine, Path dataDir,
                ComboBox<YearMonth> month, List<CheckBox> branches,
                Label status, TableView<BranchSummary> table) {
            this.engine = engine;
            this.dataDir = dataDir;
            this.month = month;
            this.branches = branches;
            this.status = status;
            this.table = table;
        }

        void onGenerate() {
            YearMonth m = month.getValue();
            if (m == null) {
                status.setText("Choose a month.");
                return;
            }
            List<BranchSummary> rows = new ArrayList<>();
            try {
                for (CheckBox box : branches) {
                    if (box.isSelected()) {
                        // runs on the FX thread: the window freezes
                        rows.add(engine.summarizeBranch(m, box.getText(),
                                dataDir));
                    }
                }
            } catch (IOException e) {
                status.setText("Load failed: " + e.getMessage());
                return;
            }
            table.getItems().setAll(rows);
            status.setText("Loaded " + rows.size() + " branch(es).");
        }
    }
}
```


 Run it with
 `mvn -q javafx:run -Dapp.main=edu.itc.salesreport.ui.mvc.MvcReportViewerApp`.
 Note how the controller holds five controls and loads on the FX thread: this is
 what you refactor. 
 
 
 
- Files this lab adds (code goes into the Maven project, written deliverables into
 `lab03/`):
 

```plaintext
src/main/java/edu/itc/salesreport/ui/
  Command.java  DelegateCommand.java        (Task 1)
  SummarySource.java  BackgroundJobs.java   (Task 1)
  ReportViewModel.java                      (Tasks 1-4)
  ReportView.java  ReportViewerApp.java     (Task 2)
  CallerThreadJobs.java                     (Task 2)
  FxBackgroundJobs.java                     (Task 4)
  FrameGapMonitor.java  LoadBenchmarkApp.java (Task 4)
  DashboardView.java  DashboardWindow.java  (challenge)
src/test/java/edu/itc/salesreport/ui/
  ManualJobs.java  ReportViewModelTest.java  ViewModelImportsTest.java
lab03/
  README.md                 (Task 5: record what changed)
  viewmodel-design.md       (Tasks 1 and 3: tables, class and state diagrams)
  measurements.md           (Task 4)
  mvc-vs-mvvm.md            (Task 5)
  platform-mapping.md       (challenge)
  screenshots/viewer.png  screenshots/dashboard.png
```


 
 
 
 

 
 
 
 1 
 

## Design the ViewModel


 Easy 
 45 min 
 
 
 

### Goal


 

Decide what the screen's state, rules and actions are, express them as properties, bindings
 and commands of `ReportViewModel`, and document the design with a class diagram,
 before touching the view.



 

### Steps


 
 
- Inventory your Lab-02 controller (or `ReportController` of starter B): in
 `lab03/viewmodel-design.md` write a table with one row per control it reads
 or writes and per `if` it contains, with columns *code*,
 *concern*, *new owner* (Model, ViewModel or View). Expect at least 8 rows.
 
 
- List the ViewModel members in three groups: **input** (`month`,
 `branchSelected(b)`, `productFilter`), **output**
 (`summaries`, `visibleProducts`, `loading`,
 `progress`, `status`, `loadError`,
 `monthChoices`), **computed** (`monthError`,
 `branchError`, `validationMessage`, `canGenerate`,
 `totalRevenue`, `totalRevenueText`), plus the commands
 `generate` and `cancel`. For each, note whether the view may write
 it.
 
- Add the ports `SummarySource` (the model seen by the ViewModel) and
 `BackgroundJobs` (threading), and `Command` with
 `DelegateCommand`, exactly as in the starter.
 
- Create `ReportViewModel` from the skeleton. Every output is exposed with a
 read-only type (`ReadOnly*Property`, `unmodifiableObservableList`,
 `StringExpression`, `ObservableBooleanValue`).
 
- Draw the class diagram (Mermaid `classDiagram`) in
 `lab03/viewmodel-design.md`: view classes, ViewModel, ports, model, test
 doubles; mark interfaces.
 
- Check the import rule and compile:
 `grep -n "^import javafx" src/main/java/edu/itc/salesreport/ui/ReportViewModel.java`
 shows only `javafx.beans` and `javafx.collections`;
 `mvn -q compile` passes.
 

 

### Starter code


 

```java
package edu.itc.salesreport.ui;

import javafx.beans.value.ObservableBooleanValue;

/** An action the view can trigger, plus an observable "can run now" flag. */
public interface Command {

    ObservableBooleanValue executableProperty();

    void execute();

    default boolean isExecutable() {
        return executableProperty().get();
    }
}
```


 

```java
package edu.itc.salesreport.ui;

import java.util.Objects;
import javafx.beans.value.ObservableBooleanValue;

/** Command that delegates to a Runnable and guards it with a condition. */
public final class DelegateCommand implements Command {

    private final Runnable action;
    private final ObservableBooleanValue executable;

    public DelegateCommand(Runnable action,
                           ObservableBooleanValue executable) {
        this.action = Objects.requireNonNull(action);
        this.executable = Objects.requireNonNull(executable);
    }

    @Override
    public ObservableBooleanValue executableProperty() {
        return executable;
    }

    @Override
    public void execute() {
        // ignore calls while disabled (key shortcuts, double clicks)
        if (executable.get()) {
            action.run();
        }
    }
}
```


 

```java
package edu.itc.salesreport.ui;

import edu.itc.salesreport.model.BranchSummary;
import java.time.YearMonth;

/** What the ViewModel needs from the model: one branch summary at a time. */
@FunctionalInterface
public interface SummarySource {
    BranchSummary load(YearMonth month, String branch) throws Exception;
}
```


 

```java
package edu.itc.salesreport.ui;

/**
 * Runs work off the UI thread. Every JobListener callback is delivered
 * on the UI thread, so the ViewModel may update its properties there.
 */
public interface BackgroundJobs {

    <T> JobHandle submit(Work<T> work, JobListener<T> listener);

    /** The work itself: runs on a worker thread, touches no property. */
    @FunctionalInterface
    interface Work<T> {
        T run(ProgressSink progress) throws Exception;
    }

    /** Given to the work so it can report progress and see cancellation. */
    interface ProgressSink {
        void update(long done, long total);
        boolean isCancelled();
    }

    /** Outcome callbacks, always called on the UI thread. */
    interface JobListener<T> {
        void progress(double fraction);
        void succeeded(T value);
        void failed(Throwable error);
        void cancelled();
    }

    @FunctionalInterface
    interface JobHandle {
        void cancel();
    }
}
```


 

```java
package edu.itc.salesreport.ui;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.ProductTotal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/** Task 1 skeleton: state, commands and ports. javafx.base only. */
public final class ReportViewModel {

    public static final List<String> BRANCHES = List.of("PNH", "REP", "BTB");

    private final SummarySource source;
    private final BackgroundJobs jobs;
    private final Clock clock;

    // ---- input state: two-way bound by the view
    private final ObjectProperty<YearMonth> month =
            new SimpleObjectProperty<>(this, "month");
    private final Map<String, BooleanProperty> branchSelected =
            new LinkedHashMap<>();
    private final StringProperty productFilter =
            new SimpleStringProperty(this, "productFilter", "");

    // ---- output state: one-way bound by the view
    private final ObservableList<YearMonth> monthChoices =
            FXCollections.observableArrayList();
    private final ObservableList<BranchSummary> summaries =
            FXCollections.observableArrayList();
    private final ObservableList<ProductTotal> products =
            FXCollections.observableArrayList();
    private final FilteredList<ProductTotal> visibleProducts =
            new FilteredList<>(products);
    private final ReadOnlyBooleanWrapper loading =
            new ReadOnlyBooleanWrapper(this, "loading", false);
    private final ReadOnlyDoubleWrapper progress =
            new ReadOnlyDoubleWrapper(this, "progress", 0);
    private final ReadOnlyStringWrapper status =
            new ReadOnlyStringWrapper(this, "status", "Choose a month.");
    private final ReadOnlyStringWrapper loadError =
            new ReadOnlyStringWrapper(this, "loadError", "");

    // ---- computed state (Task 3 replaces the placeholders)
    private final StringBinding monthError;
    private final StringBinding branchError;
    private final StringBinding validationMessage;
    private final BooleanBinding canGenerate;
    private final ObjectBinding<BigDecimal> totalRevenue;
    private final StringBinding totalRevenueText;

    private final Command generateCommand;
    private final Command cancelCommand;
    private BackgroundJobs.JobHandle running;

    public ReportViewModel(SummarySource source, BackgroundJobs jobs,
                           Clock clock) {
        this.source = Objects.requireNonNull(source);
        this.jobs = Objects.requireNonNull(jobs);
        this.clock = Objects.requireNonNull(clock);
        YearMonth now = YearMonth.now(clock);
        for (int i = 0; i < 12; i++) {
            monthChoices.add(now.minusMonths(i));
        }
        for (String b : BRANCHES) {
            branchSelected.put(b, new SimpleBooleanProperty(this, b, true));
        }
        // TODO Task 3: real rules (future month, empty branch selection)
        monthError = Bindings.createStringBinding(() -> "", month);
        branchError = Bindings.createStringBinding(() -> "");
        validationMessage = Bindings.createStringBinding(() -> "");
        canGenerate = month.isNotNull().and(loading.not());
        // TODO Task 2: sum of summaries' revenue, formatted "USD 1,234.50"
        totalRevenue = Bindings.createObjectBinding(() -> BigDecimal.ZERO);
        totalRevenueText = Bindings.createStringBinding(() -> "USD 0.00");
        // TODO Task 2: bind visibleProducts' predicate to productFilter

        generateCommand = new DelegateCommand(this::generate, canGenerate);
        cancelCommand = new DelegateCommand(this::cancel,
                loading.getReadOnlyProperty());
    }

    public List<String> selectedBranches() {
        return branchSelected.entrySet().stream()
                .filter(e -> e.getValue().get())
                .map(Map.Entry::getKey)
                .toList();
    }

    private void generate() {
        // TODO Task 2: snapshot month and selectedBranches(), set loading,
        // then jobs.submit(sink -> loadAll(...), listener); the listener
        // fills summaries/products on success and resets loading.
        // TODO Task 4: progress, cancelled(), failed() -> loadError
    }

    private void cancel() {
        // TODO Task 4: cancel the running job, if any
    }

    // ---- accessors: read-only types for everything the view must not set
    public ObjectProperty<YearMonth> monthProperty() { return month; }
    public BooleanProperty branchSelectedProperty(String branch) {
        return Objects.requireNonNull(branchSelected.get(branch), branch);
    }
    public StringProperty productFilterProperty() { return productFilter; }
    public ObservableList<YearMonth> monthChoices() {
        return FXCollections.unmodifiableObservableList(monthChoices);
    }
    public ObservableList<BranchSummary> summaries() {
        return FXCollections.unmodifiableObservableList(summaries);
    }
    public ObservableList<ProductTotal> visibleProducts() {
        return visibleProducts;
    }
    public ReadOnlyBooleanProperty loadingProperty() {
        return loading.getReadOnlyProperty();
    }
    public ReadOnlyDoubleProperty progressProperty() {
        return progress.getReadOnlyProperty();
    }
    public ReadOnlyStringProperty statusProperty() {
        return status.getReadOnlyProperty();
    }
    public ReadOnlyStringProperty loadErrorProperty() {
        return loadError.getReadOnlyProperty();
    }
    public StringExpression monthErrorProperty() { return monthError; }
    public StringExpression branchErrorProperty() { return branchError; }
    public StringExpression validationMessageProperty() {
        return validationMessage;
    }
    public ObservableBooleanValue canGenerateProperty() { return canGenerate; }
    public ObservableObjectValue<BigDecimal> totalRevenueProperty() {
        return totalRevenue;
    }
    public StringExpression totalRevenueTextProperty() {
        return totalRevenueText;
    }
    public Command generateCommand() { return generateCommand; }
    public Command cancelCommand() { return cancelCommand; }
}
```



 

### Expected output


 

Your class diagram should contain at least these elements:


 

```
classDiagram
  direction LR
  class ReportView
  class ReportViewModel {
    +monthProperty() ObjectProperty~YearMonth~
    +branchSelectedProperty(String) BooleanProperty
    +summaries() ObservableList~BranchSummary~
    +canGenerateProperty() ObservableBooleanValue
    +generateCommand() Command
    +cancelCommand() Command
  }
  class Command {
    <<interface>>
    +executableProperty() ObservableBooleanValue
    +execute()
  }
  class SummarySource {
    <<interface>>
    +load(YearMonth, String) BranchSummary
  }
  class BackgroundJobs {
    <<interface>>
    +submit(Work, JobListener) JobHandle
  }
  class DelegateCommand
  class BranchSummary
  ReportView --> ReportViewModel : binds
  ReportViewModel --> Command
  DelegateCommand ..|> Command
  ReportViewModel --> SummarySource
  ReportViewModel --> BackgroundJobs
  ReportViewModel ..> BranchSummary
```


 

```plaintext
$ grep -n "^import javafx" src/main/java/edu/itc/salesreport/ui/ReportViewModel.java
21:import javafx.beans.Observable;
22:import javafx.beans.binding.Bindings;
...
42:import javafx.collections.ObservableList;
43:import javafx.collections.transformation.FilteredList;
```



 

[** Show hints](#hint1)


 
 
 
 
- Ask of each piece of state: "would a unit test want to assert on it?" If yes, it
 belongs in the ViewModel, even formatting such as `USD 600.00`.
 
- One `BooleanProperty` per branch binds cleanly to a
 `CheckBox` with `bindBidirectional`; a single
 `ObservableList ` of selected branches does not.
 
- `ReadOnlyBooleanWrapper` gives you a writable property inside the
 class and `getReadOnlyProperty()` for the outside.
 
- Mermaid generics use tildes: `ObservableList~BranchSummary~`.
 
 
 

 

### Acceptance checklist


 
 * `lab03/viewmodel-design.md` has the
 controller inventory (at least 8 rows) with a new owner per row. 
 The Mermaid class diagram renders on GitHub and
 shows view, ViewModel, the three interfaces and the model. 
 `ReportViewModel` exposes outputs with
 read-only types only, and imports only `javafx.beans` and
 `javafx.collections`. 
 `mvn -q compile` passes with the new
 classes. 
 
 
 

 
 
 
 2 
 

## Bind the view, remove the controller logic


 Easy 
 60 min 
 
 
 

### Goal


 

Replace the Lab-02 controller by a view that only creates controls and binds them to the
 ViewModel, implement `generate` through the `BackgroundJobs` port
 (still on the calling thread for now), and leave the model package unchanged.



 

### Steps


 
 
- Complete `ReportView` (code, as in the starter, or FXML plus a thin view
 class whose `initialize()` only binds). Two-way: month combo, branch check
 boxes, product filter. One-way: progress bar, status, validation, error, total, table
 items, product list.
 
- Wire each button with exactly two statements:
 `setOnAction(e -> cmd.execute())` and
 `disableProperty().bind(Bindings.not(cmd.executableProperty()))`.
 
- Implement `generate()` in the ViewModel: snapshot `month` and
 `selectedBranches()` into locals, set `loading` and
 `status`, call
 `jobs.submit(sink -> loadAll(m, branches, sink), listener)`; on success
 fill `summaries` and the merged top products, set `status` to
 "Loaded 3 branch(es) for 2026-08." and reset `loading`.
 
- Implement `totalRevenue`, `totalRevenueText` ("USD 12,345.60",
 `NumberFormat` with `Locale.US` and 2 decimals) and the
 `visibleProducts` predicate bound to `productFilter`.
 
- Write the composition root `ReportViewerApp` with
 `CallerThreadJobs` and a `SummarySource` lambda over your Lab-02
 model.
 
- Delete the logic of the old controller (keep the Lab-02 viewer only under
 `ui/mvc` or at tag `lab-02-final` for Task 5). Prove the model is
 unchanged:
 `git diff --stat lab-02-final -- src/main/java/edu/itc/salesreport/model`
 prints nothing.
 
- Run `mvn -q javafx:run`, load 2026-08 for all branches and compare the three
 revenues with the Lab-02 viewer: they must be identical. Save
 `lab03/screenshots/viewer.png`.
 

 

### Starter code


 

```java
package edu.itc.salesreport.ui;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.ProductTotal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Task 2 skeleton: build controls and bind them. No logic here. */
public final class ReportView {

    private final BorderPane root = new BorderPane();

    public ReportView(ReportViewModel vm) {
        ComboBox<YearMonth> month = new ComboBox<>(vm.monthChoices());
        month.setEditable(true);
        month.setConverter(new YearMonthConverter());
        month.valueProperty().bindBidirectional(vm.monthProperty());

        HBox filters = new HBox(8, new Label("Month"), month);
        for (String b : ReportViewModel.BRANCHES) {
            CheckBox box = new CheckBox(b);
            // TODO two-way: box.selectedProperty() <-> branchSelected(b)
            filters.getChildren().add(box);
        }

        Button generate = new Button("Generate");
        generate.setOnAction(e -> vm.generateCommand().execute());
        generate.disableProperty().bind(
                Bindings.not(vm.generateCommand().executableProperty()));
        Button cancel = new Button("Cancel");
        // TODO same two lines for vm.cancelCommand()
        filters.getChildren().addAll(generate, cancel);

        ProgressBar bar = new ProgressBar();
        Label validation = new Label();
        Label status = new Label();
        Label error = new Label();
        Label total = new Label();
        // TODO one-way: bar.progress, bar.visible, validation.text,
        //      status.text, error.text, total.text

        TableView<BranchSummary> table = new TableView<>(vm.summaries());
        table.getColumns().add(column("Branch", BranchSummary::branch));
        table.getColumns().add(column("Revenue",
                s -> s.revenue().toPlainString()));
        // TODO columns Discounts, Receipts, Avg basket

        TextField search = new TextField();
        search.setPromptText("Filter products");
        // TODO two-way: search.text <-> vm.productFilter
        ListView<ProductTotal> productList =
                new ListView<>(vm.visibleProducts());

        VBox top = new VBox(6, filters, validation,
                new HBox(8, bar, status, error));
        top.setPadding(new Insets(10));
        root.setTop(top);
        root.setCenter(table);
        root.setRight(new VBox(6, new Label("Top products"), search,
                productList));
        root.setBottom(new HBox(8, new Label("Chain revenue:"), total));
    }

    private static TableColumn<BranchSummary, String> column(String title,
            Function<BranchSummary, String> value) {
        TableColumn<BranchSummary, String> c = new TableColumn<>(title);
        c.setCellValueFactory(
                cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return c;
    }

    public Parent root() {
        return root;
    }

    /** Text "2026-09" to YearMonth and back; null for unparsable input. */
    static final class YearMonthConverter extends StringConverter<YearMonth> {
        @Override
        public String toString(YearMonth m) {
            return m == null ? "" : m.toString();
        }

        @Override
        public YearMonth fromString(String s) {
            try {
                return s == null || s.isBlank() ? null
                        : YearMonth.parse(s.strip());
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }
}
```


 

```java
package edu.itc.salesreport.ui;

import edu.itc.salesreport.engine.SimpleReportEngine;
import java.nio.file.Path;
import java.time.Clock;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Composition root: the only class that knows every concrete type. */
public class ReportViewerApp extends Application {

    @Override
    public void start(Stage stage) {
        Path dataDir = Path.of(getParameters().getNamed()
                .getOrDefault("data", "data"));
        // adapt YOUR Lab-02 model here; the model code stays unchanged
        var engine = new SimpleReportEngine();
        SummarySource source =
                (month, branch) -> engine.summarizeBranch(month, branch,
                        dataDir);
        // Task 2: CallerThreadJobs (UI still freezes, like Lab-02)
        // Task 4: new FxBackgroundJobs(workers)
        var vm = new ReportViewModel(source, new CallerThreadJobs(),
                Clock.systemDefaultZone());
        stage.setScene(new Scene(new ReportView(vm).root(), 960, 600));
        stage.setTitle("Angkor Mart · Monthly report (MVVM)");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```


 

```java
package edu.itc.salesreport.ui;

/**
 * Runs the work immediately on the calling thread. Used as the
 * "frozen UI" baseline in Lab-03 Task 4 and in simple tests.
 */
public final class CallerThreadJobs implements BackgroundJobs {

    @Override
    public <T> JobHandle submit(Work<T> work, JobListener<T> listener) {
        ProgressSink sink = new ProgressSink() {
            @Override
            public void update(long done, long total) {
                listener.progress((double) done / total);
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };
        try {
            listener.succeeded(work.run(sink));
        } catch (Exception e) {
            listener.failed(e);
        }
        return () -> { };
    }
}
```



 

### Expected output


 

```plaintext
$ git diff --stat lab-02-final -- src/main/java/edu/itc/salesreport/model
$                                   (no output: model unchanged)

$ grep -c "^import javafx.scene" src/main/java/edu/itc/salesreport/ui/*.java
.../ReportView.java:13
.../ReportViewModel.java:0

Window after Generate (2026-08, all branches, 5,000 rows/day, seed 42):
  status   "Loaded 3 branch(es) for 2026-08."
  table    PNH / REP / BTB with the same revenue as the Lab-02 viewer
  total    "USD …" equal to the sum of the three branch revenues
  Generate disabled while the month box is empty
```



 

[** Show hints](#hint2)


 
 
 
 
- Bind the control *to* the ViewModel:
 `month.valueProperty().bindBidirectional(vm.monthProperty())`. The
 argument's value wins when the binding starts.
 
- "JavaFX runtime components are missing" means you ran an
 `Application` subclass from the class path: use
 `mvn javafx:run`, which puts JavaFX on the module path.
 
- A bound property cannot be set: if `label.setText(…)` throws "A bound
 value cannot be set", some logic is still in the view.
 
- With `CallerThreadJobs` the listener is called before
 `submit` returns; your `generate()` must therefore set
 `loading` to true *before* calling `submit`.
 
- Merge top products of several branches by SKU with `Map.merge`, then
 sort by revenue, descending.
 
 
 

 

### Acceptance checklist


 
 The view contains no `if`, no model
 call and no `setText`/`setAll` on data: only control creation,
 layout and bindings. 
 Inputs are two-way bound, outputs one-way bound,
 buttons wired to commands (2 statements each). 
 The model package is unchanged versus
 `lab-02-final` (empty `git diff --stat`). 
 Revenues for 2026-08 match the Lab-02 viewer;
 `lab03/screenshots/viewer.png` is committed. 
 
 
 

 
 
 
 3 
 

## Validation and error state as properties


 Medium 
 60 min 
 
 
 

### Goal


 

Express the screen rules (month in the future, empty branch selection) and the load errors as
 observable properties of the ViewModel, so that the view only decides how an error looks and
 every rule is testable without JavaFX.



 

### Steps


 
 
- Replace the placeholder `monthError` with a binding on `month`
 that calls `checkMonth(YearMonth)`; "today" comes from the injected
 `Clock`, never from `YearMonth.now()` without argument.
 
- Make `branchError` depend on all three `branchSelected` properties
 (pass them as the binding's dependencies).
 
- Build `validationMessage` (non-empty messages joined by a space),
 `valid` and `canGenerate = valid.and(loading.not())`.
 
- In the job listener, `failed(e)` keeps the old `summaries`, sets
 `loadError` to the exception message and `status` to "Load failed,
 previous data kept.".
 
- In the view: red border on the month box while `monthError` is not empty
 (`Bindings.when(…).then(…).otherwise(…)`), the validation label bound to
 `validationMessage`, the error label bound to `loadError`.
 
- Check the five input combinations of the expected-output table by hand, and force a load
 error once (temporarily wrap the source so that `REP` throws
 `new IOException("REP-2026-08-03.csv: unreadable")`).
 
- Add the screen state machine (Mermaid `stateDiagram-v2`: Incomplete, Invalid,
 Ready, Loading, Loaded, Failed) to `lab03/viewmodel-design.md`, with the
 property values of each state.
 

 

### Starter code


 

```java
// ReportViewModel constructor (replace the Task 1 placeholders)
Observable[] flags = branchSelected.values().toArray(Observable[]::new);
monthError = Bindings.createStringBinding(
        () -> checkMonth(month.get()), month);
branchError = Bindings.createStringBinding(
        () -> /* TODO "Select at least one branch." or "" */ "",
        flags);
validationMessage = Bindings.createStringBinding(
        () -> Stream.of(monthError.get(), branchError.get())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" ")),
        monthError, branchError);
valid = monthError.isEmpty().and(branchError.isEmpty());
canGenerate = valid.and(loading.not());

private String checkMonth(YearMonth m) {
    // TODO "Choose a month." when null,
    //      "Month 2026-10 is in the future." after YearMonth.now(clock),
    //      "" otherwise
    return "";
}

// ReportView
month.styleProperty().bind(Bindings
        .when(vm.monthErrorProperty().isEmpty())
        .then("").otherwise("-fx-border-color: crimson;"));
validation.textProperty().bind(vm.validationMessageProperty());
error.textProperty().bind(vm.loadErrorProperty());
```



 

### Expected output


 

With today = 2026-09-27:


 
 

| Month | Ticked branches | `validationMessage` | Generate |
| --- | --- | --- | --- |
| (empty) | PNH, REP, BTB | Choose a month. | disabled |
| 2026-10 | PNH, REP, BTB | Month 2026-10 is in the future. | disabled |
| 2026-08 | none | Select at least one branch. | disabled |
| (empty) | none | Choose a month. Select at least one branch. | disabled |
| 2026-09 | PNH | (empty) | enabled |


 
 

```
stateDiagram-v2
  [*] --> Incomplete
  Incomplete --> Invalid: future month or no branch
  Incomplete --> Ready: month ok and a branch ticked
  Invalid --> Ready: input fixed
  Ready --> Invalid: input changed
  Ready --> Loading: generate
  Loading --> Loaded: succeeded
  Loading --> Failed: failed
  Loading --> Ready: cancelled
  Loaded --> Loading: generate again
  Failed --> Loading: generate again
```



 

[** Show hints](#hint3)


 
 
 
 
- `Bindings.createStringBinding(callable, dependencies…)` recomputes
 only if every observable it reads is listed as a dependency. A missing
 dependency gives a message that never updates.
 
- `StringExpression.isEmpty()` returns a `BooleanBinding`,
 so `valid` needs no lambda.
 
- Editable combo boxes commit typed text on Enter or focus loss; the converter
 returns `null` for garbage, which becomes "Choose a month.".
 
- Do not open an `Alert` from the ViewModel: an error is state that the
 view presents.
 
 
 

 

### Acceptance checklist


 
 The five rows of the table behave exactly as
 shown (messages letter for letter). 
 The month rule uses the injected
 `Clock`; no `YearMonth.now()` without argument in
 `ui`. 
 A forced load error shows the file message and
 keeps the previous table content. 
 The state diagram with property values per state
 is in `lab03/viewmodel-design.md`. 
 
 
 

 
 
 
 4 
 

## Background loading with a `Task`: progress, cancel, no freeze
 


 Hard 
 90 min 
 
 
 

### Goal


 

Load branch summaries in a `javafx.concurrent.Task` behind the
 `BackgroundJobs` port, with progress bound to the `ProgressBar` and a
 working *Cancel*, and show with measurements that the UI stays responsive while the
 load time stays about the same.



 

### Steps


 
 
- Complete `FxBackgroundJobs`: forward `progressProperty()` to
 `listener.progress`, and
 `setOnSucceeded`/`setOnFailed`/`setOnCancelled` to the
 listener; `update(done, total)` calls `updateProgress`.
 
- In `loadAll` (runs on the worker) use only the snapshot arguments and the
 source; check `sink.isCancelled()` before each branch (throw
 `CancellationException`) and call `sink.update(i + 1, n)` after
 each branch. In the listener, `cancelled()` resets `loading` and
 sets "Cancelled, previous data kept.".
 
- Switch `ReportViewerApp` to
 `new FxBackgroundJobs(Executors.newVirtualThreadPerTaskExecutor())` and shut
 the executor down in `Application.stop()`.
 
- Generate the measurement data: `mvn -q compile` then
 `java -cp target/classes edu.itc.salesreport.ingest.SalesDataGenerator 2026-08 5000 42 data`
 (93 files, 465,000 rows; use your Lab-02 generator if you have one).
 
- Measure with `LoadBenchmarkApp` (copy it and `FrameGapMonitor` as
 given): 2 warm-up and 5 measured runs per mode, `CallerThreadJobs` versus
 `FxBackgroundJobs`. Record every run, the mean load time and the worst frame
 gap in `lab03/measurements.md`, with CPU, RAM, OS, JDK and JavaFX versions.
 
 
- Observe responsiveness by hand with 20,000 rows per day (regenerate into another
 folder): during a load, type in the product filter, move the window and press
 *Cancel*. Write 3 to 5 sentences in `measurements.md` comparing both
 modes.
 

 

### Starter code


 

```java
package edu.itc.salesreport.ui;

import java.util.Objects;
import java.util.concurrent.Executor;
import javafx.concurrent.Task;

/** Task 4 skeleton: one javafx.concurrent.Task per job. */
public final class FxBackgroundJobs implements BackgroundJobs {

    private final Executor executor;

    public FxBackgroundJobs(Executor executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public <T> JobHandle submit(Work<T> work, JobListener<T> listener) {
        FxTask<T> task = new FxTask<>(work);
        // TODO forward task.progressProperty() to listener.progress(...)
        // TODO setOnSucceeded / setOnFailed / setOnCancelled -> listener
        executor.execute(task);
        return () -> task.cancel(true);
    }

    private static final class FxTask<T> extends Task<T>
            implements ProgressSink {
        private final Work<T> work;

        FxTask(Work<T> work) {
            this.work = work;
        }

        @Override
        protected T call() throws Exception {
            return work.run(this);
        }

        @Override
        public void update(long done, long total) {
            // TODO publish progress (thread-safe, coalesced by Task)
        }
    }
}
```


 

```java
package edu.itc.salesreport.ui;

import javafx.animation.AnimationTimer;

/**
 * Measures UI responsiveness: the longest gap between two pulses of the
 * FX Application Thread (about 16.7 ms at 60 Hz when the UI is idle).
 */
public final class FrameGapMonitor extends AnimationTimer {

    private long last;
    private long maxGapNanos;
    private long frames;

    @Override
    public void handle(long now) {
        if (last != 0) {
            maxGapNanos = Math.max(maxGapNanos, now - last);
        }
        last = now;
        frames++;
    }

    public void reset() {
        last = 0;
        maxGapNanos = 0;
        frames = 0;
    }

    public double maxGapMillis() {
        return maxGapNanos / 1_000_000.0;
    }

    public long frames() {
        return frames;
    }
}
```


 

[** Measurement harness
 `LoadBenchmarkApp` (copy as is)](#benchApp)


 
 

```java
package edu.itc.salesreport.ui;

import edu.itc.salesreport.engine.SimpleReportEngine;
import java.nio.file.Path;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Lab-03 Task 4: load time and UI responsiveness, caller thread
 * (frozen UI) versus FxBackgroundJobs (Task on a worker thread).
 * Arguments: --data=data --month=2026-08 --warmup=2 --runs=5
 */
public class LoadBenchmarkApp extends Application {

    private record Result(String mode, double loadMs, double maxGapMs) { }

    private final FrameGapMonitor monitor = new FrameGapMonitor();
    private final List<Result> results = new ArrayList<>();
    private final Deque<Runnable> plan = new ArrayDeque<>();
    private ExecutorService workers;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Map<String, String> args = getParameters().getNamed();
        Path dataDir = Path.of(args.getOrDefault("data", "data"));
        YearMonth month = YearMonth.parse(args.getOrDefault("month",
                "2026-08"));
        int warmup = Integer.parseInt(args.getOrDefault("warmup", "2"));
        int runs = Integer.parseInt(args.getOrDefault("runs", "5"));
        var engine = new SimpleReportEngine();
        SummarySource source =
                (m, b) -> engine.summarizeBranch(m, b, dataDir);
        workers = Executors.newVirtualThreadPerTaskExecutor();

        for (String mode : List.of("caller-thread", "fx-task")) {
            for (int i = 0; i < warmup + runs; i++) {
                boolean measured = i >= warmup;
                plan.add(() -> runOnce(mode, measured, source, month));
            }
        }
        plan.add(this::printAndExit);

        stage.setScene(new Scene(new StackPane(), 960, 600));
        stage.show();
        monitor.start();
        plan.poll().run();
    }

    private void runOnce(String mode, boolean measured, SummarySource src,
            YearMonth month) {
        BackgroundJobs jobs = mode.equals("caller-thread")
                ? new CallerThreadJobs() : new FxBackgroundJobs(workers);
        var vm = new ReportViewModel(src, jobs, Clock.systemDefaultZone());
        vm.monthProperty().set(month);
        stage.getScene().setRoot(new ReportView(vm).root());

        long t0 = System.nanoTime();
        monitor.reset();
        vm.loadingProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<?
                    extends Boolean> obs, Boolean was, Boolean now) {
                if (now) return;
                obs.removeListener(this);
                double loadMs = (System.nanoTime() - t0) / 1e6;
                // let a few pulses pass so a blocked frame is measured
                PauseTransition settle = new PauseTransition(
                        Duration.millis(250));
                settle.setOnFinished(e -> {
                    if (measured) {
                        results.add(new Result(mode, loadMs,
                                monitor.maxGapMillis()));
                    }
                    plan.poll().run();
                });
                settle.play();
            }
        });
        vm.generateCommand().execute();
    }

    private void printAndExit() {
        System.out.println("mode           run  load ms  max frame gap ms");
        int[] n = new int[1];
        String prev = "";
        for (Result r : results) {
            n[0] = r.mode().equals(prev) ? n[0] + 1 : 1;
            prev = r.mode();
            System.out.println(String.format(Locale.ROOT,
                    "%-13s %4d %8.0f %17.0f",
                    r.mode(), n[0], r.loadMs(), r.maxGapMs()));
        }
        for (String mode : List.of("caller-thread", "fx-task")) {
            var rs = results.stream().filter(r -> r.mode().equals(mode))
                    .toList();
            System.out.println(String.format(Locale.ROOT,
                    "%-13s mean load %6.0f ms, worst frame gap %6.0f ms",
                    mode,
                    rs.stream().mapToDouble(Result::loadMs).average()
                            .orElse(0),
                    rs.stream().mapToDouble(Result::maxGapMs).max()
                            .orElse(0)));
        }
        monitor.stop();
        workers.shutdownNow();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```


 

 

### Expected output


 

```plaintext
$ mvn -q javafx:run -Dapp.main=edu.itc.salesreport.ui.LoadBenchmarkApp \
      "-Djavafx.args=--data=data --month=2026-08"
mode           run  load ms  max frame gap ms
caller-thread    1      480               586
caller-thread    2      458               554
caller-thread    3      497               545
caller-thread    4      372               431
caller-thread    5      382               415
fx-task          1      435                23
fx-task          2      444                27
fx-task          3      385                18
fx-task          4      342                17
fx-task          5      328                17
caller-thread mean load    438 ms, worst frame gap    586 ms
fx-task       mean load    387 ms, worst frame gap     27 ms
```


 

Reference run: Intel i7-13700F, 32 GB, Windows 11, JDK 25.0.3,
 JavaFX 25.0.4. Your numbers differ; the shape must not: similar load times, but a worst
 frame gap of hundreds of milliseconds on the caller thread against a few frames (well under
 100 ms) with the `Task`. Run-to-run spread of ±30 % is normal on a busy laptop,
 which is why you report every run.



 

[** Show hints](#hint4)


 
 
 
 
- `Task` handlers (`setOnSucceeded` …) and property
 listeners on a `Task` run on the FX Application Thread, so the
 ViewModel may set its properties inside them.
 
- `updateProgress` is safe to call thousands of times from the worker:
 `Task` coalesces updates into at most one pending
 `runLater`.
 
- A worker that reads `month.get()` races with the user changing the
 combo box. Pass the `YearMonth` and `List.copyOf(...)`
 captured in `generate()`.
 
- Frame gap is the time between two `AnimationTimer` pulses: about 16.7
 ms at 60 Hz when idle. A load on the FX thread shows up as one huge gap.
 
- `task.cancel(true)` interrupts the worker; a blocking file read may
 still finish, but a cancelled task never reaches SUCCEEDED, so late results are
 discarded.
 
 
 

 

### Acceptance checklist


 
 The progress bar advances per branch and
 *Cancel* stops a running load, keeping the previous data. 
 No ViewModel property is written from the worker;
 the worker receives only immutable snapshots. 
 `lab03/measurements.md` has all 10
 measured runs, means, worst gaps, method and hardware. 
 With `FxBackgroundJobs` the worst
 frame gap stays below 100 ms, and the manual observation is written down. 
 
 
 
 

 
 
 
 5 
 

## Unit-test the ViewModel, compare MVC and MVVM


 Hard 
 90 min 
 
 
 

### Goal


 

Prove the ViewModel's behaviour with at least eight JUnit 6 tests that never start JavaFX,
 guard the "no UI types" rule with a test, compare your Lab-02 MVC viewer and your Lab-03
 MVVM viewer with numbers, and record what changed.



 

### Steps


 
 
- Add the test double `ManualJobs` (given): jobs wait in a queue until the test
 calls `runNext()`, so the loading state is observable in between, without
 threads or sleeps.
 
- Write `ReportViewModelTest` with a fixed `Clock` (2026-09-27,
 Asia/Phnom_Penh) and a fake `SummarySource`: at least 8 tests covering
 validation (3), command enablement (2), success with totals, progress, failure,
 cancellation and the product filter.
 
- Add `ViewModelImportsTest` (given): it fails when
 `ReportViewModel.java` imports `javafx.scene`,
 `javafx.stage`, `javafx.application` or
 `javafx.concurrent`.
 
- Run `mvn verify` and read the `ReportViewModel` row of
 `target/site/jacoco/jacoco.csv` (line coverage = LINE_COVERED / (LINE_COVERED
 + LINE_MISSED)). Target: at least 85 %.
 
- Write `lab03/mvc-vs-mvvm.md` from *your own* code: lines of
 presentation code (`wc -l` per class), presentation-logic line coverage
 without toolkit, number of `javafx.scene` imports and of control types
 referenced by the logic class, test run time; end with 5 to 8 sentences: what got
 better, what got worse (more classes, binding debugging), when you would still choose
 MVC.
 
- **Record what changed**: create `lab03/README.md` with (a) a
 table of this lab's files (path, purpose, author), (b) the inputs reused from Lab-02
 with their commit or tag (`lab-02-final`) or "fallback starter A/B", (c) a
 change-log row: date, what changed, who, and whether the model package changed (it
 should not).
 

 

### Starter code


 

```java
package edu.itc.salesreport.ui;

import java.util.ArrayDeque;
import java.util.Deque;

/** Test double: jobs wait until the test runs them, on the test thread. */
final class ManualJobs implements BackgroundJobs {

    private final Deque<Pending<?>> pending = new ArrayDeque<>();

    @Override
    public <T> JobHandle submit(Work<T> work, JobListener<T> listener) {
        Pending<T> job = new Pending<>(work, listener);
        pending.add(job);
        return () -> {
            if (pending.remove(job)) {
                job.cancelled = true;
                listener.cancelled();
            }
        };
    }

    int pendingCount() {
        return pending.size();
    }

    /** Runs the oldest job to completion, as a worker thread would. */
    void runNext() {
        pending.remove().run();
    }

    private static final class Pending<T> implements ProgressSink {
        private final Work<T> work;
        private final JobListener<T> listener;
        private boolean cancelled;

        Pending(Work<T> work, JobListener<T> listener) {
            this.work = work;
            this.listener = listener;
        }

        void run() {
            T value;
            try {
                value = work.run(this);
            } catch (Exception e) {
                listener.failed(e);
                return;
            }
            listener.succeeded(value);
        }

        @Override
        public void update(long done, long total) {
            listener.progress((double) done / total);
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }
}
```


 

```java
package edu.itc.salesreport.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.ProductTotal;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Plain JUnit: no JavaFX toolkit, no window, no robot, no sleep. */
class ReportViewModelTest {

    static final Clock SEP_27 = Clock.fixed(
            Instant.parse("2026-09-27T03:00:00Z"),
            ZoneId.of("Asia/Phnom_Penh"));
    static final YearMonth AUG = YearMonth.of(2026, 8);

    ManualJobs jobs = new ManualJobs();
    List<String> loaded = new ArrayList<>();
    boolean failNext;
    ReportViewModel vm;

    @BeforeEach
    void setUp() {
        SummarySource source = (month, branch) -> {
            if (failNext) {
                throw new IOException(branch + "-2026-08-03.csv: unreadable");
            }
            loaded.add(branch);
            return summary(branch, "200.00");
        };
        vm = new ReportViewModel(source, jobs, SEP_27);
    }

    static BranchSummary summary(String branch, String revenue) {
        var rice = new ProductTotal("SKU-1001", "Jasmine Rice 5kg",
                new BigDecimal(revenue), 5);
        return new BranchSummary(branch, new BigDecimal(revenue),
                BigDecimal.ZERO, 10, Map.of(), List.of(rice), Map.of());
    }

    @Test
    void futureMonthIsRejected() {
        vm.monthProperty().set(YearMonth.of(2026, 10));
        assertEquals("Month 2026-10 is in the future.",
                vm.monthErrorProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
    }

    @Test
    void generateStartsLoadingAndDisablesItself() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        assertTrue(vm.loadingProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
        assertTrue(vm.cancelCommand().isExecutable());
        assertEquals(1, jobs.pendingCount());
    }

    // TODO at least 6 more, for example:
    // cannotGenerateBeforeAMonthIsChosen, emptyBranchSelectionIsRejected,
    // generateIsIgnoredWhileInvalid, successfulLoadFillsSummariesAndTotal,
    // progressFollowsTheBranches, failureKeepsPreviousDataAndShowsTheError,
    // cancelReenablesGenerateAndKeepsOldData, productFilterIsCaseInsensitive,
    // workerUsesASnapshotOfTheInputs
}
```


 

```java
package edu.itc.salesreport.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Architecture guard: the ViewModel may use javafx.base only. */
class ViewModelImportsTest {

    private static final List<String> FORBIDDEN = List.of(
            "import javafx.scene", "import javafx.stage",
            "import javafx.application", "import javafx.concurrent");

    @Test
    void viewModelUsesJavafxBaseOnly() throws IOException {
        Path src = Path.of("src/main/java/edu/itc/salesreport/ui",
                "ReportViewModel.java");
        List<String> bad = Files.readAllLines(src).stream()
                .filter(line -> FORBIDDEN.stream().anyMatch(line::startsWith))
                .toList();
        assertEquals(List.of(), bad);
    }
}
```



 

### Expected output


 

```plaintext
$ mvn verify
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.220 s
       -- in edu.itc.salesreport.ui.ReportViewModelTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s
       -- in edu.itc.salesreport.ui.ViewModelImportsTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS

$ grep ReportViewModel, target/site/jacoco/jacoco.csv
sales-report,edu.itc.salesreport.ui,ReportViewModel,19,635,4,26,5,118,8,49,4,38
                    (LINE_MISSED 5, LINE_COVERED 118: 96 % line coverage)
```


 

Example of `lab03/mvc-vs-mvvm.md` (reference solution with
 fallback starter B as the MVC side):


 
 

| Metric | MVC (Lab-02, `ReportController`) | MVVM (Lab-03) | How measured |
| --- | --- | --- | --- |
| Lines of presentation code | 43 (controller) in a 107-line app | 293 ViewModel + 133 view + 138 ports, commands and jobs | `wc -l` |
| Presentation-logic line coverage without toolkit | 0 % (`new ComboBox<>()` needs the toolkit) | 96 % (118 of 123 lines) | JaCoCo 0.8.15 |
| `javafx.scene` imports in the logic class | 9 | 0 | `grep -c "^import javafx.scene"` |
| Control types the logic class references | 4 (ComboBox, CheckBox, Label, TableView) | 0 | code review |
| Test run time of presentation logic | n/a | 0.08 to 0.22 s for 12 tests | Surefire report |
| UI frozen during a load | yes (worst gap 586 ms) | no (27 ms) | Task 4 |


 

 

[** Show hints](#hint5)


 
 
 
 
- Record progress values with a listener on `vm.progressProperty()` and
 assert the list: with two branches you should see `[0.5, 1.0]`.
 
- To test failure, let the fake source throw on a flag and run a second load after
 a successful one; assert that `summaries()` still has the old rows.
 
 
- If a test needs `Platform` or a `Task`, the ViewModel
 still depends on the toolkit: fix the ViewModel, not the test.
 
- Never compare against the real date:
 `Clock.fixed(Instant.parse("2026-09-27T03:00:00Z"), ZoneId.of("Asia/Phnom_Penh"))`.
 
 
 
 

 

### Acceptance checklist


 
 At least 8 `ReportViewModelTest` tests
 pass without starting JavaFX, without `Thread.sleep`, with a fixed
 `Clock`. 
 `ViewModelImportsTest` passes and
 `mvn -q verify` is green; `ReportViewModel` line coverage is
 at least 85 %. 
 `lab03/mvc-vs-mvvm.md` compares both
 versions of your own code with the metrics above and a written conclusion. 
 
 `lab03/README.md` records files,
 reused inputs (tag or fallback) and the change log. 
 
 
 

 
 
 
 * 
 

## Challenge: a second view on the same ViewModel


 Hard 
 90–120 min · optional, extra credit 
 
 
 

### Scenario


 

The Angkor Mart operations manager wants a small always-on-top dashboard next to the full
 viewer: the month, the chain revenue in large type, how many branches are loaded, a spinner
 while loading and a *Refresh* button. Both windows must always agree, and head office
 asks whether the same design would carry over to their planned Android app or to the finance
 team's WPF tools.


 

### Requirements


 
 
- Write `DashboardView` bound to the **same**
 `ReportViewModel` instance, opened in a second `Stage` by
 `DashboardWindow.open(vm)` from `ReportViewerApp`.
 
- Prove reuse:
 `git diff lab-03-task5 -- src/main/java/edu/itc/salesreport/ui/ReportViewModel.java`
 is empty (tag your Task 5 commit first). If you had to change the ViewModel, explain why
 in the README.
 
- Demonstrate: changing the month in the viewer updates the dashboard; *Refresh* in
 the dashboard fills the viewer's table; both buttons are disabled during a load. Save
 `lab03/screenshots/dashboard.png` with both windows.
 
- Add one ViewModel test that simulates two views: two listeners on
 `totalRevenueTextProperty()` both receive "USD 600.00" after one load.
 
- Write `lab03/platform-mapping.md`: a table mapping each
 `ReportViewModel` member (month, branch flags, summaries, loading, progress,
 validation, generate, cancel, BackgroundJobs) to Android Jetpack
 (`ViewModel`, `StateFlow`, `viewModelScope`)
 *or* WPF (`INotifyPropertyChanged`, `ObservableCollection`,
 `ICommand`, `async`/`await`), and 5 sentences on what
 would not carry over (two-way binding in Jetpack, lifetime across rotation).
 
 

### Skeleton


 

```java
package edu.itc.salesreport.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

/** Challenge skeleton: a compact dashboard bound to the SAME ViewModel. */
public final class DashboardView {

    private final VBox root = new VBox(8);

    public DashboardView(ReportViewModel vm) {
        Label month = new Label();
        month.textProperty().bind(Bindings.createStringBinding(
                () -> "Month: " + vm.monthProperty().get(),
                vm.monthProperty()));
        Label total = new Label();
        total.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        total.textProperty().bind(vm.totalRevenueTextProperty());
        Label branches = new Label();
        // TODO "3 branch(es) loaded": Bindings.size(vm.summaries())
        ProgressIndicator spinner = new ProgressIndicator();
        // TODO bind spinner progress and visibility to the ViewModel
        Button refresh = new Button("Refresh");
        // TODO execute vm.generateCommand(), disable when not executable
        root.getChildren().addAll(month, total, branches, spinner, refresh);
        root.setPadding(new Insets(12));
    }

    public Parent root() {
        return root;
    }
}
```


 

```java
package edu.itc.salesreport.ui;

import javafx.scene.Scene;
import javafx.stage.Stage;

/** Challenge wiring: a second window bound to the same ViewModel. */
final class DashboardWindow {

    static Stage open(ReportViewModel vm) {
        Stage dash = new Stage();
        dash.setScene(new Scene(new DashboardView(vm).root(), 320, 260));
        dash.setTitle("Angkor Mart · Dashboard");
        dash.show();
        return dash;
    }

    private DashboardWindow() { }
}
```


 

### Expected output


 

```plaintext
Viewer window                         Dashboard window
  Month 2026-08, PNH REP BTB ticked     Month: 2026-08
  [Generate] clicked                    spinner visible, Refresh disabled
  table: 3 rows                         "3 branch(es) loaded"
  Chain revenue: USD …                  USD …   (same text)

$ git diff lab-03-task5 -- src/main/java/edu/itc/salesreport/ui/ReportViewModel.java
$                                     (no output: ViewModel reused unchanged)
```


 

[** Show hints](#hintC)


 
 
 
 
- `Bindings.size(vm.summaries()).asString("%d branch(es) loaded")`
 gives a ready-made `StringBinding`.
 
- Both views must bind to the ViewModel, never to each other's controls.
 
- Call `dash.setAlwaysOnTop(true)` for the manager's request; closing
 the dashboard must not stop the application
 (`Platform.setImplicitExit` stays default, the main window owns the
 lifecycle).
 
- For the mapping: Jetpack has no two-way binding; inputs become events such as
 `onMonthSelected(YearMonth)` and the state is one immutable
 `ReportState` in a `StateFlow`.
 
 
 
 

### Acceptance checklist


 
 * The dashboard and the viewer share one
 `ReportViewModel` instance and always show the same month, total and
 loading state. 
 `ReportViewModel.java` is unchanged
 since Task 5 (or the change is justified in the README). 
 The two-listener test passes;
 `lab03/screenshots/dashboard.png` shows both windows. 
 `lab03/platform-mapping.md` maps every
 ViewModel member to Jetpack or WPF and names what does not carry over. 
 
 
 

 
 
 
 

## ** Grading rubric


 
 
 
 

| Component | Points | Full marks when… |
| --- | --- | --- |
| Task 1 · ViewModel design | 10 | Complete controller inventory, read-only outputs, ports and commands,
 rendered class diagram. |
| Task 2 · Bound view | 15 | View has only controls and bindings, commands wired, model unchanged, same
 revenues as Lab-02. |
| Task 3 · Validation and errors | 20 | All five input cases correct via injected `Clock`, load errors
 kept as state, state diagram documented. |
| Task 4 · Background loading | 20 | `Task` behind the port, progress and cancel work, 10 measured
 runs with method and hardware, frame gap below 100 ms. |
| Task 5 · Tests and comparison | 25 | At least 8 toolkit-free tests, import guard, coverage at least 85 %, MVC vs
 MVVM comparison with own numbers, README. |
| Code and diagram quality, Git history | 10 | Small focused classes, clear names, diagrams render, both members author
 meaningful commits. |
| ★ Challenge (extra credit) | +20 | Second view on the unchanged ViewModel, two-listener test, screenshot,
 platform mapping. |
| Total | 100 (+20) | |


 
 **Automatic deductions:**
 −10 if `ReportViewModel` (or any `*ViewModel` class) imports
 `javafx.scene`, `javafx.stage`, `javafx.application` or
 `javafx.concurrent`;
 −10 if the UI freezes during the demo or the measured worst frame gap with
 `FxBackgroundJobs` exceeds 100 ms;
 −5 per ViewModel property written from a worker thread;
 −5 if a ViewModel test starts the JavaFX toolkit, calls `Thread.sleep` or depends
 on the real date;
 −5 if the model package differs from `lab-02-final` without an explanation in
 `lab03/README.md`;
 −5 if a measurement is reported as a single run. 
 
 

 
 
 
 

## ** Submission


 
 
 
 
- Repository layout:
 

```plaintext
sales-report/
├── pom.xml
├── data/                      (generated, not committed: add to .gitignore)
├── src/main/java/edu/itc/salesreport/
│   ├── model/                 (unchanged since lab-02-final)
│   ├── ingest/  engine/
│   └── ui/                    ReportViewModel, ReportView, commands, jobs
├── src/test/java/edu/itc/salesreport/ui/
│   ├── ManualJobs.java
│   ├── ReportViewModelTest.java
│   └── ViewModelImportsTest.java
└── lab03/
    ├── README.md
    ├── viewmodel-design.md
    ├── measurements.md
    ├── mvc-vs-mvvm.md
    ├── platform-mapping.md    (challenge)
    └── screenshots/
```


 
 
- From a clean checkout this must pass: `mvn -q verify` (tests and JaCoCo
 report, no display needed).
 
- Branch `lab-03`, pull request titled `Lab-03 – `,
 both members as authors of commits; link the PR in `lab03/README.md`.
 
 
 [* Back
 to Chapter 03 slides](Chapter-03.html)
 [** Retake the chapter quiz](Chapter-03.html#25)
 [** Lab-02](../02-ModelViewController/Lab-02.html)
