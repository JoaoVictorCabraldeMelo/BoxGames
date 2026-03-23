package org.ada.com.domain.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class OrderItem {
    Long orderId;
    long gameId;
    String gameTitle;
    String gameGenre;
    BigDecimal unitPrice;
    int quantity;
    BigDecimal subtotal;
}
