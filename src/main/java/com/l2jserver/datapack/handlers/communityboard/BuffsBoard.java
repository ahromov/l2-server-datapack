/*
 * Copyright (C) 2004-2018 L2J Server
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
import com.l2jserver.gameserver.config.Config;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.datatables.SkillData;
import com.l2jserver.gameserver.handler.CommunityBoardHandler;
import com.l2jserver.gameserver.handler.IParseBoardHandler;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.TvTEvent;
import com.l2jserver.gameserver.model.skills.BuffInfo;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.network.serverpackets.ExShowScreenMessage;

public class BuffsBoard implements IParseBoardHandler
{

	private static final Logger LOG = LoggerFactory.getLogger(IParseBoardHandler.class);
	private static final String[] COMMANDS =
		{
			"_bbsbuffer"
		};
	private int[][] skillstable;
	private Skill skill;
	private int skilllevel;

	@Override
	public String[] getCommunityBoardCommands()
	{

		return COMMANDS;

	}

	@Override
	public boolean parseCommunityBoardCommand(String command, L2PcInstance activeChar)
	{
		if (activeChar.isDead() || activeChar.isAlikeDead() || TvTEvent.isStarted() || activeChar.isInSiege()
			|| activeChar.isCastingNow() || activeChar.isInCombat() || activeChar.isAttackingNow()
			|| activeChar.isInOlympiadMode() || activeChar.isJailed() || activeChar.isFlying()
			|| (activeChar.getKarma() > 0) || activeChar.isInDuel())
		{
			activeChar.sendMessage("In these conditions, the buff is not allowed.");
			return false;
		}

		if (skillstable == null)
		{
			try (Connection connection = ConnectionFactory.getInstance().getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM communitybuff");
				ResultSet result = statement.executeQuery();)
			{
				result.next();
				skillstable = new int[result.getInt(1)][4];

				try (PreparedStatement table = connection.prepareStatement("SELECT * FROM communitybuff");
					ResultSet rs = table.executeQuery();)
				{
					for (int i = 0; i < skillstable.length; i++)
					{
						rs.next();
						skillstable[i][0] = rs.getInt(2);
						skillstable[i][1] = rs.getInt(3);
						skillstable[i][2] = rs.getInt(4);
						skillstable[i][3] = rs.getInt(5);
					}
				} catch (SQLException e)
				{
					LOG.error("SQL exception", e);
				}
			} catch (SQLException e)
			{
				LOG.error("SQL exception", e);
			}
		}

		String content = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(),
			"data/html/CommunityBoard/buffer.html");
		CommunityBoardHandler.separateAndSend(content, activeChar);
		String[] parts = command.split("_");

		boolean petbuff = false;

		if ((parts.length >= 5) && (parts[4] != null) && parts[4].startsWith(" Player"))
		{
			petbuff = false;
		}

		if ((parts.length >= 5) && (parts[4] != null) && parts[4].startsWith(" Pet"))
		{
			petbuff = true;
		}

		if ((parts.length >= 4) && (parts[3] != null) && parts[3].startsWith("FIGHERLIST"))
		{
			buffFighterSet(activeChar, petbuff);
			return true;
		}

		if ((parts.length >= 4) && (parts[3] != null) && parts[3].startsWith("DANCEFIGHTERLIST"))
		{
			buffDSFighterSet(activeChar, petbuff);
			return true;
		}

		if ((parts.length >= 4) && (parts[3] != null) && parts[3].startsWith("MAGELIST"))
		{
			buffMageSet(activeChar, petbuff);
			return true;
		}

		if ((parts.length >= 4) && (parts[3] != null) && parts[3].startsWith("DANCEMAGELIST"))
		{
			buffDSMageSet(activeChar, petbuff);
			return true;
		}

		if ((parts.length >= 4) && (parts[3] != null) && parts[3].startsWith("SAVE"))
		{
			saveBuffsSet(activeChar, petbuff);
			return true;
		}

		if ((parts.length >= 4) && (parts[3] != null) && parts[3].startsWith("BUFF"))
		{
			buffSavedSet(activeChar, petbuff);
			return true;
		}

		if ((parts.length >= 4) && (parts[3] != null) && parts[3].startsWith("CANCEL"))
		{
			resetAllBuffs(activeChar, petbuff);
			return true;
		}

		if ((parts.length >= 3) && (parts[2] != null) && parts[2].startsWith("REGMP"))
		{
			regenMp(activeChar, petbuff);
			return true;
		}

		if ((parts.length >= 3) && (parts[2] != null) && parts[2].startsWith("REGHP"))
		{
			regenHp(activeChar, petbuff);
			return true;
		}

		for (int key = 0; key < skillstable.length; key++)
		{
			skilllevel = SkillData.getInstance().getMaxLevel(skillstable[key][0]);
			skill = SkillData.getInstance().getSkill(skillstable[key][0], skilllevel);
			if ((parts.length >= 4) && parts[3].startsWith(skill.getName()))
			{
				paidAndBuffSkill(activeChar, petbuff, key, skill);
				return true;
			}
		}

		return false;
	}

	private void paidAndBuffSkill(L2PcInstance activeChar, boolean petbuff, int key, Skill skill)
	{
		if (activeChar.getLevel() <= Config.FREE_BUFFS_TP_TO_LEVEL)
		{
			applyOnCheckedTarget(activeChar, petbuff, skill);
			return;
		}

		if (activeChar.destroyItemByItemId(null, skillstable[key][3], skillstable[key][2], activeChar, true))
		{
			applyOnCheckedTarget(activeChar, petbuff, skill);
		} else
		{
			showHaventAdena(activeChar);
		}
	}

	private void buffFighterSet(L2PcInstance activeChar, boolean petbuff)
	{
		for (int i = 0; i < skillstable.length; i++)
		{
			if ((skillstable[i][1] != 1) && (skillstable[i][1] != 3))
			{
				continue;
			}
			paidAndBuffSet(activeChar, petbuff, i);
		}
	}

	private void buffDSFighterSet(L2PcInstance activeChar, boolean petbuff)
	{
		for (int i = 0; i < skillstable.length; i++)
		{
			if ((skillstable[i][1] != 4) && (skillstable[i][1] != 6))
			{
				continue;
			}
			paidAndBuffSet(activeChar, petbuff, i);
		}
	}

	private void buffMageSet(L2PcInstance activeChar, boolean petbuff)
	{
		for (int i = 0; i < skillstable.length; i++)
		{
			if ((skillstable[i][1] != 2) && (skillstable[i][1] != 3))
			{
				continue;
			}
			paidAndBuffSet(activeChar, petbuff, i);
		}
	}

	private void buffDSMageSet(L2PcInstance activeChar, boolean petbuff)
	{
		for (int i = 0; i < skillstable.length; i++)
		{
			if ((skillstable[i][1] != 5) && (skillstable[i][1] != 6))
			{
				continue;
			}
			paidAndBuffSet(activeChar, petbuff, i);
		}
	}

	private void paidAndBuffSet(L2PcInstance activeChar, boolean petbuff, int key)
	{
		if (activeChar.getLevel() <= Config.FREE_BUFFS_TP_TO_LEVEL)
		{
			skilllevel = SkillData.getInstance().getMaxLevel(skillstable[key][0]);
			skill = SkillData.getInstance().getSkill(skillstable[key][0], skilllevel);
			applyOnCheckedTarget(activeChar, petbuff, skill);
			return;
		}
		if (activeChar.destroyItemByItemId(null, skillstable[key][3], skillstable[key][2], activeChar, true))
		{
			skilllevel = SkillData.getInstance().getMaxLevel(skillstable[key][0]);
			skill = SkillData.getInstance().getSkill(skillstable[key][0], skilllevel);
			applyOnCheckedTarget(activeChar, petbuff, skill);
		} else
		{
			showHaventAdena(activeChar);
		}
	}

	private void applyOnCheckedTarget(L2PcInstance activeChar, boolean petbuff, Skill skill)
	{
		if (!petbuff)
		{
			skill.getTargetList(activeChar, true, activeChar);
			if (skill != null)
			{
				skill.applyEffects(activeChar, activeChar);
			}
		} else
		{
			skill.getTargetList(activeChar.getSummon(), true, activeChar.getSummon());
			if (skill != null)
			{
				skill.applyEffects(activeChar.getSummon(), activeChar.getSummon());
			}
		}
	}

	private void buffSavedSet(L2PcInstance activeChar, boolean petbuff)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement statement = con
				.prepareStatement("SELECT * FROM community_skillsave WHERE charId=?;");)
		{
			statement.setInt(1, activeChar.getObjectId());
			try (ResultSet rcln = statement.executeQuery();)
			{
				rcln.next();
				if (!petbuff)
				{
					char[] allskills = rcln.getString(2).toCharArray();
					if (allskills.length == skillstable.length)
					{
						for (int i = 0; i < skillstable.length; i++)
						{
							if (allskills[i] == '1')
							{
								if (activeChar.destroyItemByItemId(null, skillstable[i][3], skillstable[i][2],
									activeChar, true))
								{
									skilllevel = SkillData.getInstance().getMaxLevel(skillstable[i][0]);
									skill = SkillData.getInstance().getSkill(skillstable[i][0], skilllevel);
									skill.getTargetList(activeChar, true, activeChar);
									if (skill != null)
									{
										skill.applyEffects(activeChar, activeChar);
									}
								} else
								{
									showHaventAdena(activeChar);
								}

							}
						}

					}
				} else
				{
					char petskills[] = rcln.getString(3).toCharArray();
					if (petskills.length == skillstable.length)
					{
						for (int i = 0; i < skillstable.length; i++)
						{
							if (petskills[i] != '1')
							{
								continue;
							}
							if (activeChar.destroyItemByItemId(null, skillstable[i][3], skillstable[i][2], activeChar,
								true))
							{
								skilllevel = SkillData.getInstance().getMaxLevel(skillstable[i][0]);
								skill = SkillData.getInstance().getSkill(skillstable[i][0], skilllevel);
								skill.getTargetList(activeChar.getSummon(), true, activeChar.getSummon());
								if (skill != null)
								{
									skill.applyEffects(activeChar, activeChar.getSummon());
								}
							} else
							{
								showHaventAdena(activeChar);
							}
						}
					}
				}
			}
		} catch (SQLException e)
		{
			LOG.error("SQL exception", e);
		}
	}

	private void saveBuffsSet(L2PcInstance activeChar, boolean petbuff)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement stat = con
				.prepareStatement("SELECT COUNT(*) FROM community_skillsave WHERE charId=?;");)
		{
			stat.setInt(1, activeChar.getObjectId());
			try (ResultSet rset = stat.executeQuery();)
			{
				rset.next();
				StringBuilder allbuff = new StringBuilder();
				if (!petbuff)
				{
					List<BuffInfo> skill = activeChar.getEffectList().getEffects();
					boolean flag = true;
					for (int i = 0; i < skillstable.length; i++)
					{
						for (int j = 0; j < skill.size(); j++)
						{
							if (skillstable[i][0] == skill.get(j).getSkill().getId())
							{
								allbuff.append(1);
								flag = false;
							}
							if ((j == (skill.size() - 1)) && flag)
							{
								allbuff.append(0);
							}
						}
						flag = true;
					}
					if (rset.getInt(1) == 0)
					{
						try (PreparedStatement statement1 = con
							.prepareStatement("INSERT INTO community_skillsave (charId,skills) values (?,?)");)
						{
							statement1.setInt(1, activeChar.getObjectId());
							statement1.setString(2, allbuff.toString());
							statement1.execute();
						} catch (SQLException e)
						{
							LOG.error("SQL exception", e);
						}
					} else
					{
						try (PreparedStatement statement = con
							.prepareStatement("UPDATE community_skillsave SET skills=? WHERE charId=?;");)
						{
							statement.setString(1, allbuff.toString());
							statement.setInt(2, activeChar.getObjectId());
							statement.execute();
						} catch (SQLException e)
						{
							LOG.error("SQL exception", e);
						}
					}
				} else
				{
					List<BuffInfo> skill = activeChar.getSummon().getEffectList().getEffects();
					boolean flag = true;
					for (int i = 0; i < skillstable.length; i++)
					{
						for (int j = 0; j < skill.size(); j++)
						{
							if (skillstable[i][0] == skill.get(j).getSkill().getId())
							{
								allbuff = allbuff.append(1);
								flag = false;
							}
							if ((j == (skill.size() - 1)) && flag)
							{
								allbuff.append(0);
							}
						}
						flag = true;
					}
					if (rset.getInt(1) == 0)
					{
						try (PreparedStatement statement1 = con
							.prepareStatement("INSERT INTO community_skillsave (charId,pet) values (?,?)");)
						{
							statement1.setInt(1, activeChar.getObjectId());
							statement1.setString(2, allbuff.toString());
							statement1.execute();
						} catch (SQLException e)
						{
							LOG.error("SQL exception", e);
						}
					} else
					{
						try (PreparedStatement statement = con
							.prepareStatement("UPDATE community_skillsave SET pet=? WHERE charId=?;");)
						{
							statement.setString(1, allbuff.toString());
							statement.setInt(2, activeChar.getObjectId());
							statement.execute();
						} catch (SQLException e)
						{
							LOG.error("SQL exception", e);
						}
					}
				}
			}
		} catch (SQLException e)
		{
			LOG.error("SQL exception", e);
		}
	}

	private void showHaventAdena(L2PcInstance activeChar)
	{
		activeChar.sendPacket(new ExShowScreenMessage("Sorry, not enough adena!", 3000));
	}

	private void resetAllBuffs(L2PcInstance activeChar, boolean petbuff)
	{
		if (!petbuff)
		{
			activeChar.stopAllEffects();
		} else
		{
			activeChar.getSummon().stopAllEffects();
		}
	}

	private void regenMp(L2PcInstance activeChar, boolean petbuff)
	{
		if (!petbuff)
		{
			activeChar.setCurrentMp(activeChar.getMaxMp());
		} else
		{
			activeChar.getSummon().setCurrentMp(activeChar.getSummon().getMaxMp());
		}
	}

	private void regenHp(L2PcInstance activeChar, boolean petbuff)
	{
		if (!petbuff)
		{
			activeChar.setCurrentHp(activeChar.getMaxHp());
		} else
		{
			activeChar.getSummon().setCurrentHp(activeChar.getSummon().getMaxHp());
		}
	}

}
