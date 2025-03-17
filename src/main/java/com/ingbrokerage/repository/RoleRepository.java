package com.ingbrokerage.repository;

import com.ingbrokerage.model.Role;
import com.ingbrokerage.model.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    Optional<Role> findByName(RoleType name);
} 