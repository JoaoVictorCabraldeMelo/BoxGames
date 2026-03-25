package org.ada.com.adapters.out.persistence.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ada.com.application.port.out.CouponRepository;
import org.ada.com.domain.model.Coupon;

public class H2CouponRepository implements CouponRepository {

    private final ConnectionProvider connectionProvider;

    public H2CouponRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Coupon create(Coupon coupon) {
        String sql = "INSERT INTO coupons(code, discount_pct, active) VALUES(?, ?, TRUE)";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, coupon.getCode());
            statement.setBigDecimal(2, coupon.getDiscountPct());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return coupon.toBuilder().id(keys.getLong(1)).active(true).build();
                }
            }
            throw new IllegalStateException("Failed to retrieve generated coupon id.");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create coupon.", ex);
        }
    }

    @Override
    public boolean update(Coupon coupon) {
        String sql = "UPDATE coupons SET code = ?, discount_pct = ?, active = ? WHERE id = ?";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, coupon.getCode());
            statement.setBigDecimal(2, coupon.getDiscountPct());
            statement.setBoolean(3, coupon.isActive());
            statement.setLong(4, coupon.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update coupon.", ex);
        }
    }

    @Override
    public boolean deactivate(long id) {
        String sql = "UPDATE coupons SET active = FALSE WHERE id = ? AND active = TRUE";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to deactivate coupon.", ex);
        }
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
        String sql = "SELECT id, code, discount_pct, active FROM coupons WHERE code = ?";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to find coupon by code.", ex);
        }
    }

    @Override
    public List<Coupon> findAllActive() {
        String sql = "SELECT id, code, discount_pct, active FROM coupons WHERE active = TRUE ORDER BY id";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Coupon> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to list coupons.", ex);
        }
    }

    private Coupon mapRow(ResultSet rs) throws SQLException {
        return Coupon.builder()
                .id(rs.getLong("id"))
                .code(rs.getString("code"))
                .discountPct(rs.getBigDecimal("discount_pct"))
                .active(rs.getBoolean("active"))
                .build();
    }
}
