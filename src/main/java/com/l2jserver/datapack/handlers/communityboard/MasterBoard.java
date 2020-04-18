/*
 * Copyright (C) 2004-2020 L2J DataPack
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

import static com.l2jserver.gameserver.network.SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT;
import static com.l2jserver.gameserver.network.SystemMessageId.NOT_ENOUGH_ITEMS;

import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.config.Configuration;
import com.l2jserver.gameserver.config.CustomClassMasterConfiguration;
import com.l2jserver.gameserver.config.CustomNobleMasterConfiguration;
import com.l2jserver.gameserver.data.xml.impl.ClassListData;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.handler.CommunityBoardHandler;
import com.l2jserver.gameserver.handler.IParseBoardHandler;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.model.entity.TvTEvent;
import com.l2jserver.gameserver.model.holders.ItemHolder;
import com.l2jserver.gameserver.network.serverpackets.ExBrExtraUserInfo;
import com.l2jserver.gameserver.network.serverpackets.UserInfo;
import com.l2jserver.gameserver.util.StringUtil;

/**
 * @author Andrii Hromov
 *
 */
public class MasterBoard implements IParseBoardHandler {

	private static final String[] COMMANDS = { "_bbsclassmaster", "_bbsclassmaster_1stClass",
			"_bbsclassmaster_2ndClass", "_bbsclassmaster_3rdClass", "_bbsclassmaster_change_class",
			"_bbsclassmaster_become_noble" };
	private static final CustomClassMasterConfiguration CLASSMASTER_CONFIG = Configuration.customClassMaster();
	private static final CustomNobleMasterConfiguration NOBLE_CONFIG = Configuration.customNobleMaster();

	@Override
	public String[] getCommunityBoardCommands() {
		return COMMANDS;
	}

	@Override
	public boolean parseCommunityBoardCommand(String command, L2PcInstance player) {
		if (!CLASSMASTER_CONFIG.getCommunityClassMaster()) {
			String content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
					"data/html/CommunityBoard/classmaster/disable.htm");

			CommunityBoardHandler.separateAndSend(content, player);

			return false;
		}

		if (player.isDead() || player.isAlikeDead() || player.isInSiege() || player.isCastingNow()
				|| player.isInCombat() || player.isAttackingNow() || player.isInOlympiadMode() || player.isJailed()
				|| player.isFlying() || (player.getKarma() > 0) || player.isInDuel() || player.isInStance()
				|| player.isInCraftMode() || player.isInStoreMode()) {
			player.sendMessage("In this condition classmaster not allowed.");

			return false;
		}

