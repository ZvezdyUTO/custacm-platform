package top.naccl.controller;

import com.custacm.platform.trainingdata.common.app.query.OjWarehouseQueryFacade;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import top.naccl.controller.player.PlayerImageController;
import top.naccl.controller.player.TrainingDataQueryController;
import top.naccl.entity.ImageAsset;
import top.naccl.exception.ImageAssetException;
import top.naccl.service.ImageAssetCleanupJob;
import top.naccl.service.ImageAssetService;
import top.naccl.service.PlayerProfileService;
import top.naccl.service.TrainingUserQueryService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
class IntegratedControllerCoverageTest {
	@Test
	void delegatesPublicProfileLookup() {
		PlayerProfileService service = mock(PlayerProfileService.class);
		new PublicProfileController(service).get("alice");

		verify(service).getPublic("alice");
	}

	@Test
	void validatesImagePurposeAndDelegatesUploadAndDelete() {
		ImageAssetService service = mock(ImageAssetService.class);
		Authentication authentication = mock(Authentication.class);
		when(authentication.getName()).thenReturn("alice");
		MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1});
		PlayerImageController controller = new PlayerImageController(service);

		controller.upload(authentication, file, ImageAsset.Purpose.ARTICLE_COVER.name());
		controller.delete(authentication, 12L);

		verify(service).upload("alice", file, ImageAsset.Purpose.ARTICLE_COVER);
		verify(service).deleteUnbound("alice", 12L);
		ImageAssetException exception = assertThrows(ImageAssetException.class,
				() -> controller.upload(authentication, file, "not-a-purpose"));
		assertEquals(ImageAssetException.ErrorCode.IMAGE_FORMAT_UNSUPPORTED, exception.errorCode());
	}

	@Test
	void delegatesEveryTrainingQueryWithoutRewritingParameters() {
		OjWarehouseQueryFacade facade = mock(OjWarehouseQueryFacade.class);
		TrainingUserQueryService users = mock(TrainingUserQueryService.class);
		TrainingDataQueryController controller = new TrainingDataQueryController(facade, users);

		controller.users();
		controller.acceptedSummary("ATCODER", "alice", "from", "to", 800, 1600);
		controller.submissionsByUser("ATCODER", "alice", "from", "to", 800, 1600, 2, 20);
		controller.submissionsByProblem("ATCODER", "abc100_a", "from", "to", 3, 30);
		controller.firstAcceptedByUser("ATCODER", "alice", "from", "to", 800, 1600, 4, 40);
		controller.firstAcceptedByProblem("ATCODER", "abc100_a", "from", "to", 5, 50);

		verify(users).listCollectableUsers();
		verify(facade).summarizeAcceptedProblems("ATCODER", "alice", "from", "to", 800, 1600);
		verify(facade).listStudentSubmissions("ATCODER", "alice", "from", "to", 800, 1600, 2, 20);
		verify(facade).listProblemSubmissions("ATCODER", "abc100_a", "from", "to", 3, 30);
		verify(facade).summarizeStudentFirstAcceptedProblems(
				"ATCODER", "alice", "from", "to", 800, 1600, 4, 40);
		verify(facade).summarizeProblemFirstAcceptedHandles("ATCODER", "abc100_a", "from", "to", 5, 50);
	}

	@Test
	void scheduledCleanupDelegatesToAssetService() {
		ImageAssetService service = mock(ImageAssetService.class);

		new ImageAssetCleanupJob(service).cleanup();

		verify(service).cleanupStaleAssets();
	}
}
