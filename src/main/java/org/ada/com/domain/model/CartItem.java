package org.ada.com.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class CartItem {
    long clientId;
    long gameId;
    int quantity;
}

