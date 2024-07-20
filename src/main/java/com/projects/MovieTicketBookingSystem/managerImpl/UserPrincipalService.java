package com.projects.MovieTicketBookingSystem.managerImpl;

import com.projects.MovieTicketBookingSystem.dao.UserRepository;
import com.projects.MovieTicketBookingSystem.dto.UserPrincipal;
import com.projects.MovieTicketBookingSystem.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserPrincipalService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserPrincipalService.class);

    public UserPrincipalService() {
    }

    public UserPrincipalService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOptional = this.userRepository.findByUsername(username);
        if(userOptional.isEmpty()){
            LOGGER.info("User not found");
            return null;
        }
        User user = userOptional.get();
        String password = user.getPassword();
        String roles = user.getRoles();
        UserPrincipal userPrincipal = new UserPrincipal(user.getPkUserId(), username, password, roles, user.getContact(), user.getEmailId());
        LOGGER.info("User found "+ userPrincipal.getUsername());
        return userPrincipal;
    }
}
