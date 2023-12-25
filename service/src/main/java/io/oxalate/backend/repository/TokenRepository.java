package io.oxalate.backend.repository;

import io.oxalate.backend.model.Token;
import io.oxalate.backend.model.TokenType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends CrudRepository<Token, Long> {
    Optional<Token> findByToken(String token);

    Optional<Token> findByUserId(long userId);

    Optional<Token> findByTokenAndTokenType(String token, TokenType tokenType);

    @Modifying
    void deleteByToken(String token);

    @Modifying
    void deleteByUserId(long userId);

    Optional<Token> findByUserIdAndTokenType(long userId, TokenType tokenType);
}
