import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Arrays;
import java.util.Scanner;

/* NOTE:
 * The hash function(line90) in this demo inadvertently
 * restricts SQL injection, and has thus been
 * omitted for demo purposes.
 * */

/**
 * Secure authentication database demo.
 *
 * - Demo can be run in either:
 *      + Non-Compliant Mode (Vulnerable to SQL Injection)[default]
 *      + Compliant Mode (Secured against SQL Injection)
 *
 * - User must first supply the credentials
 *   to be authenticated.  Username and Password
 *   are then compared with values stored in
 *   the database.
 *
 * - SQL injection vulnerabilities are mitigated
 *   through strong type checking within the method
 *   that generates the database query.
 *
 * - **OMITTED FOR THIS DEMO**
 *   After user enters their username and password,
 *   the password is hashed using MD5 and compared
 *   with the hash value of the password stored in
 *   the database. The plain-text value of the password
 *   is never transmitted nor stored in the database.
 *
 *
 * @author Joshua Davenport
 */
public class Login {

    private static final int MAX_NAME = 8;

    private static Connection connection = null;

    /**
     * Method to connect to postgresql database.
     * */
    private static Connection connect() {
        if (connection != null) {
            System.out.println("Problem when connecting to the database");
        }
        String url = "jdbc:postgresql://localhost:5432/";

        String dbUser = "postgres";
        String dbPass = "toor";

        try {
            connection = DriverManager.getConnection(url, dbUser, dbPass);

            if (connection != null) {
                System.out.print("\nConnecting to database...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.print("done.\n");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            System.out.println("Problem when connecting to the database");
            e.printStackTrace();
        }
        return connection;
    }


    /**
     * Method to generate MD5 hash of password. (BYPASSED FOR THIS DEMO)
     * */
    private static String hashPassword(char[] password) {
        String generatedHash = null;
        String inPass = Arrays.toString(password);

        try {
            MessageDigest md;
            byte[] bytes;
            md = MessageDigest.getInstance("MD5");

            if (md != null) {
                // explicitly set char encoding, and catch exception
                md.update(inPass.getBytes(Charset.forName("UTF-8")));
            }

            assert md != null;
            bytes = md.digest();


            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }

            generatedHash = sb.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return generatedHash;
    }

    private static PreparedStatement stmnt = null;
    private static ResultSet rs = null;


    /* **********************
     * NON-COMPLIANT METHOD *
     ************************/
     /**
      * Method for database operations.(vulnerable to SQL injection)
      ***/


    /*
    private static void doPrivilegedAction(String username, char[] password)
            throws SQLException {
        Connection connection = connect();
        if (connection == null) {
            //handle error
            System.out.println("No connection to authdb!");
        }
        try {
            //String pwd = hashPassword(password);
            String pwd = String.valueOf(password);

            String sqlString = "SELECT * FROM public.authtable WHERE username = '"
                    + username + "' AND password = '" + pwd + "'";

            System.out.println("Database Query:\n" + sqlString + "\n");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Statement stmnt = null;

            if (connection != null) {
                stmnt = connection.createStatement();
            }
            if (stmnt != null) {
                rs = stmnt.executeQuery(sqlString);
            }
            if (!rs.next()) {
                throw new SecurityException("ACCESS DENIED!!");
            }

            System.out.println("** YOU HAVE BEEN AUTHENTICATED!! **");
        } finally {
            //close resources when done.
            close(rs, stmnt, connection);
        }
    }

    */






    /* ******************
     * COMPLIANT METHOD *
     ********************/
    /* *
     * Method for database operations.
     * */


    private static void doPrivilegedAction(String username, char[] password)
            throws SQLException {
        Connection connection = connect();
        if (connection == null) {
            //handle error
            System.out.println("No connection to authdb!");
        }
        try {
            //String pwd = hashPassword(password);
            String pwd = String.valueOf(password);
             /* *****************************************************
             * Ensure that the length of the username is legitimate *
             *******************************************************/
            if ((username.length() > MAX_NAME)) {
                //handle error
                System.out.println("ERROR: Username must be less than 8 chars!");
            }
            String sqlString = "SELECT * FROM public.authtable WHERE username=? AND password=?";
            System.out.println("Query: " + sqlString + "\n");
             /* ****************************************************
             * Using set*() methods of the PreparedStatement class *
             * to guard against SQL injection vulnerabilities.     *
             ******************************************************/
            if (connection != null) {
                stmnt = connection.prepareStatement(sqlString);
            }
            if (stmnt != null) {
                stmnt.setString(1, username);
                stmnt.setString(2, pwd);
                rs = stmnt.executeQuery();
                if (rs != null && !rs.next()) {
                    throw new SecurityException("UserName or Password Incorrect!");
                }
            }
            System.out.println("** YOU HAVE BEEN AUTHENTICATED!! **");
        } finally {
            //close resources when done.
            close(rs, stmnt, connection);
        }
    }


    /**
     * Method to properly close all resources.
     * */
    private static void close(ResultSet rs, Statement ps, Connection conn) {
        if (rs != null) {
            try {
                rs.close();

            } catch (SQLException e) {
                System.out.println("The result set cannot be closed.");
                e.printStackTrace();
            }
        }
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                System.out.println("The statement cannot be closed.");
                e.printStackTrace();
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.out.println("The data source connection cannot be closed.");
                e.printStackTrace();
            }
        }
    }


    /**
     * Main Method
     **/
    public static void main(String[] args) {   //SQL injection: ' OR '1'='1

        //hard coded user input(optional)
        /*String uNameHC = "dbuser1";
        //String myPassHC = "pa$$word1";
        String myPassHC = "' OR '1'='1";*/


        /* user input */

        Scanner uName = new  Scanner(System.in);
        Scanner pWord = new  Scanner(System.in);

        System.out.print("Enter UserName: ");
        String username = uName.nextLine();

        System.out.print("Enter Password: ");
        String passwd1 = pWord.nextLine();
        char[] passwd = passwd1.toCharArray();


        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\nInput parameters:");
        System.out.println("Username: " + username);
        System.out.println("Password: " + passwd1);
        //System.out.println("Password: " + myPassHC);

        //char[] passHC = myPassHC.toCharArray();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        try {
            //doPrivilegedAction(uNameHC, passHC);
            doPrivilegedAction(username, passwd);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}