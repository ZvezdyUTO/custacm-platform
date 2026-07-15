package top.naccl.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import top.naccl.controller.admin.CompetitionAdminController;
import top.naccl.controller.player.PlayerCompetitionController;
import top.naccl.model.dto.CompetitionAchievementOrderRequest;
import top.naccl.model.dto.CompetitionAchievementVisibilityRequest;
import top.naccl.model.dto.CompetitionAwardCreateRequest;
import top.naccl.model.dto.CompetitionAwardLoginRequirementRequest;
import top.naccl.model.dto.CompetitionCreateRequest;
import top.naccl.model.dto.CompetitionParticipantsCreateRequest;
import top.naccl.model.vo.CompetitionPageResponse;
import top.naccl.model.vo.CompetitionResponse;
import top.naccl.service.CompetitionService;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class CompetitionControllerTest {
	@Mock
	private CompetitionService competitionService;
	@Mock
	private Authentication authentication;

	@Test
	void publicControllerDelegatesListAndDetailQueries() {
		CompetitionPageResponse page = new CompetitionPageResponse(2, 20, 1, 1, List.of(response()));
		CompetitionResponse detail = response();
		when(competitionService.list(2024, 2026, "ICPC_ASIA_REGIONAL", 2, 20, true)).thenReturn(page);
		when(competitionService.get(31L, true)).thenReturn(detail);
		CompetitionController controller = new CompetitionController(competitionService);

		assertSame(page, controller.list(
				2024, 2026, "ICPC_ASIA_REGIONAL", 2, 20, authentication).getData());
		assertSame(detail, controller.get(31L, authentication).getData());

		verify(competitionService).list(2024, 2026, "ICPC_ASIA_REGIONAL", 2, 20, true);
		verify(competitionService).get(31L, true);
	}

	@Test
	void publicControllerTreatsMissingAuthenticationAsGuest() {
		CompetitionPageResponse page = new CompetitionPageResponse(1, 10, 0, 0, List.of());
		when(competitionService.list(null, null, null, 1, 10, false)).thenReturn(page);
		CompetitionController controller = new CompetitionController(competitionService);

		assertSame(page, controller.list(null, null, null, 1, 10, null).getData());

		verify(competitionService).list(null, null, null, 1, 10, false);
	}

	@Test
	void adminControllerDelegatesCompetitionLifecycleParticipantsAndAwards() {
		CompetitionAdminController controller = new CompetitionAdminController(competitionService);
		CompetitionCreateRequest createRequest = new CompetitionCreateRequest(
				"2026 ICPC 亚洲区域赛", LocalDate.of(2026, 5, 17),
				"ICPC_ASIA_REGIONAL", "TEAM");
		CompetitionParticipantsCreateRequest participantsRequest =
				new CompetitionParticipantsCreateRequest(List.of("alice", "bob"));
		CompetitionAwardCreateRequest awardRequest = new CompetitionAwardCreateRequest(
				"TEAM", "custacm", "MEDAL_GOLD", 3, 120, true, List.of("alice", "bob"));
		CompetitionAwardLoginRequirementRequest loginRequirementRequest =
				new CompetitionAwardLoginRequirementRequest(true);
		CompetitionPageResponse recycleBin = new CompetitionPageResponse(1, 10, 1, 1, List.of(response()));
		CompetitionResponse response = response();
		when(competitionService.list(2024, 2026, "ICPC_ASIA_REGIONAL", 1, 10, true))
				.thenReturn(recycleBin);
		when(competitionService.create(createRequest)).thenReturn(response);
		when(competitionService.listRecycleBin(2024, 2026, "ICPC_ASIA_REGIONAL", 1, 10)).thenReturn(recycleBin);
		when(competitionService.restore(31L)).thenReturn(response);
		when(competitionService.addParticipants(31L, participantsRequest)).thenReturn(response);
		when(competitionService.addAward(31L, awardRequest)).thenReturn(response);
		when(competitionService.updateAwardLoginRequirement(31L, 71L, loginRequirementRequest))
				.thenReturn(response);

		assertSame(recycleBin,
				controller.list(2024, 2026, "ICPC_ASIA_REGIONAL", 1, 10).getData());
		assertSame(response, controller.create(createRequest).getData());
		assertSame(recycleBin, controller.recycleBin(2024, 2026, "ICPC_ASIA_REGIONAL", 1, 10).getData());
		controller.delete(31L);
		assertSame(response, controller.restore(31L).getData());
		assertSame(response, controller.addParticipants(31L, participantsRequest).getData());
		controller.deleteParticipant(31L, 51L);
		assertSame(response, controller.addAward(31L, awardRequest).getData());
		assertSame(response, controller.updateAwardLoginRequirement(
				31L, 71L, loginRequirementRequest).getData());
		controller.deleteAward(31L, 71L);

		verify(competitionService).list(2024, 2026, "ICPC_ASIA_REGIONAL", 1, 10, true);
		verify(competitionService).create(createRequest);
		verify(competitionService).listRecycleBin(2024, 2026, "ICPC_ASIA_REGIONAL", 1, 10);
		verify(competitionService).moveToRecycleBin(31L);
		verify(competitionService).restore(31L);
		verify(competitionService).addParticipants(31L, participantsRequest);
		verify(competitionService).deleteParticipant(31L, 51L);
		verify(competitionService).addAward(31L, awardRequest);
		verify(competitionService).updateAwardLoginRequirement(31L, 71L, loginRequirementRequest);
		verify(competitionService).deleteAward(31L, 71L);
	}

	@Test
	void playerControllerUsesAuthenticatedUsernameForArticleBindingAndUnbinding() {
		when(authentication.getName()).thenReturn("alice");
		PlayerCompetitionController controller = new PlayerCompetitionController(competitionService);
		CompetitionAchievementVisibilityRequest visibilityRequest =
				new CompetitionAchievementVisibilityRequest(true);
		CompetitionAchievementOrderRequest orderRequest =
				new CompetitionAchievementOrderRequest(List.of(72L, 71L));

		controller.bindArticle(authentication, 31L, 81L);
		controller.unbindArticle(authentication, 31L, 81L);
		controller.updateAchievementVisibility(authentication, 31L, 71L, visibilityRequest);
		controller.updateAchievementOrder(authentication, orderRequest);

		verify(competitionService).bindArticle("alice", 31L, 81L);
		verify(competitionService).unbindArticle("alice", 31L, 81L);
		verify(competitionService).updateAchievementVisibility("alice", 31L, 71L, visibilityRequest);
		verify(competitionService).updateAchievementOrder("alice", orderRequest);
	}

	private static CompetitionResponse response() {
		return new CompetitionResponse(
				31L,
				"2026 ICPC 亚洲区域赛",
				2026,
				LocalDate.of(2026, 5, 17),
				"ICPC_ASIA_REGIONAL",
				"ICPC 亚洲区域赛",
				"TEAM",
				"团队",
				List.of(new CompetitionResponse.Type("ICPC", "ICPC")),
				new Date(0),
				null,
				List.of(),
				List.of()
		);
	}
}
