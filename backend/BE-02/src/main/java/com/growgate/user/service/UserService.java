package com.growgate.user.service;

import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.user.dto.UpdateUserRequest;
import com.growgate.user.dto.UpdateUserResponse;
import com.growgate.user.dto.UserMeResponse;
import com.growgate.user.entity.User;
import com.growgate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserMeResponse getMe(String userId) {
        User user = findUser(userId);
        String role = user.getRole() != null ? user.getRole().name() : null;
        String companyId = user.getCompany() != null ? user.getCompany().getId() : null;
        String companyName = user.getCompany() != null ? user.getCompany().getName() : null;

        return UserMeResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType().name())
                .role(role)
                .companyId(companyId)
                .companyName(companyName)
                .createdAt(user.getCreatedAt().format(ISO_FMT))
                .build();
    }

    public UpdateUserResponse updateMe(String userId, UpdateUserRequest request) {
        User user = findUser(userId);
        user.updateName(request.getName());
        return UpdateUserResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .updatedAt(user.getUpdatedAt().format(ISO_FMT))
                .build();
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}
