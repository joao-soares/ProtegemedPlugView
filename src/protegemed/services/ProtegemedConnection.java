/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protegemed.services;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Joao Antonio Soares
 */
public class ProtegemedConnection {

    private static Connection connect() {

        Connection conn;

        String host;
        String database;
        String username;
        String password;
        
        ProtegemedPropertyLoad property = new ProtegemedPropertyLoad("resources/config.properties");

        host = property.getHost();
        database = property.getDatabase();
        username = property.getUsername();
        password = property.getPassword();

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
