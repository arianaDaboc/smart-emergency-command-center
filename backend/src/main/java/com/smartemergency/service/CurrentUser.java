package com.smartemergency.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.smartemergency.domain.User;
import com.smartemergency.web.ApiException;
import org.springframework.http.HttpStatus;

/** Gives access to the logged-in user (set by the JWT filter). The id never comes from the request. */
@Component
public class CurrentUser {
    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return u;
    }
}
