package org.ada.com.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Order {
    Long id;
    long clientId;
    BigDecimal total;
    LocalDateTime createdAt;
    List<OrderItem> items;
}
