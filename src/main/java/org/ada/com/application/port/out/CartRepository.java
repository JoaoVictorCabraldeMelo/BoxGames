package org.ada.com.application.port.out;

import java.util.List;
import org.ada.com.domain.model.CartItem;

public interface CartRepository {
    void addItem(long clientId, long gameId, int quantity);

    boolean removeItem(long clientId, long gameId);

    List<CartItem> findByClientId(long clientId);

    void clearByClientId(long clientId);
}

