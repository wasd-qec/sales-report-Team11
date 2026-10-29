# Lab-02: Model View Controller (MVC) design pattern

Software Engineering Concepts · Chapter 02

Your team builds the first piece of running code of the Angkor Mart monthly sales system: a JavaFX report viewer in MVC. Task 1 writes an observable, toolkit-free `SalesModel` over the branch CSV files; Task 2 adds a table and chart view; Task 3 a controller with branch and month filters and input validation; Task 4 tests model and controller without a screen and refactors a fat controller; Task 5 builds an MVP variant and compares the two. The challenge puts a Spring MVC web page on the same, unchanged model.

JavaFX 25.0.4
Spring Boot 4.1.1 (challenge)
JDK 25 · Maven 3.9 · JUnit 6.1
≈ 6 hours, pairs
5 tasks + 1 challenge

**How to work through this lab.** Do the tasks in order; each builds on the previous. Read the *Goal*, follow the *Steps*, compare with the *Expected output*, and tick the *Acceptance checklist* (saved in your browser). Open a hint only when stuck for more than 10 minutes. Work in pairs: alternate who types per task, and commit under your own name so each person's contribution is visible in the Git history.

0

## Setup

30 min

1. **Tools.** Verify the JDK and Maven. JavaFX itself needs no installation: Maven downloads the OpenJFX jars with the right platform classifier (`win`, `linux`, `mac`).

   ```
   java -version     # openjdk version "25.0.x" ... LTS
   mvn -v            # Apache Maven 3.9.16, Java version: 25.0.x
   git --version
   ```

   **Desktop session required for the window, not for the tests.** `mvn javafx:run` opens a window, so run it on Windows, macOS or a Linux desktop (X11 or Wayland). Over SSH, in WSL without WSLg or in CI it fails with "Unable to open DISPLAY"; `mvn test` and `mvn verify` work everywhere because no test starts JavaFX.
