/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;

/**
 * Core 2.8.9 resolves role privileges in a daemon's independent transaction. Call
 * create from @Before, before the test body writes any user or clinical data, to
 * commit the initial metadata fixtures (as Core's base test setup does).
 * Call close from @AfterTransaction, after the test body's writes roll back.
 * This helper refuses any database other than an in-memory H2 test database.
 */
public final class CommittedRoleFixture implements AutoCloseable {

    private final ConnectionProvider connectionProvider;

    private final String name = "rest-test-" + UUID.randomUUID();

    private final List<String> createdPrivileges = new ArrayList<>();

    private CommittedRoleFixture(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public static CommittedRoleFixture create(Connection setupConnection, String... privileges) throws SQLException {
        requireInMemoryH2(setupConnection);
        SessionFactory factory = Context.getRegisteredComponent("sessionFactory", SessionFactory.class);
        ConnectionProvider provider = factory.unwrap(SessionFactoryImplementor.class).getServiceRegistry()
                .getService(ConnectionProvider.class);
        CommittedRoleFixture fixture = new CommittedRoleFixture(provider);
        UserService service = Context.getUserService();
        Role role = new Role(fixture.name);
        role.setDescription("Isolated REST authorization test fixture");
        for (String privilegeName : privileges) {
            Privilege privilege = service.getPrivilege(privilegeName);
            if (privilege == null) {
                privilege = service.savePrivilege(new Privilege(privilegeName));
                fixture.createdPrivileges.add(privilegeName);
            }
            role.addPrivilege(privilege);
        }
        service.saveRole(role);
        Context.flushSession();
        setupConnection.commit();
        return fixture;
    }

    public String getName() {
        return name;
    }

    private Connection connection() throws SQLException {
        Connection connection = connectionProvider.getConnection();
        try {
            requireInMemoryH2(connection);
            connection.setAutoCommit(false);
            return connection;
        } catch (SQLException | RuntimeException failure) {
            connection.close();
            throw failure;
        }
    }

    private static void requireInMemoryH2(Connection connection) throws SQLException {
        if (!connection.getMetaData().getURL().startsWith("jdbc:h2:mem:")) {
            throw new IllegalStateException("Committed role fixtures require an in-memory H2 database");
        }
    }

    private static void execute(Connection connection, String sql, String... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                statement.setString(i + 1, values[i]);
            }
            statement.executeUpdate();
        }
    }

    @Override
    public void close() throws SQLException {
        try (Connection connection = connection()) {
            try {
                execute(connection, "DELETE FROM role_privilege WHERE role = ?", name);
                execute(connection, "DELETE FROM role WHERE role = ?", name);
                for (String privilege : createdPrivileges) {
                    execute(connection, "DELETE FROM privilege WHERE privilege = ?", privilege);
                }
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                connection.rollback();
                throw failure;
            }
        } finally {
            Context.clearEntireCache();
        }
    }
}
