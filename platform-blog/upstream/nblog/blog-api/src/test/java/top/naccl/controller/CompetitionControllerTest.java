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
import top.naccl.model.dto.CompetitionCreateRequest;
import top.naccl.model.dto.CompetitionParticipantsCreateRequest;
import top.naccl.model.vo.CompetitionPageResponse;
import top.naccl.model.vo.CompetitionResponse;
import top.naccl.service.CompetitionService;

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
		when(competitionService.list(2024, 2026, "ICPC_ASIA_REGIONAL", 2, 20)).thenReturn(page);
		when(competitionService.get(31L)).thenReturn(detail);
		CompetitionController controller = new CompetitionController(competitionService);

		assertSame(page, controller.list(2024, 2026, "ICPC_ASIA_REGIONAL", 2, 20).getData());
		assertSame(detail, controller.get(31L).getData());

		verify(competitionService).list(2024, 2026, "ICPC_ASIA_REGIONAL", 2, 20);
		verify(competitionService).get(31L);
	}

	@Test
	void adminControllerDelegatesCompetitionLifecycleParticipantsAndAwards() {
		CompetitionAdminController controller = new CompetitionAdminController(competitionService);
		CompetitionCreateRequest createRequest = new CompetitionCreateRequest(
				"2026 ICPC 亚洲区域赛", 2026, "ICPC_ASIA_REGIONAL", "TEAM");
		CompetitionParticipantsCreateRequest participantsRequest =
				new CompetitionParticipantsCreateRequest(List.of("alice", "bob"));
		CompetitionAwardCreateRequest awardRequest = new CompetitionAwardCreateRequest(
				"TEAM", "custacm", "MEDAL_GOLD", 3, 120, List.of("alice", "bob"));
		CompetitionPageResponse recycleBin = new CompetitionPageResponse(1, 10, 1, 1, List.of(response()));
		CompetitionResponse response = response();
		when(competitionService.create(createRequest)).thenReturn(response);
		when(competitionService.listRecycleBin(2024, 2026, "ICPC_ASIA_REGIONAL", 1, 10)).thenReturn(recycleBin);
		when(competitionService.restore(31L)).thenReturn(response);
		when(competitionService.addParticipants(31L, participantsRequest)).thenReturn(response);
		when(competitionService.addAward(31L, awardRequest)).thenReturn(response);

		assertSame(response, controller.create(createRequest).getData());
		assertSame(recycleBin, controller.recycleBin(2024, 2026, "ICPC_ASIA_REGIONAL", 1, 10).getData());
		controller.delete(31L);
		assertSame(response, controller.restore(31L).getData());
		assertSame(response, controller.addParticipants(31L, participantsRequest).getData());
		controller.deleteParticipant(31L, 51L);
		assertSame(response, controller.addAward(31L, awardRequest).getData());
		controller.deleteAward(31L, 71L);

		verify(competitionService).create(createRequest);
		verify(competitionService).listRecycleBin(2024, 2026, "ICPC_ASIA_REGIONAL", 1, 10);
		verify(competitionService).moveToRecycleBin(31L);
		verify(competitionService).restore(31L);
		verify(competitionService).addParticipants(31L, participantsRequest);
		verify(competitionService).deleteParticipant(31L, 51L);
		verify(competitionService).addAward(31L, awardRequest);
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
