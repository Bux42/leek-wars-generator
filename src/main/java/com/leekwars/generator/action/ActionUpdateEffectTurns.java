package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;

/**
 * Change la durée restante d'un effet en cours. Le client décompte les tours lui-même, au
 * tour du lanceur de l'effet : tout ce qui retire un tour en dehors de ce décompte doit le
 * lui dire, sans quoi il garde l'effet affiché un tour de trop.
 */
public class ActionUpdateEffectTurns implements Action {

	private final int id;
	private final int turns;

	public ActionUpdateEffectTurns(int id, int turns) {
		this.id = id;
		this.turns = turns;
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.UPDATE_EFFECT_TURNS);
		retour.add(id);
		retour.add(turns);
		return retour;
	}
}
