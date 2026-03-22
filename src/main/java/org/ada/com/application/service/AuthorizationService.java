package org.ada.com.application.service;

import org.ada.com.domain.model.UserRole;

public class AuthorizationService {

    public UserRole authorizeByMenuOption(String option) {
        if ("1".equals(option)) {
            return UserRole.SELLER;
        }
        if ("2".equals(option)) {
            return UserRole.CLIENT;
        }
        throw new IllegalArgumentException("Invalid option. Use 1 for Seller or 2 for Client.");
    }
}

