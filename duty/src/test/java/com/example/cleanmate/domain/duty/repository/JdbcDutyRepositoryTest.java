package com.example.cleanmate.domain.duty.repository;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * H2 인메모리 DB 로 JdbcDutyRepository 계약 검증.
 * schema.sql 이 test/resources 에 있어 @JdbcTest 가 자동으로 실행한다.
 */
@JdbcTest
class JdbcDutyRepositoryTest extends DutyRepositoryContractTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void truncate() {
        jdbcTemplate.execute("DELETE FROM duties");
    }

    @Override
    protected DutyRepository createRepository() {
        return new JdbcDutyRepository(jdbcTemplate);
    }
}
