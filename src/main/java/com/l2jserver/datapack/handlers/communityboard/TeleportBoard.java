/*
 * Copyright (C) 2004-2018 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.datapack.handlers.communityboard;

/**
 * @author Andrii Hromov
 *
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

//import javolution.text.TextBuilder;
import com.l2jserver.commons.database.ConnectionFactory;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.config.Configuration;
import com.l2jserver.gameserver.config.CustomTeleportConfiguration;
import com.l2jserver.gameserver.handler.CommunityBoardHandler;
import com.l2jserver.gameserver.handler.IParseBoardHandler;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.zone.ZoneId;

public class TeleportBoard implements IParseBoardHandler {

	private static final String[] COMMANDS = { "_bbsteleport" };
	private static final CustomTeleportConfiguration TELEPORT_CONFIG = Configuration.customTeleportConfiguration();

	@Override
	public String[] getCommunityBoardCommands() {
		return COMMANDS;
	}

	public class Teleport {
		public String locName = ""; // Location name
		public int locId = 0; // Teport location ID
		public int locCoordsX = 0; // Location coords X
		public int locCoordsY = 0; // Location coords Y
		public int locCoordsZ = 0; // Location coords Z
		public int playerId = 0; // charID
	}

	@Override
	public boolean parseCommunityBoardCommand(String command, L2PcInstance player) {
		if (!TELEPORT_CONFIG.getCommunityTeleport()) {
			String content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
					"data/html/CommunityBoard/teleports/disable.html");

			CommunityBoardHandler.separateAndSend(content, player);

			return false;
		}

		if (player.isDead() || player.isAlikeDead() || player.isInSiege() || player.isCastingNow()
				|| player.isInCombat() || player.isAttackingNow() || player.isInOlympiadMode() || player.isJailed()
				|| player.isFlying() || (player.getKarma() > 0) || player.isInDuel()
				|| player.isInStance() | player.isInCraftMode() || player.isInStoreMode()) {
			player.sendMessage("Under these conditions, teleportation is not allowed.");

			return false;
		}

		if (command.equals("_bbsteleport")) {
			showStartPage(player);
		} else if (command.startsWith("_bbsteleport ")) {
			final String path = command.replace("_bbsteleport ", "");

			if ((path.length() > 0) && path.endsWith(".html") || path.endsWith(".htm")) {
				final String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(),
						"data/html/CommunityBoard/teleports/" + path);

				CommunityBoardHandler.separateAndSend(html, player);
			}
		} else if (command.startsWith("_bbsteleport;delete;")) {
			StringTokenizer st = new StringTokenizer(command, ";");

			st.nextToken();
			st.nextToken();

			int locId = Integer.parseInt(st.nextToken());

			deletePoint(player, locId);

			showStartPage(player);
		} else if (command.startsWith("_bbsteleport;save;")) {
			StringTokenizer st = new StringTokenizer(command, ";");

			st.nextToken();
			st.nextToken();

			if (!st.hasMoreTokens()) {
				player.sendMessage("Please, enter the name of point!");
			} else {
				String tpPointName = st.nextToken();
				savePoint(player, tpPointName);

				showStartPage(player);
			}
		} else if (command.startsWith("_bbsteleport;teleport;")) {
			StringTokenizer st = new StringTokenizer(command, " ");

			st.nextToken();

			int tpX = Integer.parseInt(st.nextToken());

			int tpY = Integer.parseInt(st.nextToken());

			int tpZ = Integer.parseInt(st.nextToken());

			int tpPrice = TELEPORT_CONFIG.getTeleportPrice();

			teleport(player, tpX, tpY, tpZ, tpPrice);
		}

		return true;
	}

	private void showStartPage(L2PcInstance player) {
		String content = HtmCache.getInstance()
				.getHtm(player.getHtmlPrefix(), "data/html/CommunityBoard/teleports/index.html")
				.replaceAll("%tp%", showSavedPoints(player))
				.replace("%tp_price%", String.valueOf(TELEPORT_CONFIG.getTeleportPrice()))
				.replace("%tpPoint_price%", String.valueOf(TELEPORT_CONFIG.getTeleportToSavedPointPrice()))
				.replace("%tp_free%",
						TELEPORT_CONFIG.getFreeTeleportLevel() != 0
								? String.valueOf("to " + TELEPORT_CONFIG.getFreeTeleportLevel() + " level")
								: "none.");

		CommunityBoardHandler.separateAndSend(content, player);
	}

	private void teleport(L2PcInstance player, int tpX, int tpY, int tpZ, int tpPrice) {
		int price = TELEPORT_CONFIG.getTeleportToSavedPointPrice();

		if (player.getLevel() <= TELEPORT_CONFIG.getFreeTeleportLevel()) {
			player.teleToLocation(tpX, tpY, tpZ);

			return;
		}

		if ((price > 0) && (player.getAdena() < price)) {
			player.sendMessage("Not enough Adena.");

			return;
		} else {
			if (price > 0) {
				player.reduceAdena("Teleport", price, player, true);
			}

			player.teleToLocation(tpX, tpY, tpZ);
		}
	}

	private String showSavedPoints(L2PcInstance player) {
		Teleport tp;
		StringBuilder html = new StringBuilder();

		try (Connection con = ConnectionFactory.getInstance().getConnection();
				PreparedStatement st = con.prepareStatement("SELECT * FROM comteleport WHERE charId=?;");) {
			st.setLong(1, player.getObjectId());

			ResultSet rs = st.executeQuery();

			html.append("<table width=200");
			while (rs.next()) {
				tp = new Teleport();
				tp.locId = rs.getInt("TpId");
				tp.playerId = rs.getInt("charId");
				tp.locCoordsX = rs.getInt("Xpos");
				tp.locCoordsY = rs.getInt("Ypos");
				tp.locCoordsZ = rs.getInt("Zpos");
				tp.locName = rs.getString("name");

				html.append("<tr>");
				html.append("<td align=center>");
				html.append("<button value=\"" + tp.locName + "\" action=\"bypass -h _bbsteleport;teleport; "
						+ tp.locCoordsX + " " + tp.locCoordsY + " " + tp.locCoordsZ + " "
						+ TELEPORT_CONFIG.getTeleportPrice()
						+ "\" width=130 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
				html.append("</td>");
				html.append("<td align=center>");
				html.append("<button value=\"Delete\" action=\"bypass -h _bbsteleport;delete;" + tp.locId
						+ "\" width=50 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
				html.append("</td>");
				html.append("</tr>");
			}

			html.append("</table>");
			html.append("<br>");
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return html.toString();
	}

	private void deletePoint(L2PcInstance player, int locId) {
		try (Connection connection = ConnectionFactory.getInstance().getConnection();
				PreparedStatement statement = connection
						.prepareStatement("DELETE FROM comteleport WHERE charId=? AND TpId=?");) {
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, locId);
			statement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void savePoint(L2PcInstance player, String tpName) {
		try (Connection connection = ConnectionFactory.getInstance().getConnection();
				PreparedStatement preparCount = connection
						.prepareStatement("SELECT COUNT(*) FROM comteleport WHERE charId=?")) {
			preparCount.setLong(1, player.getObjectId());

			ResultSet rs = preparCount.executeQuery();
			rs.next();

			if (rs.getInt(1) <= 9) {
				try (PreparedStatement st1 = connection
						.prepareStatement("SELECT COUNT(*) FROM comteleport WHERE charId=? AND name=?;");) {
					st1.setLong(1, player.getObjectId());
					st1.setString(2, tpName);
					ResultSet rs1 = st1.executeQuery();
					rs1.next();
					if (rs1.getInt(1) == 0) {
						try (PreparedStatement stAdd = connection.prepareStatement(
								"INSERT INTO comteleport (charId,xPos,yPos,zPos,name) VALUES(?,?,?,?,?)");) {
							stAdd.setInt(1, player.getObjectId());
							stAdd.setInt(2, player.getX());
							stAdd.setInt(3, player.getY());
							stAdd.setInt(4, player.getZ());
							stAdd.setString(5, tpName);
							stAdd.execute();
						}
					} else {
						try (PreparedStatement stAdd = connection.prepareStatement(
								"UPDATE comteleport SET xPos=?, yPos=?, zPos=? WHERE charId=? AND name=?;");) {
							stAdd.setInt(2, player.getX());
							stAdd.setInt(3, player.getY());
							stAdd.setInt(4, player.getZ());
							stAdd.setInt(1, player.getObjectId());
							stAdd.setString(5, tpName);
							stAdd.execute();
						}
					}
				}
			}

			else
				player.sendMessage("You can't save more than 10 teleportation points!");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
