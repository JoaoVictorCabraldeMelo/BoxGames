package org.ada.com.application.port.out;

import java.util.List;
import org.ada.com.domain.model.WishlistItem;

public interface WishlistRepository {
    void addItem(long clientId, long gameId);

    boolean removeItem(long clientId, long gameId);

    List<WishlistItem> findByClientId(long clientId);
}
