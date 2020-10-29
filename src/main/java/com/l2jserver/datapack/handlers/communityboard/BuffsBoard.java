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
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.skills.BuffInfo;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.network.serverpackets.ExShowScreenMessage;

public class BuffsBoard implements IParseBoardHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(IParseBoardHandler.class);
	private static final String[] COMMANDS = {
		"_bbsbuffer"
	};
	private static final CustomBufferConfiguration BUFFER_CONFIG = Configuration.customBufferConfiguration();
	
	private static int buffTime = BUFFER_CONFIG.getBuffTime();
	private static int buffPrice = BUFFER_CONFIG.getBuffPrice();
	private static int buffFreeLevel = BUFFER_CONFIG.getBuffFreeLevel();
	private static int buffItem = BUFFER_CONFIG.getBuffItemId();
	
	private int[][] skills;
	
	@Override
	public String[] getCommunityBoardCommands() {
		return COMMANDS;
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, L2PcInstance player) {
		if (!BUFFER_CONFIG.getCommunityBuffer()) {
			CommunityBoardHandler.separateAndSend(getPath(player, "disable.html"), player);
			
			return false;
		}
		
		if (player.isDead() || player.isAlikeDead() || player.isInSiege() || player.isCastingNow() || player.isInCombat() || player.isAttackingNow() || player.isInOlympiadMode() || player.isJailed() || player.isFlying() || (player.getKarma() > 0) || player.isInDuel() || player.isInStance()
			|| player.isInCraftMode() || player.isInStoreMode()) {
			player.sendMessage("In these conditions, the buff is not allowed.");
			
			return false;
		}
		
		if (skills == null) {
			try (Connection connection = ConnectionFactory.getInstance().getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM communitybuff");
				ResultSet result = statement.executeQuery();) {
				result.next();
				
				skills = new int[result.getInt(1)][2];
				
				try (PreparedStatement table = connection.prepareStatement("SELECT * FROM communitybuff");
					ResultSet rs = table.executeQuery();) {
					for (int i = 0; i < skills.length; i++) {
						rs.next();
						
						skills[i][0] = rs.getInt(2);
						skills[i][1] = rs.getInt(3);
					}
				} catch (SQLException e) {
					LOG.error("SQL exception", e);
				}
			} catch (SQLException e) {
				LOG.error("SQL exception", e);
			}
		}
		
		String content = getPath(player, "buffer.htm").replace("%buff_price%", String.valueOf(buffPrice)).replace("%buff_level%", buffFreeLevel != 0 ? String.valueOf("to " + buffFreeLevel + " level") : "none.").replace("%buff_time%", String.valueOf(buffTime / 60));
		
		CommunityBoardHandler.separateAndSend(content, player);
		
		String[] commandParts = command.split("_");
		
		boolean pet = false;
		
		if ((commandParts.length >= 5) && (commandParts[4] != null) && commandParts[4].startsWith(" Pet")) {
			pet = true;
		}
		
		if ((commandParts.length >= 4) && (commandParts[3] != null) && commandParts[3].startsWith("FIGHERLIST")) {
			buffFighterSkills(player, pet);
			
			return true;
		}
		
		if ((commandParts.length >= 4) && (commandParts[3] != null) && commandParts[3].startsWith("DANCEFIGHTERLIST")) {
			buffFighterDS(player, pet);
			
			return true;
		}
		
		if ((commandParts.length >= 4) && (commandParts[3] != null) && commandParts[3].startsWith("MAGELIST")) {
			buffMageSkills(player, pet);
			
			return true;
		}
		
		if ((commandParts.length >= 4) && (commandParts[3] != null) && commandParts[3].startsWith("DANCEMAGELIST")) {
			buffMageDS(player, pet);
			
			return true;
		}
		
		if ((commandParts.length >= 4) && (commandParts[3] != null) && commandParts[3].startsWith("SAVE")) {
			saveBuffsSet(player, pet);
			
			return true;
		}
		
		if ((commandParts.length >= 4) && (commandParts[3] != null) && commandParts[3].startsWith("BUFF")) {
			buffSavedSet(player, pet);
			
			return true;
		}
		
		if ((commandParts.length >= 4) && (commandParts[3] != null) && commandParts[3].startsWith("CANCEL")) {
			resetAllBuffs(player, pet);
			
			return true;
		}
		
		if ((commandParts.length >= 3) && (commandParts[2] != null) && commandParts[2].startsWith("REGMP")) {
			regenMp(player, pet);
			
			return true;
		}
		
		if ((commandParts.length >= 3) && (commandParts[2] != null) && commandParts[2].startsWith("REGHP")) {
			regenHp(player, pet);
			
			return true;
		}
		
		Skill skill;
		
		for (int index = 0; index < skills.length; index++) {
			skill = findSkill(skills[index][0]);
			
			if ((commandParts.length >= 4) && commandParts[3].startsWith(skill.getName())) {
				buffSkill(player, skill, pet);
			}
		}
		
		return true;
	}
	
	private String getPath(L2PcInstance player, String page) {
		String path = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), "data/html/CommunityBoard/buffer/" + page);
		
		return path;
	}
	
	private void buffFighterSkills(L2PcInstance player, boolean pet) {
		buffSet(player, pet, 1, 3);
	}
	
	private void buffFighterDS(L2PcInstance player, boolean pet) {
		buffSet(player, pet, 4, 6);
	}
	
	private void buffMageSkills(L2PcInstance player, boolean pet) {
		buffSet(player, pet, 2, 3);
	}
	
	private void buffMageDS(L2PcInstance player, boolean pet) {
		buffSet(player, pet, 5, 6);
	}
	
	private void buffSavedSet(L2PcInstance player, boolean pet) {
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM community_skillsave WHERE charId=?;");) {
			statement.setInt(1, player.getObjectId());
			
			try (ResultSet rs = statement.executeQuery();) {
				rs.next();
				
				if (!pet) {
					if (rs.getString(2) == null)
						return;
					
					char[] allskills = rs.getString(2).toCharArray();
					
					if (allskills.length == skills.length) {
						for (int i = 0; i < allskills.length; i++) {
							if (allskills[i] == '1') {
								buffSkill(player, findSkill(skills[i][0]), pet);
							}
						}
						
					} else
						return;
				} else {
					if (rs.getString(3) == null)
						return;
					
					char petskills[] = rs.getString(3).toCharArray();
					
					if (petskills.length == skills.length) {
						for (int i = 0; i < petskills.length; i++) {
							if (petskills[i] != '1') {
								continue;
							}
							
							buffSkill(player, findSkill(skills[i][0]), pet);
						}
						
					} else
						return;
				}
			}
		} catch (SQLException e) {
			LOG.error("SQL exception", e);
		}
	}
	
	private void saveBuffsSet(L2PcInstance player, boolean pet) {
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement st = con.prepareStatement("SELECT COUNT(*) FROM community_skillsave WHERE charId=?;");) {
			st.setInt(1, player.getObjectId());
			
			try (ResultSet rs = st.executeQuery();) {
				rs.next();
				
				StringBuilder allbuff = new StringBuilder();
				
				if (!pet) {
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
					if (rs.getInt(1) == 0) {
						try (PreparedStatement statement1 = con.prepareStatement("INSERT INTO community_skillsave (charId,skills) values (?,?)");) {
							statement1.setInt(1, player.getObjectId());
							statement1.setString(2, allbuff.toString());
							statement1.execute();
						} catch (SQLException e) {
							LOG.error("SQL exception", e);
						}
					} else {
						try (PreparedStatement statement = con.prepareStatement("UPDATE community_skillsave SET skills=? WHERE charId=?;");) {
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
					if (rs.getInt(1) == 0) {
						try (PreparedStatement statement1 = con.prepareStatement("INSERT INTO community_skillsave (charId,pet) values (?,?)");) {
							statement1.setInt(1, player.getObjectId());
							statement1.setString(2, allbuff.toString());
							statement1.execute();
						} catch (SQLException e) {
							LOG.error("SQL exception", e);
						}
					} else {
						try (PreparedStatement statement = con.prepareStatement("UPDATE community_skillsave SET pet=? WHERE charId=?;");) {
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
	
	private void resetAllBuffs(L2PcInstance player, boolean pet) {
		if (!pet) {
			player.stopAllEffects();
		} else {
			player.getSummon().stopAllEffects();
		}
	}
	
	private void regenMp(L2PcInstance player, boolean pet) {
		if (!pet) {
			player.setCurrentMp(player.getMaxMp());
		} else {
			player.getSummon().setCurrentMp(player.getSummon().getMaxMp());
		}
	}
	
	private void regenHp(L2PcInstance player, boolean pet) {
		if (!pet) {
			player.setCurrentHp(player.getMaxHp());
		} else {
			player.getSummon().setCurrentHp(player.getSummon().getMaxHp());
		}
	}
	
	private void buffSet(L2PcInstance player, boolean pet, int notIndexOne, int notIndexTwo) {
		for (int index = 0; index < skills.length; index++) {
			if ((skills[index][1] != notIndexOne) && (skills[index][1] != notIndexTwo)) {
				continue;
			}
			
			buffSkill(player, findSkill(skills[index][0]), pet);
		}
	}
	
	private void buffSkill(L2PcInstance player, Skill skill, boolean pet) {
		if (pet) {
			L2Summon summon = player.getSummon();
			
			if (player.hasPet()) {
				checksAndApply(player, skill, summon);
				return;
			}
			
			player.sendPacket(new ExShowScreenMessage("You haven't pet!", 3000));
			
			return;
		}
		
		checksAndApply(player, skill, player);
	}
	
	private void checksAndApply(L2Character effector, Skill skill, L2Character effected) {
		if (effector.getLevel() <= buffFreeLevel) {
			skill.applyEffects(effector, effected, true, buffTime);
			return;
		}
		
		if (effector.destroyItemByItemId(null, buffItem, buffPrice, effector, true)) {
			skill.applyEffects(effector, effected, true, buffTime);
		} else {
			effector.sendPacket(new ExShowScreenMessage("Sorry, not enough adena!", 3000));
		}
	}
	
	private Skill findSkill(int id) {
		int maxlevel = SkillData.getInstance().getMaxLevel(id);
		
		return SkillData.getInstance().getSkill(id, maxlevel);
	}
	
}