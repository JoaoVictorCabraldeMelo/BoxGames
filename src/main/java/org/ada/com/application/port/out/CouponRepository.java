package org.ada.com.application.port.out;

import java.util.List;
import java.util.Optional;
import org.ada.com.domain.model.Coupon;

public interface CouponRepository {
    Coupon create(Coupon coupon);
    boolean update(Coupon coupon);
    boolean deactivate(long id);
    Optional<Coupon> findByCode(String code);
    List<Coupon> findAllActive();
}
