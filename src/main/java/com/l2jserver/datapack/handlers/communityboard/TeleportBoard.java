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

import static com.l2jserver.gameserver.config.Config.FREE_BUFFS_TP_TO_LEVEL;

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
import com.l2jserver.gameserver.handler.CommunityBoardHandler;
import com.l2jserver.gameserver.handler.IParseBoardHandler;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

public class TeleportBoard implements IParseBoardHandler {

    private static final String[] COMMANDS = { "_bbsteleport" };

    @Override
    public String[] getCommunityBoardCommands() {
	return COMMANDS;
    }

    public class Teleport {
	public String locationName = ""; // Location name
	public int locationId = 0; // Teport location ID
	public int locCoordsX = 0; // Location coords X
	public int locCoordsY = 0; // Location coords Y
	public int locCoordsZ = 0; // Location coords Z
	public int playerId = 0; // charID
    }

    @Override
    public boolean parseCommunityBoardCommand(String command, L2PcInstance activeChar) {
	if (activeChar.isDead() || activeChar.isAlikeDead() || activeChar.isInSiege() || activeChar.isCastingNow()
		|| activeChar.isInCombat() || activeChar.isAttackingNow() || activeChar.isInOlympiadMode()
		|| activeChar.isJailed() || activeChar.isFlying() || (activeChar.getKarma() > 0)
		|| activeChar.isInDuel()) {
	    activeChar.sendMessage("Under these conditions, teleportation is not allowed.");

	    return false;
	}

	if (command.equals("_bbsteleport")) {
	    showStartPage(activeChar);
	} else if (command.startsWith("_bbsteleport ")) {
	    final String path = command.replace("_bbsteleport ", "");
	    if ((path.length() > 0) && path.endsWith(".html") || path.endsWith(".htm")) {
		final String html = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(),
			"data/html/CommunityBoard/teleports/" + path);

		CommunityBoardHandler.separateAndSend(html, activeChar);
	    }
	} else if (command.startsWith("_bbsteleport;delete;")) {
	    StringTokenizer stDell = new StringTokenizer(command, ";");
	    stDell.nextToken();
	    stDell.nextToken();

	    int TpNameDell = Integer.parseInt(stDell.nextToken());
	    deletePoint(activeChar, TpNameDell);

	    showStartPage(activeChar);
	} else if (command.startsWith("_bbsteleport;save;")) {
	    StringTokenizer stAdd = new StringTokenizer(command, ";");
	    stAdd.nextToken();
	    stAdd.nextToken();

	    if (!stAdd.hasMoreTokens()) {
		activeChar.sendMessage("Please, enter the name of point!");
	    } else {
		String TpNameAdd = stAdd.nextToken();
		savePoint(activeChar, TpNameAdd);

		showStartPage(activeChar);
	    }
	} else if (command.startsWith("_bbsteleport;teleport;")) {
	    StringTokenizer stGoTp = new StringTokenizer(command, " ");
	    stGoTp.nextToken();

	    int xTp = Integer.parseInt(stGoTp.nextToken());
	    int yTp = Integer.parseInt(stGoTp.nextToken());
	    int zTp = Integer.parseInt(stGoTp.nextToken());
	    int priceTp = Integer.parseInt(stGoTp.nextToken());

	    teleportToPoint(activeChar, xTp, yTp, zTp, priceTp);
	}

