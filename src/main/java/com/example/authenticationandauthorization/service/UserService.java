package com.example.authenticationandauthorization.service;

import com.example.authenticationandauthorization.exception.UsernameAlreadyExistsException;
import com.example.authenticationandauthorization.jwt.JwtUtil;
import com.example.authenticationandauthorization.model.dto.LoginDTO;
import com.example.authenticationandauthorization.model.dto.UserDTO;
import com.example.authenticationandauthorization.model.entity.UserEntity;
import com.example.authenticationandauthorization.model.mapper.UserMapper;
import com.example.authenticationandauthorization.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    // Define a record (immutable, concise)
    public record TokenResponse(String token) {}

    public TokenResponse login(LoginDTO loginDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtUtil.generateToken(loginDTO.getUsername());
            return new TokenResponse("Bearer " + token);
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid username or password");
        } catch (InternalAuthenticationServiceException e) {
            throw new InternalAuthenticationServiceException("An error occurred during login");
        }
    }

    public UserDTO register(UserDTO userDTO) {
        boolean existsByUsername = userRepository.existsByUsername(userDTO.getUsername());
        if (existsByUsername) {
            throw new UsernameAlreadyExistsException("Username is already in use");
        }

        UserEntity entity = userMapper.toEntity(userDTO);
        // Hash the password before saving
        entity.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        UserEntity savedUserEntity = userRepository.save(entity);

        return userMapper.fromEntity(savedUserEntity);
    }
}
