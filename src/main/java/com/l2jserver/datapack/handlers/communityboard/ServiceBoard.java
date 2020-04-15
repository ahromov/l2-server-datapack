/*
 * Copyright (C) 2004-2018 L2J DataPack
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
package com.l2jserver.datapack.handlers.communityboard;

import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.handler.CommunityBoardHandler;
import com.l2jserver.gameserver.handler.IParseBoardHandler;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Andrew Hromov
 *
 */
public class ServiceBoard implements IParseBoardHandler {

	private static final String[] COMMANDS = { "_bbsservice" };

	@Override
	public String[] getCommunityBoardCommands() {
		return COMMANDS;
	}

	@Override
	public boolean parseCommunityBoardCommand(String command, L2PcInstance player) {
		if (player.isDead() || player.isAlikeDead() || player.isInSiege() || player.isCastingNow()
				|| player.isInCombat() || player.isAttackingNow() || player.isInOlympiadMode() || player.isJailed()
				|| player.isFlying() || (player.getKarma() > 0) || player.isInDuel()) {
			player.sendMessage("Under these conditions, this service isn't allowed.");

			return false;
		}

		if (command.equals("_bbsservice")) {
			if (player.getStat().isExpBlock() == false) {
				String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
						"data/html/CommunityBoard/services/off.html");
				CommunityBoardHandler.separateAndSend(html, player);
			} else {
				String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
						"data/html/CommunityBoard/services/on.html");
				CommunityBoardHandler.separateAndSend(html, player);
			}
		} else if (command.equals("_bbsservice_offxp")) {
			enableBlock(player);
			String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
					"data/html/CommunityBoard/services/on.html");
			CommunityBoardHandler.separateAndSend(html, player);
		} else if (command.equals("_bbsservice_onxp")) {
			disableBlock(player);
			String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
					"data/html/CommunityBoard/services/off.html");
			CommunityBoardHandler.separateAndSend(html, player);
		}

		return true;
	}

	/**
	 * @param player
	 */
	private void disableBlock(L2PcInstance player) {
		player.getStat().setExpBlock(false);
		player.getStat().setSpBlock(false);
	}

	/**
	 * @param player
	 */
	private void enableBlock(L2PcInstance player) {
		player.getStat().setExpBlock(true);
		player.getStat().setSpBlock(true);
	}

}
