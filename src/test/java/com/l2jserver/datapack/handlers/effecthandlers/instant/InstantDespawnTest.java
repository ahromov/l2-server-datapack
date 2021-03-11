/*
 * Copyright © 2004-2021 L2J DataPack
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
package com.l2jserver.datapack.handlers.effecthandlers.instant;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;

import java.util.Map;

import org.powermock.api.easymock.annotation.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.l2jserver.commons.util.Rnd;
import com.l2jserver.datapack.test.AbstractTest;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.skills.BuffInfo;

/**
 * Despawn instant effect test.
 * @author Zoey76
 * @version 2.6.2.0
 */
@PrepareForTest({
	BuffInfo.class,
	Rnd.class
})
public class InstantDespawnTest extends AbstractTest {
	
	private static final int CHANCE = 75;
	
	@Mock
	private BuffInfo buffInfo;
	@Mock
	private L2Character effected;
	@Mock
	private L2PcInstance player;
	@Mock
	private L2Summon summon;
	
	private InstantDespawn effect;
	
	@BeforeSuite
	void init() {
		final var set = new StatsSet(Map.of("name", "InstantDespawn"));
		final var params = new StatsSet(Map.of("chance", CHANCE));
		effect = new InstantDespawn(null, null, set, params);
	}
	
	@Test
	public void test_null_effected() {
		effect.onStart(buffInfo);
	}
	
	@Test
	public void test_null_player() {
		expect(buffInfo.getEffected()).andReturn(effected);
		expect(effected.getActingPlayer()).andReturn(null);
		replayAll();
		
		effect.onStart(buffInfo);
	}
	
	@Test
	public void test_null_summon() {
		expect(buffInfo.getEffected()).andReturn(effected);
		expect(effected.getActingPlayer()).andReturn(player);
		expect(player.getSummon()).andReturn(null);
		replayAll();
		
		effect.onStart(buffInfo);
	}
	
	@Test
	public void test_chance_fail() {
		expect(buffInfo.getEffected()).andReturn(effected);
		expect(effected.getActingPlayer()).andReturn(player);
		expect(player.getSummon()).andReturn(summon);
		mockStatic(Rnd.class);
		expect(Rnd.get(100)).andReturn(CHANCE - 1);
		replayAll();
		
		effect.onStart(buffInfo);
	}
	
	@Test
	public void test_chance_success() {
		expect(buffInfo.getEffected()).andReturn(effected);
		expect(effected.getActingPlayer()).andReturn(player);
		expect(player.getSummon()).andReturn(summon);
		mockStatic(Rnd.class);
		expect(Rnd.get(100)).andReturn(CHANCE);
		summon.unSummon(player);
		expectLastCall();
		replayAll();
		
		effect.onStart(buffInfo);
	}
}
