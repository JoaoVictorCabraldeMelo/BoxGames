package org.ada.com.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class WishlistItem {
    long clientId;
    long gameId;
}
