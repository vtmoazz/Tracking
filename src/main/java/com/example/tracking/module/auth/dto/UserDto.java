package com.example.tracking.module.auth.dto;

import com.example.tracking.module.auth.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {
    private String id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private User.Role role;
    private User.Status status;

    public static UserDto from(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
