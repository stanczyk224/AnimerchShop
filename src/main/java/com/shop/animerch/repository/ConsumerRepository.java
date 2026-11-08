package com.shop.animerch.repository;

import com.shop.animerch.model.Consumer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsumerRepository extends JpaRepository<Consumer, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Consumer c where c.user.email = :email")
    Optional<Consumer> findByUserEmailForUpdate(@Param("email") String email);

    Optional<Consumer> findByUserEmail(String email);
}
