package com.chandramcsr.cardauthorization.repository;

import com.chandramcsr.cardauthorization.model.CardAccount;

import java.util.Optional;

public interface CardAccountRepository {

    Optional<CardAccount> findById(String cardId);

    void save(CardAccount account);
}
