package org.ada.com.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ada.com.application.port.out.CartRepository;
import org.ada.com.application.port.out.ClientRepository;
import org.ada.com.application.port.out.GameRepository;
import org.ada.com.application.port.out.OrderRepository;
import org.ada.com.application.port.out.WishlistRepository;
import org.ada.com.domain.model.CartItem;
import org.ada.com.domain.model.ClientAccount;
import org.ada.com.domain.model.Coupon;
import org.ada.com.domain.model.Game;
import org.ada.com.domain.model.Order;
import org.ada.com.domain.model.OrderItem;
import org.ada.com.domain.model.WishlistItem;

@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final GameRepository gameRepository;
    private final ClientRepository clientRepository;
    private final WishlistRepository wishlistRepository;
    private final OrderRepository orderRepository;
    private final CouponService couponService;

    public void addGame(long clientId, long gameId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        Game game = gameRepository.findById(gameId)
                .filter(Game::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Game not found in active catalog."));

        if (game.getPrice().signum() <= 0) {
            throw new IllegalArgumentException("Invalid game price.");
        }

        cartRepository.addItem(clientId, gameId, quantity);
    }

    public boolean removeGame(long clientId, long gameId) {
        return cartRepository.removeItem(clientId, gameId);
    }

    public List<CartItem> getCart(long clientId) {
        return cartRepository.findByClientId(clientId);
    }

    public void addGameToWishlist(long clientId, long gameId) {
        ensureClientExists(clientId);
        gameRepository.findById(gameId)
                .filter(Game::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Game not found in active catalog."));
        wishlistRepository.addItem(clientId, gameId);
    }

    public boolean removeGameFromWishlist(long clientId, long gameId) {
        return wishlistRepository.removeItem(clientId, gameId);
    }

    public List<WishlistItem> getWishlist(long clientId) {
        return wishlistRepository.findByClientId(clientId);
    }

    public boolean moveGameFromWishlistToCart(long clientId, long gameId, int quantity) {
        ensureClientExists(clientId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        boolean removed = wishlistRepository.removeItem(clientId, gameId);
        if (!removed) {
            return false;
        }

        addGame(clientId, gameId, quantity);
        return true;
    }

    public List<Order> getOrderHistory(long clientId) {
        ensureClientExists(clientId);
        return orderRepository.findByClientId(clientId);
    }

    /**
     * Performs checkout, optionally applying a discount coupon.
     *
     * @param couponCode coupon code to apply, or {@code null} / blank to skip
     */
    public CheckoutResult checkout(long clientId, String couponCode) {
        ClientAccount client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client account not found."));

        List<CartItem> items = cartRepository.findByClientId(clientId);
        if (items.isEmpty()) {
            return CheckoutResult.builder()
                    .success(false)
                    .total(BigDecimal.ZERO)
                    .discount(BigDecimal.ZERO)
                    .remainingCredits(client.getCredits())
                    .message("Cart is empty.")
                    .build();
        }

        BigDecimal subtotalSum = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : items) {
            Game game = gameRepository.findById(cartItem.getGameId())
                .orElseThrow(() -> new IllegalStateException("Game in cart no longer exists."));
            BigDecimal subtotal = game.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotalSum = subtotalSum.add(subtotal);
            orderItems.add(OrderItem.builder()
                .gameId(game.getId())
                .gameTitle(game.getTitle())
                .gameGenre(game.getGenre())
                .unitPrice(game.getPrice())
                .quantity(cartItem.getQuantity())
                .subtotal(subtotal)
                .build());
        }

        // Apply coupon discount if provided
        BigDecimal discount = BigDecimal.ZERO;
        String appliedCode = null;
        if (couponCode != null && !couponCode.isBlank()) {
            Coupon coupon = couponService.applyCoupon(couponCode);
            discount = subtotalSum
                    .multiply(coupon.getDiscountPct())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            appliedCode = coupon.getCode();
        }

        BigDecimal total = subtotalSum.subtract(discount);

        if (client.getCredits().compareTo(total) < 0) {
            return CheckoutResult.builder()
                    .success(false)
                    .total(total)
                    .discount(discount)
                    .remainingCredits(client.getCredits())
                    .couponCode(appliedCode)
                    .message("Insufficient credits.")
                    .build();
        }

        BigDecimal newCredits = client.getCredits().subtract(total);
        orderRepository.createOrder(clientId, total, orderItems);
        clientRepository.updateCredits(clientId, newCredits);
        cartRepository.clearByClientId(clientId);

        return CheckoutResult.builder()
                .success(true)
                .total(total)
                .discount(discount)
                .remainingCredits(newCredits)
                .couponCode(appliedCode)
                .message("Purchase completed.")
                .build();
    }

    private void ensureClientExists(long clientId) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client account not found."));
    }
}
