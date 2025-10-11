
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.sql.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
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
public class AdjustedTB extends javax.swing.JFrame {

    /**
     * Creates new form UnadjustTB
     */
    private home hm;
    private ShowInitialDetails SI;
    private int projectId;
    private int parentX;
    private int parentY;
    private boolean haveAdjustments = false;
    public AdjustedTB(home hm) {
        initComponents();
        this.hm = hm;
        
        parentX = hm.getX();
        parentY = hm.getY();
        
        
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
        } else if ("Interim Reporting".equalsIgnoreCase(reportingPeriod)) {
            
            switch (periodType) {
                case "Quarterly" -> headerText3 = "For the quarterly ended " + getEndDate(startMonth, year, 3); 
                case "Semi-Annual" -> headerText3 = "For the six months ended " + getEndDate(startMonth, year, 6); 
                case "Monthly" -> headerText3 = "For the month ended " + getEndDate(startMonth, year, 1); 
                default -> headerText3 = "For the period ended ???"; 
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
    public void loadATB() {
        DecimalFormat pesoFormat = new DecimalFormat("₱ #,##0.00");

        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0); // clear existing rows

         String tableName = haveAdjustments ? "adjusted_ledger" : "unadjusted_ledger";
        String query = "SELECT account_name, debit_total, credit_total FROM " + tableName + " WHERE project_id = ?";

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/accountingcycle", "root", "123456789");
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, projectId);

            List<ATBRow> rows = new ArrayList<>();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String accountName = rs.getString("account_name");
                    BigDecimal debit = rs.getBigDecimal("debit_total");
                    BigDecimal credit = rs.getBigDecimal("credit_total");

                    rows.add(new ATBRow(accountName, debit, credit));

                    if (debit != null) totalDebit = totalDebit.add(debit);
                    if (credit != null) totalCredit = totalCredit.add(credit);
                }
            }

            // --- SORTING LOGIC ---
            AccountTitle[] orderedTitles = AccountTitle.ACCOUNT_TITLE; // predefined order
            Map<String, Integer> titleOrder = new HashMap<>();
            for (int i = 0; i < orderedTitles.length; i++) {
                titleOrder.put(orderedTitles[i].getTitle(), i); // use getTitle() for display name
            }

            rows.sort((a, b) -> {
                int idxA = titleOrder.getOrDefault(a.accountName, Integer.MAX_VALUE);
                int idxB = titleOrder.getOrDefault(b.accountName, Integer.MAX_VALUE);
                return Integer.compare(idxA, idxB);
            });
            // --- END SORTING ---

            // Add sorted rows to the table
            for (ATBRow row : rows) {
                String debitStr = (row.debit == null || row.debit.compareTo(BigDecimal.ZERO) == 0) ? "" : pesoFormat.format(row.debit);
                String creditStr = (row.credit == null || row.credit.compareTo(BigDecimal.ZERO) == 0) ? "" : pesoFormat.format(row.credit);

                model.addRow(new Object[]{row.accountName, debitStr, creditStr});
            }

            // Add totals row
            model.addRow(new Object[]{
                "Total",
                pesoFormat.format(totalDebit),
                pesoFormat.format(totalCredit)
            });

            // --- KEEP ALL EXISTING RENDERERS ---
            DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
            rightRenderer.setHorizontalAlignment(SwingConstants.LEFT);
            jTable1.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
            jTable1.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

            jTable1.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table,
                                                               Object value,
                                                               boolean isSelected,
                                                               boolean hasFocus,
                                                               int row,
                                                               int column) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (row == table.getRowCount() - 1) {
                        setHorizontalAlignment(SwingConstants.RIGHT);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setHorizontalAlignment(SwingConstants.LEFT);
                        setFont(getFont().deriveFont(Font.PLAIN));
                    }
                    return this;
                }
            });

            DefaultTableCellRenderer totalsRenderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table,
                                                               Object value,
                                                               boolean isSelected,
                                                               boolean hasFocus,
                                                               int row,
                                                               int column) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    setOpaque(true);
                    int lastRow = table.getRowCount() - 1;
                    if (row == lastRow) {
                        setBackground(Color.YELLOW);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setBackground(Color.WHITE);
                    }
                    return this;
                }
            };
            jTable1.getColumnModel().getColumn(1).setCellRenderer(totalsRenderer);
            jTable1.getColumnModel().getColumn(2).setCellRenderer(totalsRenderer);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Helper class
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
        jLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        setLocation(new java.awt.Point(parentX + 290, parentY + 170));
        setMaximumSize(new java.awt.Dimension(830, 560));
        setMinimumSize(new java.awt.Dimension(830, 560));
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
                "Accoun Title/Particulars", "Debit", "Credit"
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
        final int TABLE_WIDTH = 806;

        // Now compute proportional widths
        int col0Width = (int) (TABLE_WIDTH * 0.40);
        int col1Width = (int) (TABLE_WIDTH * 0.30);
        int col2Width = (int) (TABLE_WIDTH * 0.30);

        jTable1.getColumnModel().getColumn(0).setPreferredWidth(col0Width);
        jTable1.getColumnModel().getColumn(1).setPreferredWidth(col1Width);
        jTable1.getColumnModel().getColumn(2).setPreferredWidth(col2Width);

        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(10, 100, 810, 440);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("No company name found");

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Adjusted Trial Balance");

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
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 796, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 796, Short.MAX_VALUE))
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
        jPanel1.setBounds(10, 20, 810, 80);
        getContentPane().add(jSeparator1);
        jSeparator1.setBounds(10, 110, 810, 10);

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/all page background image.png"))); // NOI18N
        getContentPane().add(jLabel4);
        jLabel4.setBounds(0, 0, 800, 670);

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
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
