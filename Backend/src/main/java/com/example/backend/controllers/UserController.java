package com.example.backend.controllers;

import com.example.backend.dtos.User.UpdateUserDetailsRequest;
import com.example.backend.dtos.User.UpdateUserRequest;
import com.example.backend.dtos.User.UserFilter;
import com.example.backend.dtos.User.UserResponse;
import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/confirm-login")
    public ResponseEntity<String> confirmLogin() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok("User with id " + userId + " is logged in!");
    }

    @GetMapping("/my-details")
    public ResponseEntity<UserResponse> getMyDetails() {
        return ResponseEntity.ok(userService.getMyDetails());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id){
        return ResponseEntity.ok(userService.getUserByUserId(id));
    }

    //admin
    @PostMapping("/search")
    public ResponseEntity<Page<UserResponse>> getUsers(@RequestBody UserFilter userFilter, @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userService.getUsersByFilters(userFilter , pageable));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable String id, @Valid @RequestBody UpdateUserRequest user) {
        userService.updateUser(id, user);
        return new ResponseEntity("User updated successfully!", HttpStatus.OK);
    }
    //change user details
    @PatchMapping("/my-details")
    public ResponseEntity<String> updateUserDetails(@Valid @RequestBody UpdateUserDetailsRequest userDetails) {
        userService.updateUserDetails(userDetails);
        return new ResponseEntity("User updated successfully!", HttpStatus.OK);
    }
    @GetMapping("/getMyBankAccount")
    public ResponseEntity<String> getBankAccount(){
        return ResponseEntity.ok(userService.getBankAccount());
    }
    @GetMapping("/my-role")
    public ResponseEntity<String> getRole(){
        return ResponseEntity.ok(userService.getRole());
    }
}
