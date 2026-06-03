package com.pck4x.users_service.infrastructure.persistence.jpa.adapters;

import com.pck4x.sharedcontracts.objects.QueryResult;
import com.pck4x.users_service.application.dto.response.AccountResponse;
import com.pck4x.users_service.application.dto.response.UserAccountsResponse;
import com.pck4x.users_service.application.port.output.AccountProjectionRepository;
import com.pck4x.users_service.domain.AccountProjection;
import com.pck4x.users_service.infrastructure.persistence.jpa.entities.AccountProjectionEntity;
import com.pck4x.users_service.infrastructure.persistence.jpa.repositories.JpaAccountProjectionRepository;
import com.pck4x.users_service.infrastructure.persistence.mapper.AccountProjectionMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AccountProjectionRepositoryAdapter implements AccountProjectionRepository {

    private final JpaAccountProjectionRepository jpaRepository;

    @PersistenceContext
    private EntityManager em;

    public AccountProjectionRepositoryAdapter(JpaAccountProjectionRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(AccountProjection projection) {
        jpaRepository.save(AccountProjectionMapper.INSTANCE.toEntity(projection));
    }

    @Override
    public Optional<AccountProjection> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(AccountProjectionMapper.INSTANCE::toDomain);
    }

    @Override
    public List<AccountProjection> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId)
                .stream()
                .map(AccountProjectionMapper.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public UserAccountsResponse findUserAccountsByUserId(UUID userId) {
        String jpql = """
                SELECT u.id, u.firstName, u.lastName, a
                FROM UserEntity u, AccountProjectionEntity a
                WHERE u.id = a.userId AND u.id = :userId
                ORDER BY a.createdAt DESC
                """;

        List<Object[]> rows = em.createQuery(jpql, Object[].class)
                .setParameter("userId", userId)
                .getResultList();

        if (rows.isEmpty()) {
            return null;
        }

        UUID uid = (UUID) rows.getFirst()[0];
        String firstName = (String) rows.getFirst()[1];
        String lastName = (String) rows.getFirst()[2];

        List<AccountResponse> accounts = rows.stream()
                .map(row -> toAccountResponse((AccountProjectionEntity) row[3]))
                .toList();

        return new UserAccountsResponse(uid, firstName, lastName, accounts);
    }

    @Override
    public QueryResult<List<AccountResponse>> findAccountsByUserId(UUID userId, int page, int size) {
        Long totalCount = em.createQuery(
                        "SELECT COUNT(a) FROM AccountProjectionEntity a WHERE a.userId = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();

        List<AccountProjectionEntity> projections = em.createQuery(
                        "SELECT a FROM AccountProjectionEntity a WHERE a.userId = :userId ORDER BY a.createdAt DESC",
                        AccountProjectionEntity.class)
                .setParameter("userId", userId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        int totalPages = totalCount > 0 ? (int) Math.ceil((double) totalCount / size) : 0;

        List<AccountResponse> accounts = projections.stream()
                .map(this::toAccountResponse)
                .toList();

        return QueryResult.of(accounts, totalCount.intValue(), totalPages, page, size);
    }

    private AccountResponse toAccountResponse(AccountProjectionEntity p) {
        return new AccountResponse(
                p.getId(),
                p.getAccountNumber(),
                p.getBalance(),
                p.getCurrency(),
                p.getStatus(),
                p.getUserId(),
                p.getCreatedAt()
        );
    }
}