		if (command.equals("_bbsclassmaster")) {
			String html = HtmCache.getInstance()
					.getHtm(player.getHtmlPrefix(), "data/html/CommunityBoard/classmaster/index.htm")
					.replace("%req_noble_items%", getRequiredNobleItems(1));

			CommunityBoardHandler.separateAndSend(html, player);
		} else if (command.equals("_bbsclassmaster_1stClass")) {
			showHtmlMenu(player, 1);
		} else if (command.equals("_bbsclassmaster_2ndClass")) {
			showHtmlMenu(player, 2);
		} else if (command.equals("_bbsclassmaster_3rdClass")) {
			showHtmlMenu(player, 3);
		} else if (command.startsWith("_bbsclassmaster_change_class")) {
			int val = Integer.parseInt(command.substring(29));

			if (checkAndChangeClass(player, val)) {
				String msg = HtmCache.getInstance()
						.getHtm(player.getHtmlPrefix(), "data/html/CommunityBoard/classmaster/ok.htm")
						.replace("%name%", ClassListData.getInstance().getClass(val).getClientCode());

				CommunityBoardHandler.separateAndSend(msg, player);

				return true;
			}
		} else if (command.startsWith("_bbsclassmaster_become_noble")) {
			if (checkAndSetNoble(player)) {
				String msg = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
						"data/html/CommunityBoard/classmaster/nobleok.htm");

				CommunityBoardHandler.separateAndSend(msg, player);

				return true;
			}
		}

		return true;
	}

	private void showHtmlMenu(L2PcInstance player, int level) {
		if (!CLASSMASTER_CONFIG.getCommunityClassMaster()) {
			int classIdLevel = player.getClassId().level();

			StringBuilder html = new StringBuilder();
			html.append("<html><body>");

			switch (classIdLevel) {
			case 0:
				if (CLASSMASTER_CONFIG.getClassMasterSettings().isAllowed(1)) {
					html.append("Come back here when you reached level 20 to change your class.<br>");
				} else if (CLASSMASTER_CONFIG.getClassMasterSettings().isAllowed(2)) {
					html.append("Come back after your first occupation change.<br>");
				} else if (CLASSMASTER_CONFIG.getClassMasterSettings().isAllowed(3)) {
					html.append("Come back after your second occupation change.<br>");
				} else {
					html.append("I can't change your occupation.<br>");
				}

				break;
			case 1:
				if (CLASSMASTER_CONFIG.getClassMasterSettings().isAllowed(2)) {
					html.append("Come back here when you reached level 40 to change your class.<br>");
				} else if (CLASSMASTER_CONFIG.getClassMasterSettings().isAllowed(3)) {
					html.append("Come back after your second occupation change.<br>");
				} else {
					html.append("I can't change your occupation.<br>");
				}

				break;
			case 2:
				if (CLASSMASTER_CONFIG.getClassMasterSettings().isAllowed(3)) {
					html.append("Come back here when you reached level 76 to change your class.<br>");
				} else {
					html.append("I can't change your occupation.<br>");
				}

				break;
			case 3:
				html.append("There is no class change available for you anymore.<br>");

				break;

			}

			html.append("</body></html>");
			String content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), html.toString());

			CommunityBoardHandler.separateAndSend(content, player);
		}

		final ClassId currentClassId = player.getClassId();

		if (currentClassId.level() >= level) {
			String msg = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
					"data/html/CommunityBoard/classmaster/nomore.htm");

			CommunityBoardHandler.separateAndSend(msg, player);

			return;
		}

		final int minLevel = getMinLevel(currentClassId.level());

		if ((player.getLevel() >= minLevel) || Configuration.character().allowEntireTree()) {
			final StringBuilder menu = new StringBuilder();

			for (ClassId cid : ClassId.values()) {
				if ((cid == ClassId.inspector) && (player.getTotalSubClasses() < 2)) {
					continue;
				}

				if (validateClassId(currentClassId, cid) && (cid.level() == level)) {
					StringUtil.append(menu, "<a action=\"bypass -h _bbsclassmaster_change_class ",
							String.valueOf(cid.getId()), "\">",
							ClassListData.getInstance().getClass(cid).getClientCode(), "</a><br>");
				}
			}

			if (menu.length() > 0) {
				String msg = HtmCache.getInstance()
						.getHtm(player.getHtmlPrefix(), "data/html/CommunityBoard/classmaster/template.htm")
						.replace("%menu%", menu.toString()).replace("%req_items%", getRequiredItems(level));

				CommunityBoardHandler.separateAndSend(msg, player);

				return;
			}

			String msg = HtmCache.getInstance()
					.getHtm(player.getHtmlPrefix(), "data/html/CommunityBoard/classmaster/comebacklater.htm")
					.replace("%level%", String.valueOf(getMinLevel(level - 1)));

			CommunityBoardHandler.separateAndSend(msg, player);

			return;
		}

		if (minLevel < Integer.MAX_VALUE) {
			String msg = HtmCache.getInstance()
					.getHtm(player.getHtmlPrefix(), "data/html/CommunityBoard/classmaster/comebacklater.htm")
					.replace("%level%", String.valueOf(minLevel));

			CommunityBoardHandler.separateAndSend(msg, player);

			return;
		}

		String msg = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
				"data/html/CommunityBoard/classmaster/nomore.htm");

		CommunityBoardHandler.separateAndSend(msg, player);
	}

	private static String getRequiredItems(int level) {
		if ((CLASSMASTER_CONFIG.getClassMasterSettings().getRequireItems(level) == null)
				|| CLASSMASTER_CONFIG.getClassMasterSettings().getRequireItems(level).isEmpty()) {

			return "none";
		}

		final StringBuilder sb = new StringBuilder();

		for (ItemHolder holder : CLASSMASTER_CONFIG.getClassMasterSettings().getRequireItems(level)) {
			sb.append("<font color=\"LEVEL\">");
			sb.append(holder.getCount());
			sb.append("</font>");
			sb.append(" " + ItemTable.getInstance().getTemplate(holder.getId()).getName());
			sb.append("<br>");
		}

		return sb.toString();
	}

	private static String getRequiredNobleItems(int level) {
		if ((NOBLE_CONFIG.getNobleMasterSettings().getRequireItems(level) == null)
				|| NOBLE_CONFIG.getNobleMasterSettings().getRequireItems(level).isEmpty()) {

			return "none";
		}

		final StringBuilder sb = new StringBuilder();

		for (ItemHolder holder : NOBLE_CONFIG.getNobleMasterSettings().getRequireItems(level)) {
			sb.append("<font color=\"LEVEL\">");
			sb.append(holder.getCount());
			sb.append("</font>");
			sb.append(" " + ItemTable.getInstance().getTemplate(holder.getId()).getName());
			sb.append(" ");
		}

		return sb.toString();
	}

	private boolean checkAndChangeClass(L2PcInstance player, int val) {
		ClassId currentClassId = player.getClassId();

		if ((getMinLevel(currentClassId.level()) > player.getLevel())) {
			return false;
		}

		if (!validateClassId(currentClassId, val)) {
			return false;
		}

		int newClassIdLevel = currentClassId.level() + 1;

		// Weight/Inventory check
		if (NOBLE_CONFIG.getNobleMasterSettings().getRewardItems(newClassIdLevel) != null) {
			if (!NOBLE_CONFIG.getNobleMasterSettings().getRewardItems(newClassIdLevel).isEmpty()
					&& !player.isInventoryUnder90(false)) {
				player.sendPacket(INVENTORY_LESS_THAN_80_PERCENT);

				return false;
			}
		}

		// check if player have all required items for class transfer
		if ((NOBLE_CONFIG.getNobleMasterSettings().getRequireItems(newClassIdLevel) != null)) {
			for (ItemHolder holder : NOBLE_CONFIG.getNobleMasterSettings().getRequireItems(newClassIdLevel)) {
				if (player.getInventory().getInventoryItemCount(holder.getId(), -1) < holder.getCount()) {
					player.sendPacket(NOT_ENOUGH_ITEMS);

					return false;
				}
			}
		}

		// get all required items for class transfer
		if (NOBLE_CONFIG.getNobleMasterSettings().getRequireItems(newClassIdLevel) != null) {
			for (ItemHolder holder : NOBLE_CONFIG.getNobleMasterSettings().getRequireItems(newClassIdLevel)) {
				if (!player.destroyItemByItemId("ClassMaster", holder.getId(), holder.getCount(), player, true)) {
					return false;
				}
			}
		}

		// reward player with items
		if (NOBLE_CONFIG.getNobleMasterSettings().getRewardItems(newClassIdLevel) != null) {
			for (ItemHolder holder : NOBLE_CONFIG.getNobleMasterSettings().getRewardItems(newClassIdLevel)) {
				player.addItem("ClassMaster", holder.getId(), holder.getCount(), player, true);
			}
		}

		player.setClassId(val);

		if (player.isSubClassActive()) {
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		} else {
			player.setBaseClass(player.getActiveClass());
		}

		player.broadcastUserInfo();

		return true;
	}

	private boolean checkAndSetNoble(L2PcInstance player) {
		if (!player.isNoble()) {
			// check if player have all required items for class transfer
			if ((NOBLE_CONFIG.getNobleMasterSettings().getRequireItems(1) != null)) {
				for (ItemHolder holder : NOBLE_CONFIG.getNobleMasterSettings().getRequireItems(1)) {
					if (player.getInventory().getInventoryItemCount(holder.getId(), -1) < holder.getCount()) {
						player.sendPacket(NOT_ENOUGH_ITEMS);

						return false;
					}
				}
			}

			// get all required items for set noble
			if (NOBLE_CONFIG.getNobleMasterSettings().getRequireItems(1) != null) {
				for (ItemHolder holder : NOBLE_CONFIG.getNobleMasterSettings().getRequireItems(1)) {
					if (!player.destroyItemByItemId("ClassMaster", holder.getId(), holder.getCount(), player, true)) {
						return false;
					}
				}
			}

			// Weight/Inventory check
			if (NOBLE_CONFIG.getNobleMasterSettings().getRewardItems(1) != null) {
				if (!NOBLE_CONFIG.getNobleMasterSettings().getRewardItems(1).isEmpty()
						&& !player.isInventoryUnder90(false)) {
					player.sendPacket(INVENTORY_LESS_THAN_80_PERCENT);

					return false;
				}
			}

			// reward player with items
			if (NOBLE_CONFIG.getNobleMasterSettings().getRewardItems(1) != null) {
				for (ItemHolder holder : NOBLE_CONFIG.getNobleMasterSettings().getRewardItems(1)) {
					player.addItem("ClassMaster", holder.getId(), holder.getCount(), player, true);
				}
			}

			player.setNoble(true);
			player.sendPacket(new UserInfo(player));
			player.sendPacket(new ExBrExtraUserInfo(player));

			return true;
		} else {
			String msg = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
					"data/html/CommunityBoard/classmaster/arenoble.htm");
			CommunityBoardHandler.separateAndSend(msg, player);

			return false;
		}
	}

	private static boolean validateClassId(ClassId oldCID, int val) {
		return validateClassId(oldCID, ClassId.getClassId(val));
	}

	private static boolean validateClassId(ClassId oldCID, ClassId newCID) {
		return (newCID != null) && (newCID.getRace() != null) && ((oldCID.equals(newCID.getParent())
				|| (Configuration.character().allowEntireTree() && newCID.childOf(oldCID))));
	}

	private static int getMinLevel(int level) {
		switch (level) {
		case 0:
			return 20;
		case 1:
			return 40;
		case 2:
			return 76;
		default:
			return Integer.MAX_VALUE;
		}
	}

}
