package org.ada.com.adapters.out.persistence.h2;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.ada.com.application.port.out.ClientRepository;
import org.ada.com.domain.model.ClientAccount;

public class H2ClientRepository implements ClientRepository {

    private final ConnectionProvider connectionProvider;

    public H2ClientRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<ClientAccount> findById(long id) {
        String sql = "SELECT id, name, credits FROM clients WHERE id = ?";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapAccount(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load client account.", ex);
        }
    }

    @Override
    public ClientAccount upsert(long id, String name) {
        String cleanName = (name == null || name.isBlank()) ? "Client-" + id : name.trim();
        String insertSql = "INSERT INTO clients(id, name, credits) VALUES(?, ?, 0)";
        String updateSql = "UPDATE clients SET name = ? WHERE id = ?";

        try (Connection connection = connectionProvider.getConnection()) {
            if (findById(id).isPresent()) {
                try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                    update.setString(1, cleanName);
                    update.setLong(2, id);
                    update.executeUpdate();
                }
            } else {
                try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                    insert.setLong(1, id);
                    insert.setString(2, cleanName);
                    insert.executeUpdate();
                }
            }

            return findById(id).orElseThrow(() -> new IllegalStateException("Client account was not persisted."));
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to upsert client account.", ex);
        }
    }

    @Override
    public boolean updateCredits(long clientId, BigDecimal credits) {
        String sql = "UPDATE clients SET credits = ? WHERE id = ?";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, credits);
            statement.setLong(2, clientId);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update client credits.", ex);
        }
    }

    private ClientAccount mapAccount(ResultSet resultSet) throws SQLException {
        return ClientAccount.builder()
                .id(resultSet.getLong("id"))
                .name(resultSet.getString("name"))
                .credits(resultSet.getBigDecimal("credits"))
                .build();
    }
}


