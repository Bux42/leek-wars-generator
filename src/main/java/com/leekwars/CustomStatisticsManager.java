package com.leekwars;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.chips.Chip;
import com.leekwars.generator.effect.Effect;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.StatisticsManager;
import com.leekwars.generator.items.Item;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.weapons.Weapon;

public class CustomStatisticsManager implements StatisticsManager {
    Map<Integer, Long> operationsByEntity = new java.util.HashMap<>();
    int kills;
    int bullets;
    long usedChips;
    long useWeapons;
    long summons;
    long directDamage;
    long heal;
    long distance;
    long stackOverflow;
    long errors;
    long resurrects;
    long damagePoison;
    long damageReturn;
    long criticalHits;
    long tpUsed;
    long mpUsed;
    long operations;
    long says;
    long saysLength;
    long chests;
    long chestKills;

    int teleportations;
    int tooMuchOperations;

    Map<Integer, Map<Integer, Integer>> leekResources = new java.util.HashMap<>();

    @Override
    public void init(Entity entity) {
    }

    @Override
    public void say(Entity entity, String message) {
        this.says++;
        this.saysLength += message.length();
    }

    @Override
    public void teleportation(Entity entity, Entity caster, Cell start, Cell end) {
        this.teleportations++;
    }

    @Override
    public void lama(Entity entity) {
    }

    @Override
    public void characteristics(Entity entity) {
    }

    @Override
    public void updateStat(Entity entity, int characteristic, int delta, Entity caster) {
    }

    @Override
    public void tooMuchOperations(Entity entity) {
        tooMuchOperations++;
    }

    @Override
    public void stackOverflow(Entity entity) {
        stackOverflow++;
    }

    @Override
    public void damage(Entity entity, Entity attacker, int damage, DamageType direct, Effect effect) {
        directDamage += damage;
    }

    @Override
    public void summon(Entity entity, Entity summon) {
        summons++;
    }

    @Override
    public void useTP(int tp) {
        tpUsed += tp;
    }

    @Override
    public void heal(Entity healer, Entity entity, int pv) {
        heal += pv;
    }

    @Override
    public void error(Entity entity) {
        errors++;
    }

    @Override
    public void useChip(Entity caster, Chip chip, Cell cell, List<Entity> targets, Entity cellEntity) {
        usedChips++;
    }

    @Override
    public void useWeapon(Entity caster, Weapon weapon, Cell cell, List<Entity> targets, Entity cellEntity) {
        useWeapons++;
    }

    @Override
    public void kill(Entity killer, Entity entity, Item item, Cell killCell) {
        kills++;
    }

    @Override
    public void critical(Entity launcher) {
        criticalHits++;
    }

    @Override
    public void endFight(Collection<Entity> values) {
    }

    @Override
    public void addTimes(Entity current, long time, long operations) {
    }

    @Override
    public void move(Entity mover, Entity entity, Cell start, List<Cell> path) {
    }

    @Override
    public void resurrect(Entity caster, Entity target) {
        resurrects++;
    }

    @Override
    public Map<Integer, Long> getOperationsByEntity() {
        return operationsByEntity;
    }

    @Override
    public int getKills() {
        return kills;
    }

    @Override
    public int getBullets() {
        return bullets;
    }

    @Override
    public long getUsedChips() {
        return usedChips;
    }

    @Override
    public long getSummons() {
        return summons;
    }

    @Override
    public long getDirectDamage() {
        return directDamage;
    }

    @Override
    public long getHeal() {
        return heal;
    }

    @Override
    public long getDistance() {
        return distance;
    }

    @Override
    public long getStackOverflow() {
        return stackOverflow;
    }

    @Override
    public long getErrors() {
        return errors;
    }

    @Override
    public long getResurrects() {
        return resurrects;
    }

    @Override
    public long getDamagePoison() {
        return damagePoison;
    }

    @Override
    public long getDamageReturn() {
        return damageReturn;
    }

    @Override
    public long getCriticalHits() {
        return criticalHits;
    }

    @Override
    public long getTPUsed() {
        return tpUsed;
    }

    @Override
    public long getMPUsed() {
        return mpUsed;
    }

    @Override
    public long getOperations() {
        return operations;
    }

    @Override
    public long getSays() {
        return says;
    }

    @Override
    public long getSaysLength() {
        return saysLength;
    }

    @Override
    public void tooMuchDebug(int farmer) {
    }

    @Override
    public void show(Entity mEntity, int cell_id) {
    }

    @Override
    public void slide(Entity entity, Entity caster, Cell start, Cell cell) {
    }

    @Override
    public void useInvalidPosition(Entity caster, Attack attack, Cell target) {
    }

    @Override
    public void effect(Entity entity, Entity caster, Effect effect) {
    }

    @Override
    public void entityTurn(Entity entity) {
    }

    @Override
    public void antidote(Entity entity, Entity caster, int poisonsRemoved) {
    }

    @Override
    public void vitality(Entity entity, Entity caster, int vitality) {
    }

    @Override
    public void registerWrite(Entity entity, String key, String value) {
    }

    @Override
    public void setWeapon(Entity entity, Weapon w) {
    }

    @Override
    public void chest() {
    }

    @Override
    public void chestKilled(Entity killer, Entity entity, Map<Integer, Integer> resources) {
    }

    @Override
    public Map<Integer, Map<Integer, Integer>> getLeekResources() {
        return leekResources;
    }

    @Override
    public long getChests() {
        return chests;
    }

    @Override
    public long getChestsKills() {
        return chestKills;
    }

    public int getTeleportation() {
        return teleportations;
    }

    public int getTooMuchOperations() {
        return tooMuchOperations;
    }

    public long getUseWeapons() {
        return useWeapons;
    }

    @Override
    public void setGeneratorFight(Fight fight) {

    }
}
