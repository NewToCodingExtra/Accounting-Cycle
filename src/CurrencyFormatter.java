/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Joshua
 */
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class CurrencyFormatter {
    private static final DecimalFormat PESO_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setCurrencySymbol("₱");
        symbols.setGroupingSeparator(',');
        symbols.setMonetaryDecimalSeparator('.');
        PESO_FORMAT = new DecimalFormat("¤#,##0.00", symbols);
    }

    /**
     * Formats a BigDecimal amount into a peso string with commas and two decimals.
     * E.g. 1234.5 → "₱1,234.50"
     */
    public static String formatPeso(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return PESO_FORMAT.format(amount);
    }

}
