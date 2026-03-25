package com.campus.task.module.user.service;

import com.campus.task.module.user.entity.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security UserDetailsService 实现
 */
public interface UserDetailsServiceImpl {
    UserDetails loadUserById(Long userId);
}
