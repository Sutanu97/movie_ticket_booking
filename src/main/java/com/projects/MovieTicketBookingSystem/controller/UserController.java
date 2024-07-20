package com.projects.MovieTicketBookingSystem.controller;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.projects.MovieTicketBookingSystem.dao.UserRepository;
import com.projects.MovieTicketBookingSystem.dto.JwtRequest;
import com.projects.MovieTicketBookingSystem.dto.JwtResponse;
import com.projects.MovieTicketBookingSystem.entity.User;
import com.projects.MovieTicketBookingSystem.managerImpl.UserPrincipalService;
import com.projects.MovieTicketBookingSystem.security.JwtHelper;

import jakarta.validation.Valid;

@RestController
public class UserController {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserPrincipalService userPrincipalService;
    private AuthenticationManager authenticationManager;
    private JwtHelper jwtHelper;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, UserPrincipalService userPrincipalService, AuthenticationManager authenticationManager, JwtHelper jwtHelper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userPrincipalService = userPrincipalService;
        this.authenticationManager = authenticationManager;
        this.jwtHelper = jwtHelper;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers(){
        LOGGER.debug("In method getAllUsers");
        return this.userRepository.findAll();
    }

    @GetMapping("/users/{username}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and (#username = authentication.principal.username))")
    public User getUserByUsername(@PathVariable String username){
        LOGGER.debug("In method getUserByEmailId");
        Optional<User> userOptional =  userRepository.findByUsername(username);
        return userOptional.isPresent() ? userOptional.get() : null;
    }

    @PostMapping("/users/login")
    public ResponseEntity<Object> login(@RequestBody JwtRequest request){
        LOGGER.debug("In method login");
        authenticate(request.getUsername(), request.getPassword());
        UserDetails userDetails = userPrincipalService.loadUserByUsername(request.getUsername());
        String token = this.jwtHelper.generateToken(userDetails);
        JwtResponse response = new JwtResponse(token, userDetails.getUsername());
        LOGGER.info("User authenticated with username"+ SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private void authenticate(String username, String password) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        try{
            authenticationManager.authenticate(authenticationToken);
        }catch (BadCredentialsException e){
            throw new BadCredentialsException(" Invalid Username or Password  !!");
        }

    }

    @PostMapping("/users/new")
    public ResponseEntity<Object> signup(@Valid @RequestBody User user){
        LOGGER.debug("In method signup");
        String password = passwordEncoder.encode(user.getPassword());
        user.setPassword(password);
        String authorities = Arrays.stream(user.getRoles().split(","))
                .map(e -> "ROLE_"+e)
                .collect(Collectors.joining(","));
        user.setRoles(authorities);
        user = this.userRepository.save(user);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{username}")
                .buildAndExpand(user.getEmailId())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/users/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable String username){
        LOGGER.debug("In method deleteUser");
        Optional<User> user = this.userRepository.findByUsername(username);
        if(user.isPresent()){
            this.userRepository.delete(user.get());
            LOGGER.info("User with username {} deleted", username);
        }else{
            LOGGER.info("No user found with username {} ",username);
        }
    }

}
