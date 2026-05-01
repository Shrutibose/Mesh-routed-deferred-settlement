package com.meshrouteddeferredsettlement.upi.model;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Account findByVpa(String vpa);
}