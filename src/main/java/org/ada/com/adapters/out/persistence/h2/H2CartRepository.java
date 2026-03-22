package org.ada.com.adapters.out.persistence.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.ada.com.application.port.out.CartRepository;
import org.ada.com.domain.model.CartItem;

public class H2CartRepository implements CartRepository {

    private final ConnectionProvider connectionProvider;

    public H2CartRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public void addItem(long clientId, long gameId, int quantity) {
        String updateSql = "UPDATE cart_items SET quantity = quantity + ? WHERE client_id = ? AND game_id = ?";
        String insertSql = "INSERT INTO cart_items(client_id, game_id, quantity) VALUES(?, ?, ?)";

        try (Connection connection = connectionProvider.getConnection()) {
            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                update.setInt(1, quantity);
                update.setLong(2, clientId);
                update.setLong(3, gameId);
                int affected = update.executeUpdate();
                if (affected > 0) {
                    return;
                }
            }

            try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                insert.setLong(1, clientId);
                insert.setLong(2, gameId);
                insert.setInt(3, quantity);
                insert.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to add game to cart.", ex);
        }
    }

    @Override
    public boolean removeItem(long clientId, long gameId) {
        String sql = "DELETE FROM cart_items WHERE client_id = ? AND game_id = ?";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clientId);
            statement.setLong(2, gameId);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to remove game from cart.", ex);
        }
    }

    @Override
    public List<CartItem> findByClientId(long clientId) {
        String sql = "SELECT client_id, game_id, quantity FROM cart_items WHERE client_id = ? ORDER BY game_id";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CartItem> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(CartItem.builder()
                            .clientId(resultSet.getLong("client_id"))
                            .gameId(resultSet.getLong("game_id"))
                            .quantity(resultSet.getInt("quantity"))
                            .build());
                }
                return items;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load cart items.", ex);
        }
    }

    @Override
    public void clearByClientId(long clientId) {
        String sql = "DELETE FROM cart_items WHERE client_id = ?";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clientId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to clear cart items.", ex);
        }
    }
}


