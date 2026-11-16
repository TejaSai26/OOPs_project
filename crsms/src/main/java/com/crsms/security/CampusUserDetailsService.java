package com.crsms.security;

import com.crsms.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CampusUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public CampusUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.findByEmailIgnoreCase(username)
                .map(CampusUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown email"));
    }
}
