package org.ada.com.adapters.out.persistence.h2;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ada.com.application.port.out.OrderRepository;
import org.ada.com.domain.model.Order;
import org.ada.com.domain.model.OrderItem;

public class H2OrderRepository implements OrderRepository {

    private final ConnectionProvider connectionProvider;

    public H2OrderRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public long createOrder(long clientId, BigDecimal total, List<OrderItem> items) {
        String insertOrderSql = "INSERT INTO orders(client_id, total) VALUES(?, ?)";
        String insertItemSql = """
                INSERT INTO order_items(order_id, game_id, game_title, game_genre, unit_price, quantity, subtotal)
                VALUES(?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long orderId;
                try (PreparedStatement orderStatement = connection.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                    orderStatement.setLong(1, clientId);
                    orderStatement.setBigDecimal(2, total);
                    orderStatement.executeUpdate();
                    try (ResultSet keys = orderStatement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new IllegalStateException("Order id was not generated.");
                        }
                        orderId = keys.getLong(1);
                    }
                }

                try (PreparedStatement itemStatement = connection.prepareStatement(insertItemSql)) {
                    for (OrderItem item : items) {
                        itemStatement.setLong(1, orderId);
                        itemStatement.setLong(2, item.getGameId());
                        itemStatement.setString(3, item.getGameTitle());
                        itemStatement.setString(4, item.getGameGenre());
                        itemStatement.setBigDecimal(5, item.getUnitPrice());
                        itemStatement.setInt(6, item.getQuantity());
                        itemStatement.setBigDecimal(7, item.getSubtotal());
                        itemStatement.addBatch();
                    }
                    itemStatement.executeBatch();
                }

                connection.commit();
                return orderId;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create order.", ex);
        }
    }

    @Override
    public List<Order> findByClientId(long clientId) {
        String sql = """
                SELECT o.id AS order_id,
                       o.client_id,
                       o.total,
                       o.created_at,
                       oi.game_id,
                       oi.game_title,
                       oi.game_genre,
                       oi.unit_price,
                       oi.quantity,
                       oi.subtotal
                FROM orders o
                LEFT JOIN order_items oi ON oi.order_id = o.id
                WHERE o.client_id = ?
                ORDER BY o.created_at DESC, o.id DESC, oi.id ASC
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Long, MutableOrder> ordersMap = new LinkedHashMap<>();
                while (resultSet.next()) {
                    long orderId = resultSet.getLong("order_id");
                    MutableOrder mutable = ordersMap.computeIfAbsent(orderId, id ->
                            new MutableOrder(
                                    id,
                                    getLong(resultSet, "client_id"),
                                    getBigDecimal(resultSet, "total"),
                                    getLocalDateTime(resultSet, "created_at")));

                    Long gameId = getNullableLong(resultSet, "game_id");
                    if (gameId != null) {
                        mutable.items.add(OrderItem.builder()
                                .orderId(orderId)
                                .gameId(gameId)
                                .gameTitle(getString(resultSet, "game_title"))
                                .gameGenre(getString(resultSet, "game_genre"))
                                .unitPrice(getBigDecimal(resultSet, "unit_price"))
                                .quantity(getInt(resultSet, "quantity"))
                                .subtotal(getBigDecimal(resultSet, "subtotal"))
                                .build());
                    }
                }

                List<Order> orders = new ArrayList<>();
                for (MutableOrder mutable : ordersMap.values()) {
                    orders.add(Order.builder()
                            .id(mutable.id)
                            .clientId(mutable.clientId)
                            .total(mutable.total)
                            .createdAt(mutable.createdAt)
                            .items(mutable.items)
                            .build());
                }
                return orders;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load order history.", ex);
        }
    }

    private long getLong(ResultSet resultSet, String column) {
        try {
            return resultSet.getLong(column);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to map column " + column + ".", ex);
        }
    }

    private Long getNullableLong(ResultSet resultSet, String column) {
        try {
            long value = resultSet.getLong(column);
            return resultSet.wasNull() ? null : value;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to map column " + column + ".", ex);
        }
    }

    private int getInt(ResultSet resultSet, String column) {
        try {
            return resultSet.getInt(column);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to map column " + column + ".", ex);
        }
    }

    private String getString(ResultSet resultSet, String column) {
        try {
            return resultSet.getString(column);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to map column " + column + ".", ex);
        }
    }

    private BigDecimal getBigDecimal(ResultSet resultSet, String column) {
        try {
            return resultSet.getBigDecimal(column);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to map column " + column + ".", ex);
        }
    }

    private LocalDateTime getLocalDateTime(ResultSet resultSet, String column) {
        try {
            Timestamp timestamp = resultSet.getTimestamp(column);
            return timestamp == null ? null : timestamp.toLocalDateTime();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to map column " + column + ".", ex);
        }
    }

    private static class MutableOrder {
        private final long id;
        private final long clientId;
        private final BigDecimal total;
        private final LocalDateTime createdAt;
        private final List<OrderItem> items;

        private MutableOrder(long id, long clientId, BigDecimal total, LocalDateTime createdAt) {
            this.id = id;
            this.clientId = clientId;
            this.total = total;
            this.createdAt = createdAt;
            this.items = new ArrayList<>();
        }
    }
}
