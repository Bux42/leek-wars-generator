package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.state.ComponentInstance;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.FightLoadout;
import com.leekwars.generator.state.State;

/**
 * Le pot commun des pièces AMÉLIORÉES, le temps d'un combat.
 *
 * <p>Une pièce améliorée est un objet unique : elle ne peut valoir que pour un poireau. Les
 * ensembles d'un éleveur étant partagés par tous ses poireaux, la même pièce laissée au stock
 * profitait à chacun d'eux à la fois. La règle : au début du combat chaque poireau TIENT ce
 * qu'il porte, le stock libre est un pot commun, et un {@code setLoadout()} rend au pot ce que
 * le poireau tenait puis y prend ce que son ensemble demande — premier arrivé, premier servi.
 *
 * <p>Garantie centrale : un poireau qui n'appelle pas {@code setLoadout()} ne rend rien, donc
 * on ne lui retire jamais son équipement.
 */
public class TestFightComponentPool extends FightTestBase {

	private static final int TEMPLATE = 4242;
	private static final int BONUS = 50;

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "L1");
		leek2 = defaultLeek(2, "L2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	/** Une pièce améliorée : +50 force. */
	private static ComponentInstance improved(int id) {
		return new ComponentInstance(id, TEMPLATE, Map.of(Entity.STAT_STRENGTH, BONUS));
	}

	/**
	 * Un ensemble qui demande la pièce améliorée. `stats` porte déjà les stats de base du
	 * modèle : c'est l'amélioration, et elle seule, qui se dispute à l'appel.
	 */
	private static FightLoadout loadoutWanting(String name, Map<Integer, Integer> wanted) {
		var stats = new HashMap<Integer, Integer>();
		stats.put(Entity.STAT_LIFE, 800);
		stats.put(Entity.STAT_STRENGTH, 250);
		List<FightLoadout.ComponentChoice> components = new ArrayList<>();
		components.add(new FightLoadout.ComponentChoice(TEMPLATE, wanted));
		return new FightLoadout(name, java.util.Collections.emptyList(), java.util.Collections.emptyList(),
			java.util.Collections.emptyList(), stats, null, false, components);
	}

	@Test
	public void twoLeeksCannotShareTheSameImprovedPiece() throws Exception {
		// Une seule pièce améliorée au stock, et les deux poireaux du même éleveur la
		// réclament dans le même combat.
		fight.getState().setFreeComponents(0, List.of(improved(10)));
		leek1.addLoadout(loadoutWanting("pvp", Map.of(Entity.STAT_STRENGTH, BONUS)));
		leek2.addLoadout(loadoutWanting("pvp", Map.of(Entity.STAT_STRENGTH, BONUS)));
		attachAI(leek1, "function beforeFight() { setLoadout('pvp'); }"
			+ "setRegister('strength', '' + getStrength());");
		attachAI(leek2, "function beforeFight() { setLoadout('pvp'); }"
			+ "setRegister('strength', '' + getStrength());");
		runFight();

		int s1 = Integer.parseInt(leek1.getRegister("strength"));
		int s2 = Integer.parseInt(leek2.getRegister("strength"));
		// Premier arrivé, premier servi : un seul des deux l'a, et la pièce n'a pas été
		// comptée deux fois.
		Assert.assertEquals("la pièce ne vaut que pour un poireau", 250 + 250 + BONUS, s1 + s2);
		Assert.assertTrue("l'un des deux a le bonus entier", s1 == 250 + BONUS || s2 == 250 + BONUS);
	}

	@Test
	public void aLeekWithoutSetLoadoutKeepsItsEquipment() throws Exception {
		// leek2 PORTE la pièce améliorée (il la tient) et ne joue aucun ensemble ; leek1 la
		// réclame. Le pot est vide : rien à prendre, on ne dépouille pas leek2.
		fight.getState().setFreeComponents(0, new ArrayList<>());
		leek2.setHeldComponents(List.of(improved(10)));
		leek1.addLoadout(loadoutWanting("pvp", Map.of(Entity.STAT_STRENGTH, BONUS)));
		attachAI(leek1, "function beforeFight() { setLoadout('pvp'); }"
			+ "setRegister('strength', '' + getStrength());");
		attachAI(leek2, "");
		runFight();

		Assert.assertEquals("250", leek1.getRegister("strength"));
		Assert.assertEquals("leek2 tient toujours sa pièce", 1, leek2.getHeldComponents().size());
	}

	@Test
	public void aLeekGetsBackTheImprovedPieceItWasWearing() throws Exception {
		// Ensemble d'avant les altérations : aucune stat mémorisée, donc aucun exemplaire
		// désigné. Le poireau ne doit pas perdre l'amélioration qu'il portait.
		fight.getState().setFreeComponents(0, new ArrayList<>());
		leek1.setHeldComponents(List.of(improved(10)));
		leek1.addLoadout(loadoutWanting("legacy", null));
		attachAI(leek1, "function beforeFight() { setLoadout('legacy'); }"
			+ "setRegister('strength', '' + getStrength());");
		attachAI(leek2, "");
		runFight();

		Assert.assertEquals(String.valueOf(250 + BONUS), leek1.getRegister("strength"));
	}

	// ---------- Le pot, en direct ----------

	@Test
	public void anUnaskedImprovedPieceIsNotTaken() {
		State state = new State();
		state.setFreeComponents(7, List.of(improved(10)));
		// Sans stats voulues, l'ensemble ne désigne aucun exemplaire : on ne va pas se servir
		// dans le pot, sinon un ensemble d'avant les altérations raflerait la pièce d'un autre.
		Assert.assertNull(state.takeComponent(7, TEMPLATE, null, null));
		Assert.assertNull(state.takeComponent(7, TEMPLATE, Map.of(), new ArrayList<>()));
		// La même, si le poireau la tenait déjà, lui revient.
		var mine = improved(10);
		state.setFreeComponents(7, List.of(mine));
		Assert.assertSame(mine, state.takeComponent(7, TEMPLATE, null, List.of(mine)));
	}

	@Test
	public void theClosestPieceWinsAndLeavesThePool() {
		State state = new State();
		var weak = new ComponentInstance(10, TEMPLATE, Map.of(Entity.STAT_STRENGTH, 10));
		var exact = new ComponentInstance(11, TEMPLATE, Map.of(Entity.STAT_STRENGTH, BONUS));
		state.setFreeComponents(7, List.of(weak, exact));

		Assert.assertSame(exact, state.takeComponent(7, TEMPLATE, Map.of(Entity.STAT_STRENGTH, BONUS), null));
		// Prise = retirée du pot : le poireau suivant n'a plus qu'elle.
		Assert.assertSame(weak, state.takeComponent(7, TEMPLATE, Map.of(Entity.STAT_STRENGTH, BONUS), null));
		Assert.assertNull(state.takeComponent(7, TEMPLATE, Map.of(Entity.STAT_STRENGTH, BONUS), null));
	}

	@Test
	public void releasedPiecesGoBackToThePool() {
		State state = new State();
		var instance = improved(10);
		state.setFreeComponents(7, new ArrayList<>());
		Assert.assertNull("rien au pot", state.takeComponent(7, TEMPLATE, Map.of(Entity.STAT_STRENGTH, BONUS), null));
		// Le poireau qui la tenait se rééquipe : elle redevient disponible pour les suivants.
		state.releaseComponents(7, List.of(instance));
		Assert.assertSame(instance, state.takeComponent(7, TEMPLATE, Map.of(Entity.STAT_STRENGTH, BONUS), null));
	}

	@Test
	public void distanceIsWeighted() {
		// Un PT d'écart (80) pèse plus que 50 points de vie : mêmes poids que côté PHP.
		Assert.assertEquals(80, ComponentInstance.distance(Map.of(Entity.STAT_TP, 1), null));
		Assert.assertEquals(50, ComponentInstance.distance(Map.of(Entity.STAT_LIFE, 50), Map.of()));
		Assert.assertEquals(0, ComponentInstance.distance(Map.of(Entity.STAT_LIFE, 10), Map.of(Entity.STAT_LIFE, 10)));
		Assert.assertEquals(0, ComponentInstance.distance(null, null));
	}
}
