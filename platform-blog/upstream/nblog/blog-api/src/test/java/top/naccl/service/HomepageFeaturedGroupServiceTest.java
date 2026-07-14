package top.naccl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.exception.BadRequestException;
import top.naccl.entity.Tag;
import top.naccl.model.dto.HomepageFeaturedGroupUpsertRequest;
import top.naccl.model.vo.HomepageFeaturedCandidate;
import top.naccl.model.vo.HomepageFeaturedGroup;
import top.naccl.repository.HomepageFeaturedGroupRepository;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class HomepageFeaturedGroupServiceTest {
	@Mock
	private HomepageFeaturedGroupRepository repository;
	@Mock
	private TagService tagService;

	private HomepageFeaturedGroupService service;

	@BeforeEach
	void setUp() {
		service = new HomepageFeaturedGroupService(repository, tagService);
	}

	@Test
	void publicListKeepsOnlyCompleteAvailableGroupsAndRendersDescriptions() {
		when(repository.findAllRows()).thenReturn(List.of(
				row(1L, "夏季训练", 0, 11L, 0, true, "**总结**"),
				row(1L, "夏季训练", 0, 12L, 1, true, "二"),
				row(1L, "夏季训练", 0, 13L, 2, true, "三"),
				row(2L, "往期精选", 1, 21L, 0, true, "一"),
				row(2L, "往期精选", 1, 22L, 1, false, "二"),
				row(2L, "往期精选", 1, 23L, 2, true, "三")
		));
		Tag tag = new Tag();
		tag.setId(9L);
		tag.setName("复盘");
		tag.setColor("#7c3aed");
		when(tagService.getTagListsByBlogIds(List.of(11L, 12L, 13L, 21L, 22L, 23L)))
				.thenReturn(Map.of(11L, List.of(tag)));

		List<HomepageFeaturedGroup> admin = service.listAdmin();
		List<HomepageFeaturedGroup> publicGroups = service.listPublic();

		assertEquals(2, admin.size());
		assertTrue(admin.getFirst().complete());
		assertFalse(admin.get(1).complete());
		assertTrue(admin.getFirst().articles().getFirst().description().contains("<strong>总结</strong>"));
		assertEquals("#7c3aed", admin.getFirst().articles().getFirst().tags().getFirst().getColor());
		assertEquals(List.of(1L), publicGroups.stream().map(HomepageFeaturedGroup::id).toList());
	}

	@Test
	void createsTrimmedThirdGroupWithExactlyThreeAvailableUnassignedArticles() {
		List<Long> articleIds = List.of(11L, 12L, 13L);
		when(repository.findAvailableBlogIds(articleIds)).thenReturn(new LinkedHashSet<>(articleIds));
		when(repository.findAssignments(articleIds)).thenReturn(Map.of());
		when(repository.countGroups()).thenReturn(2);
		when(repository.findGroupIdsInOrder()).thenReturn(List.of(1L, 2L));
		when(repository.insertGroup("第三组", 2)).thenReturn(3L);
		when(repository.findAllRows()).thenReturn(List.of());

		service.create(new HomepageFeaturedGroupUpsertRequest("  第三组  ", articleIds));

		verify(repository).insertGroup("第三组", 2);
		verify(repository).replaceArticles(3L, articleIds);
	}

	@Test
	void rejectsInvalidArticleCountAvailabilityReuseAndFourthGroup() {
		BadRequestException countError = assertThrows(BadRequestException.class,
				() -> service.create(new HomepageFeaturedGroupUpsertRequest("精选", List.of(1L, 2L))));
		assertEquals("每个精选组必须选择三篇不同的文章", countError.getMessage());

		List<Long> articleIds = List.of(1L, 2L, 3L);
		when(repository.findAvailableBlogIds(articleIds)).thenReturn(Set.of(1L, 2L));
		assertThrows(BadRequestException.class,
				() -> service.create(new HomepageFeaturedGroupUpsertRequest("精选", articleIds)));

		when(repository.findAvailableBlogIds(articleIds)).thenReturn(Set.copyOf(articleIds));
		when(repository.findAssignments(articleIds)).thenReturn(Map.of(2L, 9L));
		assertThrows(BadRequestException.class,
				() -> service.create(new HomepageFeaturedGroupUpsertRequest("精选", articleIds)));

		when(repository.findAssignments(articleIds)).thenReturn(Map.of());
		when(repository.countGroups()).thenReturn(3);
		BadRequestException groupError = assertThrows(BadRequestException.class,
				() -> service.create(new HomepageFeaturedGroupUpsertRequest("精选", articleIds)));
		assertEquals("首页最多创建三个精选组", groupError.getMessage());
	}

	@Test
	void updateAllowsArticlesAlreadyAssignedToTheSameGroup() {
		List<Long> articleIds = List.of(7L, 8L, 9L);
		when(repository.existsGroup(4L)).thenReturn(true);
		when(repository.findAvailableBlogIds(articleIds)).thenReturn(Set.copyOf(articleIds));
		when(repository.findAssignments(articleIds)).thenReturn(Map.of(7L, 4L, 8L, 4L, 9L, 4L));
		when(repository.updateGroup(4L, "新标题")).thenReturn(1);
		when(repository.findAllRows()).thenReturn(List.of());

		service.update(4L, new HomepageFeaturedGroupUpsertRequest("新标题", articleIds));

		verify(repository).replaceArticles(4L, articleIds);
	}

	@Test
	void reorderRequiresAnExactPermutationOfAllGroups() {
		when(repository.findGroupIdsInOrder()).thenReturn(List.of(1L, 2L, 3L));
		when(repository.findAllRows()).thenReturn(List.of());

		service.reorder(List.of(3L, 1L, 2L));

		verify(repository).replaceGroupOrder(List.of(3L, 1L, 2L));
		assertThrows(BadRequestException.class, () -> service.reorder(List.of(1L, 1L, 2L)));
	}

	@Test
	void candidatesAreLimitedAndExposeTheirCurrentGroup() {
		when(repository.findCandidates("训练", 50)).thenReturn(List.of(new HomepageFeaturedGroupRepository.CandidateRow(
				7L, "训练总结", "**简介**", "/cover.jpg", new Date(1), "题解",
				"alice", "Alice", "/avatar.png", 2, 4L)));

		List<HomepageFeaturedCandidate> result = service.candidates("  训练  ");

		assertEquals(4L, result.getFirst().featuredGroupId());
		assertEquals(2, result.getFirst().sortOrder());
		assertTrue(result.getFirst().available());
		assertTrue(result.getFirst().description().contains("<strong>简介</strong>"));
	}

	private static HomepageFeaturedGroupRepository.FeaturedRow row(
			long groupId, String groupTitle, int groupSortOrder,
			long articleId, int articleSortOrder, boolean available, String description) {
		return new HomepageFeaturedGroupRepository.FeaturedRow(
				groupId, groupTitle, groupSortOrder, articleId, "文章 " + articleId, description,
				"/cover.jpg", new Date(articleId), "题解", "alice", "Alice", "/avatar.png",
				articleSortOrder, available);
	}
}
