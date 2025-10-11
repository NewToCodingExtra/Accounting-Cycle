
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.sql.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author Joshua
 */
public class SProfitOrLoss extends javax.swing.JFrame {

    /**
     * Creates new form UnadjustTB
     */
    private home hm;
    private ShowInitialDetails SI;
    private int projectId;
    private int parentX;
    private int parentY;
    private boolean haveAdjustments = false;
    public SProfitOrLoss(home hm) {
        initComponents();
        this.hm = hm;
        
        parentX = hm.getX();
        parentY = hm.getY();
        int realWidth =  jScrollPane1.getViewport().getWidth();
        System.out.println("Table width: " + realWidth);

        
    }
    public void setProjectId(int projectId) {
        this.projectId = projectId;
        SI = new ShowInitialDetails(projectId);
        
        setHeader();
    }
    public void setHeader() {
        String companyName = SI.getCompanyName();
        String[] reportingPeriodAndType = SI.getReportingPeriod();
        String reportingPeriod = reportingPeriodAndType[0];
        String periodType = reportingPeriodAndType[1];
        
        int date[] = SI.getDate();
        int startMonth = date[0];
        int year = date[1];
        
        String headerText3 = "";
        
          if ("Annual Reporting".equalsIgnoreCase(reportingPeriod)) {
            if ("Calendar Year".equalsIgnoreCase(periodType)) {
                headerText3 = "For the year ended December 31, " + year;
            } else if ("Fiscal Year".equalsIgnoreCase(periodType)) {
                headerText3 = "For the year ended " + getEndDate(startMonth, year, 12);
            }
        } else if ("Interval Reporting".equalsIgnoreCase(reportingPeriod)) {
            
            switch (periodType) {
                case "Quarterly" -> headerText3 = "For the three months ended " + getEndDate(startMonth, year, 3); 
                case "Semi-Annual" -> headerText3 = "For the six months ended " + getEndDate(startMonth, year, 6); 
                case "Monthly" -> headerText3 = "For the month ended " + getEndDate(startMonth, year, 1); 
                default -> headerText3 = "For the year ended ???"; 
            }
        }
        jLabel1.setText("\""+companyName+"\"");
        jLabel3.setText(headerText3);
        
    }
    private String getEndDate(int startMonth, int startYear, int months) {
        Calendar cal = Calendar.getInstance();
        cal.set(startYear, startMonth -1, 1); // set start date
        cal.add(Calendar.MONTH, months);
        cal.add(Calendar.DAY_OF_MONTH, -1); // go to last day of interval
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy");
        return sdf.format(cal.getTime());
    }

