/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protegemed.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Joao Antonio Soares
 */
public class ProtegemedConnection {

    private static Connection connect() {

        Properties prop = new Properties();
        InputStream input;
        Connection conn;

        //Default values
        String host = "10.8.0.1";
        String database = "protegemed";
        String username = "root";
        String password = "senha.123";

        try {
            input = new FileInputStream("resources/config.properties");
            prop.load(input);

            host = prop.getProperty("host");
            database = prop.getProperty("database");
            username = prop.getProperty("username");
            password = prop.getProperty("password");

        } catch (IOException e) {
            System.out.println("Properties file not found. Continue with default values...");
        }

        String url = "jdbc:mysql://" + host + "/" + database;

        try {

            if (InetAddress.getByName(host).isReachable(5000)) {

                conn = DriverManager.getConnection(url, username, password);
                return conn;

            } else {

                return null;
            }

        } catch (SQLException e) {

            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
            return null;

        } catch (UnknownHostException e) {
            
            System.out.println("UnknownHostException: " + e.getMessage());
            return null;
            
        } catch (IOException e) {
            
            System.out.println("IOException: " + e.getMessage());
            return null;
            
        }

    }

    public static Connection getConnection() {
        return connect();
    }

    public static void closeConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(ProtegemedConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
