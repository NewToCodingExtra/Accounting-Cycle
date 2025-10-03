
import com.toedter.calendar.JCalendar;
import com.toedter.calendar.JDateChooser;
import java.awt.Component;
import java.awt.TextField;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Joshua
 */
public class JEAndAdjusmentSC {
    AccountTitle[] account_title;
    
    public JEAndAdjusmentSC() {
    }
    public void feedComboBox(AccountTitle[] account_title, JComboBox<String> parent) {
        parent.setEditable(true);
        parent.setSelectedItem("Select an account title...");
        for (AccountTitle acc : account_title) {
            parent.addItem(acc.getTitle());
        }    
        autoCompleteComboBox(parent);
    }
    private void autoCompleteComboBox(JComboBox<String> component) {
        //contentPane.add(jComboBox1);
        AutoCompleteDecorator.decorate(component);
    }
    public boolean isValidAccountTitle(String input) {
        if (input == null) return false;
        String trimmed = input.trim();
        String titlePart;
        
        if (trimmed.contains(",")) {
            // owner prefix exists
            String[] parts = trimmed.split(",", 2); // split into at most 2
            titlePart = parts[1].trim(); // the actual account title
        } else {
            titlePart = trimmed;
        }

        // now check titlePart against your list
        for (AccountTitle acc : account_title) {
            if (acc.getTitle().equalsIgnoreCase(titlePart)) {
                return true;
            }
        }
        return false;
    }
    public BigDecimal parseCellToDouble(Object cell) {
        if (cell == null) return BigDecimal.ZERO;

        if (cell instanceof BigDecimal) {
            return (BigDecimal) cell;
        }
        if (cell instanceof Long || cell instanceof Integer || cell instanceof Short || cell instanceof Byte) {
            return new BigDecimal(((Number) cell).longValue());
        }
        if (cell instanceof Number) { // Float/Double or others
            // Use string constructor to avoid floating point binary issues
            return new BigDecimal(cell.toString());
        }

        // else treat as string (remove currency formatting)
        String s = cell.toString().replace("₱", "").replace(",", "").trim();
        if (s.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(s);
    
    }
    public void changeDefaultDate(JDateChooser calendar, int start_year, int start_month) {
        Calendar cal = Calendar.getInstance();
        System.out.println("before using start_month: "+ start_month);
        System.out.println("before start_year: "+ start_year);
        cal.set(Calendar.YEAR, start_year);
        cal.set(Calendar.MONTH, start_month - 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        System.out.println("after using start_month: "+ start_month);
        System.out.println("after start_year: "+ start_year);
        Date defaultDate = cal.getTime();
        System.out.println(defaultDate);
        calendar.setDate(defaultDate);
    }
    public void jComboBoxEditor(TextField component, JComboBox combo) {
        JTextField editor = (JTextField) combo.getEditor().getEditorComponent();

        // Handle Enter key
        editor.addActionListener(e -> {
            component.repaint();
        });

        // Handle losing focus
        editor.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                component.repaint();
            }
        });
    }
    public BigDecimal sumDebitTotal(int transacNo, JTable table) {
        BigDecimal sum = BigDecimal.ZERO;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for(int x = 0; x < model.getRowCount(); x++) {
            int rowTransNo = Integer.parseInt(model.getValueAt(x, 2).toString());
            //DEBUG
            BigDecimal rowDebit = parseCellToDouble(model.getValueAt(x,3));
            
            System.out.println("DEBUG: sumDebitTotal: row "+ x + " trasNo = " + rowTransNo + "debit =" + rowDebit);
            //END OF DEBUG        
            if(rowTransNo == transacNo) {
                sum = sum.add( parseCellToDouble(rowDebit));
            }
        }
        System.out.println("DEBUG: sumDebitTotal: total debit for transac n0  "+ transacNo + "=" + sum);
        return sum;
    }
    public BigDecimal sumCreditTotal(int transacNo, JTable table) {
        BigDecimal sum = BigDecimal.ZERO;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for(int x = 0; x < model.getRowCount(); x++) {
            int rowTransNo = Integer.parseInt(model.getValueAt(x, 2).toString());
            BigDecimal rowCredit = parseCellToDouble(model.getValueAt(x, 4));
            
            System.out.println("DEBUG: sumCreditTotal: row "+ x + " trasNo = " + rowTransNo + "credit =" + rowCredit);
            if(rowTransNo == transacNo) {
                sum = sum.add(parseCellToDouble(rowCredit));
            }
        }
        System.out.println("DEBUG: sumCreditTotal: total credit for transac n0  "+ transacNo + "=" + sum);
        return sum;
    }
}