	return true;
    }

    private void showStartPage(L2PcInstance activeChar) {
	String content = HtmCache.getInstance()
		.getHtm(activeChar.getHtmlPrefix(), "data/html/CommunityBoard/teleports/index.html")
		.replaceAll("%tp%", showSavedPoints(activeChar).toString());

	CommunityBoardHandler.separateAndSend(content, activeChar);
    }

    private void teleportToPoint(L2PcInstance activeChar, int xTp, int yTp, int zTp, int priceTp) {
	if (activeChar.getLevel() <= FREE_BUFFS_TP_TO_LEVEL) {
	    activeChar.teleToLocation(xTp, yTp, zTp);
	    return;
	}

	if ((priceTp > 0) && (activeChar.getAdena() < priceTp)) {
	    activeChar.sendMessage("Not enough Adena.");
	    return;
	} else {
	    if (priceTp > 0) {
		activeChar.reduceAdena("Teleport", priceTp, activeChar, true);
	    }

	    activeChar.teleToLocation(xTp, yTp, zTp);
	}
    }

    private String showSavedPoints(L2PcInstance activeChar) {
	Teleport tp;
	StringBuilder html = new StringBuilder();

	try (Connection con = ConnectionFactory.getInstance().getConnection();
		PreparedStatement st = con.prepareStatement("SELECT * FROM comteleport WHERE charId=?;");) {
	    st.setLong(1, activeChar.getObjectId());

	    ResultSet rs = st.executeQuery();

	    html.append("<table width=200");
	    while (rs.next()) {
		tp = new Teleport();
		tp.locationId = rs.getInt("locationId");
		tp.locationName = rs.getString("name");
		tp.playerId = rs.getInt("charId");
		tp.locCoordsX = rs.getInt("xPos");
		tp.locCoordsY = rs.getInt("yPos");
		tp.locCoordsZ = rs.getInt("zPos");

		html.append("<tr>");
		html.append("<td align=center>");
		html.append("<button value=\"" + tp.locationName + "\" action=\"bypass -h _bbsteleport;teleport; "
			+ tp.locCoordsX + " " + tp.locCoordsY + " " + tp.locCoordsZ + " " + 100000
			+ "\" width=130 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		html.append("</td>");
		html.append("<td align=center>");
		html.append("<button value=\"Delete\" action=\"bypass -h _bbsteleport;delete;" + tp.locationId
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

    private void deletePoint(L2PcInstance activeChar, int TpNameDell) {
	try (Connection connection = ConnectionFactory.getInstance().getConnection();
		PreparedStatement statement = connection
			.prepareStatement("DELETE FROM comteleport WHERE charId=? AND locationId=?;");) {
	    statement.setInt(1, activeChar.getObjectId());
	    statement.setInt(2, TpNameDell);
	    statement.execute();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private void savePoint(L2PcInstance activeChar, String TpNameAdd) {
	try (Connection connection = ConnectionFactory.getInstance().getConnection();
		PreparedStatement statement = connection
			.prepareStatement("SELECT COUNT(*) FROM comteleport WHERE charId=?;");) {
	    statement.setLong(1, activeChar.getObjectId());
	    ResultSet rs = statement.executeQuery();
	    rs.next();
	    if (rs.getInt(1) <= 9) {
		try (PreparedStatement st1 = connection
			.prepareStatement("SELECT COUNT(*) FROM comteleport WHERE charId=? AND name=?;");) {
		    st1.setLong(1, activeChar.getObjectId());
		    st1.setString(2, TpNameAdd);
		    ResultSet rs1 = st1.executeQuery();
		    rs1.next();
		    if (rs1.getInt(1) == 0) {
			try (PreparedStatement stAdd = connection.prepareStatement(
				"INSERT INTO comteleport (charId,xPos,yPos,zPos,name) VALUES(?,?,?,?,?)");) {
			    stAdd.setInt(1, activeChar.getObjectId());
			    stAdd.setInt(2, activeChar.getX());
			    stAdd.setInt(3, activeChar.getY());
			    stAdd.setInt(4, activeChar.getZ());
			    stAdd.setString(5, TpNameAdd);
			    stAdd.execute();
			}
		    } else {
			try (PreparedStatement stAdd = connection.prepareStatement(
				"UPDATE comteleport SET xPos=?, yPos=?, zPos=? WHERE charId=? AND name=?;");) {
			    stAdd.setInt(1, activeChar.getObjectId());
			    stAdd.setInt(2, activeChar.getX());
			    stAdd.setInt(3, activeChar.getY());
			    stAdd.setInt(4, activeChar.getZ());
			    stAdd.setString(5, TpNameAdd);
			    stAdd.execute();
			}
		    }
		}
	    } else {
		activeChar.sendMessage("You can't save more than 10 bookmarks!");
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
