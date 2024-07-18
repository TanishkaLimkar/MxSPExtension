package com.mendix.jettyserver;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Logger;

public class MendixServer {
    private static String VENDORLIB_PATH;

    public static void main(String[] args) throws Exception {
        Server server = new Server(8081);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new MyServlet()), "/hello");
        context.addServlet(new ServletHolder(new VendorlibPathServlet()), "/setVendorlibPath");

        server.start();
        server.join();
    }

    public static void setVendorlibPath(String path) {
        VENDORLIB_PATH = path;
        loadJarsFromVendorLib();
        if (!registerJdbcDrivers()) {
            System.err.println("No suitable database driver found in vendorlib.");
        }
    }

    public static String getVendorlibPath() {
        return VENDORLIB_PATH;
    }

    public static void loadJarsFromVendorLib() {
        if (VENDORLIB_PATH != null) {
            File vendorLibDir = new File(VENDORLIB_PATH);
            if (vendorLibDir.exists() && vendorLibDir.isDirectory()) {
                File[] jarFiles = vendorLibDir.listFiles((dir, name) -> name.endsWith(".jar"));
                if (jarFiles != null) {
                    URL[] jarUrls = new URL[jarFiles.length];
                    for (int i = 0; i < jarFiles.length; i++) {
                        try {
                            jarUrls[i] = jarFiles[i].toURI().toURL();
                            System.out.println("Loading JAR: " + jarFiles[i].getName());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    URLClassLoader urlClassLoader = new URLClassLoader(jarUrls, MendixServer.class.getClassLoader());
                    Thread.currentThread().setContextClassLoader(urlClassLoader);
                }
            } else {
                System.err.println("Vendorlib directory not found or is not a directory.");
            }
        } else {
            System.err.println("Vendorlib path is not set.");
        }
    }

    private static boolean registerJdbcDrivers() {
        boolean driverFound = false;
        URLClassLoader urlClassLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
        URL[] urls = urlClassLoader.getURLs();

        boolean mysqlDriverFound = false;

        for (URL url : urls) {
            String path = url.getPath();
            if (path.contains("mysql-connector-java-8.0.26.jar")) {
                mysqlDriverFound = true;
                break;
            }
        }

        if (!mysqlDriverFound) {
            System.err.println("MySQL JDBC driver not found in vendorlib.");
        }

        ServiceLoader<Driver> drivers = ServiceLoader.load(Driver.class);
        for (Driver driver : drivers) {
            try {
                System.out.println("Registering driver: " + driver.getClass().getName());
                DriverManager.registerDriver(new DriverShim(driver));
                driverFound = true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return driverFound;
    }

    static class DriverShim implements Driver {
        private final Driver driver;

        DriverShim(Driver driver) {
            this.driver = driver;
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return this.driver.acceptsURL(url);
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return this.driver.connect(url, info);
        }

        @Override
        public int getMajorVersion() {
            return this.driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return this.driver.getMinorVersion();
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return this.driver.getPropertyInfo(url, info);
        }

        @Override
        public boolean jdbcCompliant() {
            return this.driver.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return this.driver.getParentLogger();
        }
    }
}





