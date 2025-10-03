
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Month;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JWindow;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Joshua
 */
public class ShowInitialDetails {
    private  int projectId;
    public JWindow popup;
    private String name;
    private String reportingPeriod;
    private String periodType;
    private int month;
    private int year;
    
    public ShowInitialDetails(int projectId) {
        this.projectId = projectId;
        
        getInitialDetails(projectId);
    }
    public void showPopup(Component parent) {
        int project_key = this.projectId;
       
        StringBuilder details;
        details = new StringBuilder(getInitialDetails(project_key));
        
        JLabel label = new JLabel(details.toString());
        label.setOpaque(true);
        label.setBackground(new Color(245, 245, 245));
        label.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        
        if(popup == null) {
            popup = new JWindow();
            popup.getContentPane().setLayout(new BorderLayout());
            popup.getContentPane().add(label, BorderLayout.CENTER);
            popup.getRootPane().setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            popup.pack();
        } else {
            popup.getContentPane().removeAll();
            popup.getContentPane().add(label, BorderLayout.CENTER);
            popup.getRootPane().setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            popup.pack();
        }
        Point location = parent.getLocationOnScreen();
        int offset = 5;
        int x = location.x + parent.getWidth() + offset;
        int y = location.y + (parent.getHeight() - popup.getHeight()) / 2;
        popup.setLocation(x, y);
        popup.setVisible(true);
    }
    private StringBuilder getInitialDetails(int project_id) {
        StringBuilder result = new StringBuilder();
        String projectName = "No details found";
        String companyName = "No details found";
        String accountingPeriod = "No details found";
        String periodType = "No details found";
        String monthStarted = "No details found";
        int numMonth = 0;
        int yearStarted = 1999;
        String sqlInitDetails = "SELECT companyName, accountingPeriod, reportingPeriod, monthStarted, yearStarted FROM initialdetails WHERE project_id = ?";
        String sqlProj = "SELECT project_name FROM projects WHERE project_id = ?";
        
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/accountingcycle", "root", "123456789");
             PreparedStatement psProj = conn.prepareStatement(sqlProj);
             PreparedStatement psInitDetails = conn.prepareStatement(sqlInitDetails)) {
           
            psProj.setInt(1, project_id);
            try(ResultSet rs = psProj.executeQuery()) {
                if(rs.next()) {
                    projectName = rs.getString("project_name");
                }
            }
            psProj.close();
            psInitDetails.setInt(1, project_id);
            try(ResultSet rs = psInitDetails.executeQuery()) {
                if(rs.next()) {
                    companyName = rs.getString("companyName");
                    accountingPeriod = rs.getString("accountingPeriod");
                    periodType = rs.getString("reportingPeriod");
                    numMonth = rs.getInt("monthStarted");
                    if(numMonth >= 1 && numMonth <=12) {
                        monthStarted = Month.of(numMonth).name();
                        monthStarted = monthStarted.substring(0,1).toUpperCase() + monthStarted.substring(1).toLowerCase();
                    }
                    yearStarted = rs.getInt("yearStarted");
                }
            }
        
            System.out.println("Before passing to method numMonth: "+numMonth);
            
            System.out.println("Before passing to method Year Started"+ yearStarted);
            year = yearStarted;
            month = numMonth;
            name = companyName;
            reportingPeriod = accountingPeriod;
            this.periodType = periodType;
            //STRING BUILD
            result.append("<html>")
                .append("<table style='font-family:Tahoma; font-size:14pt;'>")
                .append("<tr><td><b>Project:</b></td><td>").append(projectName).append("</td></tr>")
                .append("<tr><td><b>Project Key:</b></td><td>").append(project_id).append("</td></tr>")
                .append("<tr><td><b>Company name:</b></td><td>").append(companyName).append("</td></tr>")
                .append("<tr><td><b>Reporting Period:</b></td><td>").append(accountingPeriod).append("</td></tr>")
                .append("<tr><td><b>Period Type:</b></td><td>").append(periodType).append("</td></tr>")
                .append("<tr><td><b>Start of accounting:</b></td><td>").append(monthStarted).append(" ").append(yearStarted).append("</td></tr>")
                .append("</table>")
                .append("</html>");
            
            
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        
        return result;
    }
 
    public int[] getDate() {
        int[] returnDate = new int[2];
        returnDate[0] = month;
        returnDate[1] = year;
        return returnDate;
    }
    public String[] getReportingPeriod() {
        String[] reportingPeriodAndType = new String[2];
        reportingPeriodAndType[0] = reportingPeriod;
        reportingPeriodAndType[1] = periodType;
        
        return reportingPeriodAndType;
    }
    public String getCompanyName() {
        return name;
    }
}
