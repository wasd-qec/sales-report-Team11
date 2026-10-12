package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;

import java.nio.file.Path;

/**
 * Strategy interface for parsing a raw transaction string into a SaleTransaction model.
 */
public interface TransactionParser {

    /**
     * Parses a transaction line without file context.
     *
     * @param lineNumber 1-based line number in the source
     * @param line raw text line
     * @return parsed SaleTransaction
     * @throws InvalidRowException if the line format or values are invalid
     */
    default SaleTransaction parse(int lineNumber, String line) throws InvalidRowException {
        return parse(null, lineNumber, line);
    }

    /**
     * Parses a transaction line with file context.
     *
     * @param file source file path (may be null)
     * @param lineNumber 1-based line number in the source
     * @param line raw text line
     * @return parsed SaleTransaction
     * @throws InvalidRowException if the line format or values are invalid
     */
    SaleTransaction parse(Path file, int lineNumber, String line) throws InvalidRowException;
}
