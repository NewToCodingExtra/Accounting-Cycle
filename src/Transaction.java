
import java.math.BigDecimal;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Joshua
 */
public class Transaction {
    private BigDecimal amount;
    private int transactionNo;

    public Transaction() {
        
    }
    public Transaction(BigDecimal amount, int transactionNo) {
        this.amount = amount;
        this.transactionNo = transactionNo;
    }

    public BigDecimal getAmount() { return amount; }
    public int getTransactionNo() { return transactionNo; }

    @Override
    public String toString() {
        return transactionNo + " : " + amount; // optional for debugging
    }
}

