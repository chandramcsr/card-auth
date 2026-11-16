package cardauth.repository;

import cardauth.model.CardAccount;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCardAccountRepository implements CardAccountRepository {

    private final Map<String, CardAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public Optional<CardAccount> findById(String cardId) {
        return Optional.ofNullable(accounts.get(cardId));
    }

    @Override
    public void save(CardAccount account) {
        accounts.put(account.cardId(), account);
    }
}
