package com.leekwars.api.mongo.services;

import com.leekwars.api.mongo.repositories.LeekRepository;

public class LeekService {
    private final LeekRepository leeks;

    public LeekService(LeekRepository leeks) {
        this.leeks = leeks;
    }

    public void deleteLeek(String leekId) {
        leeks.delete(leekId);
    }
}
