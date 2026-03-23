package org.ada.com.adapters.out.persistence.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.ada.com.application.port.out.WishlistRepository;
import org.ada.com.domain.model.WishlistItem;

public class H2WishlistRepository implements WishlistRepository {

    private final ConnectionProvider connectionProvider;

    public H2WishlistRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public void addItem(long clientId, long gameId) {
        String sql = "MERGE INTO wishlist_items(client_id, game_id) KEY(client_id, game_id) VALUES(?, ?)";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clientId);
            statement.setLong(2, gameId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to add game to wishlist.", ex);
        }
    }

    @Override
    public boolean removeItem(long clientId, long gameId) {
        String sql = "DELETE FROM wishlist_items WHERE client_id = ? AND game_id = ?";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clientId);
            statement.setLong(2, gameId);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to remove game from wishlist.", ex);
        }
    }

    @Override
    public List<WishlistItem> findByClientId(long clientId) {
        String sql = "SELECT client_id, game_id FROM wishlist_items WHERE client_id = ? ORDER BY game_id";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<WishlistItem> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(WishlistItem.builder()
                            .clientId(resultSet.getLong("client_id"))
                            .gameId(resultSet.getLong("game_id"))
                            .build());
                }
                return items;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to list wishlist items.", ex);
        }
    }
}
