
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
public class SFPosition extends javax.swing.JFrame {

    /**
     * Creates new form UnadjustTB
     */
    private home hm;
    private ShowInitialDetails SI;
    private int projectId;
    private int parentX;
    private int parentY;
    private boolean haveAdjustments = false;
    public SFPosition(home hm) {
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
                headerText3 = "As of December 31, " + year;
            } else if ("Fiscal Year".equalsIgnoreCase(periodType)) {
                headerText3 = "As of " + getEndDate(startMonth, year, 12);
            }
        } else if ("Interim Reporting".equalsIgnoreCase(reportingPeriod)) {
            
            switch (periodType) {
                case "Quarterly" -> headerText3 = "As of " + getEndDate(startMonth, year, 3); 
                case "Semi-Annual" -> headerText3 = "As of " + getEndDate(startMonth, year, 6); 
                case "Monthly" -> headerText3 = "As of " + getEndDate(startMonth, year, 1); 
                default -> headerText3 = "As of ???"; 
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

    public void loadFPostion() {
        DecimalFormat pesoFormat = new DecimalFormat("₱ #,##0.00");
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);

        String tableName = "adjusted_ledger";
        String query = "SELECT account_name, debit_total, credit_total FROM " + tableName + " WHERE project_id = ?";

        // Data storage
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

                    // Store raw debit and credit separately
                    accountBalances.put(accountName + "_DEBIT", debit);
                    accountBalances.put(accountName + "_CREDIT", credit);
                }
            }

            // Classification maps
            Set<String> currentAssets = new HashSet<>(Arrays.asList(
                "Cash", "Accounts Recievable", "Allowance for Doubtful Accounts", 
                "Inventory", "Allowance for Invetory Write-Down", "Office Supply"
            ));

            Set<String> nonCurrentAssets = new HashSet<>(Arrays.asList(
                "Prepaid Rent Expense", "Prepaid Insurance Expense", "Prepaid Supplies Expense",
                "Prepaid Subscription Expense", "Furniture & Fixture", 
                "Accumulated Depreciation - Furniture & Fixture", "Equipment",
                "Accumulated Depreciation - Equipment", "Vehicle",
                "Accumulated Depreciation - Vehicle", "Machinery",
                "Accumulated Depreciation - Machinery", "Building",
                "Accumulated Depreciation - Building", "Land"
            ));

            Set<String> currentLiabilities = new HashSet<>(Arrays.asList(
                "Accounts Payable", "Uneared Revenue", "Salaries Payable",
                "Wages Payable", "Taxes Payable", "Dividends Payable"
            ));

            Set<String> nonCurrentLiabilities = new HashSet<>(Arrays.asList(
                "Bonds Payable", "Notes Payable", "Loans Payable",
                "Mortgage Payable", "Vehicle Loan Payable"
            ));

            Set<String> equity = new HashSet<>(Arrays.asList(
                "Capital", "Retained Earnings", "Drawings", "Dividends", "Teasury Stock"
            ));

            Set<String> contraAssets = new HashSet<>(Arrays.asList(
                "Allowance for Doubtful Accounts", "Allowance for Invetory Write-Down",
                "Accumulated Depreciation - Furniture & Fixture",
                "Accumulated Depreciation - Equipment", "Accumulated Depreciation - Vehicle",
                "Accumulated Depreciation - Machinery", "Accumulated Depreciation - Building"
            ));

            Set<String> contraEquity = new HashSet<>(Arrays.asList(
                "Drawings", "Dividends", "Teasury Stock"
            ));

            // Build sections
            BigDecimal totalCurrentAssets = BigDecimal.ZERO;
            BigDecimal totalNonCurrentAssets = BigDecimal.ZERO;
            BigDecimal totalCurrentLiabilities = BigDecimal.ZERO;
            BigDecimal totalNonCurrentLiabilities = BigDecimal.ZERO;
            BigDecimal totalEquity = BigDecimal.ZERO;

            // ASSETS section
            model.addRow(new Object[]{"ASSETS", "", ""});

            // Current Assets
            model.addRow(new Object[]{"    Current:", "", ""});
            for (AccountTitle at : AccountTitle.ACCOUNT_TITLE) {
                String name = at.getTitle();
                if (currentAssets.contains(name)) {
                    BigDecimal debit = accountBalances.getOrDefault(name + "_DEBIT", BigDecimal.ZERO);
                    BigDecimal credit = accountBalances.getOrDefault(name + "_CREDIT", BigDecimal.ZERO);

                    if (debit.compareTo(BigDecimal.ZERO) != 0 || credit.compareTo(BigDecimal.ZERO) != 0) {
                        BigDecimal balance;

                        if (contraAssets.contains(name)) {
                            // Contra-assets have CREDIT balance, display as negative (decreases assets)
                            balance = credit.negate();
                            String displayName = "        Less: " + name;
                            model.addRow(new Object[]{displayName, "(" + pesoFormat.format(credit) + ")", ""});
                        } else {
                            // Normal assets have DEBIT balance
                            balance = debit;
                            model.addRow(new Object[]{"        " + name, pesoFormat.format(debit), ""});
                        }

                        totalCurrentAssets = totalCurrentAssets.add(balance);
                    }
                }
            }
            model.addRow(new Object[]{"", "", pesoFormat.format(totalCurrentAssets) + " "});
            model.addRow(new Object[]{"", "", ""}); // spacing

            // Non-Current Assets
            model.addRow(new Object[]{"    Non-current:", "", ""});
            for (AccountTitle at : AccountTitle.ACCOUNT_TITLE) {
                String name = at.getTitle();
                if (nonCurrentAssets.contains(name)) {
                    BigDecimal debit = accountBalances.getOrDefault(name + "_DEBIT", BigDecimal.ZERO);
                    BigDecimal credit = accountBalances.getOrDefault(name + "_CREDIT", BigDecimal.ZERO);

                    if (debit.compareTo(BigDecimal.ZERO) != 0 || credit.compareTo(BigDecimal.ZERO) != 0) {
                        BigDecimal balance;

                        if (contraAssets.contains(name)) {
                            // Contra-assets have CREDIT balance, display as negative (decreases assets)
                            balance = credit.negate();
                            String displayName = "        Less: " + name;
                            model.addRow(new Object[]{displayName, "(" + pesoFormat.format(credit) + ")", ""});
                        } else {
                            // Normal assets have DEBIT balance
                            balance = debit;
                            model.addRow(new Object[]{"        " + name, pesoFormat.format(debit), ""});
                        }

                        totalNonCurrentAssets = totalNonCurrentAssets.add(balance);
                    }
                }
            }
            model.addRow(new Object[]{"", "", pesoFormat.format(totalNonCurrentAssets) + " "});

            BigDecimal totalAssets = totalCurrentAssets.add(totalNonCurrentAssets);
            model.addRow(new Object[]{"TOTAL ASSETS", "", pesoFormat.format(totalAssets) + " "});
            model.addRow(new Object[]{"", "", ""}); // spacing

            // LIABILITIES AND OWNER'S EQUITY section
            model.addRow(new Object[]{"LIABILITIES AND OWNER'S EQUITY", "", ""});
            model.addRow(new Object[]{"    LIABILITIES", "", ""});

            // Current Liabilities
            model.addRow(new Object[]{"    Current:", "", ""});
            for (AccountTitle at : AccountTitle.ACCOUNT_TITLE) {
                String name = at.getTitle();
                if (currentLiabilities.contains(name)) {
                    BigDecimal credit = accountBalances.getOrDefault(name + "_CREDIT", BigDecimal.ZERO);

                    if (credit.compareTo(BigDecimal.ZERO) != 0) {
                        totalCurrentLiabilities = totalCurrentLiabilities.add(credit);
                        model.addRow(new Object[]{"        " + name, pesoFormat.format(credit), ""});
                    }
                }
            }
            model.addRow(new Object[]{"", "", pesoFormat.format(totalCurrentLiabilities) + " "});
            model.addRow(new Object[]{"", "", ""}); // spacing

            // Non-Current Liabilities
            model.addRow(new Object[]{"    Noncurrent:", "", ""});
            for (AccountTitle at : AccountTitle.ACCOUNT_TITLE) {
                String name = at.getTitle();
                if (nonCurrentLiabilities.contains(name)) {
                    BigDecimal credit = accountBalances.getOrDefault(name + "_CREDIT", BigDecimal.ZERO);

                    if (credit.compareTo(BigDecimal.ZERO) != 0) {
                        totalNonCurrentLiabilities = totalNonCurrentLiabilities.add(credit);
                        model.addRow(new Object[]{"        " + name, pesoFormat.format(credit), ""});
                    }
                }
            }
            model.addRow(new Object[]{"", "", pesoFormat.format(totalNonCurrentLiabilities) + " "});
            model.addRow(new Object[]{"", "", ""}); // spacing

            // EQUITY section
            model.addRow(new Object[]{"    EQUITY", "", ""});

            // Get Net Income/Loss from project_net_result table
            BigDecimal netIncomeFromDB = BigDecimal.ZERO;
            BigDecimal netLossFromDB = BigDecimal.ZERO;

            String netResultQuery = "SELECT net_income, net_loss FROM project_net_result WHERE project_id = ?";
            try (PreparedStatement psNet = conn.prepareStatement(netResultQuery)) {
                psNet.setInt(1, projectId);
                try (ResultSet rsNet = psNet.executeQuery()) {
                    if (rsNet.next()) {
                        netIncomeFromDB = rsNet.getBigDecimal("net_income");
                        netLossFromDB = rsNet.getBigDecimal("net_loss");
                        if (netIncomeFromDB == null) netIncomeFromDB = BigDecimal.ZERO;
                        if (netLossFromDB == null) netLossFromDB = BigDecimal.ZERO;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

             for (AccountTitle at : AccountTitle.ACCOUNT_TITLE) {
                 String name = at.getTitle();
                 if (equity.contains(name) && !contraEquity.contains(name)) {  
                     BigDecimal debit = accountBalances.getOrDefault(name + "_DEBIT", BigDecimal.ZERO);
                     BigDecimal credit = accountBalances.getOrDefault(name + "_CREDIT", BigDecimal.ZERO);

                     if (debit.compareTo(BigDecimal.ZERO) != 0 || credit.compareTo(BigDecimal.ZERO) != 0) {
                         BigDecimal balance = credit;  
                         model.addRow(new Object[]{"        " + name, pesoFormat.format(credit), ""});
                         totalEquity = totalEquity.add(balance);
                     }
                 }
             }

             // 2. Add Net Income
             if (netIncomeFromDB.compareTo(BigDecimal.ZERO) != 0) {
                 model.addRow(new Object[]{"        Add: Net Income", pesoFormat.format(netIncomeFromDB), ""});
                 totalEquity = totalEquity.add(netIncomeFromDB);
             } else {
                 model.addRow(new Object[]{"        Add: Net Income", pesoFormat.format(BigDecimal.ZERO), ""});
             }
             
             for (AccountTitle at : AccountTitle.ACCOUNT_TITLE) {
                 String name = at.getTitle();
                 if (equity.contains(name) && contraEquity.contains(name)) {  
                     BigDecimal debit = accountBalances.getOrDefault(name + "_DEBIT", BigDecimal.ZERO);

                     if (debit.compareTo(BigDecimal.ZERO) != 0) {
                         BigDecimal balance = debit.negate();  
                         String displayName = "        Less: " + name;
                         model.addRow(new Object[]{displayName, "(" + pesoFormat.format(debit) + ")", ""});
                         totalEquity = totalEquity.add(balance); 
                     }
                 }
             }

             // 4. Less Net Loss
             if (netLossFromDB.compareTo(BigDecimal.ZERO) != 0) {
                 model.addRow(new Object[]{"        Less: Net Loss", "(" + pesoFormat.format(netLossFromDB) + ")", ""});
                 totalEquity = totalEquity.subtract(netLossFromDB);
             } else {
                 model.addRow(new Object[]{"        Less: Net Loss", pesoFormat.format(BigDecimal.ZERO), ""});
             }


            model.addRow(new Object[]{"    Equity, end", "", pesoFormat.format(totalEquity) + " "});
            model.addRow(new Object[]{"", "", ""}); // spacing

            BigDecimal totalLiabilitiesEquity = totalCurrentLiabilities.add(totalNonCurrentLiabilities).add(totalEquity);
            model.addRow(new Object[]{"TOTAL LIABILITIES & OWNER'S EQUITY", "", pesoFormat.format(totalLiabilitiesEquity) + " "});

            // Custom renderer for formatting
            // Custom renderer for formatting
            DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
                Font boldFont = new Font("Tahoma", Font.BOLD, 14);
                Font normalFont = new Font("Tahoma", Font.PLAIN, 14);

                // flags set per-cell in getTableCellRendererComponent, used in paintComponent
                private boolean drawSingleUnderline = false;
                private boolean drawDoubleUnderline = false;

                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    // reset per-cell flags
                    drawSingleUnderline = false;
                    drawDoubleUnderline = false;

                    String cellText = value != null ? value.toString() : "";
                    String col0Text = table.getValueAt(row, 0) != null ? table.getValueAt(row, 0).toString() : "";

                    // Reset to defaults
                    setBackground(Color.WHITE);
                    setForeground(Color.BLACK);
                    setFont(normalFont);
                    setBorder(new EmptyBorder(2, 4, 2, 4));

                    // Bold for main headings and sub-headings in column 0
                    if (column == 0 && (cellText.equals("ASSETS") || 
                        cellText.equals("LIABILITIES AND OWNER'S EQUITY") ||
                        cellText.equals("    LIABILITIES") || 
                        cellText.equals("    EQUITY") ||
                        cellText.equals("    Current:") ||
                        cellText.equals("    Non-current:") ||
                        cellText.equals("    Current:") ||
                        cellText.equals("    Noncurrent:") ||
                        cellText.equals("TOTAL ASSETS") ||
                        cellText.equals("TOTAL LIABILITIES & OWNER'S EQUITY") ||
                        cellText.equals("    Equity, end"))) {
                        setFont(boldFont);
                    }

                    // Subtotal / Total logic (single/double underline)
                    if (column == 2 && !cellText.isEmpty()) {
                        if ((col0Text.trim().isEmpty() && !col0Text.equals("    Equity, end")) || col0Text.equals("    Equity, end")) {
                            setFont(boldFont);
                            setBackground(new Color(255, 255, 204)); // Light yellow for subtotals
                            drawSingleUnderline = true;
                        } else if (col0Text.equals("TOTAL ASSETS") || col0Text.equals("TOTAL LIABILITIES & OWNER'S EQUITY")) {
                            setFont(boldFont);
                            setBackground(new Color(255, 255, 153)); // Brighter yellow for totals
                            drawDoubleUnderline = true;
                        }
                    }

                    // Alignment
                    if (column == 0) {
                        setHorizontalAlignment(SwingConstants.LEFT);
                    } else {
                        setHorizontalAlignment(SwingConstants.RIGHT);
                    }

                    // Make sure the component text is exactly the amount (no HTML)
                    setText(cellText);

                    return this;
                }

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);

                    // Only draw underline(s) if requested and there is visible text
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
        setMaximumSize(new java.awt.Dimension(1014, 560));
        setMinimumSize(new java.awt.Dimension(1014, 560));
        setUndecorated(true);
        setPreferredSize(new java.awt.Dimension(1014, 560));
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
        jLabel2.setText("Statement of Financial Position");

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
