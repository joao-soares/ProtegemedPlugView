/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protegemed.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author João Antônio Soares
 */
public class ProtegemedPropertyLoad {
    
    private final String host;
    private final String database;
    private final String username;
    private final String password;
    private final Integer maxTime;
    
    public ProtegemedPropertyLoad(String propertyFile){
        Properties prop = new Properties();
        InputStream input;
        
        String vHost;
        String vDatabase;
        String vUsername;
        String vPassword;
        Integer vMaxTime;
        
        try {
            input = new FileInputStream(propertyFile);
            prop.load(input);

            vHost = prop.getProperty("host");
            vDatabase = prop.getProperty("database");
            vUsername = prop.getProperty("username");
            vPassword = prop.getProperty("password");
            vMaxTime = Integer.parseInt(prop.getProperty("maxTime"));

        } catch (IOException e) {
            System.out.println(e);
            System.out.println("Using default HSVP values");
            
            vHost = "10.8.0.1";
            vDatabase = "protegemed";
            vUsername = "root";
            vPassword = "senha.123";
            vMaxTime = 30;
        }
        
        this.host = vHost;
        this.database = vDatabase;
        this.username = vUsername;
        this.password = vPassword;
        this.maxTime = vMaxTime;
        
    }

    public String getHost() {
        return host;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Integer getMaxTime() {
        return maxTime;
    }
   
}
