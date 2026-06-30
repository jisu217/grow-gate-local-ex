package com.growgate.security;

// Spring Security 사용자 로드
import com.growgate.membership.entity.Membership;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        com.growgate.user.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("TYPE_" + user.getUserType().name().toUpperCase()));

        membershipRepository.findFirstByUserId(user.getId())
                .map(Membership::getRole)
                .ifPresent(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name().toUpperCase())));

        if (authorities.stream().noneMatch(auth -> auth.getAuthority().startsWith("ROLE_"))
                && user.getUserType().name().equals("personal")) {
            authorities.add(new SimpleGrantedAuthority("ROLE_OWNER"));
        }

        return new User(user.getId(), user.getPasswordHash(), authorities);
    }
}
