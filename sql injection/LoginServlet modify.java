package com.journaldev.examples;
import java.io.IOException;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
 
@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception e) {}
    }
 
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        boolean success = false;
        String username = request.getParameter("username"); //snyk 1
        String password = request.getParameter("password");
        // Unsafe query which uses string concatenation
        String query = "select * from tbluser where username=? and password =?";  //snyk 2  //modi 1
        Connection conn = null;
        PreparedStatement stmt = null;  //modi 2
        try {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/user", "root", "root");
            stmt = conn.prepareStatement(query); //modi 3
            stmt.setString(1, username); //modi 4
            stmt.setString(2, password); //modi 5
            ResultSet rs = stmt.executeQuery();  //snyk 3  //modi 6
            if (rs.next()) {
                // Login Successful if match is found
                success = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                stmt.close();
                conn.close();
            } catch (Exception e) {}
        }
        if (success) {
            response.sendRedirect("home.html");
        } else {
            response.sendRedirect("login.html?error=1");
        }
    }
}