2. **Case-study brief.** Everything this lab needs to know about the system: Case-study brief: Angkor Mart monthly sales report

   Company
   :   Angkor Mart, a fictional retail chain with three branches: `PNH` (Phnom Penh), `REP` (Siem Reap), `BTB` (Battambang). Head office needs a monthly sales report per branch and for the chain, viewed on a desktop and later on a web page.

   Data files
   :   One CSV per branch and day: `data/<yyyy-MM>/<BRANCH>-<yyyy-MM-dd>.csv`, UTF-8, header row, comma separated.

   Row format
   :   Header `branch,date,receipt_no,sku,product_name,category,quantity,unit_price,discount,payment_method`; example `PNH,2026-09-01,PNH-000123,SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,KHQR`. Categories `Grocery`, `Beverages`, `Household`, `Personal Care`, `Electronics`, `Stationery`. Payment `CASH`, `CARD`, `KHQR` (Cambodia's national QR payment standard). Currency USD.

   Rules
   :   Line revenue = `quantity × unit_price − discount` with `BigDecimal`, scale 2, `RoundingMode.HALF_UP`. A row is invalid when a field is missing or unparsable, the branch or category is unknown, quantity ≤ 0, an amount is negative, or the discount exceeds `quantity × unit_price`; invalid rows are reported, never summed.

   Figures in this lab
   :   Per branch or for the chain (`ALL`): total revenue, total discounts, number of receipts (distinct `receipt_no`), average basket (revenue / receipts), revenue by category, top 10 products by revenue, revenue by payment method, rejected rows. Month-over-month change and anomaly detection come in later labs.

   Java names
   :   Maven `edu.itc:sales-report`, root package `edu.itc.salesreport` with `model`, `ingest`, `engine`, `ui`. Types: `SaleTransaction` (record with `revenue()`), `PaymentMethod`, `ProductTotal`, `BranchSummary`, `TransactionParser` + `InvalidRowException`, `SalesAggregator`, `SalesDataGenerator`. Keep these names: later labs build on them.

   Repository
   :   One team repository with the Maven project `sales-report` at its root (Labs 02 to 08 extend it) and one folder per lab for written deliverables (`lab02/`).
3. **Inputs from earlier labs.** This is the first code lab. If a Lab-01 item is missing, use the fallback and carry on.

   | Input                             | Where                 | What you need from it                                               | Fallback if missing                                                                                                            |
   | --------------------------------- | --------------------- | ------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
   | Team repository and roles         | Lab-01, `README.md` | A repository with every member as collaborator                      | Create `sales-report-<team>` now and invite everyone.                                                                        |
   | Quality-attribute scenarios       | Lab-01 (`lab01/`)   | A testability and a modifiability scenario to cite in Task 5        | Write two one-line scenarios in `lab02/README.md`, e.g. "a new chart type is added in under a day without touching parsing". |
   | C4 container diagram              | Lab-01 (`lab01/`)   | The name of the desktop viewer container                            | Call it "Report viewer (JavaFX desktop)".                                                                                      |
   | ADR template                      | Lab-01 (`lab01/`)   | Format for the MVC-or-MVP decision in Task 5                        | Title, Status, Context, Decision, Consequences.                                                                                |
   | Domain types, parser, sample data | none yet              | Canonical records, `CsvTransactionParser`, `SalesDataGenerator` | Copy the starter kit in step 5 (tested code).                                                                                  |
4. **Project.** Create `pom.xml` at the repository root. JavaFX is `optional` so that projects reusing the domain (the challenge) do not inherit it; `javafx.mainClass` is a property so you can start another class from the command line.

   ```
   <project xmlns="http://maven.apache.org/POM/4.0.0">
     <modelVersion>4.0.0</modelVersion>
     <groupId>edu.itc</groupId>
     <artifactId>sales-report</artifactId>
     <version>0.2.0-SNAPSHOT</version>

     <properties>
       <maven.compiler.release>25</maven.compiler.release>
       <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
       <javafx.version>25.0.4</javafx.version>
       <!-- override on the command line: -Djavafx.mainClass=... -->
       <javafx.mainClass>edu.itc.salesreport.ui.fx.ReportApp</javafx.mainClass>
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
         <optional>true</optional>
       </dependency>
       <dependency>
         <groupId>org.openjfx</groupId>
         <artifactId>javafx-fxml</artifactId>
         <version>${javafx.version}</version>
         <optional>true</optional>
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
           <groupId>org.openjfx</groupId>
           <artifactId>javafx-maven-plugin</artifactId>
           <version>0.0.8</version>
           <configuration>
             <mainClass>${javafx.mainClass}</mainClass>
             <options>
               <option>--enable-native-access=javafx.graphics</option>
             </options>
           </configuration>
         </plugin>
       </plugins>
     </build>
   </project>
   ```
5. **Starter kit** (the canonical domain types, the parser and the sample-data generator). Copy these files unchanged; they are tested and later labs rely on them.

   [Show `model/`: `PaymentMethod`, `SaleTransaction`, `ProductTotal`, `BranchSummary`](#kitModel)

   ```
   // src/main/java/edu/itc/salesreport/model/PaymentMethod.java
   package edu.itc.salesreport.model;

   public enum PaymentMethod { CASH, CARD, KHQR }

   // src/main/java/edu/itc/salesreport/model/SaleTransaction.java
   package edu.itc.salesreport.model;

   import java.math.BigDecimal;
   import java.math.RoundingMode;
   import java.time.LocalDate;

   /** One CSV line of a branch's daily export. */
   public record SaleTransaction(String branch, LocalDate date, String receiptNo,
           String sku, String productName, String category, int quantity,
           BigDecimal unitPrice, BigDecimal discount, PaymentMethod paymentMethod) {

       /** quantity x unit price - discount, scale 2, HALF_UP. */
       public BigDecimal revenue() {
           return unitPrice.multiply(BigDecimal.valueOf(quantity))
                   .subtract(discount)
                   .setScale(2, RoundingMode.HALF_UP);
       }
   }

   // src/main/java/edu/itc/salesreport/model/ProductTotal.java
   package edu.itc.salesreport.model;

   import java.math.BigDecimal;

   public record ProductTotal(String sku, String productName,
           BigDecimal revenue, long quantity) {}

   // src/main/java/edu/itc/salesreport/model/BranchSummary.java
   package edu.itc.salesreport.model;

   import java.math.BigDecimal;
   import java.math.RoundingMode;
   import java.util.Collections;
   import java.util.EnumMap;
   import java.util.List;
   import java.util.Map;
   import java.util.TreeMap;

   public record BranchSummary(String branch, BigDecimal revenue,
           BigDecimal discounts, long receipts,
           Map<String, BigDecimal> revenueByCategory,
           List<ProductTotal> topProducts,
           Map<PaymentMethod, BigDecimal> revenueByPayment) {

       public BranchSummary {
           revenueByCategory = Collections.unmodifiableMap(
                   new TreeMap<>(revenueByCategory));
           topProducts = List.copyOf(topProducts);
           var byPayment = new EnumMap<PaymentMethod, BigDecimal>(PaymentMethod.class);
           byPayment.putAll(revenueByPayment);
           revenueByPayment = Collections.unmodifiableMap(byPayment);
       }

       /** Revenue per receipt, 0.00 when there are no receipts. */
       public BigDecimal averageBasket() {
           return receipts == 0
                   ? BigDecimal.ZERO.setScale(2)
                   : revenue.divide(BigDecimal.valueOf(receipts), 2,
                           RoundingMode.HALF_UP);
       }
   }
   ```

   [Show `ingest/`: `InvalidRowException`, `TransactionParser`, `CsvTransactionParser`](#kitIngest)

   ```
   // src/main/java/edu/itc/salesreport/ingest/InvalidRowException.java
   package edu.itc.salesreport.ingest;

   /** A CSV line that does not satisfy the canonical format. */
   public class InvalidRowException extends Exception {
       private final String line;

       public InvalidRowException(String reason, String line) {
           super(reason);
           this.line = line;
       }

       public String line() { return line; }
   }

   // src/main/java/edu/itc/salesreport/ingest/TransactionParser.java
   package edu.itc.salesreport.ingest;

   import edu.itc.salesreport.model.SaleTransaction;

   @FunctionalInterface
   public interface TransactionParser {
       SaleTransaction parse(String line) throws InvalidRowException;
   }

   // src/main/java/edu/itc/salesreport/ingest/CsvTransactionParser.java
   package edu.itc.salesreport.ingest;

   import edu.itc.salesreport.model.PaymentMethod;
   import edu.itc.salesreport.model.SaleTransaction;
   import java.math.BigDecimal;
   import java.time.LocalDate;
   import java.time.format.DateTimeParseException;
   import java.util.Set;

   /** The canonical parser for one line of a branch CSV file. */
   public final class CsvTransactionParser implements TransactionParser {
       public static final String HEADER = "branch,date,receipt_no,sku,"
               + "product_name,category,quantity,unit_price,discount,"
               + "payment_method";
       public static final Set<String> BRANCHES = Set.of("PNH", "REP", "BTB");
       public static final Set<String> CATEGORIES = Set.of("Grocery",
               "Beverages", "Household", "Personal Care", "Electronics",
               "Stationery");

       @Override
       public SaleTransaction parse(String line) throws InvalidRowException {
           String[] f = line.split(",", -1);
           if (f.length != 10) {
               throw new InvalidRowException(
                       "expected 10 fields, found " + f.length, line);
           }
           try {
               String branch = oneOf(f[0], BRANCHES, "branch", line);
               LocalDate date = LocalDate.parse(f[1]);
               String category = oneOf(f[5], CATEGORIES, "category", line);
               int quantity = Integer.parseInt(f[6]);
               var unitPrice = new BigDecimal(f[7]);
               var discount = new BigDecimal(f[8]);
               var payment = PaymentMethod.valueOf(f[9]);
               if (quantity <= 0) {
                   throw new InvalidRowException(
                           "quantity must be positive: " + quantity, line);
               }
               if (unitPrice.signum() < 0 || discount.signum() < 0) {
                   throw new InvalidRowException("negative amount", line);
               }
               var gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
               if (discount.compareTo(gross) > 0) {
                   throw new InvalidRowException(
                           "discount exceeds line amount", line);
               }
               return new SaleTransaction(branch, date, f[2], f[3], f[4],
                       category, quantity, unitPrice, discount, payment);
           } catch (DateTimeParseException | IllegalArgumentException e) {
               // NumberFormatException is an IllegalArgumentException
               throw new InvalidRowException("bad value: " + e.getMessage(), line);
           }
       }

       private static String oneOf(String value, Set<String> allowed,
               String field, String line) throws InvalidRowException {
           if (!allowed.contains(value)) {
               throw new InvalidRowException(
                       "unknown " + field + " '" + value + "'", line);
           }
           return value;
       }
   }
   ```

   [Show `ingest/SalesDataGenerator` (seeded sample data)](#kitGen)

   ```
   // src/main/java/edu/itc/salesreport/ingest/SalesDataGenerator.java
   package edu.itc.salesreport.ingest;

   import edu.itc.salesreport.model.PaymentMethod;
   import java.io.IOException;
   import java.nio.charset.StandardCharsets;
   import java.nio.file.Files;
   import java.nio.file.Path;
   import java.time.LocalDate;
   import java.time.YearMonth;
   import java.util.ArrayList;
   import java.util.List;
   import java.util.Random;

   /** Seeded generator of small sample files: same seed, same files. */
   public final class SalesDataGenerator {
       private record Product(String sku, String name, String category,
               String price) {}

       private static final List<Product> CATALOG = List.of(
           new Product("SKU-1001", "Jasmine Rice 5kg", "Grocery", "6.50"),
           new Product("SKU-1002", "Fish Sauce 700ml", "Grocery", "1.80"),
           new Product("SKU-1003", "Palm Sugar 1kg", "Grocery", "2.40"),
           new Product("SKU-2001", "Drinking Water 1.5L", "Beverages", "0.60"),
           new Product("SKU-2002", "Iced Coffee 250ml", "Beverages", "0.90"),
           new Product("SKU-2003", "Sugarcane Juice 1L", "Beverages", "1.60"),
           new Product("SKU-3001", "Laundry Powder 2kg", "Household", "5.90"),
           new Product("SKU-3002", "Dish Soap 500ml", "Household", "1.30"),
           new Product("SKU-3003", "Mosquito Coil 10pc", "Household", "1.10"),
           new Product("SKU-4001", "Toothpaste 150g", "Personal Care", "1.70"),
           new Product("SKU-4002", "Shampoo 400ml", "Personal Care", "3.80"),
           new Product("SKU-4003", "Sunscreen SPF50", "Personal Care", "7.50"),
           new Product("SKU-5001", "USB-C Cable 1m", "Electronics", "3.50"),
           new Product("SKU-5002", "Power Bank 10000mAh", "Electronics",
                   "18.90"),
           new Product("SKU-5003", "LED Bulb 9W", "Electronics", "2.20"),
           new Product("SKU-6001", "Notebook A5", "Stationery", "0.80"),
           new Product("SKU-6002", "Ballpoint Pen 10pc", "Stationery", "1.50"),
           new Product("SKU-6003", "Sticky Notes", "Stationery", "1.20"));
       private static final List<String> BRANCHES = List.of("PNH", "REP", "BTB");

       /** Writes one file per branch and day; returns the number of rows. */
       public int generate(YearMonth month, int rowsPerDay, long seed,
               Path dataDir) throws IOException {
           var random = new Random(seed);
           Path dir = Files.createDirectories(dataDir.resolve(month.toString()));
           int total = 0;
           for (String branch : BRANCHES) {
               int receipt = 0;
               for (int day = 1; day <= month.lengthOfMonth(); day++) {
                   LocalDate date = month.atDay(day);
                   var lines = new ArrayList<String>();
                   lines.add(CsvTransactionParser.HEADER);
                   while (lines.size() <= rowsPerDay) {
                       receipt++;
                       int items = 1 + random.nextInt(4);
                       var pay = PaymentMethod.values()[random.nextInt(3)];
                       for (int i = 0; i < items && lines.size() <= rowsPerDay;
                               i++) {
                           var p = CATALOG.get(random.nextInt(CATALOG.size()));
                           int qty = 1 + random.nextInt(3);
                           String discount =
                                   random.nextInt(10) == 0 ? "0.50" : "0.00";
                           lines.add(String.join(",", branch, date.toString(),
                                   "%s-%06d".formatted(branch, receipt),
                                   p.sku(), p.name(), p.category(),
                                   Integer.toString(qty), p.price(), discount,
                                   pay.name()));
                       }
                   }
                   total += lines.size() - 1;
                   Files.write(dir.resolve(branch + "-" + date + ".csv"),
                           lines, StandardCharsets.UTF_8);
               }
           }
           return total;
       }

       public static void main(String[] args) throws IOException {
           Path dataDir = Path.of(args.length > 0 ? args[0] : "data");
           var generator = new SalesDataGenerator();
           for (String m : List.of("2026-08", "2026-09")) {
               var month = YearMonth.parse(m);
               int rows = generator.generate(month, 40,
                       202600L + month.getMonthValue(), dataDir);
               System.out.printf("%s: %d files, %d rows%n", month,
                       3 * month.lengthOfMonth(), rows);
           }
       }
   }
   ```
6. **Sample data.** Generate two months (the generator is deterministic, so every team gets the same files and the same totals). Add `data/` to `.gitignore`: it is reproducible from the seed.

   ```
   mvn -q compile
   java -cp target/classes edu.itc.salesreport.ingest.SalesDataGenerator data
   # 2026-08: 93 files, 3720 rows
   # 2026-09: 90 files, 3600 rows
   head -3 data/2026-09/PNH-2026-09-01.csv
   # branch,date,receipt_no,sku,product_name,category,quantity,unit_price,discount,payment_method
   # PNH,2026-09-01,PNH-000001,SKU-4002,Shampoo 400ml,Personal Care,3,3.80,0.00,KHQR
   # PNH,2026-09-01,PNH-000001,SKU-2001,Drinking Water 1.5L,Beverages,2,0.60,0.00,KHQR
   ```
7. **Target structure and deliverables.** Packages marked *toolkit-free* must never import `javafx.*`, `java.awt.*` or `javax.swing.*` (Task 4 turns this into a test).

   ```
   flowchart TB
     subgraph adapters["Toolkit adapters"]
       fx["ui.fx: ReportView, ReportApp, FxReportScreen, ReportMvpApp"]
       console["ui.console: ConsoleReportView"]
     end
     subgraph free["Toolkit-free"]
       ui["ui: SalesModel, SalesEvent, ReportController, InputRules"]
       mvp["ui.mvp: ReportPresenter, ReportScreen, RowItem"]
       ingest["ingest: CsvTransactionParser, CsvMonthReader"]
       engine["engine: DefaultSalesAggregator"]
       model["model: SaleTransaction, BranchSummary"]
     end
     fx --> ui
     fx --> mvp
     console --> ui
     mvp --> ui
     ui --> ingest
     ui --> engine
     ingest --> model
     engine --> model
   ```

   ```
   sales-report-<team>/
   ├── pom.xml
   ├── .gitignore                         # target/, data/
   ├── src/main/java/edu/itc/salesreport/
   │   ├── model/  ingest/  engine/       # starter kit + Task 1
   │   └── ui/  ui/console/  ui/fx/  ui/mvp/  ui/fx/legacy/
   ├── src/test/java/edu/itc/salesreport/ # Tasks 1, 4, 5
   └── lab02/
       ├── README.md                      # Task 5: record what changed
       ├── screenshots/mvc-viewer.png     # Task 2
       ├── screenshots/validation.png     # Task 3
       ├── refactoring.md                 # Task 4
       ├── mvc-vs-mvp.md                  # Task 5
       └── adr-002-presentation-pattern.md  # Task 5
   ```

1

## An observable, toolkit-free model

Easy
60 min

### Goal

Write the M of MVC: `SalesModel` loads one month of branch CSV files through the canonical parser, computes a `BranchSummary` for the selected branch, and notifies listeners of every change. Prove it with a console view and unit tests, before any widget exists.

### Steps

1. In `ingest`, create `record MonthData(YearMonth month, List<SaleTransaction> rows, List<String> rejected)` (defensive `List.copyOf`) and `CsvMonthReader(TransactionParser)` with `MonthData read(Path dataDir, YearMonth month) throws IOException`: list `dataDir/yyyy-MM/`, keep files matching `(PNH|REP|BTB)-\d{4}-\d{2}-\d{2}\.csv` sorted by name, skip each header line, collect `InvalidRowException`s as `"<file>:<line> <reason>"`. Throw `NoSuchFileException` when the month folder does not exist.
2. In `engine`, create `SalesAggregator` (`BranchSummary summarize(String branch, List<SaleTransaction> rows)`) and `DefaultSalesAggregator`: sum revenue and discounts, count distinct receipt numbers, revenue by category (`TreeMap`) and by payment (`EnumMap`), top 10 products by revenue (ties by SKU).
3. In `ui`, create the sealed `SalesEvent` (`MonthLoaded(month, rows, rejected)`, `BranchSelected(branch)`), the functional interface `SalesModelListener` and `SalesModel` from the starter. `selectBranch` fires only when the branch actually changes; a failed `load` leaves the old data in place and fires nothing.
4. Create `ui.console.ConsoleReportView`: it registers itself in its constructor and prints one line per event plus the summary (starter below). Run it and compare with the expected output.
5. Copy `TestData` into `src/test/java/edu/itc/salesreport/` and write `ui.SalesModelTest` with four tests: load fires `MonthLoaded(2026-09, 3, 1)` and the chain revenue is `52.00`; selecting `PNH` twice fires once and gives revenue and basket `31.00`; a load of `2026-07` throws `NoSuchFileException`, fires nothing and keeps 3 rows; a removed listener is not called.
6. Run `mvn test`. Commit as "Task 1: observable SalesModel" (each partner commits at least one of the classes).

### Starter code

```
// src/main/java/edu/itc/salesreport/ui/SalesModel.java
package edu.itc.salesreport.ui;

// imports: engine.SalesAggregator, ingest.CsvMonthReader, ingest.MonthData,
// model.*, java.io.IOException, java.nio.file.Path, java.time.YearMonth,
// java.util.*, java.util.concurrent.CopyOnWriteArrayList  (no javafx!)

/** MVC model of the report viewer. Confined to one thread (the UI thread). */
public final class SalesModel {
    public static final String ALL = "ALL";

    private final CsvMonthReader reader;
    private final SalesAggregator aggregator;
    private final List<SalesModelListener> listeners = new CopyOnWriteArrayList<>();
    private MonthData data;            // null until the first successful load
    private String branch = ALL;

    public SalesModel(CsvMonthReader reader, SalesAggregator aggregator) {
        this.reader = reader;
        this.aggregator = aggregator;
    }

    public void addListener(SalesModelListener l) { listeners.add(l); }
    public void removeListener(SalesModelListener l) { listeners.remove(l); }

    /** Replaces the data; on failure the old state is kept. */
    public void load(Path dataDir, YearMonth month) throws IOException {
        // TODO read first, then assign, then fire MonthLoaded(month, rows, rejected)
    }

    public void selectBranch(String newBranch) {
        // TODO ignore an unchanged branch (no event), else assign and fire
    }

    public String branch() { return branch; }

    public Optional<YearMonth> month() {
        return Optional.ofNullable(data).map(MonthData::month);
    }

    public List<SaleTransaction> visibleTransactions() {
        // TODO List.of() before the first load; all rows for ALL;
        //      otherwise only the rows of the selected branch
        return List.of();
    }

    public BranchSummary summary() {
        return aggregator.summarize(branch, visibleTransactions());
    }

    public List<String> rejectedRows() {
        return data == null ? List.of() : data.rejected();
    }

    private void fire(SalesEvent event) {
        for (var l : listeners) l.modelChanged(event);
    }
}

// src/main/java/edu/itc/salesreport/ui/console/ConsoleReportView.java (core)
@Override
public void modelChanged(SalesEvent event) {
    out.println(switch (event) {
        case MonthLoaded(var month, var rows, var rejected) ->
            "Loaded %s: %d rows, %d rejected".formatted(month, rows, rejected);
        case BranchSelected(var branch) -> "Branch " + branch;
    });
    var s = model.summary();
    out.printf("  %-4s revenue %9s  receipts %4d  basket %5s%n",
            s.branch(), s.revenue(), s.receipts(), s.averageBasket());
    s.revenueByCategory().forEach(
            (cat, rev) -> out.printf("    %-14s %9s%n", cat, rev));
}
// main: build the model, new ConsoleReportView(model, System.out),
//       model.load(Path.of("data"), YearMonth.parse("2026-09")),
//       model.selectBranch("PNH")

// src/test/java/edu/itc/salesreport/TestData.java (copy as is)
package edu.itc.salesreport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Two branch files for 2026-09: three valid rows, one invalid row. */
public final class TestData {
    public static final String HEADER = "branch,date,receipt_no,sku,"
            + "product_name,category,quantity,unit_price,discount,"
            + "payment_method";

    private TestData() {}

    public static Path writeSample(Path dataDir) throws IOException {
        Path month = Files.createDirectories(dataDir.resolve("2026-09"));
        Files.writeString(month.resolve("PNH-2026-09-01.csv"), """
            %s
            PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,KHQR
            PNH,2026-09-01,PNH-000001,SKU-5002,Power Bank 10000mAh,Electronics,1,18.90,0.90,KHQR
            PNH,2026-09-01,PNH-000002,SKU-2001,Drinking Water 1.5L,Beverages,-3,0.60,0.00,CASH
            """.formatted(HEADER));
        Files.writeString(month.resolve("REP-2026-09-01.csv"), """
            %s
            REP,2026-09-01,REP-000001,SKU-4003,Sunscreen SPF50,Personal Care,3,7.50,1.50,CASH
            """.formatted(HEADER));
        return dataDir;
    }
}
```

### Expected output

```
$ java -cp target/classes edu.itc.salesreport.ui.console.ConsoleReportView data
Loaded 2026-09: 3600 rows, 0 rejected
  ALL  revenue  26721.90  receipts 1493  basket 17.90
    Beverages        1131.40
    Electronics     11173.40
    Grocery          4114.30
    Household        3404.90
    Personal Care    5520.30
    Stationery       1377.60
Branch PNH
  PNH  revenue   8455.90  receipts  489  basket 17.29
    Beverages         402.30
    Electronics      3404.50
    Grocery          1281.80
    Household        1094.40
    Personal Care    1801.20
    Stationery        471.70

$ mvn test
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in edu.itc.salesreport.ui.SalesModelTest
```

[Show hints](#hint1)

- `Files.list` returns a stream that holds a directory handle: use it in try-with-resources, or Windows keeps the folder locked.
- Assign `data` only after `reader.read` returned: if it throws, the assignment never happens and the model stays consistent.
- A listener in a test can be a method reference: `model.addListener(events::add)` with `List<SalesEvent> events`. Records give you `equals`, so `assertEquals(List.of(new MonthLoaded(...)), events)` works.
- The fixture's 52.00 is 13.00 (rice) + 18.00 (power bank, 18.90 − 0.90) + 21.00 (sunscreen, 22.50 − 1.50); the water row with quantity −3 is rejected.
- If your chain revenue is not 26721.90, compare row counts first (3,600), then category by category.

### Acceptance checklist

`CsvMonthReader` and `DefaultSalesAggregator` exist; the console view prints the figures above for `data/2026-09`.`SalesModel` fires `MonthLoaded` and `BranchSelected`, and fires nothing for an unchanged branch or a failed load.No class in `model`, `ingest`, `engine` or `ui` imports `javafx.*`.`SalesModelTest` has 4 passing tests based on `TestData`; both partners have commits.

2

## View: transaction table and category chart

Easy
60 min

### Goal

Write the V of MVC in JavaFX: a `ReportView` that observes `SalesModel` and renders the visible transactions in a `TableView` and the revenue by category in a `BarChart`. The view contains no calculation: everything it shows comes from the model.

### Steps

1. Create `ui.ErrorDisplay` (`showError(String)`, `clearError()`); the controller of Task 3 will talk to the view only through it.
2. Create `ui.fx.ReportView implements SalesModelListener, ErrorDisplay` with: a month `TextField` (default `2026-09`), a branch `ComboBox` (`ALL`, `PNH`, `REP`, `BTB`), a `TableView<SaleTransaction>` with columns Date, Receipt, Product, Category, Qty, Revenue, a `BarChart<String, Number>` titled "Revenue by category (USD)", a status `Label` and a red error `Label`. Layout: filters in an `HBox` on top, `SplitPane(table, chart)` in the centre, labels at the bottom of a `BorderPane`.
3. Implement `modelChanged`: set the combo box to `model.branch()`, replace the table items, rebuild one chart series from `summary().revenueByCategory()`, and set the status text with a `switch` over the event.
4. Create `ui.fx.ReportApp extends Application`. For now, `start` builds the model and the view, shows the stage and calls `model.load(Path.of("data"), YearMonth.of(2026, 9))` directly (Task 3 replaces this with the controller).
5. Run `mvn javafx:run`. Check the status line against the Task 1 console output, switch nothing yet, and save a screenshot as `lab02/screenshots/mvc-viewer.png`.
6. Optional: build the same layout in FXML (`src/main/resources/edu/itc/salesreport/ui/fx/report.fxml`) with an `fx:controller` class `ReportFxmlView`; see the hints for what that class may contain.

### Starter code

```
// src/main/java/edu/itc/salesreport/ui/fx/ReportView.java
package edu.itc.salesreport.ui.fx;

/** MVC view: observes SalesModel, forwards gestures to the controller. */
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
        branchBox.getItems().setAll("ALL", "PNH", "REP", "BTB");
        branchBox.setValue("ALL");
        table.getColumns().setAll(List.of(
                column("Date", t -> t.date().toString()),
                column("Receipt", SaleTransaction::receiptNo)
                // TODO Product, Category, Qty, Revenue
        ));
        chart.setTitle("Revenue by category (USD)");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        error.setStyle("-fx-text-fill: firebrick;");
        // TODO layout: HBox(filters) top, SplitPane(table, chart) centre,
        //      VBox(status, error) bottom
        model.addListener(this);
    }

    public Parent root() { return root; }

    @Override
    public void modelChanged(SalesEvent event) {
        // TODO 1. branchBox.setValue(model.branch())
        // TODO 2. table.getItems().setAll(model.visibleTransactions())
        // TODO 3. one XYChart.Series from model.summary().revenueByCategory()
        // TODO 4. status text: switch (event) { case MonthLoaded m -> ...
        //                                       case BranchSelected b -> ... }
    }

    @Override public void showError(String message) { error.setText(message); }
    @Override public void clearError() { error.setText(""); }

    // records have no JavaFX properties: adapt each column with a lambda
    private static TableColumn<SaleTransaction, String> column(String title,
            Function<SaleTransaction, String> value) {
        var col = new TableColumn<SaleTransaction, String>(title);
        col.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(value.apply(c.getValue())));
        return col;
    }
}
```

### Expected output

The window after `mvn javafx:run` (layout sketch; your bar colours come from the JavaFX default theme):

[Show hints](#hint2)

- `PropertyValueFactory` looks for `getX()` or `xProperty()`; record accessors are called `x()`, so use the lambda `column(...)` helper instead.
- `chart.getData().setAll(List.of(series))` replaces the old series; adding a new series each time stacks bars. `BigDecimal` is a `Number`, so it can go straight into `XYChart.Data`.
- "Unable to open DISPLAY" or "Graphics Device initialization failed" means there is no desktop session; the tests still run.
- FXML: the class named in `fx:controller` holds `@FXML` fields and `onAction` methods. In MVC terms it is part of the view: let it implement `ErrorDisplay` and forward to `ReportController`, nothing more.

### Acceptance checklist

`mvn javafx:run` shows 3,600 rows, six bars and the status line above; the screenshot is in `lab02/screenshots/`.`ReportView` refreshes only in `modelChanged`; it computes no totals and reads no files.All JavaFX imports are in `ui.fx`; `ErrorDisplay` lives in the toolkit-free `ui` package.The console view from Task 1 still works unchanged next to the JavaFX view.

3

## Controller: filters, validation, error display

Medium
75 min

### Goal

Write the C of MVC: `ReportController` turns raw text from the month field and the branch combo box into validated command records, applies them to the model, and reports every problem through `ErrorDisplay`. The controller has no JavaFX import.

### Steps

1. Create `ui.Validation<T>`, a sealed interface with records `Valid<T>(T value)` and `Invalid<T>(String message)`.
2. Create `ui.InputRules` with `month(String text, Clock clock)` and `branch(String text)`, returning exactly the messages in the expected-output table. Trim input; upper-case branch codes.
3. Create the sealed `ui.ReportCommand` with `LoadMonth(YearMonth)` and `SelectBranch(String)`.
4. Create `ui.ReportController(SalesModel model, Path dataDir, Clock clock, ErrorDisplay errors)` with `monthEntered(String)`, `branchChosen(String)` and a package-private `execute(ReportCommand)` that switches over the command, calls `errors.clearError()` on success and maps `NoSuchFileException` to `"No sales data found in <path>"`.
5. Add `ReportView.bind(ReportController)`: `monthField.setOnAction` and `branchBox.setOnAction` forward the raw text, nothing else. In `ReportApp.start`, create the controller with `Clock.systemDefaultZone()`, call `view.bind(controller)` and replace the direct `model.load` with `controller.monthEntered("2026-09")`.
6. Try every input in the table below in the running viewer. Then append the broken row `PNH,2026-09-15,PNH-999999,SKU-1001,Jasmine Rice 5kg,Grocery,-1,6.50,0.00,KHQR` to `data/2026-09/PNH-2026-09-15.csv`, press Enter in the month field, and check that the status line reports `1 rejected` while the revenue stays `26721.90`. Save `lab02/screenshots/validation.png` showing one error message.

### Starter code

```
// src/main/java/edu/itc/salesreport/ui/InputRules.java
/** Pure input rules: no UI toolkit, no I/O, clock injected. */
public final class InputRules {
    private static final Set<String> BRANCHES =
            Set.of(SalesModel.ALL, "PNH", "REP", "BTB");

    private InputRules() {}

    public static Validation<YearMonth> month(String text, Clock clock) {
        if (text == null || text.isBlank()) {
            return new Invalid<>("Enter a month as yyyy-MM, e.g. 2026-09");
        }
        // TODO YearMonth.parse(text.strip()); on DateTimeParseException:
        //      "'<text>' is not a month in the form yyyy-MM"
        // TODO after YearMonth.now(clock): "<month> is in the future"
        return null;
    }

    public static Validation<String> branch(String text) {
        // TODO strip + upper-case; unknown: "Unknown branch '<CODE>': use ALL, PNH, REP or BTB"
        return null;
    }
}

// src/main/java/edu/itc/salesreport/ui/ReportController.java
/** MVC controller: raw input, then validation, command, model. */
public final class ReportController {
    // fields + constructor: model, dataDir, clock, errors

    public void monthEntered(String text) {
        switch (InputRules.month(text, clock)) {
            case Valid(var month) -> execute(new LoadMonth(month));
            case Invalid(var message) -> errors.showError(message);
        }
    }

    public void branchChosen(String text) {
        // TODO same shape with InputRules.branch and SelectBranch
    }

    void execute(ReportCommand command) {
        try {
            switch (command) {
                case LoadMonth(var month) -> model.load(dataDir, month);
                case SelectBranch(var branch) -> model.selectBranch(branch);
            }
            errors.clearError();
        } catch (NoSuchFileException e) {
            errors.showError("No sales data found in " + e.getMessage());
        } catch (IOException e) {
            errors.showError("Could not read the sales files: " + e);
        }
    }
}
```

### Expected output

| Input (then Enter / pick)        | Model change                                      | Error label shows                                                             |
| -------------------------------- | ------------------------------------------------- | ----------------------------------------------------------------------------- |
| month `2026-08`                | MonthLoaded: 3720 rows                            | (empty)                                                                       |
| month `(blank)`                | none                                              | `Enter a month as yyyy-MM, e.g. 2026-09`                                    |
| month `2026/09`                | none                                              | `'2026/09' is not a month in the form yyyy-MM`                              |
| month `2026-13`                | none                                              | `'2026-13' is not a month in the form yyyy-MM`                              |
| a future month, e.g. `2099-01` | none                                              | `2099-01 is in the future`                                                  |
| month `2026-07`                | none, old data kept                               | `No sales data found in data\2026-07` (`data/2026-07` on Linux and macOS) |
| branch `PNH` with 2026-09      | BranchSelected: revenue USD 8455.90, receipts 489 | (empty)                                                                       |

```
status after the broken row is appended and 2026-09 reloaded:
2026-09: 3600 rows, 1 rejected · revenue USD 26721.90 · receipts 1493
rejected row: PNH-2026-09-15.csv:42 quantity must be positive: -1
```

[Show hints](#hint3)

- With `import edu.itc.salesreport.ui.Validation.Valid;` the pattern `case Valid(var month)` is enough: the compiler infers `Valid<YearMonth>` from the selector type.
- A `switch` over a sealed interface needs no `default`; leave it out so that a new command type becomes a compile error everywhere it is not handled.
- Setting the combo box in `modelChanged` fires its `onAction` again. That is harmless only because `selectBranch` ignores an unchanged branch: check it by putting a breakpoint in `fire`.
- To see the rejected rows, show `model.rejectedRows().getFirst()` in a tooltip or a second label when the list is not empty; do not move the list into the view.

### Acceptance checklist

Every row of the table above behaves as stated; no input produces a stack trace or a silent failure.`ReportController`, `InputRules`, `Validation` and `ReportCommand` import nothing from JavaFX; the `Clock` is injected.The view's event handlers are one-liners that forward raw text to the controller.With the broken row the viewer reports 1 rejected row and the chain revenue stays USD 26,721.90; `validation.png` is committed.

4

## Tests without the UI, and a fat controller

Medium
75 min

### Goal

Make the MVC split pay off: test the parser, the input rules, the model and the controller on a machine without a display, make the "no toolkit in the core" rule executable, and refactor a legacy fat controller into the structure you built.

### Steps

1. `ingest.CsvTransactionParserTest`: the canonical example row gives revenue `13.00` and `KHQR`; a `@ParameterizedTest` with `@ValueSource` rejects at least 7 invalid rows (unknown branch, bad date, quantity 0, discount above the line amount, unknown category, unknown payment, missing fields).
2. `ui.InputRulesTest` with `Clock.fixed(Instant.parse("2026-09-27T03:00:00Z"), ZoneId.of("Asia/Phnom_Penh"))`: a trimmed valid month, six invalid inputs (`""`, `" "`, `2026/09`, `2026-9`, `Sept`, `2026-10`), the exact future-month message, branch normalisation.
3. `ui.ReportControllerTest` with a recording `FakeErrors implements ErrorDisplay`: a valid month loads and clears the error; `09/2026` never reaches the model; `2026-07` becomes "No sales data found…"; `branchChosen("rep")` leaves 1 visible row.
4. `UiIndependenceTest`: read every `.java` file directly in `model`, `ingest`, `engine`, `ui` (and later `ui/mvp`) and fail if a line starts with `import javafx.`, `import java.awt.` or `import javax.swing.`. Break the rule once on purpose to see it fail, then revert.
5. Copy the legacy class below into `ui/fx/legacy/FatReportController.java`. In `lab02/refactoring.md`, list each smell with its consequence (at least five), and compute what it shows for 2026-09 `ALL` with the broken row of Task 3 present (it silently subtracts 6.50). Then replace its body so that `onLoad()` only forwards `monthField.getText()` to `ReportController`, and record the before/after line counts.
6. Run `mvn test` with the display disconnected or over SSH if you can: it must pass. Commit as "Task 4: headless tests and refactoring".

### Starter code

```
// src/main/java/edu/itc/salesreport/ui/fx/legacy/FatReportController.java
package edu.itc.salesreport.ui.fx.legacy;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Smell on purpose: I/O, parsing, filtering and totals in a handler. */
public class FatReportController {
    private final TextField monthField;
    private final ComboBox<String> branchBox;
    private final Label status;

    public FatReportController(TextField monthField,
            ComboBox<String> branchBox, Label status) {
        this.monthField = monthField;
        this.branchBox = branchBox;
        this.status = status;
    }

    public void onLoad() {
        try {
            var dir = Path.of("data", monthField.getText());
            BigDecimal revenue = BigDecimal.ZERO;
            for (Path f : Files.list(dir).toList()) {
                var lines = Files.readAllLines(f);
                for (String line : lines.subList(1, lines.size())) {
                    String[] c = line.split(",");
                    if (!branchBox.getValue().equals("ALL")
                            && !c[0].equals(branchBox.getValue())) continue;
                    revenue = revenue.add(new BigDecimal(c[7])
                            .multiply(new BigDecimal(c[6]))
                            .subtract(new BigDecimal(c[8])));
                }
            }
            status.setText("Revenue: " + revenue);
        } catch (Exception e) {
            status.setText("Error");
        }
    }
}

// src/test/java/edu/itc/salesreport/ui/ReportControllerTest.java (skeleton)
/** No JavaFX here: the view is replaced by a recording fake. */
class ReportControllerTest {
    static final class FakeErrors implements ErrorDisplay {
        final List<String> shown = new ArrayList<>();
        @Override public void showError(String message) { shown.add(message); }
        @Override public void clearError() { shown.add("<cleared>"); }
    }

    @TempDir Path dataDir;
    private final FakeErrors errors = new FakeErrors();
    private final List<SalesEvent> events = new ArrayList<>();
    private SalesModel model;
    private ReportController controller;

    @BeforeEach
    void setUp() throws IOException {
        TestData.writeSample(dataDir);
        // TODO model + events::add listener + controller with a fixed clock
    }

    @Test
    void invalidMonthNeverReachesModel() {
        controller.monthEntered("09/2026");
        assertTrue(events.isEmpty());
        assertEquals(List.of("'09/2026' is not a month in the form yyyy-MM"),
                errors.shown);
    }
    // TODO validMonthLoadsModelAndClearsError, missingFolderBecomesMessage,
    //      branchFilterNarrowsRows
}
```

### Expected output

```
$ mvn test
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- in edu.itc.salesreport.ingest.CsvTransactionParserTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- in edu.itc.salesreport.ui.InputRulesTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in edu.itc.salesreport.ui.ReportControllerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in edu.itc.salesreport.ui.SalesModelTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in edu.itc.salesreport.UiIndependenceTest
[INFO] Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS          (about 0.3 s of test time, no window opened)
```

| lab02/refactoring.md (excerpt)  | Fat controller                        | After refactoring                              |
| ------------------------------- | ------------------------------------- | ---------------------------------------------- |
| 2026-09 ALL, broken row present | Revenue: 26715.40 (wrong, no warning) | USD 26721.90 · 1 rejected                     |
| Month `2026/09`               | "Error"                               | "'2026/09' is not a month in the form yyyy-MM" |
| Lines in the handler            | about 20                              | 1                                              |
| Tests possible without JavaFX   | 0                                     | all rules                                      |

[Show hints](#hint4)

- Prefer a hand-written fake to a Mockito mock for `ErrorDisplay`: the list of shown messages is exactly what you want to assert, in order.
- Surefire runs tests with the project folder as working directory, so `Path.of("src/main/java/edu/itc/salesreport")` works in `UiIndependenceTest`. Use `Files.list` (direct children), not `Files.walk`, so `ui/fx` is not scanned as part of `ui`.
- Smells to look for: unclosed `Files.list`, no row validation, no rounding, `catch (Exception)` with a useless message, widgets read inside the loop, hard-coded `"data"`, `split(",")` dropping trailing empty fields.
- A test that calls `Platform.startup` or uses TestFX is a UI test: keep it out of this task (see the rubric).

### Acceptance checklist

Parser, input-rule, model and controller tests pass with `mvn test`; none starts JavaFX.All time-dependent tests use a fixed `Clock` and pass on any date.`UiIndependenceTest` passes and was seen failing once when a `javafx` import was added to `ui`.`lab02/refactoring.md` lists at least five smells with consequences and the before/after figures; `FatReportController` now only delegates.

5

## MVP variant, comparison, record what changed

Hard
90 min

### Goal

Build the same screen as Model View Presenter with a passive view, test all of its display text without JavaFX, compare MVC and MVP with evidence from your own code, decide which one the team keeps, and record what this lab changed.

### Steps

1. In the toolkit-free package `ui.mvp`, create `record RowItem(String date, String receipt, String product, String category, String quantity, String revenue)`, the passive-view interface `ReportScreen` and `ReportPresenter implements SalesModelListener` (starter below). The presenter formats money with `String.format(Locale.US, "%,.2f", amount)`.
2. In `ui.fx`, create `FxReportScreen implements ReportScreen` (same widgets as `ReportView`, but only setters and an `attach(ReportPresenter)` that forwards gestures) and `ReportMvpApp`. Run it with `mvn javafx:run -Djavafx.mainClass=edu.itc.salesreport.ui.fx.ReportMvpApp`.
3. Write `ui.mvp.ReportPresenterTest` with a recording `FakeScreen`: after `monthEntered("2026-09")` and `branchChosen("PNH")` on `TestData`, the status is exactly `PNH · 1 receipts · revenue USD 31.00 · basket USD 31.00` and the second row is `RowItem("2026-09-01", "PNH-000001", "Power Bank 10000mAh", "Electronics", "1", "18.00")`; `money(1234.5)` is `1,234.50`. Add `ui/mvp` to `UiIndependenceTest`.
4. Measure both variants and fill `lab02/mvc-vs-mvp.md` (template below): lines of code per class (`wc -l` or `(Get-Content f).Count`), number of tests that need no toolkit, where formatting lives, and what you had to change to add a "Discount" column in each variant (do it in both).
5. Write `lab02/adr-002-presentation-pattern.md`: which variant the team keeps for Lab-03 and why, citing one quality-attribute scenario from Lab-01 (or the fallback).
6. **Record what changed:** create `lab02/README.md` with a table of this lab's files (path, task, author), the Lab-01 inputs reused with their commit hash or tag (or "fallback used"), and a change-log row. Tag the commit `lab-02`.

### Starter code

```
// src/main/java/edu/itc/salesreport/ui/mvp/ReportScreen.java
/** Passive view: no model access, no formatting, no decisions. */
public interface ReportScreen {
    void showRows(List<RowItem> rows);
    void showCategoryRevenue(Map<String, BigDecimal> revenue);
    void showStatus(String text);
    void showError(String message);
}

// src/main/java/edu/itc/salesreport/ui/mvp/ReportPresenter.java
/** MVP presenter: handles input and pushes formatted data. */
public final class ReportPresenter implements SalesModelListener {
    private final SalesModel model;
    private final ReportScreen screen;
    private final Path dataDir;
    private final Clock clock;

    public ReportPresenter(SalesModel model, ReportScreen screen,
            Path dataDir, Clock clock) {
        this.model = model;
        this.screen = screen;
        this.dataDir = dataDir;
        this.clock = clock;
        model.addListener(this);
    }

    public void monthEntered(String text) {
        // TODO InputRules.month; Valid: model.load (IOException ->
        //      screen.showError("No sales data for " + month)); Invalid: showError
    }

    public void branchChosen(String text) {
        // TODO InputRules.branch; Valid: model.selectBranch; Invalid: showError
    }

    @Override
    public void modelChanged(SalesEvent event) {
        var s = model.summary();
        // TODO screen.showRows(...toRow...), showCategoryRevenue(...)
        // TODO showStatus("%s · %d receipts · revenue USD %s · basket USD %s"
        //                  .formatted(...)), then showError("")
    }

    static String money(BigDecimal amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }
}
```

```
# lab02/mvc-vs-mvp.md
| Criterion                                  | MVC (ReportView + ReportController) | MVP (FxReportScreen + ReportPresenter) |
|--------------------------------------------|-------------------------------------|----------------------------------------|
| Lines of code (view / controller-presenter)|                                     |                                        |
| Tests that run without JavaFX              |                                     |                                        |
| Where money and status text are formatted  |                                     |                                        |
| Files changed to add a "Discount" column   |                                     |                                        |
| Does the view hold a model reference?      |                                     |                                        |
| Effort for a second screen (web, Swing)    |                                     |                                        |
```

### Expected output

```
classDiagram
  direction LR
  class ReportScreen {
    <<interface>>
    +showRows(List~RowItem~)
    +showCategoryRevenue(Map)
    +showStatus(String)
    +showError(String)
  }
  class ReportPresenter {
    +monthEntered(String)
    +branchChosen(String)
    +modelChanged(SalesEvent)
  }
  class FxReportScreen {
    +attach(ReportPresenter)
  }
  class FakeScreen {
    +status String
  }
  FxReportScreen ..|> ReportScreen
  FakeScreen ..|> ReportScreen
  FxReportScreen --> ReportPresenter : gestures
  ReportPresenter --> ReportScreen : formatted values
  ReportPresenter --> SalesModel : commands
  SalesModel --> ReportPresenter : notifies
```

```
$ mvn test
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in edu.itc.salesreport.ui.mvp.ReportPresenterTest
[INFO] Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

[Show hints](#hint5)

- The status expects "1 receipts": keep the presenter's text exactly as specified; pluralisation is a nice presenter-level improvement once the test says so.
- `FxReportScreen` has a `TableView<RowItem>`: every cell is already a `String`, so the column helper becomes trivial. If you find an `if` in it, it probably belongs in the presenter.
- Both apps can run from the same project: they share `SalesModel` and only differ in the `ui.fx` classes and the composition root.
- For the ADR, name the trade-off explicitly: MVP costs one interface per screen and some boilerplate; it buys testable display logic. Lab-03 moves to MVVM, so say what you expect bindings to replace.

### Acceptance checklist

`ReportMvpApp` shows the same figures as `ReportApp`; `FxReportScreen` has no reference to `SalesModel`.`ReportPresenterTest` checks the exact status text and a formatted row without JavaFX; `ui/mvp` is covered by `UiIndependenceTest`.`mvc-vs-mvp.md` is filled with measured numbers and the "Discount" column exists in both variants; the ADR records the team's choice.`lab02/README.md` lists the files, the reused inputs (commit or fallback) and a change-log row; the commit is tagged `lab-02`.

## Challenge: a Spring MVC web view of the same model

Hard
90–120 min · optional, extra credit

### Scenario

Branch managers do not have the desktop viewer. Head office asks for a web page with the same monthly figures. Prove that your domain and model are UI-independent: add a Spring MVC controller and one Thymeleaf template in a separate project, and change **nothing** in `model`, `ingest`, `engine` or `ui`.

### Requirements

1. Run `mvn install` in `sales-report`. Create a sibling project `sales-web/` with parent `org.springframework.boot:spring-boot-starter-parent:4.1.1`, dependencies `edu.itc:sales-report:0.2.0-SNAPSHOT`, `spring-boot-starter-webmvc`, `spring-boot-starter-thymeleaf` and (test) `spring-boot-starter-webmvc-test`. `mvn dependency:tree -Dincludes=org.openjfx` must print no JavaFX artifact.
2. `SalesWebApp` declares beans for `CsvMonthReader`, `SalesAggregator` and `Clock`. `ReportWebController` handles `GET /report?month=&branch=`, validates both parameters with `InputRules`, creates one `SalesModel` per request, and puts `summary`, `rejected` or `error` into the Spring `Model`.
3. `templates/report.html` shows a month field, a branch select, the error, the totals line, revenue by category and the top products.
4. A `@WebMvcTest` with `MockMvcTester` checks one valid request and one invalid month.
5. Run it against the Task 1 data and compare with the desktop: `PNH` in 2026-09 must show USD 8455.90 and 489 receipts. `git diff --stat lab-02 -- src/main/java` in `sales-report` shows no change after the tag.

### Skeleton

```
// sales-web/src/main/java/edu/itc/salesreport/web/ReportWebController.java
@Controller
public class ReportWebController {
    private record Inputs(Validation<YearMonth> month, Validation<String> branch) {}

    private final CsvMonthReader reader;
    private final SalesAggregator aggregator;
    private final Clock clock;
    private final Path dataDir;

    public ReportWebController(CsvMonthReader reader, SalesAggregator aggregator,
            Clock clock, @Value("${salesreport.data-dir:data}") String dataDir) {
        this.reader = reader;
        this.aggregator = aggregator;
        this.clock = clock;
        // a String, not a Path: Spring would resolve a relative Path
        // against the servlet context instead of the working directory
        this.dataDir = Path.of(dataDir);
    }

    @GetMapping("/report")
    public String report(@RequestParam(defaultValue = "2026-09") String month,
            @RequestParam(defaultValue = "ALL") String branch, Model page) {
        page.addAttribute("month", month);
        page.addAttribute("branch", branch);
        page.addAttribute("branches", List.of("ALL", "PNH", "REP", "BTB"));
        var inputs = new Inputs(InputRules.month(month, clock),
                InputRules.branch(branch));
        switch (inputs) {
            case Inputs(Valid<YearMonth>(var m), Valid<String>(var b)) ->
                    show(m, b, page);
            // TODO the two Invalid cases -> page.addAttribute("error", msg)
        }
        return "report";
    }

    private void show(YearMonth month, String branch, Model page) {
        // TODO new SalesModel(reader, aggregator), load, selectBranch,
        //      attributes "summary" and "rejected"; IOException -> "error"
    }
}

// sales-web/src/test/java/edu/itc/salesreport/web/ReportWebControllerTest.java
@WebMvcTest(controllers = ReportWebController.class,
        properties = "salesreport.data-dir=target/test-data")
class ReportWebControllerTest {
    @Autowired MockMvcTester mvc;
    // @BeforeAll: write PNH and REP files into target/test-data/2026-09

    @Test
    void rendersSummaryOfOneBranch() {
        assertThat(mvc.get().uri("/report?month=2026-09&branch=PNH"))
                .hasStatusOk()
                .hasViewName("report")
                .bodyText().contains("Revenue USD 13.00", "Jasmine Rice 5kg");
    }
}
```

### Expected output

```
$ java -jar target/sales-web-0.2.0-SNAPSHOT.jar --salesreport.data-dir=../data
$ curl -s "localhost:8080/report?month=2026-09&branch=PNH"
<h1>Angkor Mart · 2026-09 · PNH</h1>
...
  <p>Revenue USD 8455.90 · receipts 489 · basket USD 17.29 · rejected rows 0</p>
  ...
      <td>Electronics</td><td>3404.50</td>
  ...
    <li>Power Bank 10000mAh: USD 2604.70</li>
    <li>Sunscreen SPF50: USD 1064.00</li>
$ curl -s "localhost:8080/report?month=2026-10" | grep error
<p class="error">2026-10 is in the future</p>      (when run in September 2026)
```

[Show hints](#hintC)

- Spring Boot 4 split the web starters: `spring-boot-starter-webmvc` for the runtime and `spring-boot-starter-webmvc-test` for `@WebMvcTest`, whose package is `org.springframework.boot.webmvc.test.autoconfigure`.
- Beans declared in the `@SpringBootApplication` class are loaded by `@WebMvcTest` too, so the controller's constructor dependencies are satisfied without mocks.
- In Thymeleaf, call record accessors as methods: `${summary.revenue()}`, `${p.productName()}`.
- A singleton controller must not share one `SalesModel` between requests: its selected branch is per-user state. One model per request (or per session) is correct.

### Acceptance checklist

`sales-web` builds with `mvn verify`, has no JavaFX dependency, and its `@WebMvcTest` passes.The web page shows the same figures as the desktop viewer for at least two branch and month combinations.Invalid month and branch parameters render a message, never a 500 page.`git diff --stat lab-02 -- src/main/java` in `sales-report` is empty: the domain and model were reused unchanged.

## Grading rubric

| Component                           | Points    | Full marks when…                                                                                                     |
| ----------------------------------- | --------- | --------------------------------------------------------------------------------------------------------------------- |
| Task 1 · Observable model          | 15        | Console figures match; events fire exactly as specified; four model tests pass.                                       |
| Task 2 · Table and chart view      | 15        | Window shows 3,600 rows, six bars and the correct status; the view contains no calculation.                           |
| Task 3 · Controller and validation | 20        | All seven inputs behave as in the table; commands and injected clock; the broken row is reported, not summed.         |
| Task 4 · Tests and refactoring     | 20        | Headless tests for parser, rules, model and controller; architecture test; refactoring note with figures.             |
| Task 5 · MVP and comparison        | 20        | Passive view with a tested presenter; measured comparison; ADR; README with inputs and change log.                    |
| Code quality and Git history        | 10        | Canonical names, small classes, meaningful commits from both partners, `mvn -q verify` green from a clean checkout. |
| ★ Challenge (extra credit)         | +20       | Spring MVC page with the same figures, tested, and no change to the reused packages.                                  |
| Total                               | 100 (+20) |                                                                                                                       |

**Automatic deductions:**

- −10 if any class in `model`, `ingest`, `engine`, `ui` or `ui.mvp` imports `javafx`, `java.awt` or `javax.swing` (the architecture test fails).
- −10 if the totals shown by the viewer differ from the model's (2026-09, `ALL`: USD 26,721.90, 1,493 receipts; `PNH`: USD 8,455.90, 489 receipts).
- −5 per business calculation (revenue, totals, filtering, rounding, file parsing) found in a view, an FXML controller class or `ReportController`.
- −5 per input from the Task 3 table that throws an uncaught exception or fails silently instead of showing a message.
- −5 if a model, controller or presenter test starts the JavaFX toolkit (`Platform.startup`, `Application.launch`, TestFX) or depends on today's date.

## Submission

1. Repository layout:

   ```
   sales-report-<team>/
   ├── pom.xml
   ├── .gitignore
   ├── src/main/java/edu/itc/salesreport/{model,ingest,engine,ui,ui/console,ui/fx,ui/mvp}
   ├── src/main/java/edu/itc/salesreport/ui/fx/legacy/FatReportController.java
   ├── src/test/java/edu/itc/salesreport/...   # at least 28 tests
   ├── lab02/
   │   ├── README.md
   │   ├── refactoring.md
   │   ├── mvc-vs-mvp.md
   │   ├── adr-002-presentation-pattern.md
   │   └── screenshots/{mvc-viewer.png,validation.png}
   └── sales-web/                               # challenge only
   ```
2. Self-check from a clean checkout (no display needed): `mvn -q verify` must succeed; for the challenge also `mvn install` in the root, then `mvn -q verify` in `sales-web/`.
3. Branch `lab-02`, pull request titled `Lab-02 – <team name>`, tag `lab-02` on the merged commit. Each partner must have commits on at least two tasks.

 [Back to Chapter 02 slides](Chapter-02.html)
 [Retake the chapter quiz](Chapter-02.html#26)
 [Lab-01](../01-IntroductionToSoftwareEngineeringConcepts/Lab-01.html)
