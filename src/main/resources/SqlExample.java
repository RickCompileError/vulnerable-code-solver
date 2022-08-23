package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SqlExample extends HttpServlet{

    public ResultSet sqlAtTheEnd(String username, String password) throws Exception{
        Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/user", "root", "root");
        Statement stmt = conn.createStatement();
        String sql = "select * from myTable where username = '" + username + "' and password = '" + password + "'"; 
        ResultSet rs = stmt.executeQuery(sql);
        rs.getMetaData();
        return rs;
    }

    public ResultSet sqlBeforeStatement(String username, String password) throws Exception{
        Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/user", "root", "root");
        String sql = "select * from myTable where username = '" + username + "' and password = '" + password + "'"; 
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        return rs;
    }

    public ResultSet sqlBeforeConnection(String username, String password) throws Exception{
        String sql = "select * from myTable where username = '" + username + "' and password = '" + password + "'"; 
        Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/user", "root", "root");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        return rs;
    }

    public ResultSet commandDeclareVariable(String username, String password) throws Exception{
        Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/user", "root", "root");
        String base = "select * from myTable where";
        String sql = base + " username = '" + username + "' and password = '" + password + "'"; 
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        return rs;
    }

    public ResultSet sqlDeclareLate(String username, String password) throws Exception{
        Connection conn = null;
        Statement stmt = null;
        String sql = null;
        conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/user", "root", "root");
        stmt = conn.createStatement();
        sql = "select * from myTable where username = '" + username + "' and password = '" + password + "'";
        ResultSet rs = stmt.executeQuery(sql);
        return rs;
    }

    public ResultSet noSqlDeclare(String username, String password) throws Exception{
        Connection conn = null;
        Statement stmt = null;
        conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/user", "root", "root");
        stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select * from myTable where username = '" + username + "' and password = '" + password + "'");
        return rs;
    }

    public ResultSet noDeclareAny(String username, String password) throws Exception{
        return DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/user", "root", "root")
                            .createStatement()
                            .executeQuery("select * from myTable where username = '" + username +
                                                                "' and password = '" + password + "'");
    }

    public ResultSet ifThenElseSql(String username, String password) throws Exception{
        Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/user", "root", "root");
        Statement stmt = conn.createStatement();
        String sql;
        if (username.equals("admin")){
            sql = "select * from admin where username = '" + username + "' and password = '" + password + "'"; 
        }else{
            sql = "select * from myTable where username = '" + username + "' and password = '" + password + "'"; 
        }
        ResultSet rs = stmt.executeQuery(sql);
        return rs;
    }

    public ResultSet multipleIfThenElseSql(String username1, String username2, String password1, String password2) throws Exception{
        Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/user", "root", "root");
        Statement stmt = conn.createStatement();
        String sql;
        if (username1.equals("admin")){
            sql = "select * from myTable where username = '" + username1;
        }else{
            sql = "select * from myTable where username = '" + username2;
        }
        if (password1.contains("prefix")){
            sql += "' and password = '" + password1 + "'"; 
        }else{
            sql += "' and password = '" + password2 + "'"; 
        }
        ResultSet rs = stmt.executeQuery(sql);
        return rs;
    }

    public ResultSet switchSql(String username, String password) throws Exception{
        Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/user", "root", "root");
        Statement stmt = conn.createStatement();
        String sql;
        switch (username){
            case "admin":
                sql = "select * from admin where username = '" + username + "' and password = '" + password + "'"; 
                break;
            default:
                sql = "select * from myTable where username = '" + username + "' and password = '" + password + "'"; 
                break;
        }
        ResultSet rs = stmt.executeQuery(sql);
        return rs;
    }

    public ResultSet loopSql(String username, String[] columnName, String[] columnValue) throws Exception{
        Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/user", "root", "root");
        Statement stmt = conn.createStatement();
        String sql = "select * from myTable where username = '" + username;
        for (int i=0;i<columnName.length;i++){
            sql += "' and " + columnName[i] + " = '" + columnValue[i] + "'";
        }
        ResultSet rs = stmt.executeQuery(sql);
        return rs;
    }

    public ResultSet safeMethod(String username, String password) throws Exception{
        Connection conn = null;
        PreparedStatement stmt = null;
        String sql = "select * from myTable where username = ? and password = ?";
        conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/user", "root", "root");
        stmt = conn.prepareStatement(sql);
        stmt.setString(1, username);
        stmt.setString(2, password);
        ResultSet rs = stmt.executeQuery();
        return rs;
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        sqlAtTheEnd(username, password);
        sqlBeforeConnection(username, password);
        sqlBeforeStatement(username, password);
        commandDeclareVariable(username, password);
        sqlDeclareLate(username, password);
        noSqlDeclare(username, password);
        ifThenElseSql(username, password);
        multipleIfThenElseSql(username, password, request.getParameter("username2"), request.getParameter("password2"));
        switchSql(username, password);
        String[] columnName = {"a", "b", "c"};
        String[] columnValue = {"A", "B", "C"};
        loopSql(username, columnName, columnValue);
        safeMethod(username, password);
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

}