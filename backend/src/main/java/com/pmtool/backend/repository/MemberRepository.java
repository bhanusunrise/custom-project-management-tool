package com.pmtool.backend.repository;

import com.pmtool.backend.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByCognitoSub(String cognitoSub);

    // Find an existing registered member by email.
    Optional<Member> findByEmailIgnoreCase(String email);
}