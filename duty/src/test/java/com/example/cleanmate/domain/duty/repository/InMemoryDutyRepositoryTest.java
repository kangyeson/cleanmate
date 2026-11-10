package com.example.cleanmate.domain.duty.repository;

class InMemoryDutyRepositoryTest extends DutyRepositoryContractTest {

    @Override
    protected DutyRepository createRepository() {
        return new InMemoryDutyRepository();
    }
}
