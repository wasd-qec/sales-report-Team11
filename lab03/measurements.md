# Lab-03 Task 4: UI Responsiveness & Background Loading Measurements

## 1. Environment & Hardware Specifications

- **CPU**: Intel(R) Core(TM) Ultra 9 275HX (24 cores / 24 threads)
- **RAM**: 32 GB DDR5
- **OS**: Windows 11 Pro 64-bit
- **JDK**: OpenJDK 25.0.5.0.2
- **JavaFX**: 25.0.4
- **Dataset**: August 2026 (`2026-08`), 93 CSV files (3 branches × 31 days), 5,000 rows/day, seed 42 (465,000 total rows).

---

## 2. Benchmark Measurement Results

Benchmark executed using `edu.itc.salesreport.ui.LoadBenchmarkApp` (2 warmup runs discarded, 5 measured runs per mode):

```plaintext
mode           run  load ms  max frame gap ms
caller-thread    1      389               479
caller-thread    2      373               432
caller-thread    3      347               400
caller-thread    4      347               399
caller-thread    5      449               512
fx-task          1      372                32
fx-task          2      355                47
fx-task          3      334                33
fx-task          4      359                32
fx-task          5      342                33
caller-thread mean load    381 ms, worst frame gap    512 ms
fx-task       mean load    352 ms, worst frame gap     47 ms
```

### Summary Comparison Table
| Mode | Mean Load Time | Worst Frame Gap | Status |
| --- | --- | --- | --- |
| **CallerThreadJobs** (FX thread) | 381 ms | 512 ms | Frozen UI |
| **FxBackgroundJobs** (Worker Task) | 352 ms | 47 ms | Fluid 60 FPS |

---

## 3. Manual Observations & Analysis

When running under `CallerThreadJobs`, the entire JavaFX Application Thread is occupied by sequential disk I/O and transaction aggregation, resulting in a completely unresponsive interface. During the load window, mouse clicks are queued without feedback, window dragging stutters or shows OS "Not Responding" hints, and the Cancel button cannot be triggered. In contrast, `FxBackgroundJobs` delegates data processing to a background worker thread (`Executors.newVirtualThreadPerTaskExecutor()`) while coalescing progress notifications through JavaFX Task pulses. Even during intensive loads, the worst frame gap remains at 47 ms (well below the 100 ms threshold), allowing immediate window relocation, smooth real-time text input in the product search box, and instant task abortion upon clicking Cancel.
