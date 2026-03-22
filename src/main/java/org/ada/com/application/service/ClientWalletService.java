package org.ada.com.application.service;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.ada.com.application.port.out.ClientRepository;
import org.ada.com.domain.model.ClientAccount;

@RequiredArgsConstructor
public class ClientWalletService {

    private final ClientRepository clientRepository;

    public ClientAccount loadOrCreateClient(long clientId, String name) {
        return clientRepository.upsert(clientId, name);
    }

    public ClientAccount addCredits(long clientId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        ClientAccount account = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client account not found."));
        BigDecimal newCredits = account.getCredits().add(amount);
        clientRepository.updateCredits(clientId, newCredits);

        return account.toBuilder().credits(newCredits).build();
    }
}

