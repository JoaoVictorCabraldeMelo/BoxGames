package org.ada.com.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.ada.com.adapters.out.persistence.h2.ConnectionProvider;
import org.ada.com.adapters.out.persistence.h2.H2CartRepository;
import org.ada.com.adapters.out.persistence.h2.H2ClientRepository;
import org.ada.com.adapters.out.persistence.h2.H2GameRepository;
import org.ada.com.adapters.out.persistence.h2.SchemaInitializer;
import org.ada.com.domain.model.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartServiceIntegrationTest {

    private H2GameRepository gameRepository;
    private H2ClientRepository clientRepository;
    private CartService cartService;
    private ClientWalletService walletService;

    @BeforeEach
    void setUp() {
        ConnectionProvider connectionProvider = new ConnectionProvider("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        new SchemaInitializer(connectionProvider).initialize();

        gameRepository = new H2GameRepository(connectionProvider);
        clientRepository = new H2ClientRepository(connectionProvider);
        H2CartRepository cartRepository = new H2CartRepository(connectionProvider);

        cartService = new CartService(cartRepository, gameRepository, clientRepository);
        walletService = new ClientWalletService(clientRepository);
    }

    @Test
    void shouldCheckoutWhenClientHasEnoughCredits() {
        Game game = gameRepository.create(Game.builder()
                .title("Test Game")
                .genre("Action")
                .price(new BigDecimal("20.00"))
                .active(true)
                .build());

        walletService.loadOrCreateClient(10L, "Alice");
        walletService.addCredits(10L, new BigDecimal("100.00"));
        cartService.addGame(10L, game.getId(), 2);

        CheckoutResult result = cartService.checkout(10L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotal()).isEqualByComparingTo("40.00");
        assertThat(result.getRemainingCredits()).isEqualByComparingTo("60.00");
        assertThat(cartService.getCart(10L)).isEmpty();
    }
}

