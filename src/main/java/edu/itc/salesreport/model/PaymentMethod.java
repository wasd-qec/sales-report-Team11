package edu.itc.salesreport.model;

/**
 * Supported payment methods for sales transactions.
 */
public enum PaymentMethod {
    CASH,
    CARD,
    KHQR;

    /**
     * Parses a string into a PaymentMethod enum constant.
     *
     * @param value the raw string representation (case-insensitive)
     * @return matching PaymentMethod
     * @throws IllegalArgumentException if the value does not match any valid method
     */
    public static PaymentMethod fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Payment method cannot be null");
        }
        return switch (value.trim().toUpperCase()) {
            case "CASH" -> CASH;
            case "CARD" -> CARD;
            case "KHQR" -> KHQR;
            default -> throw new IllegalArgumentException("Unknown or unsupported payment method: " + value);
        };
    }
}
