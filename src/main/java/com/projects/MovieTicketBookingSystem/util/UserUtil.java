package com.projects.MovieTicketBookingSystem.util;

import com.projects.MovieTicketBookingSystem.dto.UserPrincipal;
import com.projects.MovieTicketBookingSystem.entity.User;

public class UserUtil {
    public static User getUserFromUserPrincipal(UserPrincipal principal) {
        User user = new User();
        user.setPkUserId(principal.getId());
        user.setUsername(principal.getUsername());
        user.setPassword(principal.getPassword());
        return user;
    }
}

