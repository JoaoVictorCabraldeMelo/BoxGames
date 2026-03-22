package org.ada.com.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import org.ada.com.domain.model.ClientAccount;

public interface ClientRepository {
    Optional<ClientAccount> findById(long id);

    ClientAccount upsert(long id, String name);

    boolean updateCredits(long clientId, BigDecimal credits);
}

