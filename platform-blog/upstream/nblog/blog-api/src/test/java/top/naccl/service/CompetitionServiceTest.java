package top.naccl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.entity.Competition;
import top.naccl.entity.CompetitionAwardFlatProjection;
import top.naccl.entity.CompetitionAward;
import top.naccl.entity.CompetitionAwardRecipient;
import top.naccl.entity.CompetitionParticipant;
import top.naccl.entity.CompetitionTypeTag;
import top.naccl.entity.User;
import top.naccl.enums.CompetitionAwardMode;
import top.naccl.enums.CompetitionAwardScope;
import top.naccl.enums.CompetitionAwardTier;
import top.naccl.enums.CompetitionCategory;
import top.naccl.enums.CompetitionParticipationMode;
import top.naccl.enums.CompetitionType;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.ForbiddenException;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.CompetitionMapper;
import top.naccl.mapper.UserMapper;
import top.naccl.model.dto.CompetitionAchievementVisibilityRequest;
import top.naccl.model.dto.CompetitionAchievementOrderRequest;
import top.naccl.model.dto.CompetitionAwardCreateRequest;
import top.naccl.model.dto.CompetitionAwardLoginRequirementRequest;
import top.naccl.model.dto.CompetitionCreateRequest;
import top.naccl.model.dto.CompetitionParticipantsCreateRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class CompetitionServiceTest {
	private static final Instant NOW = Instant.parse("2026-07-14T04:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final LocalDate COMPETITION_DATE = LocalDate.of(2026, 5, 17);

	@Mock
	private CompetitionMapper competitionMapper;
	@Mock
	private UserMapper userMapper;

	private CompetitionService service;

	@BeforeEach
	void setUp() {
		service = new CompetitionService(competitionMapper, userMapper, CLOCK);
	}

	@Test
	void canonicalCategoriesExposeTheLockedApiValuesLabelsAndStableTypeSets() {
		assertEquals(List.of(
				"PROVINCIAL",
				"ICPC_NATIONAL_INVITATIONAL",
				"CCPC_NATIONAL_INVITATIONAL",
				"ICPC_ASIA_REGIONAL",
				"CCPC_REGIONAL",
				"EC_FINAL",
				"CCPC_FINAL",
				"BAIDU_STAR",
				"GPLT_NATIONAL",
				"LANQIAO_CUP_NATIONAL"
		), java.util.Arrays.stream(CompetitionCategory.values()).map(Enum::name).toList());
		assertEquals(List.of(
				"省赛",
				"ICPC 全国邀请赛",
				"CCPC 全国邀请赛",
				"ICPC 亚洲区域赛",
				"CCPC 区域赛",
				"EC-Final",
				"CCPC-Final",
				"百度之星",
				"GPLT 团体程序设计天梯赛（国赛）",
				"蓝桥杯程序设计竞赛（国奖）"
		), java.util.Arrays.stream(CompetitionCategory.values()).map(CompetitionCategory::label).toList());
		assertEquals(List.of(CompetitionType.ICPC, CompetitionType.INVITATIONAL),
				CompetitionCategory.ICPC_NATIONAL_INVITATIONAL.types());
		assertEquals(List.of(CompetitionType.CCPC, CompetitionType.NATIONAL_SITE),
				CompetitionCategory.CCPC_REGIONAL.types());
		assertEquals(List.of(CompetitionType.ICPC, CompetitionType.ASIA_EAST_CONTINENT_FINAL),
				CompetitionCategory.EC_FINAL.types());
	}

	@Test
	void canonicalAwardTiersExposeTheLockedApiValuesAndLabels() {
		assertEquals(List.of(
				"MEDAL_GOLD", "MEDAL_SILVER", "MEDAL_BRONZE", "MEDAL_HONORABLE_MENTION",
				"BAIDU_NATIONAL_FIRST", "BAIDU_NATIONAL_SECOND", "BAIDU_NATIONAL_THIRD",
				"BAIDU_NATIONAL_FOURTH", "BAIDU_PROVINCIAL_FIRST", "BAIDU_PROVINCIAL_SECOND",
				"BAIDU_PROVINCIAL_THIRD", "FIRST_PRIZE", "SECOND_PRIZE", "THIRD_PRIZE"
		), java.util.Arrays.stream(CompetitionAwardTier.values()).map(Enum::name).toList());
		assertEquals(List.of(
				"金牌", "银牌", "铜牌", "优胜奖",
				"国赛一等奖", "国赛二等奖", "国赛三等奖", "国赛四等奖",
				"省赛一等奖", "省赛二等奖", "省赛三等奖",
				"一等奖", "二等奖", "三等奖"
		), java.util.Arrays.stream(CompetitionAwardTier.values()).map(CompetitionAwardTier::label).toList());
	}

	@Test
	void createsCompetitionWithAllNormalizedTypeTags() {
		Competition persisted = activeCompetition(31L, "2026 ICPC 亚洲区域赛");
		persisted.setCompetitionDate(COMPETITION_DATE);
		doAnswer(invocation -> {
			Competition source = invocation.getArgument(0);
			source.setId(31L);
			return 1;
		}).when(competitionMapper).insertCompetition(any(Competition.class));
		when(competitionMapper.insertTypeTags(31L,
				List.of(CompetitionType.ICPC, CompetitionType.ASIA_REGIONAL))).thenReturn(2);
		stubActiveResponse(persisted, CompetitionType.ICPC, CompetitionType.ASIA_REGIONAL);

		var response = service.create(new CompetitionCreateRequest(
				" 2026 ICPC 亚洲区域赛 ", COMPETITION_DATE,
				"icpc_asia_regional", "team"));

		ArgumentCaptor<Competition> captor = ArgumentCaptor.forClass(Competition.class);
		verify(competitionMapper).insertCompetition(captor.capture());
		assertEquals("2026 ICPC 亚洲区域赛", captor.getValue().getFullName());
		assertEquals("2026 ICPC 亚洲区域赛", captor.getValue().getActiveFullName());
		assertEquals(2026, captor.getValue().getCompetitionYear());
		assertEquals(COMPETITION_DATE, captor.getValue().getCompetitionDate());
		assertEquals(CompetitionParticipationMode.TEAM, captor.getValue().getParticipationMode());
		assertEquals(2026, response.year());
		assertEquals(COMPETITION_DATE, response.competitionDate());
		assertEquals("ICPC_ASIA_REGIONAL", response.category());
		assertEquals("ICPC 亚洲区域赛", response.categoryLabel());
		assertEquals(List.of("ICPC", "ASIA_REGIONAL"),
				response.types().stream().map(type -> type.code()).toList());
	}

	@Test
	void createsCompetitionWithoutAnOptionalDateOrDerivedYear() {
		Competition persisted = activeCompetition(31L, "无明确日期的比赛");
		persisted.setCompetitionYear(null);
		doAnswer(invocation -> {
			Competition source = invocation.getArgument(0);
			source.setId(31L);
			return 1;
		}).when(competitionMapper).insertCompetition(any(Competition.class));
		when(competitionMapper.insertTypeTags(31L, List.of(CompetitionType.PROVINCIAL))).thenReturn(1);
		stubActiveResponse(persisted, CompetitionType.PROVINCIAL);

		var response = service.create(new CompetitionCreateRequest(
				"无明确日期的比赛", null, "PROVINCIAL", "INDIVIDUAL"));

		ArgumentCaptor<Competition> captor = ArgumentCaptor.forClass(Competition.class);
		verify(competitionMapper).insertCompetition(captor.capture());
		assertNull(captor.getValue().getCompetitionYear());
		assertNull(captor.getValue().getCompetitionDate());
		assertNull(response.year());
		assertNull(response.competitionDate());
	}

	@Test
	void rejectsCompetitionDatesOutsideTheSupportedYearRange() {
		assertThrows(BadRequestException.class, () -> service.create(new CompetitionCreateRequest(
				"历史比赛", LocalDate.of(1899, 12, 31), "PROVINCIAL", "INDIVIDUAL")));

		verifyNoInteractions(competitionMapper, userMapper);
	}

	@Test
	void rejectsInvalidYearBoundsBeforeQueryingPersistence() {
		assertThrows(BadRequestException.class, () -> service.list(2026, 2025, null, 1, 10));
		assertThrows(BadRequestException.class, () -> service.list(1899, null, null, 1, 10));
		assertThrows(BadRequestException.class, () -> service.list(null, 10000, null, 1, 10));

		verifyNoInteractions(competitionMapper, userMapper);
	}

	@Test
	void aggregateReadsUseRepeatableReadOnlySnapshots() throws NoSuchMethodException {
		assertRepeatableReadOnly(CompetitionService.class.getMethod(
				"list", Integer.class, Integer.class, String.class, Integer.class, Integer.class));
		assertRepeatableReadOnly(CompetitionService.class.getMethod(
				"listRecycleBin", Integer.class, Integer.class, String.class, Integer.class, Integer.class));
		assertRepeatableReadOnly(CompetitionService.class.getMethod("get", Long.class));
	}

	@Test
	void rejectsParticipationModeThatConflictsWithTheCanonicalCategory() {
		assertThrows(BadRequestException.class, () -> service.create(new CompetitionCreateRequest(
				"2026 蓝桥杯", COMPETITION_DATE, "LANQIAO_CUP_NATIONAL", "TEAM")));
		verify(competitionMapper, never()).insertCompetition(any());
	}

	@Test
	void historicalYearOnlyCompetitionsRemainReadable() {
		Competition historical = activeCompetition(31L, "历史比赛");
		stubActiveResponse(historical, CompetitionType.OTHER);

		var response = service.get(31L, true);

		assertEquals(2026, response.year());
		assertNull(response.competitionDate());
	}

	@Test
	void categoryFilterIsPassedToPersistenceBeforePaginationAsAnExactTypeCombination() {
		when(competitionMapper.findActiveCompetitions(2024, 2026,
				List.of(CompetitionType.CCPC, CompetitionType.INVITATIONAL), 2)).thenReturn(List.of());

		service.list(2024, 2026, "CCPC_NATIONAL_INVITATIONAL", 1, 10);

		verify(competitionMapper).findActiveCompetitions(2024, 2026,
				List.of(CompetitionType.CCPC, CompetitionType.INVITATIONAL), 2);
	}

	@Test
	void recycleBinCategoryFilterUsesTheSameExactTypeCombination() {
		Date cutoff = Date.from(NOW.minus(CompetitionService.RETENTION));
		when(competitionMapper.findRecycleBinCompetitions(2024, 2026,
				List.of(CompetitionType.ICPC, CompetitionType.ASIA_REGIONAL), 2, cutoff))
				.thenReturn(List.of());

		service.listRecycleBin(2024, 2026, "ICPC_ASIA_REGIONAL", 1, 10);

		verify(competitionMapper).findRecycleBinCompetitions(2024, 2026,
				List.of(CompetitionType.ICPC, CompetitionType.ASIA_REGIONAL), 2, cutoff);
	}

	@Test
	void deletionOnlyMarksRecycleBinAtFixedClockInstant() {
		Competition competition = activeCompetition(31L, "比赛");
		when(competitionMapper.findCompetitionByIdForUpdate(31L)).thenReturn(competition);
		when(competitionMapper.moveCompetitionToRecycleBin(31L, Date.from(NOW))).thenReturn(1);

		service.moveToRecycleBin(31L);

		verify(competitionMapper).findCompetitionByIdForUpdate(31L);
		verify(competitionMapper).moveCompetitionToRecycleBin(31L, Date.from(NOW));
		verify(competitionMapper, never()).deleteCompetitionById(anyLong());
		verifyNoMoreInteractions(competitionMapper);
	}

	@Test
	void restoresRecordStrictlyWithinSevenDaysUsingFixedCutoff() {
		Competition deleted = deletedCompetition(31L, "比赛",
				NOW.minus(CompetitionService.RETENTION).plusMillis(1));
		Competition restored = activeCompetition(31L, "比赛");
		when(competitionMapper.findCompetitionByIdForUpdate(31L)).thenReturn(deleted);
		when(competitionMapper.restoreCompetition(31L,
				Date.from(NOW.minus(CompetitionService.RETENTION)))).thenReturn(1);
		stubActiveResponse(restored, CompetitionType.OTHER);

		var response = service.restore(31L);

		assertEquals(31L, response.id());
		assertEquals("比赛", response.fullName());
		verify(competitionMapper).restoreCompetition(31L,
				Date.from(NOW.minus(CompetitionService.RETENTION)));
	}

	@Test
	void rejectsRestoreAtOrAfterSevenDayDeadline() {
		Competition expired = deletedCompetition(31L, "比赛", NOW.minus(CompetitionService.RETENTION));
		when(competitionMapper.findCompetitionByIdForUpdate(31L)).thenReturn(expired);

		assertThrows(NotFoundException.class, () -> service.restore(31L));

		verify(competitionMapper, never()).restoreCompetition(anyLong(), any(Date.class));
	}

	@Test
	void rejectsRestoreWhenAnotherActiveCompetitionUsesTheSameFullName() {
		Competition deleted = deletedCompetition(31L, "比赛", NOW.minusSeconds(60));
		when(competitionMapper.findCompetitionByIdForUpdate(31L)).thenReturn(deleted);
		when(competitionMapper.findActiveCompetitionByFullName("比赛"))
				.thenReturn(activeCompetition(41L, "比赛"));

		assertThrows(BadRequestException.class, () -> service.restore(31L));

		verify(competitionMapper, never()).restoreCompetition(anyLong(), any(Date.class));
	}

	@Test
	void purgesEveryCompetitionWhoseRetentionPeriodExpired() {
		Date cutoff = Date.from(NOW.minus(CompetitionService.RETENTION));
		when(competitionMapper.findExpiredCompetitionIdsForUpdate(cutoff)).thenReturn(List.of(31L, 32L));
		when(competitionMapper.deleteCompetitionById(31L)).thenReturn(1);
		when(competitionMapper.deleteCompetitionById(32L)).thenReturn(1);

		assertEquals(2, service.purgeExpired());

		verify(competitionMapper).deleteCompetitionById(31L);
		verify(competitionMapper).deleteCompetitionById(32L);
	}

	@Test
	void batchParticipantsPersistCurrentNicknameAsHistoricalDisplaySnapshot() {
		Competition competition = activeCompetition(31L, "比赛");
		when(competitionMapper.findCompetitionByIdForUpdate(31L)).thenReturn(competition);
		when(userMapper.findByUsernames(List.of("alice", "bob")))
				.thenReturn(List.of(user("alice", "Alice 昵称"), user("bob", " ")));
		when(competitionMapper.findParticipantsByCompetitionIdAndUsernames(
				31L, List.of("alice", "bob"))).thenReturn(List.of());
		when(competitionMapper.insertParticipants(any())).thenAnswer(invocation -> {
			List<CompetitionParticipant> participants = invocation.getArgument(0);
			return participants.size();
		});
		stubActiveResponse(competition, CompetitionType.OTHER);

		service.addParticipants(31L,
				new CompetitionParticipantsCreateRequest(List.of(" alice ", "bob")));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<CompetitionParticipant>> captor = ArgumentCaptor.forClass(List.class);
		verify(competitionMapper).insertParticipants(captor.capture());
		assertEquals(List.of("alice", "bob"),
				captor.getValue().stream().map(CompetitionParticipant::getUsername).toList());
		assertEquals(List.of("Alice 昵称", "bob"),
				captor.getValue().stream().map(CompetitionParticipant::getDisplayNameSnapshot).toList());
	}

	@Test
	void refusesToDeleteParticipantReferencedByAnAward() {
		Competition competition = activeCompetition(31L, "比赛");
		CompetitionParticipant participant = participant(51L, 31L, "alice");
		when(competitionMapper.findCompetitionByIdForUpdate(31L)).thenReturn(competition);
		when(competitionMapper.findParticipantByIdForUpdate(51L)).thenReturn(participant);
		when(competitionMapper.countAwardReferencesByParticipantId(51L)).thenReturn(1);

		assertThrows(BadRequestException.class, () -> service.deleteParticipant(31L, 51L));

		verify(competitionMapper, never()).deleteParticipant(anyLong(), anyLong());
	}

	@Test
	void medalAwardsRequireRankAndRejectTiersFromAnotherCategory() {
		Competition competition = activeCompetition(31L, "比赛");
		when(competitionMapper.findCompetitionByIdForUpdate(31L)).thenReturn(competition);
		when(competitionMapper.findTypeTagsByCompetitionIds(List.of(31L)))
				.thenReturn(typeTags(31L, CompetitionType.ICPC, CompetitionType.ASIA_REGIONAL));

		assertThrows(BadRequestException.class, () -> service.addAward(31L,
				award("TEAM", null, "MEDAL_GOLD", null, null, List.of("alice"))));
		assertThrows(BadRequestException.class, () -> service.addAward(31L,
				award("TEAM", null, "FIRST_PRIZE", 1, 10, List.of("alice"))));

		verify(competitionMapper, never()).insertAward(any());
	}

	@Test
	void ordinaryAwardsRejectRankAndBaiduAcceptsOnlyItsSemanticTiers() {
		Competition competition = activeCompetition(31L, "比赛", CompetitionParticipationMode.INDIVIDUAL);
		when(competitionMapper.findCompetitionByIdForUpdate(31L)).thenReturn(competition);
		when(competitionMapper.findTypeTagsByCompetitionIds(List.of(31L)))
				.thenReturn(typeTags(31L, CompetitionType.BAIDU_STAR));

		assertThrows(BadRequestException.class, () -> service.addAward(31L,
				award("INDIVIDUAL", null, "BAIDU_NATIONAL_FIRST", 1, 10, List.of("alice"))));
		assertThrows(BadRequestException.class, () -> service.addAward(31L,
				award("INDIVIDUAL", null, "FIRST_PRIZE", null, null, List.of("alice"))));

		verify(competitionMapper, never()).insertAward(any());
	}

	@Test
	void teamAwardAllowsOneRecipientAndMapsTierToLegacyStorageColumns() {
		Competition competition = activeCompetition(31L, "比赛");
		CompetitionParticipant alice = participant(51L, 31L, "alice");
		when(competitionMapper.findCompetitionByIdForUpdate(31L)).thenReturn(competition);
		when(competitionMapper.findParticipantsByCompetitionIdAndUsernames(31L, List.of("alice")))
				.thenReturn(List.of(alice));
		when(competitionMapper.insertAward(any())).thenAnswer(invocation -> {
			top.naccl.entity.CompetitionAward award = invocation.getArgument(0);
			award.setId(71L);
			return 1;
		});
		when(competitionMapper.insertAwardRecipients(any())).thenReturn(1);
		stubActiveResponse(competition, CompetitionType.ICPC, CompetitionType.ASIA_REGIONAL);

		service.addAward(31L,
				award("TEAM", "custacm", "MEDAL_SILVER", 3, 120, true, List.of("alice")));

		ArgumentCaptor<top.naccl.entity.CompetitionAward> captor =
				ArgumentCaptor.forClass(top.naccl.entity.CompetitionAward.class);
		verify(competitionMapper).insertAward(captor.capture());
		assertEquals(2, captor.getValue().getAwardLevel());
		assertEquals("银牌", captor.getValue().getAwardName());
		assertEquals(null, captor.getValue().getAwardScope());
		assertEquals(3, captor.getValue().getRankPosition());
		assertTrue(captor.getValue().getRequiresLogin());
	}

	@Test
	void guestCompetitionReadsExcludeLoginRequiredAwards() {
		Competition competition = activeCompetition(31L, "比赛");
		CompetitionAward publicAward = competitionAward(71L, 31L, false);
		CompetitionAward loginRequiredAward = competitionAward(72L, 31L, true);
		List<Long> ids = List.of(31L);
		when(competitionMapper.findActiveCompetitionById(31L)).thenReturn(competition);
		when(competitionMapper.findTypeTagsByCompetitionIds(ids))
				.thenReturn(typeTags(31L, CompetitionType.ICPC, CompetitionType.ASIA_REGIONAL));
		when(competitionMapper.findParticipantsByCompetitionIds(ids)).thenReturn(List.of());
		when(competitionMapper.findAwardRecipientsByCompetitionIds(ids)).thenReturn(List.of());
		when(competitionMapper.findAwardsByCompetitionIds(ids))
				.thenReturn(List.of(publicAward, loginRequiredAward));

		var guest = service.get(31L, false);
		var member = service.get(31L, true);

		assertEquals(List.of(71L), guest.awards().stream().map(award -> award.id()).toList());
		assertEquals(List.of(71L, 72L), member.awards().stream().map(award -> award.id()).toList());
		assertTrue(member.awards().get(1).requiresLogin());
	}

	@Test
	void adminCanUpdateAnAwardsLoginRequirement() {
		Competition competition = activeCompetition(31L, "比赛");
		CompetitionAward award = competitionAward(71L, 31L, false);
		when(competitionMapper.findCompetitionByIdForUpdate(31L)).thenReturn(competition);
		when(competitionMapper.findAwardByIdForUpdate(71L)).thenReturn(award);
		when(competitionMapper.updateAwardLoginRequirement(31L, 71L, true)).thenReturn(1);
		stubActiveResponse(competition, CompetitionType.ICPC, CompetitionType.ASIA_REGIONAL);

		service.updateAwardLoginRequirement(
				31L, 71L, new CompetitionAwardLoginRequirementRequest(true));

		verify(competitionMapper).updateAwardLoginRequirement(31L, 71L, true);
	}

	@Test
	void awardLoginRequirementRejectsAnEmptyChoiceOrMismatchedAward() {
		assertThrows(BadRequestException.class, () -> service.updateAwardLoginRequirement(
				31L, 71L, new CompetitionAwardLoginRequirementRequest(null)));

		when(competitionMapper.findCompetitionByIdForUpdate(31L))
				.thenReturn(activeCompetition(31L, "比赛"));
		when(competitionMapper.findAwardByIdForUpdate(71L))
				.thenReturn(competitionAward(71L, 41L, false));

		assertThrows(NotFoundException.class, () -> service.updateAwardLoginRequirement(
				31L, 71L, new CompetitionAwardLoginRequirementRequest(true)));
		verify(competitionMapper, never()).updateAwardLoginRequirement(anyLong(), anyLong(),
				org.mockito.ArgumentMatchers.anyBoolean());
	}

	@Test
	void teamAwardDoesNotApplyTheParticipantBatchSizeLimitToRecipients() {
		Competition competition = activeCompetition(31L, "比赛");
		List<String> usernames = IntStream.range(0, 101).mapToObj(index -> "player" + index).toList();
		List<CompetitionParticipant> participants = IntStream.range(0, usernames.size())
				.mapToObj(index -> participant(1000L + index, 31L, usernames.get(index)))
				.toList();
		when(competitionMapper.findCompetitionByIdForUpdate(31L)).thenReturn(competition);
		when(competitionMapper.findParticipantsByCompetitionIdAndUsernames(31L, usernames))
				.thenReturn(participants);
		when(competitionMapper.insertAward(any())).thenAnswer(invocation -> {
			top.naccl.entity.CompetitionAward award = invocation.getArgument(0);
			award.setId(71L);
			return 1;
		});
		when(competitionMapper.insertAwardRecipients(any())).thenReturn(usernames.size());
		stubActiveResponse(competition, CompetitionType.ICPC, CompetitionType.ASIA_REGIONAL);

		service.addAward(31L,
				award("TEAM", null, "MEDAL_BRONZE", 8, 120, usernames));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<CompetitionAwardRecipient>> captor = ArgumentCaptor.forClass(List.class);
		verify(competitionMapper).insertAwardRecipients(captor.capture());
		assertEquals(101, captor.getValue().size());
	}

	@Test
	void baiduTierMapsScopeLevelAndLabelWhileKeepingRanksNull() {
		Competition competition = activeCompetition(31L, "百度之星", CompetitionParticipationMode.INDIVIDUAL);
		CompetitionParticipant alice = participant(51L, 31L, "alice");
		when(competitionMapper.findCompetitionByIdForUpdate(31L)).thenReturn(competition);
		when(competitionMapper.findParticipantsByCompetitionIdAndUsernames(31L, List.of("alice")))
				.thenReturn(List.of(alice));
		when(competitionMapper.insertAward(any())).thenAnswer(invocation -> {
			top.naccl.entity.CompetitionAward award = invocation.getArgument(0);
			award.setId(71L);
			return 1;
		});
		when(competitionMapper.insertAwardRecipients(any())).thenReturn(1);
		stubActiveResponse(competition, CompetitionType.BAIDU_STAR);

		service.addAward(31L,
				award("INDIVIDUAL", null, "BAIDU_PROVINCIAL_SECOND", null, null, List.of("alice")));

		ArgumentCaptor<top.naccl.entity.CompetitionAward> captor =
				ArgumentCaptor.forClass(top.naccl.entity.CompetitionAward.class);
		verify(competitionMapper).insertAward(captor.capture());
		assertEquals(CompetitionAwardScope.PROVINCIAL, captor.getValue().getAwardScope());
		assertEquals(2, captor.getValue().getAwardLevel());
		assertEquals("省赛二等奖", captor.getValue().getAwardName());
		assertEquals(null, captor.getValue().getRankPosition());
		assertEquals(null, captor.getValue().getRankTotal());
	}

	@Test
	void rejectsAwardRecipientWhoIsNotACompetitionParticipant() {
		Competition competition = activeCompetition(31L, "比赛");
		when(competitionMapper.findCompetitionByIdForUpdate(31L)).thenReturn(competition);
		when(competitionMapper.findTypeTagsByCompetitionIds(List.of(31L)))
				.thenReturn(typeTags(31L, CompetitionType.ICPC, CompetitionType.ASIA_REGIONAL));
		when(competitionMapper.findParticipantsByCompetitionIdAndUsernames(
				31L, List.of("alice", "bob"))).thenReturn(List.of(participant(51L, 31L, "alice")));

		assertThrows(BadRequestException.class, () -> service.addAward(31L,
				award("TEAM", "custacm", "MEDAL_SILVER", 3, 100, List.of("alice", "bob"))));

		verify(competitionMapper, never()).insertAward(any());
	}

	@Test
	void rejectsArticleBindingByNonParticipant() {
		when(competitionMapper.findCompetitionByIdForUpdate(31L))
				.thenReturn(activeCompetition(31L, "比赛"));
		when(competitionMapper.findParticipantsByCompetitionIdAndUsernames(31L, List.of("alice")))
				.thenReturn(List.of());

		assertThrows(ForbiddenException.class, () -> service.bindArticle("alice", 31L, 81L));

		verify(competitionMapper, never()).bindOwnedPublicArticle(anyLong(), anyString(), anyLong());
	}

	@Test
	void bindsArticleOnlyThroughTheMapperAtomicOwnershipWrite() {
		CompetitionParticipant participant = participant(51L, 31L, "alice");
		when(competitionMapper.findCompetitionByIdForUpdate(31L))
				.thenReturn(activeCompetition(31L, "比赛"));
		when(competitionMapper.findParticipantsByCompetitionIdAndUsernames(31L, List.of("alice")))
				.thenReturn(List.of(participant));
		when(competitionMapper.bindOwnedPublicArticle(51L, "alice", 81L)).thenReturn(1);

		service.bindArticle("alice", 31L, 81L);

		verify(competitionMapper).bindOwnedPublicArticle(51L, "alice", 81L);
		verifyNoInteractions(userMapper);
	}

	@Test
	void profileAchievementsCollapseMultipleTypeRowsForTheSameAward() {
		CompetitionAwardFlatProjection regional = projection(CompetitionType.ASIA_REGIONAL);
		CompetitionAwardFlatProjection icpc = projection(CompetitionType.ICPC);
		regional.setCompetitionDate(COMPETITION_DATE);
		icpc.setCompetitionDate(COMPETITION_DATE);
		when(competitionMapper.findActiveAwardProjectionsByUsername("alice"))
				.thenReturn(List.of(regional, icpc));

		var achievements = service.achievements("alice");

		assertEquals(1, achievements.size());
		assertEquals(List.of("ICPC", "ASIA_REGIONAL"),
				achievements.getFirst().types().stream().map(type -> type.code()).toList());
		assertEquals("(3/120)", achievements.getFirst().rank());
		assertEquals(71L, achievements.getFirst().awardId());
		assertEquals(COMPETITION_DATE, achievements.getFirst().competitionDate());
		assertTrue(achievements.getFirst().profileVisible());
	}

	@Test
	void publicAchievementsOnlyReturnAwardsThePlayerChoseToDisplay() {
		CompetitionAwardFlatProjection visible = projection(CompetitionType.ICPC);
		CompetitionAwardFlatProjection hidden = projection(CompetitionType.CCPC);
		hidden.setAwardId(72L);
		hidden.setProfileVisible(false);
		when(competitionMapper.findActiveAwardProjectionsByUsername("alice"))
				.thenReturn(List.of(hidden, visible));

		var privateAchievements = service.achievements("alice");
		var publicAchievements = service.publicAchievements("alice");

		assertEquals(List.of(71L, 72L),
				privateAchievements.stream().map(achievement -> achievement.awardId()).toList());
		assertEquals(List.of(71L),
				publicAchievements.stream().map(achievement -> achievement.awardId()).toList());
	}

	@Test
	void publicProfilesExposeLoginRequiredAchievementsOnlyToMembers() {
		CompetitionAwardFlatProjection publicAward = projection(CompetitionType.ICPC);
		publicAward.setProfileOrder(1L);
		CompetitionAwardFlatProjection loginRequiredAward = projection(CompetitionType.CCPC);
		loginRequiredAward.setAwardId(72L);
		loginRequiredAward.setProfileOrder(2L);
		loginRequiredAward.setRequiresLogin(true);
		when(competitionMapper.findActiveAwardProjectionsByUsername("alice"))
				.thenReturn(List.of(publicAward, loginRequiredAward));

		var guest = service.publicAchievements("alice", false);
		var member = service.publicAchievements("alice", true);

		assertEquals(List.of(71L), guest.stream().map(achievement -> achievement.awardId()).toList());
		assertEquals(List.of(71L, 72L),
				member.stream().map(achievement -> achievement.awardId()).toList());
	}

	@Test
	void visibleProfileAchievementsFollowTheirExplicitOrder() {
		CompetitionAwardFlatProjection later = projection(CompetitionType.ICPC);
		later.setProfileOrder(2L);
		CompetitionAwardFlatProjection earlier = projection(CompetitionType.CCPC);
		earlier.setCompetitionId(41L);
		earlier.setAwardId(72L);
		earlier.setProfileOrder(1L);
		when(competitionMapper.findActiveAwardProjectionsByUsername("alice"))
				.thenReturn(List.of(later, earlier));

		var achievements = service.publicAchievements("alice");

		assertEquals(List.of(72L, 71L),
				achievements.stream().map(achievement -> achievement.awardId()).toList());
	}

	@Test
	void playerCanUpdateOnlyTheirOwnAchievementProfileVisibility() {
		when(competitionMapper.findCompetitionByIdForUpdate(31L))
				.thenReturn(activeCompetition(31L, "比赛"));
		when(competitionMapper.findAchievementProfileStateForUpdate(31L, 71L, "alice"))
				.thenReturn(profileState(31L, 71L, false, null));
		when(competitionMapper.findVisibleAchievementOrdersForUpdate("alice"))
				.thenReturn(List.of(profileState(41L, 72L, true, 4L)));
		when(competitionMapper.updateAchievementProfileVisibility(31L, 71L, "alice", true, 5L))
				.thenReturn(1);

		service.updateAchievementVisibility("alice", 31L, 71L,
				new CompetitionAchievementVisibilityRequest(true));

		verify(competitionMapper).updateAchievementProfileVisibility(31L, 71L, "alice", true, 5L);
	}

	@Test
	void hidingAchievementClearsItsProfileOrder() {
		when(competitionMapper.findCompetitionByIdForUpdate(31L))
				.thenReturn(activeCompetition(31L, "比赛"));
		when(competitionMapper.findAchievementProfileStateForUpdate(31L, 71L, "alice"))
				.thenReturn(profileState(31L, 71L, true, 2L));
		when(competitionMapper.updateAchievementProfileVisibility(31L, 71L, "alice", false, null))
				.thenReturn(1);

		service.updateAchievementVisibility("alice", 31L, 71L,
				new CompetitionAchievementVisibilityRequest(false));

		verify(competitionMapper).updateAchievementProfileVisibility(31L, 71L, "alice", false, null);
	}

	@Test
	void achievementVisibilityUpdateIsIdempotent() {
		when(competitionMapper.findCompetitionByIdForUpdate(31L))
				.thenReturn(activeCompetition(31L, "比赛"));
		when(competitionMapper.findAchievementProfileStateForUpdate(31L, 71L, "alice"))
				.thenReturn(profileState(31L, 71L, true, 2L));

		service.updateAchievementVisibility("alice", 31L, 71L,
				new CompetitionAchievementVisibilityRequest(true));

		verify(competitionMapper, never()).updateAchievementProfileVisibility(
				anyLong(), anyLong(), anyString(), org.mockito.ArgumentMatchers.anyBoolean(), anyLong());
	}

	@Test
	void achievementVisibilityUpdateRejectsMissingRecipientAndEmptyChoice() {
		when(competitionMapper.findCompetitionByIdForUpdate(31L))
				.thenReturn(activeCompetition(31L, "比赛"));
		when(competitionMapper.findAchievementProfileStateForUpdate(31L, 71L, "alice")).thenReturn(null);

		assertThrows(NotFoundException.class, () -> service.updateAchievementVisibility(
				"alice", 31L, 71L, new CompetitionAchievementVisibilityRequest(true)));
		assertThrows(BadRequestException.class, () -> service.updateAchievementVisibility(
				"alice", 31L, 71L, new CompetitionAchievementVisibilityRequest(null)));

		verify(competitionMapper, never()).updateAchievementProfileVisibility(
				anyLong(), anyLong(), anyString(), org.mockito.ArgumentMatchers.anyBoolean(),
				org.mockito.ArgumentMatchers.<Long>isNull());
	}

	@Test
	void playerMustSubmitEveryVisibleAwardExactlyOnceWhenReordering() {
		when(competitionMapper.findVisibleAchievementOrdersForUpdate("alice")).thenReturn(List.of(
				profileState(31L, 71L, true, 1L), profileState(41L, 72L, true, 2L)));
		when(competitionMapper.updateAchievementProfileOrder("alice", 72L, 1L)).thenReturn(1);
		when(competitionMapper.updateAchievementProfileOrder("alice", 71L, 2L)).thenReturn(1);

		service.updateAchievementOrder("alice",
				new CompetitionAchievementOrderRequest(List.of(72L, 71L)));

		verify(competitionMapper).updateAchievementProfileOrder("alice", 72L, 1L);
		verify(competitionMapper).updateAchievementProfileOrder("alice", 71L, 2L);
		assertThrows(BadRequestException.class, () -> service.updateAchievementOrder("alice",
				new CompetitionAchievementOrderRequest(List.of(71L, 71L))));
	}

	private void stubActiveResponse(Competition competition, CompetitionType... types) {
		List<Long> ids = List.of(competition.getId());
		when(competitionMapper.findActiveCompetitionById(competition.getId())).thenReturn(competition);
		when(competitionMapper.findTypeTagsByCompetitionIds(ids))
				.thenReturn(typeTags(competition.getId(), types));
		when(competitionMapper.findParticipantsByCompetitionIds(ids)).thenReturn(List.of());
		when(competitionMapper.findAwardRecipientsByCompetitionIds(ids)).thenReturn(List.of());
		when(competitionMapper.findAwardsByCompetitionIds(ids)).thenReturn(List.of());
	}

	private static void assertRepeatableReadOnly(java.lang.reflect.Method method) {
		Transactional transactional = method.getAnnotation(Transactional.class);
		assertNotNull(transactional);
		assertTrue(transactional.readOnly());
		assertEquals(Isolation.REPEATABLE_READ, transactional.isolation());
	}

	private static Competition activeCompetition(long id, String fullName) {
		return activeCompetition(id, fullName, CompetitionParticipationMode.TEAM);
	}

	private static Competition activeCompetition(long id, String fullName,
			CompetitionParticipationMode participationMode) {
		Competition competition = new Competition();
		competition.setId(id);
		competition.setFullName(fullName);
		competition.setActiveFullName(fullName);
		competition.setCompetitionYear(2026);
		competition.setParticipationMode(participationMode);
		competition.setCreateTime(Date.from(NOW.minusSeconds(3600)));
		return competition;
	}

	private static Competition deletedCompetition(long id, String fullName, Instant deletedAt) {
		Competition competition = activeCompetition(id, fullName);
		competition.setActiveFullName(null);
		competition.setDeletedAt(Date.from(deletedAt));
		return competition;
	}

	private static User user(String username, String nickname) {
		User user = new User();
		user.setUsername(username);
		user.setNickname(nickname);
		return user;
	}

	private static CompetitionParticipant participant(long id, long competitionId, String username) {
		CompetitionParticipant participant = new CompetitionParticipant();
		participant.setId(id);
		participant.setCompetitionId(competitionId);
		participant.setUsername(username);
		participant.setDisplayNameSnapshot(username);
		return participant;
	}

	private static List<CompetitionTypeTag> typeTags(long competitionId, CompetitionType... types) {
		return java.util.Arrays.stream(types).map(type -> {
			CompetitionTypeTag tag = new CompetitionTypeTag();
			tag.setCompetitionId(competitionId);
			tag.setType(type);
			return tag;
		}).toList();
	}

	private static CompetitionAwardCreateRequest award(String mode, String teamName, String tier,
			Integer rankPosition, Integer rankTotal, List<String> recipients) {
		return award(mode, teamName, tier, rankPosition, rankTotal, false, recipients);
	}

	private static CompetitionAwardCreateRequest award(String mode, String teamName, String tier,
			Integer rankPosition, Integer rankTotal, boolean requiresLogin, List<String> recipients) {
		return new CompetitionAwardCreateRequest(
				mode, teamName, tier, rankPosition, rankTotal, requiresLogin, recipients);
	}

	private static CompetitionAward competitionAward(long id, long competitionId, boolean requiresLogin) {
		CompetitionAward award = new CompetitionAward();
		award.setId(id);
		award.setCompetitionId(competitionId);
		award.setAwardMode(CompetitionAwardMode.TEAM);
		award.setAwardLevel(1);
		award.setAwardName("金牌");
		award.setRankPosition(1);
		award.setRankTotal(10);
		award.setRequiresLogin(requiresLogin);
		return award;
	}

	private static CompetitionAwardRecipient profileState(long competitionId, long awardId,
			boolean visible, Long order) {
		CompetitionAwardRecipient state = new CompetitionAwardRecipient();
		state.setCompetitionId(competitionId);
		state.setAwardId(awardId);
		state.setParticipantId(51L);
		state.setProfileVisible(visible);
		state.setProfileSortOrder(order);
		return state;
	}

	private static CompetitionAwardFlatProjection projection(CompetitionType type) {
		CompetitionAwardFlatProjection projection = new CompetitionAwardFlatProjection();
		projection.setCompetitionId(31L);
		projection.setCompetitionFullName("2026 ICPC 亚洲区域赛");
		projection.setCompetitionYear(2026);
		projection.setCompetitionType(type);
		projection.setAwardId(71L);
		projection.setAwardMode(CompetitionAwardMode.INDIVIDUAL);
		projection.setAwardScope(CompetitionAwardScope.NATIONAL);
		projection.setAwardLevel(1);
		projection.setAwardName("金奖");
		projection.setRequiresLogin(false);
		projection.setRankPosition(3);
		projection.setRankTotal(120);
		projection.setProfileVisible(true);
		projection.setProfileOrder(2L);
		return projection;
	}
}
