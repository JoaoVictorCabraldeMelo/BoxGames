package org.ada.com.domain.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Game {
    Long id;
    String title;
    String genre;
    BigDecimal price;
    boolean active;
}

