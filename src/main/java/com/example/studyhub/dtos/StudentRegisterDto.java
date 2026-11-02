package com.example.studyhub.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRegisterDto {

    private String fullName;
    private Integer departmentId;
    private String username;
    private String email;
    private String phone;
    private String password;
    private String confirmPassword;
}
