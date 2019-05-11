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

import static com.l2jserver.gameserver.config.Config.ALLOW_ENTIRE_TREE;
import static com.l2jserver.gameserver.config.Config.CB_CLASS_MASTER;
import static com.l2jserver.gameserver.config.Config.CLASS_MASTER_SETTINGS;
import static com.l2jserver.gameserver.config.Config.NOBLE_MASTER_SETTINGS;
import static com.l2jserver.gameserver.network.SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT;
import static com.l2jserver.gameserver.network.SystemMessageId.NOT_ENOUGH_ITEMS;

import com.l2jserver.gameserver.cache.HtmCache;
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
public class MasterBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
		{
			"_bbsclassmaster",
			"_bbsclassmaster_1stClass",
			"_bbsclassmaster_2ndClass",
			"_bbsclassmaster_3rdClass",
			"_bbsclassmaster_change_class",
			"_bbsclassmaster_become_noble"
		};

	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}

	@Override
	public boolean parseCommunityBoardCommand(String command, L2PcInstance player)
	{
		if (TvTEvent.isStarted() || player.isInOlympiadMode() || player.isJailed() || (player.getKarma() > 0))
		{
			player.sendMessage("In this condition classmaster not allowed.");
			return false;
		}
		if (!CB_CLASS_MASTER)
		{
			String content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
				"data/html/CommunityBoard/classmaster/disabled.htm");
			CommunityBoardHandler.separateAndSend(content, player);
			return false;
		}
		if (command.equals("_bbsclassmaster"))
		{
			String html = HtmCache.getInstance()
				.getHtm(player.getHtmlPrefix(), "data/html/CommunityBoard/classmaster/index.htm")
				.replace("%req_noble_items%", getRequiredNobleItems(1));
			CommunityBoardHandler.separateAndSend(html, player);
		} else if (command.equals("_bbsclassmaster_1stClass"))
		{
			showHtmlMenu(player, 1);
		} else if (command.equals("_bbsclassmaster_2ndClass"))
		{
			showHtmlMenu(player, 2);
		} else if (command.equals("_bbsclassmaster_3rdClass"))
		{
			showHtmlMenu(player, 3);
		} else if (command.startsWith("_bbsclassmaster_change_class"))
		{
			int val = Integer.parseInt(command.substring(29));
			if (checkAndChangeClass(player, val))
			{
				String msg = HtmCache.getInstance()
					.getHtm(player.getHtmlPrefix(), "data/html/CommunityBoard/classmaster/ok.htm")
					.replace("%name%", ClassListData.getInstance().getClass(val).getClientCode());
				CommunityBoardHandler.separateAndSend(msg, player);
				return true;
			}
		} else if (command.startsWith("_bbsclassmaster_become_noble"))
		{
			if (checkAndSetNoble(player))
			{
				String msg = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
					"data/html/CommunityBoard/classmaster/nobleok.htm");
				CommunityBoardHandler.separateAndSend(msg, player);
				return true;
			}
		}
		return true;
	}

	private void showHtmlMenu(L2PcInstance player, int level)
	{
		if (!CLASS_MASTER_SETTINGS.isAllowed(level))
		{
			final int jobLevel = player.getClassId().level();
			final StringBuilder html = new StringBuilder();
			html.append("<html><body>");
			switch (jobLevel)
			{
			case 0:
				if (CLASS_MASTER_SETTINGS.isAllowed(1))
				{
					html.append("Come back here when you reached level 20 to change your class.<br>");
				} else if (CLASS_MASTER_SETTINGS.isAllowed(2))
				{
					html.append("Come back after your first occupation change.<br>");
				} else if (CLASS_MASTER_SETTINGS.isAllowed(3))
				{
					html.append("Come back after your second occupation change.<br>");
				} else
				{
					html.append("I can't change your occupation.<br>");
				}
				break;
			case 1:
				if (CLASS_MASTER_SETTINGS.isAllowed(2))
				{
					html.append("Come back here when you reached level 40 to change your class.<br>");
				} else if (CLASS_MASTER_SETTINGS.isAllowed(3))
				{
					html.append("Come back after your second occupation change.<br>");
				} else
				{
					html.append("I can't change your occupation.<br>");
				}
				break;
			case 2:
				if (CLASS_MASTER_SETTINGS.isAllowed(3))
				{
					html.append("Come back here when you reached level 76 to change your class.<br>");
				} else
				{
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
		if (currentClassId.level() >= level)
		{
			String msg = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
				"data/html/CommunityBoard/classmaster/nomore.htm");
			CommunityBoardHandler.separateAndSend(msg, player);
			return;
		}
		final int minLevel = getMinLevel(currentClassId.level());
		if ((player.getLevel() >= minLevel) || ALLOW_ENTIRE_TREE)
		{
			final StringBuilder menu = new StringBuilder();
			for (ClassId cid : ClassId.values())
			{
				if ((cid == ClassId.inspector) && (player.getTotalSubClasses() < 2))
				{
					continue;
				}
				if (validateClassId(currentClassId, cid) && (cid.level() == level))
				{
					StringUtil.append(menu, "<a action=\"bypass -h _bbsclassmaster_change_class ",
						String.valueOf(cid.getId()), "\">",
						ClassListData.getInstance().getClass(cid).getClientCode(), "</a><br>");
				}
			}
			if (menu.length() > 0)
			{
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
		if (minLevel < Integer.MAX_VALUE)
		{
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

	private static String getRequiredItems(int level)
	{
		if ((CLASS_MASTER_SETTINGS.getRequireItems(level) == null)
			|| CLASS_MASTER_SETTINGS.getRequireItems(level).isEmpty())
		{
			return "none";
		}
		final StringBuilder sb = new StringBuilder();
		for (ItemHolder holder : CLASS_MASTER_SETTINGS.getRequireItems(level))
		{
			sb.append("<font color=\"LEVEL\">");
			sb.append(holder.getCount());
			sb.append("</font>");
			sb.append(" " + ItemTable.getInstance().getTemplate(holder.getId()).getName());
			sb.append("<br>");
		}
		return sb.toString();
	}

	private static String getRequiredNobleItems(int level)
	{
		if ((NOBLE_MASTER_SETTINGS.getRequireItems(level) == null)
			|| NOBLE_MASTER_SETTINGS.getRequireItems(level).isEmpty())
		{
			return "none";
		}
		final StringBuilder sb = new StringBuilder();
		for (ItemHolder holder : NOBLE_MASTER_SETTINGS.getRequireItems(level))
		{
			sb.append("<font color=\"LEVEL\">");
			sb.append(holder.getCount());
			sb.append("</font>");
			sb.append(" " + ItemTable.getInstance().getTemplate(holder.getId()).getName());
			sb.append(" ");
		}
		return sb.toString();
	}

	private boolean checkAndChangeClass(L2PcInstance player, int val)
	{
		final ClassId currentClassId = player.getClassId();
		if ((getMinLevel(currentClassId.level()) > player.getLevel()) && !ALLOW_ENTIRE_TREE)
		{
			return false;
		}
		if (!validateClassId(currentClassId, val))
		{
			return false;
		}
		final int newJobLevel = currentClassId.level() + 1;
		// Weight/Inventory check
		if (CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel) != null)
		{
			if (!CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel).isEmpty() && !player.isInventoryUnder90(false))
			{
				player.sendPacket(INVENTORY_LESS_THAN_80_PERCENT);
				return false;
			}
		}
		// check if player have all required items for class transfer
		if ((CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel) != null))
		{
			for (ItemHolder holder : CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel))
			{
				if (player.getInventory().getInventoryItemCount(holder.getId(), -1) < holder.getCount())
				{
					player.sendPacket(NOT_ENOUGH_ITEMS);
					return false;
				}
			}
		}
		// get all required items for class transfer
		if (CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel) != null)
		{
			for (ItemHolder holder : CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel))
			{
				if (!player.destroyItemByItemId("ClassMaster", holder.getId(), holder.getCount(), player, true))
				{
					return false;
				}
			}
		}
		// reward player with items
		if (CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel) != null)
		{
			for (ItemHolder holder : CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel))
			{
				player.addItem("ClassMaster", holder.getId(), holder.getCount(), player, true);
			}
		}
		player.setClassId(val);
		if (player.isSubClassActive())
		{
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		} else
		{
			player.setBaseClass(player.getActiveClass());
		}
		player.broadcastUserInfo();
		return true;
	}

	private boolean checkAndSetNoble(L2PcInstance player)
	{
		if (!player.isNoble())
		{
			// check if player have all required items for class transfer
			if ((NOBLE_MASTER_SETTINGS.getRequireItems(1) != null))
			{
				for (ItemHolder holder : NOBLE_MASTER_SETTINGS.getRequireItems(1))
				{
					if (player.getInventory().getInventoryItemCount(holder.getId(), -1) < holder.getCount())
					{
						player.sendPacket(NOT_ENOUGH_ITEMS);
						return false;
					}
				}
			}
			// get all required items for set noble
			if (NOBLE_MASTER_SETTINGS.getRequireItems(1) != null)
			{
				for (ItemHolder holder : NOBLE_MASTER_SETTINGS.getRequireItems(1))
				{
					if (!player.destroyItemByItemId("ClassMaster", holder.getId(), holder.getCount(), player, true))
					{
						return false;
					}
				}
			}
			// Weight/Inventory check
			if (NOBLE_MASTER_SETTINGS.getRewardItems(1) != null)
			{
				if (!NOBLE_MASTER_SETTINGS.getRewardItems(1).isEmpty() && !player.isInventoryUnder90(false))
				{
					player.sendPacket(INVENTORY_LESS_THAN_80_PERCENT);
					return false;
				}
			}
			// reward player with items
			if (NOBLE_MASTER_SETTINGS.getRewardItems(1) != null)
			{
				for (ItemHolder holder : NOBLE_MASTER_SETTINGS.getRewardItems(1))
				{
					player.addItem("ClassMaster", holder.getId(), holder.getCount(), player, true);
				}
			}
			player.setNoble(true);
			player.sendPacket(new UserInfo(player));
			player.sendPacket(new ExBrExtraUserInfo(player));
			return true;
		} else
		{
			String msg = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
				"data/html/CommunityBoard/classmaster/arenoble.htm");
			CommunityBoardHandler.separateAndSend(msg, player);
			return false;
		}
	}

	private static boolean validateClassId(ClassId oldCID, int val)
	{
		return validateClassId(oldCID, ClassId.getClassId(val));
	}

	private static boolean validateClassId(ClassId oldCID, ClassId newCID)
	{
		return (newCID != null) && (newCID.getRace() != null)
			&& ((oldCID.equals(newCID.getParent()) || (ALLOW_ENTIRE_TREE && newCID.childOf(oldCID))));
	}

	private static int getMinLevel(int level)
	{
		switch (level)
		{
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
