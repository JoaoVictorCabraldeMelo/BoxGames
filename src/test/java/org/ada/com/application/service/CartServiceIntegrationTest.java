package org.ada.com.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.ada.com.adapters.out.persistence.h2.ConnectionProvider;
import org.ada.com.adapters.out.persistence.h2.H2CartRepository;
import org.ada.com.adapters.out.persistence.h2.H2ClientRepository;
import org.ada.com.adapters.out.persistence.h2.H2CouponRepository;
import org.ada.com.adapters.out.persistence.h2.H2GameRepository;
import org.ada.com.adapters.out.persistence.h2.H2OrderRepository;
import org.ada.com.adapters.out.persistence.h2.H2WishlistRepository;
import org.ada.com.adapters.out.persistence.h2.SchemaInitializer;
import org.ada.com.domain.model.Game;
import org.ada.com.domain.model.Order;
import org.ada.com.domain.model.OrderItem;
import org.ada.com.domain.model.WishlistItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartServiceIntegrationTest {

    private H2GameRepository gameRepository;
    private H2ClientRepository clientRepository;
    private H2CouponRepository couponRepository;
    private CartService cartService;
    private ClientWalletService walletService;
    private CouponService couponService;
    private ConnectionProvider connectionProvider;

    @BeforeEach
    void setUp() {
        connectionProvider = new ConnectionProvider("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        new SchemaInitializer(connectionProvider).initialize();
        cleanDatabase();

        gameRepository = new H2GameRepository(connectionProvider);
        clientRepository = new H2ClientRepository(connectionProvider);
        H2CartRepository cartRepository = new H2CartRepository(connectionProvider);
        H2WishlistRepository wishlistRepository = new H2WishlistRepository(connectionProvider);
        H2OrderRepository orderRepository = new H2OrderRepository(connectionProvider);
        couponRepository = new H2CouponRepository(connectionProvider);

        cartService = new CartService(cartRepository, gameRepository, clientRepository, wishlistRepository, orderRepository, couponRepository);
        walletService = new ClientWalletService(clientRepository);
        couponService = new CouponService(couponRepository);
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

        CheckoutResult result = cartService.checkout(10L, null);

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

    @Test
    void shouldCreateOrderHistoryAfterCheckout() {
        Game game = gameRepository.create(Game.builder()
                .title("Order Game")
                .genre("Action")
                .price(new BigDecimal("25.00"))
                .active(true)
                .build());

        walletService.loadOrCreateClient(50L, "Eve");
        walletService.addCredits(50L, new BigDecimal("100.00"));
        cartService.addGame(50L, game.getId(), 2);

        CheckoutResult result = cartService.checkout(50L, null);

        assertThat(result.isSuccess()).isTrue();
        List<Order> history = cartService.getOrderHistory(50L);
        assertThat(history).hasSize(1);

        Order order = history.getFirst();
        assertThat(order.getTotal()).isEqualByComparingTo("50.00");
        assertThat(order.getItems()).hasSize(1);

        OrderItem item = order.getItems().getFirst();
        assertThat(item.getGameId()).isEqualTo(game.getId());
        assertThat(item.getGameTitle()).isEqualTo("Order Game");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getSubtotal()).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldReturnEmptyOrderHistoryForClientWithoutOrders() {
        walletService.loadOrCreateClient(60L, "Frank");

        List<Order> history = cartService.getOrderHistory(60L);

        assertThat(history).isEmpty();
    }

    // --- Coupon tests ---

    @Test
    void shouldApplyCouponDiscountAtCheckout() {
        Game game = gameRepository.create(Game.builder()
                .title("Discounted Game")
                .genre("RPG")
                .price(new BigDecimal("100.00"))
                .active(true)
                .build());

        walletService.loadOrCreateClient(70L, "Grace");
        walletService.addCredits(70L, new BigDecimal("200.00"));
        cartService.addGame(70L, game.getId(), 1);

        couponService.createCoupon("SAVE10", new BigDecimal("10"));
        CheckoutResult result = cartService.checkout(70L, "SAVE10");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDiscount()).isEqualByComparingTo("10.00");
        assertThat(result.getTotal()).isEqualByComparingTo("90.00");
        assertThat(result.getRemainingCredits()).isEqualByComparingTo("110.00");
        assertThat(result.getCouponCode()).isEqualTo("SAVE10");
    }

    @Test
    void shouldThrowWhenCouponCodeIsInvalid() {
        Game game = gameRepository.create(Game.builder()
                .title("Another Game")
                .genre("Action")
                .price(new BigDecimal("50.00"))
                .active(true)
                .build());

        walletService.loadOrCreateClient(80L, "Henry");
        walletService.addCredits(80L, new BigDecimal("200.00"));
        cartService.addGame(80L, game.getId(), 1);

        assertThatThrownBy(() -> cartService.checkout(80L, "INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coupon not found");
    }

    @Test
    void shouldThrowWhenCouponIsDeleted() {
        Game game = gameRepository.create(Game.builder()
                .title("Yet Another Game")
                .genre("Strategy")
                .price(new BigDecimal("40.00"))
                .active(true)
                .build());

        walletService.loadOrCreateClient(90L, "Iris");
        walletService.addCredits(90L, new BigDecimal("200.00"));
        cartService.addGame(90L, game.getId(), 1);

        var coupon = couponService.createCoupon("DELETED20", new BigDecimal("20"));
        couponService.deleteCoupon(coupon.getId());

        assertThatThrownBy(() -> cartService.checkout(90L, "DELETED20"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coupon is no longer active");
    }

    @Test
    void shouldListAndEditCoupons() {
        couponService.createCoupon("FIRST5", new BigDecimal("5"));
        var second = couponService.createCoupon("SECOND15", new BigDecimal("15"));

        assertThat(couponService.listCoupons()).hasSize(2);

        boolean edited = couponService.editCoupon(second.getId(), "UPDATED15", new BigDecimal("15"));
        assertThat(edited).isTrue();
        assertThat(couponService.listCoupons())
                .extracting(c -> c.getCode())
                .contains("UPDATED15");
    }

    private void cleanDatabase() {
        String sql = "DELETE FROM order_items; DELETE FROM orders; DELETE FROM cart_items;"
                + " DELETE FROM wishlist_items; DELETE FROM clients; DELETE FROM games;"
                + " DELETE FROM coupons;";
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to clean test database.", ex);
        }
    }
}
