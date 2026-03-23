package org.ada.com.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import org.ada.com.domain.model.Order;
import org.ada.com.domain.model.OrderItem;

public interface OrderRepository {
    long createOrder(long clientId, BigDecimal total, List<OrderItem> items);

    List<Order> findByClientId(long clientId);
}
