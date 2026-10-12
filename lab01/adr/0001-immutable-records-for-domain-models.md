# ADR 0001: Immutable Records for Core Domain Models

## Context and Problem Statement
The sales reporting system ingests thousands of transactions per batch and performs multi-dimensional aggregations (by branch, by SKU, and by payment method). Mutable state or complex JavaBean classes with setters introduce risks of accidental state corruption, thread safety issues, and verbose boilerplate code. We need a lightweight, clean, and safe representation for domain entities (`SaleTransaction`, `BranchSummary`, `ProductTotal`, `MonthlyReport`).

## Decision Drivers
* Thread safety and concurrency friendliness.
* Data integrity: domain rules (e.g. non-negative prices, valid discount ranges, positive quantities) must be enforced at instantiation.
* Elimination of boilerplate (constructors, getters, `equals`, `hashCode`, `toString`).
* Strict architectural decoupling: domain models must have zero dependencies on I/O or rendering logic.

## Considered Options
1. Standard mutable JavaBean POJOs with getters and setters.
2. Lombok-annotated `@Value` classes.
3. Modern Java `record` types (Java 16+).

## Decision Outcome
Chosen option: **Modern Java `record` types**.

### Rationale
* Java Records provide language-native immutability, compact syntax, and transparent semantics for data carriers.
* Compact constructors allow centralized validation and defensive copying (e.g. `List.copyOf`, unmodifiable maps).
* No external libraries (like Lombok) or annotation processing dependencies required.
* Naturally safe for streaming operations and concurrent collections.

## Consequences
### Positive
* Predictable state: records cannot be mutated once parsed.
* Compact, highly readable codebase adhering to Domain-Driven Design value object principles.
* Easy testing: creating test fixtures does not require complex builders or state orchestration.

### Negative / Trade-offs
* Fields cannot be modified in-place; modifications require creating a new record instance (acceptable since transactions are read-only historical records).
