/*
 * Copyright (C) 2004-2020 L2J Server
 * This file is part of L2J Server.
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.commons.database.ConnectionFactory;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.config.Configuration;
import com.l2jserver.gameserver.config.CustomBufferConfiguration;
import com.l2jserver.gameserver.datatables.SkillData;
import com.l2jserver.gameserver.handler.CommunityBoardHandler;
import com.l2jserver.gameserver.handler.IParseBoardHandler;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.skills.BuffInfo;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.network.serverpackets.ExShowScreenMessage;

public class BuffsBoard implements IParseBoardHandler {

	private static final Logger LOG = LoggerFactory.getLogger(IParseBoardHandler.class);
	private static final String[] COMMANDS = { "_bbsbuffer" };
	private static final CustomBufferConfiguration BUFFER_CONFIG = Configuration.customBufferConfiguration();
	
	private int[][] skills;
	private Skill skill;
	private int skillMaxlevel;

	@Override
	public String[] getCommunityBoardCommands() {
		return COMMANDS;
	}

	@Override
	public boolean parseCommunityBoardCommand(String command, L2PcInstance player) {
		if (player.isDead() || player.isAlikeDead() || player.isInSiege() || player.isCastingNow()
				|| player.isInCombat() || player.isAttackingNow() || player.isInOlympiadMode() || player.isJailed()
				|| player.isFlying() || (player.getKarma() > 0) || player.isInDuel() || player.isInOlympiadMode()
				|| player.isInStance()) {
			player.sendMessage("In these conditions, the buff is not allowed.");
			return false;
		}

		if (skills == null) {
			try (Connection connection = ConnectionFactory.getInstance().getConnection();
					PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM communitybuff");
					ResultSet result = statement.executeQuery();) {
				result.next();
				skills = new int[result.getInt(1)][4];

				try (PreparedStatement table = connection.prepareStatement("SELECT * FROM communitybuff");
						ResultSet rs = table.executeQuery();) {
					for (int i = 0; i < skills.length; i++) {
						rs.next();
						skills[i][0] = rs.getInt(2);
						skills[i][1] = rs.getInt(3);
						skills[i][2] = rs.getInt(4);
						skills[i][3] = rs.getInt(5);
					}
				} catch (SQLException e) {
					LOG.error("SQL exception", e);
				}
			} catch (SQLException e) {
				LOG.error("SQL exception", e);
			}
		}

		String content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), "data/html/CommunityBoard/buffer.html");
		CommunityBoardHandler.separateAndSend(content, player);

		String[] commandParts = command.split("_");
		boolean petbuff = false;

//	if ((commandParts.length >= 5) && (commandParts[4] != null) && commandParts[4].startsWith(" Player")) {
//	    petbuff = false;
//	}

		if ((commandParts.length >= 5) && (commandParts[4] != null) && commandParts[4].startsWith(" Pet")) {
			petbuff = true;
		}

		if ((commandParts.length >= 4) && (commandParts[3] != null) && commandParts[3].startsWith("FIGHERLIST")) {
			buffFighterSet(player, petbuff);
			return true;
		}

		if ((commandParts.length >= 4) && (commandParts[3] != null) && commandParts[3].startsWith("DANCEFIGHTERLIST")) {
			buffDSFighterSet(player, petbuff);
			return true;
		}

		if ((commandParts.length >= 4) && (commandParts[3] != null) && commandParts[3].startsWith("MAGELIST")) {
			buffMageSet(player, petbuff);
			return true;
		}

		if ((commandParts.length >= 4) && (commandParts[3] != null) && commandParts[3].startsWith("DANCEMAGELIST")) {
			buffDSMageSet(player, petbuff);
			return true;
		}

		if ((commandParts.length >= 4) && (commandParts[3] != null) && commandParts[3].startsWith("SAVE")) {
			saveBuffsSet(player, petbuff);
			return true;
		}

		if ((commandParts.length >= 4) && (commandParts[3] != null) && commandParts[3].startsWith("BUFF")) {
			buffSavedSet(player, petbuff);
			return true;
		}

		if ((commandParts.length >= 4) && (commandParts[3] != null) && commandParts[3].startsWith("CANCEL")) {
			resetAllBuffs(player, petbuff);
			return true;
		}

		if ((commandParts.length >= 3) && (commandParts[2] != null) && commandParts[2].startsWith("REGMP")) {
			regenMp(player, petbuff);
			return true;
		}

		if ((commandParts.length >= 3) && (commandParts[2] != null) && commandParts[2].startsWith("REGHP")) {
			regenHp(player, petbuff);
			return true;
		}

		for (int index = 0; index < skills.length; index++) {
			skillMaxlevel = SkillData.getInstance().getMaxLevel(skills[index][0]);
			skill = SkillData.getInstance().getSkill(skills[index][0], skillMaxlevel);

			if ((commandParts.length >= 4) && commandParts[3].startsWith(skill.getName())) {
				paidAndBuffSkill(player, petbuff, index, skill);
				return true;
			}
		}

		return false;
	}

	private void paidAndBuffSkill(L2PcInstance player, boolean petbuff, int index, Skill skill) {
		if (player.getLevel() <= BUFFER_CONFIG.paidFreeLevel()) {
			applyOnCheckedTarget(player, petbuff, skill);
			return;
		}

		if (player.destroyItemByItemId(null, skills[index][3], skills[index][2], player, true)) {
			applyOnCheckedTarget(player, petbuff, skill);
		} else {
			showHaventAdena(player);
		}
	}

	private void buffFighterSet(L2PcInstance player, boolean petbuff) {
		for (int i = 0; i < skills.length; i++) {
			if ((skills[i][1] != 1) && (skills[i][1] != 3)) {
				continue;
			}
			paidAndBuffSet(player, petbuff, i);
		}
	}

	private void buffDSFighterSet(L2PcInstance player, boolean petbuff) {
		for (int i = 0; i < skills.length; i++) {
			if ((skills[i][1] != 4) && (skills[i][1] != 6)) {
				continue;
			}
			paidAndBuffSet(player, petbuff, i);
		}
	}

	private void buffMageSet(L2PcInstance player, boolean petbuff) {
		for (int i = 0; i < skills.length; i++) {
			if ((skills[i][1] != 2) && (skills[i][1] != 3)) {
				continue;
			}
			paidAndBuffSet(player, petbuff, i);
		}
	}

	private void buffDSMageSet(L2PcInstance player, boolean petbuff) {
		for (int i = 0; i < skills.length; i++) {
			if ((skills[i][1] != 5) && (skills[i][1] != 6)) {
				continue;
			}
			paidAndBuffSet(player, petbuff, i);
		}
	}

	private void paidAndBuffSet(L2PcInstance palyer, boolean petbuff, int key) {
		if (palyer.getLevel() <= BUFFER_CONFIG.paidFreeLevel()) {
			skillMaxlevel = SkillData.getInstance().getMaxLevel(skills[key][0]);
			skill = SkillData.getInstance().getSkill(skills[key][0], skillMaxlevel);
			applyOnCheckedTarget(palyer, petbuff, skill);
			return;
		}
		
		if (palyer.destroyItemByItemId(null, skills[key][3], skills[key][2], palyer, true)) {
			skillMaxlevel = SkillData.getInstance().getMaxLevel(skills[key][0]);
			skill = SkillData.getInstance().getSkill(skills[key][0], skillMaxlevel);
			applyOnCheckedTarget(palyer, petbuff, skill);
		} else {
			showHaventAdena(palyer);
		}
	}

	private void applyOnCheckedTarget(L2PcInstance player, boolean petbuff, Skill skill) {
		if (!petbuff) {
			skill.getTargetList(player, true, player);
			if (skill != null) {
				skill.applyEffects(player, player);
			}
		} else {
			skill.getTargetList(player.getSummon(), true, player.getSummon());
			if (skill != null) {
				skill.applyEffects(player.getSummon(), player.getSummon());
			}
		}
	}

	private void buffSavedSet(L2PcInstance player, boolean petbuff) {
		try (Connection con = ConnectionFactory.getInstance().getConnection();
				PreparedStatement statement = con
						.prepareStatement("SELECT * FROM community_skillsave WHERE charId=?;");) {
			statement.setInt(1, player.getObjectId());
			try (ResultSet rcln = statement.executeQuery();) {
				rcln.next();
				if (!petbuff) {
					char[] allskills = rcln.getString(2).toCharArray();
					if (allskills.length == skills.length) {
						for (int i = 0; i < skills.length; i++) {
							if (allskills[i] == '1') {
								if (player.destroyItemByItemId(null, skills[i][3], skills[i][2], player, true)) {
									skillMaxlevel = SkillData.getInstance().getMaxLevel(skills[i][0]);
									skill = SkillData.getInstance().getSkill(skills[i][0], skillMaxlevel);
									skill.getTargetList(player, true, player);
									if (skill != null) {
										skill.applyEffects(player, player);
									}
								} else {
									showHaventAdena(player);
								}

							}
						}

					}
				} else {
					char petskills[] = rcln.getString(3).toCharArray();
					if (petskills.length == skills.length) {
						for (int i = 0; i < skills.length; i++) {
							if (petskills[i] != '1') {
								continue;
							}
							if (player.destroyItemByItemId(null, skills[i][3], skills[i][2], player, true)) {
								skillMaxlevel = SkillData.getInstance().getMaxLevel(skills[i][0]);
								skill = SkillData.getInstance().getSkill(skills[i][0], skillMaxlevel);
								skill.getTargetList(player.getSummon(), true, player.getSummon());
								if (skill != null) {
									skill.applyEffects(player, player.getSummon());
								}
							} else {
								showHaventAdena(player);
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			LOG.error("SQL exception", e);
		}
	}

	private void saveBuffsSet(L2PcInstance player, boolean petbuff) {
		try (Connection con = ConnectionFactory.getInstance().getConnection();
				PreparedStatement stat = con
						.prepareStatement("SELECT COUNT(*) FROM community_skillsave WHERE charId=?;");) {
			stat.setInt(1, player.getObjectId());
			try (ResultSet rset = stat.executeQuery();) {
				rset.next();
				StringBuilder allbuff = new StringBuilder();
				if (!petbuff) {
					List<BuffInfo> skill = player.getEffectList().getEffects();
					boolean flag = true;
					for (int i = 0; i < skills.length; i++) {
						for (int j = 0; j < skill.size(); j++) {
							if (skills[i][0] == skill.get(j).getSkill().getId()) {
								allbuff.append(1);
								flag = false;
							}
							if ((j == (skill.size() - 1)) && flag) {
								allbuff.append(0);
							}
						}
						flag = true;
					}
					if (rset.getInt(1) == 0) {
						try (PreparedStatement statement1 = con
								.prepareStatement("INSERT INTO community_skillsave (charId,skills) values (?,?)");) {
							statement1.setInt(1, player.getObjectId());
							statement1.setString(2, allbuff.toString());
							statement1.execute();
						} catch (SQLException e) {
							LOG.error("SQL exception", e);
						}
					} else {
						try (PreparedStatement statement = con
								.prepareStatement("UPDATE community_skillsave SET skills=? WHERE charId=?;");) {
							statement.setString(1, allbuff.toString());
							statement.setInt(2, player.getObjectId());
							statement.execute();
						} catch (SQLException e) {
							LOG.error("SQL exception", e);
						}
					}
				} else {
					List<BuffInfo> skill = player.getSummon().getEffectList().getEffects();
					boolean flag = true;
					for (int i = 0; i < skills.length; i++) {
						for (int j = 0; j < skill.size(); j++) {
							if (skills[i][0] == skill.get(j).getSkill().getId()) {
								allbuff = allbuff.append(1);
								flag = false;
							}
							if ((j == (skill.size() - 1)) && flag) {
								allbuff.append(0);
							}
						}
						flag = true;
					}
					if (rset.getInt(1) == 0) {
						try (PreparedStatement statement1 = con
								.prepareStatement("INSERT INTO community_skillsave (charId,pet) values (?,?)");) {
							statement1.setInt(1, player.getObjectId());
							statement1.setString(2, allbuff.toString());
							statement1.execute();
						} catch (SQLException e) {
							LOG.error("SQL exception", e);
						}
					} else {
						try (PreparedStatement statement = con
								.prepareStatement("UPDATE community_skillsave SET pet=? WHERE charId=?;");) {
							statement.setString(1, allbuff.toString());
							statement.setInt(2, player.getObjectId());
							statement.execute();
						} catch (SQLException e) {
							LOG.error("SQL exception", e);
						}
					}
				}
			}
		} catch (SQLException e) {
			LOG.error("SQL exception", e);
		}

	}

	private void showHaventAdena(L2PcInstance player) {
		player.sendPacket(new ExShowScreenMessage("Sorry, not enough adena!", 3000));
	}

	private void resetAllBuffs(L2PcInstance player, boolean petbuff) {
		if (!petbuff) {
			player.stopAllEffects();
		} else {
			player.getSummon().stopAllEffects();
		}
	}

	private void regenMp(L2PcInstance player, boolean petbuff) {
		if (!petbuff) {
			player.setCurrentMp(player.getMaxMp());
		} else {
			player.getSummon().setCurrentMp(player.getSummon().getMaxMp());
		}
	}

	private void regenHp(L2PcInstance player, boolean petbuff) {
		if (!petbuff) {
			player.setCurrentHp(player.getMaxHp());
		} else {
			player.getSummon().setCurrentHp(player.getSummon().getMaxHp());
		}
	}

}
