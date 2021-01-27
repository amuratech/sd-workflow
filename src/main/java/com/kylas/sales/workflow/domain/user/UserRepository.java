package com.kylas.sales.workflow.domain.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByIdAndTenantId(long userId, long tenantId);
}
