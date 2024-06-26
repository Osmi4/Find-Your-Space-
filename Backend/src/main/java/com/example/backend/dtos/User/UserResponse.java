package com.example.backend.dtos.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private String userId;
    private String email;
    private String contactInfo;
    private String firstName;
    private String lastName;
    private String pictureUrl;
    private Boolean detailsConfigured;
}
