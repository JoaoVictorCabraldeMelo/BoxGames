package org.ada.com.application.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ada.com.application.port.out.CouponRepository;
import org.ada.com.domain.model.Coupon;

@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public Coupon createCoupon(String code, BigDecimal discountPct) {
        validate(code, discountPct);
        return couponRepository.create(Coupon.builder()
                .code(code.trim().toUpperCase())
                .discountPct(discountPct)
                .active(true)
                .build());
    }

    public boolean editCoupon(long id, String code, BigDecimal discountPct) {
        validate(code, discountPct);
        return couponRepository.update(Coupon.builder()
                .id(id)
                .code(code.trim().toUpperCase())
                .discountPct(discountPct)
                .active(true)
                .build());
    }

    public boolean deleteCoupon(long id) {
        return couponRepository.deactivate(id);
    }

    public List<Coupon> listCoupons() {
        return couponRepository.findAllActive();
    }

    /** Resolves and validates a coupon code at checkout time. Returns the coupon if found and active. */
    public Coupon applyCoupon(String code) {
        Coupon coupon = couponRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + code));
        if (!coupon.isActive()) {
            throw new IllegalArgumentException("Coupon is no longer active: " + code);
        }
        return coupon;
    }

    private void validate(String code, BigDecimal discountPct) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Coupon code is required.");
        }
        if (discountPct == null || discountPct.compareTo(BigDecimal.ZERO) <= 0
                || discountPct.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Discount percentage must be between 0 (exclusive) and 100.");
        }
    }
}
