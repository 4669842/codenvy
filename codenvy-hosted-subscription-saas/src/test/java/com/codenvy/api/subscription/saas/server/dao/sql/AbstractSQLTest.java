/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2015] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.subscription.saas.server.dao.sql;

import com.codenvy.sql.StorageInitializer;

import org.postgresql.ds.PGPoolingDataSource;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

/**
 * @author Sergii Kabashniuk
 */
public class AbstractSQLTest {

    protected static DataSource source;

    protected static SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy HH:mm:ss");

    @BeforeSuite
    public void init() throws SQLException {
        final PGPoolingDataSource postgresql = new PGPoolingDataSource();
        postgresql.setDataSourceName("codenvy");
        postgresql.setServerName("dev.box.com");
        postgresql.setDatabaseName("dbcodenvy");
        postgresql.setUser("pgcodenvy");
        postgresql.setPassword("codenvy");
        postgresql.setMaxConnections(10);
        postgresql.setPortNumber(5432);
        source = postgresql;
    }

    @BeforeMethod
    @AfterMethod
    public void cleanup() throws SQLException {
        if (source != null) {
            StorageInitializer initializer = new StorageInitializer(source, true);
            initializer.clean();
            initializer.init();
        }
    }
}