    public void isAdjustmentUsed() {
        boolean adjUsed = false;
        String qCheckAdj = "SELECT adjustment_is_used FROM projects WHERE project_id = ?";
        try (Connection conn = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/accountingcycle", "root", "123456789");
                PreparedStatement psCheck = conn.prepareStatement(qCheckAdj)) {
            psCheck.setInt(1, projectId);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) adjUsed = rs.getBoolean("adjustment_is_used");
            }
        } catch(SQLException e) {
            e.printStackTrace();
        } finally {
            haveAdjustments = adjUsed;
        }
    }

    public void loadIncomeStatement() {
        DecimalFormat pesoFormat = new DecimalFormat("₱ #,##0.00");
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);

        String tableName = "adjusted_ledger";
        String query = "SELECT account_name, debit_total, credit_total FROM " + tableName + " WHERE project_id = ?";

        Map<String, BigDecimal> accountBalances = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/accountingcycle", "root", "123456789");
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, projectId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String accountName = rs.getString("account_name");
                    BigDecimal debit = rs.getBigDecimal("debit_total");
                    BigDecimal credit = rs.getBigDecimal("credit_total");

                    if (debit == null) debit = BigDecimal.ZERO;
                    if (credit == null) credit = BigDecimal.ZERO;

                    accountBalances.put(accountName + "_DEBIT", debit);
                    accountBalances.put(accountName + "_CREDIT", credit);
                }
            }

            // Define account classifications
            Set<String> revenue = new HashSet<>(Arrays.asList(
                "Sales Revenue", "Service Revenue", "Interest Income", "Rent Income",
                "Dividend Income", "Miscellaneous Income", "Gain On Sale of Assets"
            ));

            Set<String> contraRevenue = new HashSet<>(Arrays.asList(
                "Sales Returns", "Sales Allowances", "Sales Discounts", "Loss On Sale of Assets"
            ));

            Set<String> expenses = new HashSet<>(Arrays.asList(
                "Rent Expense", "Salaries Expense", "Wages Expense",
                "Utilities Expense", "Insurance Expense", "Supplies Expense", "Interest Expense",
                "Subscription Expense", "Depreciation Expense - Furnitures & Fixture",
                "Depreciation Expense - Equipment", "Depreciation Expense - Vehicle",
                "Depreciation Expense - Machinery", "Depreciation Expense - Building",
                "Advertising Expense", "Office Supply Expense", "Maintenance & Repairs Expense",
                "Telephone Expense", "Postage Expense", "Travel Expense", "Miscellaneous Expense"
            ));

            // Calculate totals
            BigDecimal grossSales = BigDecimal.ZERO;
            BigDecimal salesDeductions = BigDecimal.ZERO;
            BigDecimal totalExpenses = BigDecimal.ZERO;

            // REVENUE SECTION
            model.addRow(new Object[]{"REVENUE", "", ""});

            // Calculate Gross Sales
            for (AccountTitle at : AccountTitle.ACCOUNT_TITLE) {
                String name = at.getTitle();
                if (revenue.contains(name)) {
                    BigDecimal credit = accountBalances.getOrDefault(name + "_CREDIT", BigDecimal.ZERO);
                    if (credit.compareTo(BigDecimal.ZERO) != 0) {
                        grossSales = grossSales.add(credit);
                        model.addRow(new Object[]{"        " + name, pesoFormat.format(credit), ""});
                    }
                }
            }

            // Sales Deductions (contra-revenue)
            for (AccountTitle at : AccountTitle.ACCOUNT_TITLE) {
                String name = at.getTitle();
                if (contraRevenue.contains(name)) {
                    BigDecimal debit = accountBalances.getOrDefault(name + "_DEBIT", BigDecimal.ZERO);
                    if (debit.compareTo(BigDecimal.ZERO) != 0) {
                        salesDeductions = salesDeductions.add(debit);
                        String displayName = "        Less: " + name;
                        model.addRow(new Object[]{displayName, "(" + pesoFormat.format(debit) + ")", ""});
                    }
                }
            }

            BigDecimal netSales = grossSales.subtract(salesDeductions);
            model.addRow(new Object[]{"Net Sales", "", pesoFormat.format(netSales) + " "});
            model.addRow(new Object[]{"", "", ""}); // spacing

            // COST OF GOODS SOLD
            BigDecimal costOfGoodsSold = accountBalances.getOrDefault("Cost of Goods Sold_DEBIT", BigDecimal.ZERO);
            if (costOfGoodsSold.compareTo(BigDecimal.ZERO) != 0) {
                model.addRow(new Object[]{"        Less: Cost of Goods Sold", pesoFormat.format(costOfGoodsSold), ""});
            } else {
                model.addRow(new Object[]{"        Less: Cost of Goods Sold", pesoFormat.format(BigDecimal.ZERO), ""});
            }

            BigDecimal grossProfit = netSales.subtract(costOfGoodsSold);
            model.addRow(new Object[]{"GROSS PROFIT", "", pesoFormat.format(grossProfit) + " "});
            model.addRow(new Object[]{"", "", ""}); // spacing

            // OPERATING EXPENSES
            model.addRow(new Object[]{"OPERATING EXPENSES", "", ""});
            for (AccountTitle at : AccountTitle.ACCOUNT_TITLE) {
                String name = at.getTitle();
                if (expenses.contains(name)) {
                    BigDecimal debit = accountBalances.getOrDefault(name + "_DEBIT", BigDecimal.ZERO);
                    if (debit.compareTo(BigDecimal.ZERO) != 0) {
                        totalExpenses = totalExpenses.add(debit);
                        model.addRow(new Object[]{"        " + name, pesoFormat.format(debit), ""});
                    }
                }
            }
            model.addRow(new Object[]{"Total Operating Expenses", "", pesoFormat.format(totalExpenses) + " "});
            model.addRow(new Object[]{"", "", ""}); // spacing

            // INCOME BEFORE TAX
            BigDecimal incomeBeforeTax = grossProfit.subtract(totalExpenses);
            model.addRow(new Object[]{"INCOME BEFORE TAX", "", pesoFormat.format(incomeBeforeTax) + " "});
            model.addRow(new Object[]{"", "", ""}); // spacing

            // TAX EXPENSE
            BigDecimal taxExpense = accountBalances.getOrDefault("Taxes Expense_DEBIT", BigDecimal.ZERO);
            if (taxExpense.compareTo(BigDecimal.ZERO) != 0) {
                model.addRow(new Object[]{"        Less: Taxes Expense", pesoFormat.format(taxExpense), ""});
            } else {
                model.addRow(new Object[]{"        Less: Taxes Expense", pesoFormat.format(BigDecimal.ZERO), ""});
            }

            // NET INCOME/LOSS
            BigDecimal netResult = incomeBeforeTax.subtract(taxExpense);
            String resultLabel = netResult.compareTo(BigDecimal.ZERO) >= 0 ? "NET INCOME" : "NET LOSS";
            BigDecimal absResult = netResult.abs();

            model.addRow(new Object[]{resultLabel, "", pesoFormat.format(absResult) + " "});

            // Save to database
            saveNetIncomeOrLoss(netResult);

            // Apply renderer (same as Statement of Financial Position)
            DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
                Font boldFont = new Font("Tahoma", Font.BOLD, 14);
                Font normalFont = new Font("Tahoma", Font.PLAIN, 14);

                private boolean drawSingleUnderline = false;
                private boolean drawDoubleUnderline = false;

                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    drawSingleUnderline = false;
                    drawDoubleUnderline = false;

                    String cellText = value != null ? value.toString() : "";
                    String col0Text = table.getValueAt(row, 0) != null ? table.getValueAt(row, 0).toString() : "";

                    setBackground(Color.WHITE);
                    setForeground(Color.BLACK);
                    setFont(normalFont);
                    setBorder(new EmptyBorder(2, 4, 2, 4));

                    // Bold for headings
                    if (column == 0 && (cellText.equals("REVENUE") || 
                        cellText.equals("GROSS PROFIT") ||
                        cellText.equals("OPERATING EXPENSES") ||
                        cellText.equals("INCOME BEFORE TAX") ||
                        cellText.equals("Net Sales") ||
                        cellText.equals("Total Operating Expenses") ||
                        cellText.equals("NET INCOME") ||
                        cellText.equals("NET LOSS"))) {
                        setFont(boldFont);
                    }

                    // Subtotals and totals in column 2
                    if (column == 2 && !cellText.isEmpty()) {
                        if (col0Text.equals("Net Sales") || 
                            col0Text.equals("GROSS PROFIT") ||
                            col0Text.equals("Total Operating Expenses") ||
                            col0Text.equals("INCOME BEFORE TAX")) {
                            setFont(boldFont);
                            setBackground(new Color(255, 255, 204)); // Light yellow
                            drawSingleUnderline = true;
                        }
                        else if (col0Text.equals("NET INCOME") || col0Text.equals("NET LOSS")) {
                            setFont(boldFont);
                            setBackground(new Color(255, 255, 153)); // Brighter yellow
                            drawDoubleUnderline = true;
                        }
                    }

                    if (column == 0) {
                        setHorizontalAlignment(SwingConstants.LEFT);
                    } else {
                        setHorizontalAlignment(SwingConstants.RIGHT);
                    }

                    setText(cellText);
                    return this;
                }

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (!(drawSingleUnderline || drawDoubleUnderline)) return;
                    String text = getText();
                    if (text == null || text.isEmpty()) return;

                    Graphics2D g2 = (Graphics2D) g.create();
                    try {
                        g2.setStroke(new BasicStroke(1f));
                        g2.setColor(getForeground());

                        FontMetrics fm = g2.getFontMetrics(getFont());
                        int textWidth = fm.stringWidth(text);

                        Insets ins = getInsets();
                        int availableWidth = getWidth() - ins.left - ins.right;

                        int x;
                        int align = getHorizontalAlignment();
                        if (align == SwingConstants.RIGHT) {
                            x = getWidth() - ins.right - textWidth;
                        } else if (align == SwingConstants.CENTER) {
                            x = ins.left + (availableWidth - textWidth) / 2;
                        } else {
                            x = ins.left;
                        }

                        int textBaseline = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                        int y1 = textBaseline + 2;

                        if (drawSingleUnderline) {
                            g2.drawLine(x, y1, x + textWidth, y1);
                        } else if (drawDoubleUnderline) {
                            g2.drawLine(x, y1, x + textWidth, y1);
                            g2.drawLine(x, y1 + 3, x + textWidth, y1 + 3);
                        }
                    } finally {
                        g2.dispose();
                    }
                }
            };

            jTable1.getColumnModel().getColumn(0).setCellRenderer(customRenderer);
            jTable1.getColumnModel().getColumn(1).setCellRenderer(customRenderer);
            jTable1.getColumnModel().getColumn(2).setCellRenderer(customRenderer);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveNetIncomeOrLoss(BigDecimal netResult) {
        String updateQuery = "INSERT INTO project_net_result (project_id, net_income, net_loss) VALUES (?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE net_income = ?, net_loss = ?";

        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/accountingcycle", "root", "123456789");
             PreparedStatement ps = conn.prepareStatement(updateQuery)) {

            BigDecimal netIncome = BigDecimal.ZERO;
            BigDecimal netLoss = BigDecimal.ZERO;

            if (netResult.compareTo(BigDecimal.ZERO) >= 0) {
                netIncome = netResult;
            } else {
                netLoss = netResult.abs();
            }

            ps.setInt(1, projectId);
            ps.setBigDecimal(2, netIncome);
            ps.setBigDecimal(3, netLoss);
            ps.setBigDecimal(4, netIncome);
            ps.setBigDecimal(5, netLoss);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static class ATBRow {
        String accountName;
        BigDecimal debit;
        BigDecimal credit;

        public ATBRow(String accountName, BigDecimal debit, BigDecimal credit) {
            this.accountName = accountName;
            this.debit = debit;
            this.credit = credit;
        }
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        setLocation(new java.awt.Point(parentX + 290, parentY + 170));
        setMinimumSize(new java.awt.Dimension(1014, 560));
        setUndecorated(true);
        setResizable(false);
        setType(java.awt.Window.Type.UTILITY);
        getContentPane().setLayout(null);

        jTable1.getTableHeader().setReorderingAllowed(false);
        jTable1.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Headings/Sub-headings/Account Title/Particulars", "Amount", "Total/Subtotal"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jTable1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jTable1.setEnabled(false);
        jTable1.setGridColor(new java.awt.Color(0, 0, 0));
        jTable1.setRowHeight(30);
        jTable1.setSelectionBackground(new java.awt.Color(255, 255, 255));
        jTable1.setShowGrid(true);
        jTable1.getTableHeader().setResizingAllowed(false);
        jTable1.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer boldRenderer = new DefaultTableCellRenderer() {
            Font boldFont = new Font("Tahoma", Font.BOLD, 14);
            Font normalFont = new Font("Tahoma", Font.PLAIN, 14);

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (column == 0) { // first column only
                    c.setFont(boldFont);
                } else {
                    c.setFont(normalFont);
                }

                return c;
            }
        };

        jTable1.getColumnModel().getColumn(0).setCellRenderer(boldRenderer);
        // Change header font
        jTable1.getTableHeader().setFont(new java.awt.Font("Tahoma", java.awt.Font.BOLD, 14));

        ((javax.swing.table.DefaultTableCellRenderer)
            jTable1.getTableHeader().getDefaultRenderer())
        .setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jScrollPane1.setViewportView(jTable1);
        jScrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setResizable(false);
            jTable1.getColumnModel().getColumn(1).setResizable(false);
            jTable1.getColumnModel().getColumn(2).setResizable(false);
        }
        final int TABLE_WIDTH = 974;

        // Now compute proportional widths
        int col0Width = (int) (TABLE_WIDTH * 0.40);
        int col1Width = (int) (TABLE_WIDTH * 0.30);
        int col2Width = (int) (TABLE_WIDTH * 0.30);

        jTable1.getColumnModel().getColumn(0).setPreferredWidth(col0Width);
        jTable1.getColumnModel().getColumn(1).setPreferredWidth(col1Width);
        jTable1.getColumnModel().getColumn(2).setPreferredWidth(col2Width);

        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(10, 100, 990, 440);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("No company name found");

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Statement of Comprehensive Income (Profit or Loss)");

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Can't Find Date");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 976, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 976, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addContainerGap(9, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel1);
        jPanel1.setBounds(10, 20, 990, 80);
        getContentPane().add(jSeparator1);
        jSeparator1.setBounds(10, 110, 810, 10);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(UnadjustTB.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(UnadjustTB.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(UnadjustTB.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(UnadjustTB.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AdjustedTB(null).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
