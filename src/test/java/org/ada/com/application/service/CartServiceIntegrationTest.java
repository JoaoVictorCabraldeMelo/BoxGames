package org.ada.com.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.ada.com.adapters.out.persistence.h2.ConnectionProvider;
import org.ada.com.adapters.out.persistence.h2.H2CartRepository;
import org.ada.com.adapters.out.persistence.h2.H2ClientRepository;
import org.ada.com.adapters.out.persistence.h2.H2GameRepository;
import org.ada.com.adapters.out.persistence.h2.H2WishlistRepository;
import org.ada.com.adapters.out.persistence.h2.SchemaInitializer;
import org.ada.com.domain.model.Game;
import org.ada.com.domain.model.WishlistItem;
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
        H2WishlistRepository wishlistRepository = new H2WishlistRepository(connectionProvider);

        cartService = new CartService(cartRepository, gameRepository, clientRepository, wishlistRepository);
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

        @Test
        void shouldAddAndListWishlistItems() {
        Game first = gameRepository.create(Game.builder()
            .title("First")
            .genre("Action")
            .price(new BigDecimal("10.00"))
            .active(true)
            .build());
        Game second = gameRepository.create(Game.builder()
            .title("Second")
            .genre("RPG")
            .price(new BigDecimal("15.00"))
            .active(true)
            .build());

        walletService.loadOrCreateClient(20L, "Bob");
        cartService.addGameToWishlist(20L, first.getId());
        cartService.addGameToWishlist(20L, second.getId());

        assertThat(cartService.getWishlist(20L))
            .extracting(WishlistItem::getGameId)
            .containsExactly(first.getId(), second.getId());
        }

        @Test
        void shouldMoveWishlistItemToCart() {
        Game game = gameRepository.create(Game.builder()
            .title("Move Me")
            .genre("Adventure")
            .price(new BigDecimal("12.00"))
            .active(true)
            .build());

        walletService.loadOrCreateClient(30L, "Carol");
        cartService.addGameToWishlist(30L, game.getId());

        boolean moved = cartService.moveGameFromWishlistToCart(30L, game.getId(), 3);

        assertThat(moved).isTrue();
        assertThat(cartService.getWishlist(30L)).isEmpty();
        assertThat(cartService.getCart(30L))
            .hasSize(1)
            .first()
            .extracting("gameId", "quantity")
            .containsExactly(game.getId(), 3);
        }

        @Test
        void shouldReturnFalseWhenMovingItemNotPresentInWishlist() {
        Game game = gameRepository.create(Game.builder()
            .title("Missing")
            .genre("Action")
            .price(new BigDecimal("9.00"))
            .active(true)
            .build());

        walletService.loadOrCreateClient(40L, "Dora");

        boolean moved = cartService.moveGameFromWishlistToCart(40L, game.getId(), 1);

        assertThat(moved).isFalse();
        assertThat(cartService.getWishlist(40L)).isEmpty();
        assertThat(cartService.getCart(40L)).isEmpty();
        }
}

