package org.ada.com.adapters.out.persistence.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ada.com.application.port.out.GameRepository;
import org.ada.com.domain.model.Game;

public class H2GameRepository implements GameRepository {

    private final ConnectionProvider connectionProvider;

    public H2GameRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Game create(Game game) {
        String sql = "INSERT INTO games(title, genre, price, active) VALUES(?, ?, ?, ?)";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, game.getTitle());
            statement.setString(2, game.getGenre());
            statement.setBigDecimal(3, game.getPrice());
            statement.setBoolean(4, game.isActive());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return game.toBuilder().id(keys.getLong(1)).build();
                }
            }
            throw new IllegalStateException("Game id was not generated.");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create game.", ex);
        }
    }

    @Override
    public boolean update(Game game) {
        String sql = "UPDATE games SET title = ?, genre = ?, price = ?, active = ? WHERE id = ?";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, game.getTitle());
            statement.setString(2, game.getGenre());
            statement.setBigDecimal(3, game.getPrice());
            statement.setBoolean(4, game.isActive());
            statement.setLong(5, game.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update game.", ex);
        }
    }

    @Override
    public boolean deactivate(long id) {
        String sql = "UPDATE games SET active = FALSE WHERE id = ?";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to deactivate game.", ex);
        }
    }

    @Override
    public Optional<Game> findById(long id) {
        String sql = "SELECT id, title, genre, price, active FROM games WHERE id = ?";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapGame(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load game.", ex);
        }
    }

    @Override
    public List<Game> findAllActive() {
        String sql = "SELECT id, title, genre, price, active FROM games WHERE active = TRUE ORDER BY id";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return readGames(resultSet);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to list active games.", ex);
        }
    }

    @Override
    public List<Game> filterActive(String titleContains, String genre) {
        String sql = """
                SELECT id, title, genre, price, active
                FROM games
                WHERE active = TRUE
                  AND (? IS NULL OR LOWER(title) LIKE ?)
                  AND (? IS NULL OR LOWER(genre) = LOWER(?))
                ORDER BY id
                """;

        String cleanTitle = titleContains == null || titleContains.isBlank() ? null : titleContains.trim().toLowerCase();
        String cleanGenre = genre == null || genre.isBlank() ? null : genre.trim();

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cleanTitle);
            statement.setString(2, cleanTitle == null ? null : "%" + cleanTitle + "%");
            statement.setString(3, cleanGenre);
            statement.setString(4, cleanGenre);

            try (ResultSet resultSet = statement.executeQuery()) {
                return readGames(resultSet);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to filter active games.", ex);
        }
    }

    private List<Game> readGames(ResultSet resultSet) throws SQLException {
        List<Game> games = new ArrayList<>();
        while (resultSet.next()) {
            games.add(mapGame(resultSet));
        }
        return games;
    }

    private Game mapGame(ResultSet resultSet) throws SQLException {
        return Game.builder()
                .id(resultSet.getLong("id"))
                .title(resultSet.getString("title"))
                .genre(resultSet.getString("genre"))
                .price(resultSet.getBigDecimal("price"))
                .active(resultSet.getBoolean("active"))
                .build();
    }
}

