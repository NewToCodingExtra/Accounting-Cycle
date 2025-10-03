
import java.math.BigDecimal;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Joshua
 */
public class LedgerSummary {
    private String accountName;
    private BigDecimal debitTotal;
    private BigDecimal creditTotal;

    public LedgerSummary(String accountName, BigDecimal debitTotal, BigDecimal creditTotal) {
        this.accountName = accountName;
        this.debitTotal = debitTotal;
        this.creditTotal = creditTotal;
    }

    public String getAccountName() { return accountName; }
    public BigDecimal getDebitTotal() { return debitTotal; }
    public BigDecimal getCreditTotal() { return creditTotal; }


}
