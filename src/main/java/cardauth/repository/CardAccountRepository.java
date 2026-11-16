package cardauth.repository;

import cardauth.model.CardAccount;

import java.util.Optional;

public interface CardAccountRepository {

    Optional<CardAccount> findById(String cardId);

    void save(CardAccount account);
}
