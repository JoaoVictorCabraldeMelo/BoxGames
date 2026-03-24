package org.ada.com.application.service;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckoutResult {
    boolean success;
    BigDecimal total;
    BigDecimal discount;
    BigDecimal remainingCredits;
    String message;
    String couponCode;
}

