package org.ada.com.config;

import org.ada.com.adapters.in.cli.TerminalApp;
import org.ada.com.adapters.out.persistence.h2.ConnectionProvider;
import org.ada.com.adapters.out.persistence.h2.H2CartRepository;
import org.ada.com.adapters.out.persistence.h2.H2ClientRepository;
import org.ada.com.adapters.out.persistence.h2.H2CouponRepository;
import org.ada.com.adapters.out.persistence.h2.H2GameRepository;
import org.ada.com.adapters.out.persistence.h2.H2OrderRepository;
import org.ada.com.adapters.out.persistence.h2.H2WishlistRepository;
import org.ada.com.adapters.out.persistence.h2.SchemaInitializer;
import org.ada.com.application.service.AuthorizationService;
import org.ada.com.application.service.CartService;
import org.ada.com.application.service.ClientCatalogService;
import org.ada.com.application.service.ClientWalletService;
import org.ada.com.application.service.CouponService;
import org.ada.com.application.service.SellerCatalogService;

public final class AppFactory {

    private AppFactory() {
    }

    public static TerminalApp createTerminalApp() {
        ConnectionProvider connectionProvider = new ConnectionProvider("jdbc:h2:mem:boxgames;DB_CLOSE_DELAY=-1", "sa", "");
        new SchemaInitializer(connectionProvider).initialize();

        H2GameRepository gameRepository = new H2GameRepository(connectionProvider);
        H2ClientRepository clientRepository = new H2ClientRepository(connectionProvider);
        H2CartRepository cartRepository = new H2CartRepository(connectionProvider);
        H2WishlistRepository wishlistRepository = new H2WishlistRepository(connectionProvider);
        H2OrderRepository orderRepository = new H2OrderRepository(connectionProvider);
        H2CouponRepository couponRepository = new H2CouponRepository(connectionProvider);

        AuthorizationService authorizationService = new AuthorizationService();
        SellerCatalogService sellerCatalogService = new SellerCatalogService(gameRepository);
        ClientCatalogService clientCatalogService = new ClientCatalogService(gameRepository);
        ClientWalletService clientWalletService = new ClientWalletService(clientRepository);
        CouponService couponService = new CouponService(couponRepository);
        CartService cartService = new CartService(
            cartRepository,
            gameRepository,
            clientRepository,
            wishlistRepository,
            orderRepository,
            couponService);

        return new TerminalApp(
                authorizationService,
                sellerCatalogService,
                clientCatalogService,
                clientWalletService,
                cartService,
                couponService,
                gameRepository);
    }
}

