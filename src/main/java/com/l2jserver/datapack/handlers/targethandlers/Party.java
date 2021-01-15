/*
 * Copyright © 2004-2020 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.datapack.handlers.targethandlers;

import java.util.ArrayList;
import java.util.List;

import com.l2jserver.gameserver.handler.ITargetTypeHandler;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.model.skills.targets.TargetType;

/**
 * @author UnAfraid
 */
public class Party implements ITargetTypeHandler {
	@Override
	public L2Object[] getTargetList(Skill skill, L2Character activeChar, boolean onlyFirst, L2Character target) {
		List<L2Character> targetList = new ArrayList<>();
		if (onlyFirst) {
			return new L2Character[] {
				activeChar
			};
		}
		
		targetList.add(activeChar);
		
		final int radius = skill.getAffectRange();
		L2PcInstance player = activeChar.getActingPlayer();
		if (activeChar.isSummon()) {
			if (Skill.addCharacter(activeChar, player, radius, false)) {
				targetList.add(player);
			}
		} else if (activeChar.isPlayer()) {
			if (Skill.addSummon(activeChar, player, radius, false)) {
				targetList.add(player.getSummon());
			}
		}
		
		if (activeChar.isInParty()) {
			// Get a list of Party Members
			for (L2PcInstance partyMember : activeChar.getParty().getMembers()) {
				if ((partyMember == null) || (partyMember == player)) {
					continue;
				}
				
				if (Skill.addCharacter(activeChar, partyMember, radius, false)) {
					targetList.add(partyMember);
				}
				
				if (Skill.addSummon(activeChar, partyMember, radius, false)) {
					targetList.add(partyMember.getSummon());
				}
			}
		}
		return targetList.toArray(EMPTY_TARGET_LIST);
	}
	
	@Override
	public Enum<TargetType> getTargetType() {
		return TargetType.PARTY;
	}
}
