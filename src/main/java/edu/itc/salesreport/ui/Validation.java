package edu.itc.salesreport.ui;

/** Result of applying a pure input rule. */
public sealed interface Validation<T>
        permits Validation.Valid, Validation.Invalid {
    record Valid<T>(T value) implements Validation<T> {}

    record Invalid<T>(String message) implements Validation<T> {}
}
