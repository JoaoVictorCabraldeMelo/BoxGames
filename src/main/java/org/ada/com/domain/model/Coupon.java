package org.ada.com.domain.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Coupon {
    Long id;
    String code;
    BigDecimal discountPct;
    boolean active;
}
