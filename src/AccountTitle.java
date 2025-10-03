
import java.util.Date;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Joshua
 */
public class AccountTitle {
    private String title;
    private int number;
    private Transaction credit;
    private Transaction debit;
    private Date date;
    private String type; // Asset, Liability, Equity, Income, Expense
    
    public AccountTitle() {
        
    }
    public AccountTitle(String title, String type) {
        this.title = title;
        this.type = type;
    }
    public AccountTitle(String title, int number, Transaction debit, Transaction credit, Date date) {
        this.title = title;
        this.number = number; 
        this.debit = debit;
        this.credit = credit;
        this.date = date;
    }
    public AccountTitle(String title, int number, Transaction debit, Transaction credit, Date date, String type) {
        this.title = title;
        this.number = number;
        this.debit = debit;
        this.credit = credit;
        this.date = date;
        this.type = type;
    }

    //Setters
    public void setTitle(String title) { this.title = title; }
    public void setNumber(int number) { this.number = number; }
    public void setCredit(Transaction credit) { this.credit = credit; }
    public void setDebit(Transaction debit) { this.debit = debit; }
    public void setDate(Date date) { this.date = date; }
    public void setType(String type) { this.type = type; }
    // Getters
    public String getTitle() { return title; }
    public static final AccountTitle[] ACCOUNT_TITLE = {
        new AccountTitle("Cash", "Asset"),
        new AccountTitle("Accounts Recievable", "Asset"),
        new AccountTitle("Allowance for Doubtful Accounts", "Contra-Asset"),
        new AccountTitle("Inventory", "Asset"),
        new AccountTitle("Allowance for Invetory Write-Down", "Contra-Asset"),
        new AccountTitle("Office Supply", "Asset"),
        new AccountTitle("Prepaid Rent Expense", "Asset"),
        new AccountTitle("Prepaid Insurance Expense", "Asset"),
        new AccountTitle("Prepaid Supplies Expense", "Asset"),
        new AccountTitle("Prepaid Subscription Expense", "Asset"),
        new AccountTitle("Furniture & Fixture", "Asset"),
        new AccountTitle("Accumulated Depreciation - Furniture & Fixture", "Contra-Asset"),
        new AccountTitle("Equipment", "Asset"),
        new AccountTitle("Accumulated Depreciation - Equipment", "Contra-Asset"),
        new AccountTitle("Vehicle", "Asset"),
        new AccountTitle("Accumulated Depreciation - Vehicle", "Contra-Asset"),
        new AccountTitle("Machinery", "Asset"),
        new AccountTitle("Accumulated Depreciation - Machinery", "Contra-Asset"),
        new AccountTitle("Building", "Asset"),
        new AccountTitle("Accumulated Depreciation - Building", "Contra-Asset"),
        new AccountTitle("Land", "Asset"),
        
        new AccountTitle("Accounts Payable", "Liability"),
        new AccountTitle("Uneared Revenue", "Liability"),
        new AccountTitle("Bonds Payable", "Liability"),
        new AccountTitle("Salaries Payable", "Liability"),
        new AccountTitle("Wages Payable", "Liability"),
        new AccountTitle("Taxes Payable", "Liability"),
        new AccountTitle("Dividends Payable", "Liability"),
        new AccountTitle("Notes Payable", "Liability"),
        new AccountTitle("Loans Payable", "Liability"),
        new AccountTitle("Mortgage Payable", "Liability"),
        new AccountTitle("Vehicle Loan Payable", "Liability"),
        
        new AccountTitle("Capital", "Equity"),
        new AccountTitle("Retained Earnings", "Equity"),
        new AccountTitle("Drawings", "Contra-Equity"),
        new AccountTitle("Dividends", "Contra-Equity"),
        new AccountTitle("Teasury Stock", "Contra-Equity"),
        
        new AccountTitle("Sales Revenue", "Income"),
        new AccountTitle("Service Revenue", "Income"),
        new AccountTitle("Interest Income", "Income"),
        new AccountTitle("Rent Income", "Income"),
        new AccountTitle("Dividend Income", "Income"),
        new AccountTitle("Miscellaneous Income", "Income"),
        new AccountTitle("Gain On Sale of Assets", "Income"),
        
        new AccountTitle("Rent Expense", "Expense"),
        new AccountTitle("Salaries Expense", "Expense"),
        new AccountTitle("Wages Expense", "Expense"),
        new AccountTitle("Utilities Expense", "Expense"),
        new AccountTitle("Insurance Expense", "Expense"),
        new AccountTitle("Supplies Expense", "Expense"),
        new AccountTitle("Interest Expense", "Expense"),
        new AccountTitle("Subscription Expense", "Expense"),
        new AccountTitle("Taxes Expense", "Expense"),
        new AccountTitle("Cost of Goods Sold", "Expense"),
        new AccountTitle("Depreciation Expense - Furnitures & Fixture", "Expense"),
        new AccountTitle("Depreciation Expense - Equipment", "Expense"),
        new AccountTitle("Depreciation Expense - Vehicle", "Expense"),
        new AccountTitle("Depreciation Expense - Machinery", "Expense"),
        new AccountTitle("Depreciation Expense - Building", "Expense"),
        new AccountTitle("Advertising Expense", "Expense"),
        new AccountTitle("Office Supply Expense", "Expense"),
        new AccountTitle("Maintenance & Repairs Expense", "Expense"),
        new AccountTitle("Telephone Expense", "Expense"),
        new AccountTitle("Postage Expense", "Expense"),
        new AccountTitle("Travel Expense", "Expense"),
        new AccountTitle("Miscellaneous Expense", "Expense")
    }; 
    public int getNumber() { return number; }
    public Transaction getCredit() { return credit; }
    public Transaction getDebit() { return debit; }
    public Date getDate() { return date; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return title; // shown in JComboBox
    }
}
