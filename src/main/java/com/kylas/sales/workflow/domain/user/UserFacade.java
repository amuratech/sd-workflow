package com.kylas.sales.workflow.domain.user;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserFacade {

  private final UserRepository userRepository;

  @Autowired
  public UserFacade(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User getExistingOrCreateNewUser(User userDetails, long tenantId) {
    return userRepository
        .findByIdAndTenantId(userDetails.getId(), tenantId)
        .map(
            user -> {
              var updatedUser = user.withName(userDetails.getName());
              return userRepository.saveAndFlush(updatedUser);
            })
        .orElse(userRepository.saveAndFlush(userDetails.withTenantId(tenantId)))
        .withPermissions(userDetails.getPermissions());
  }

  public Optional<User> tryGetUserByIdAndTenantId(Long userId, Long tenantId) {
    return userRepository.findByIdAndTenantId(userId, tenantId);
  }

  public Optional<User> tryUpdateUser(
      Long userId, Long tenantId, String firstName, String lastName) {
    return tryGetUserByIdAndTenantId(userId, tenantId)
        .map(user -> user.withName(firstName, lastName))
        .map(userRepository::saveAndFlush);
  }
}
