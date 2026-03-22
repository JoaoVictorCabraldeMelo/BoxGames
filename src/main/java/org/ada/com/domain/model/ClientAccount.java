package org.ada.com.domain.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ClientAccount {
    long id;
    String name;
    BigDecimal credits;
}

