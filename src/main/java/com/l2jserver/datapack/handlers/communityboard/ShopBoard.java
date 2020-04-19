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

import java.util.StringTokenizer;

import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.config.Configuration;
import com.l2jserver.gameserver.data.xml.impl.MultisellData;
import com.l2jserver.gameserver.handler.CommunityBoardHandler;
import com.l2jserver.gameserver.handler.IParseBoardHandler;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Andrii Hromov
 *
 */
public class ShopBoard implements IParseBoardHandler {

	private static final String[] COMMANDS = { "_bbsshop", "_bbsmultisell" };

	@Override
	public String[] getCommunityBoardCommands() {
		return COMMANDS;
	}

	@Override
	public boolean parseCommunityBoardCommand(String command, L2PcInstance player) {
		if (!Configuration.customShopConfiguration().getCommunityShop()) {
			showPage(player, "disable.html");

			return true;
		}

		if (player.isInOlympiadMode() || player.isJailed() || (player.getKarma() > 0) || player.isInStance()) {
			player.sendMessage("In this condition shopping not allowed.");

			return true;
		}

		if (command.equals("_bbsshop")) {
			showPage(player, "10002.htm");

			return true;
		} else if (command.startsWith("_bbsshop")) {
			final String page = getFirstToken(command, " ").nextToken();

			showPage(player, page);

			return true;
		} else if (command.startsWith("_bbsmultisell;")) {
			MultisellData.getInstance().separateAndSend(Integer.parseInt(getFirstToken(command, ";").nextToken()),
					player, null, false);

			return true;
		}

		return false;
	}

	private StringTokenizer getFirstToken(String command, String token) {
		final StringTokenizer stFirst = new StringTokenizer(command, token);
		stFirst.nextToken();

		return stFirst;
	}

	private void showPage(L2PcInstance player, String page) {
		if (((page.length() > 0) && page.endsWith(".html")) || page.endsWith(".htm")) {
			String content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
					"data/html/CommunityBoard/shop/" + page);

			CommunityBoardHandler.separateAndSend(content, player);
		}
	}

}
