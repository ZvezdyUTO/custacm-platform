package top.naccl.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.entity.Tag;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.NotFoundException;
import top.naccl.model.dto.HomepageFeaturedGroupUpsertRequest;
import top.naccl.model.vo.HomepageFeaturedArticle;
import top.naccl.model.vo.HomepageFeaturedCandidate;
import top.naccl.model.vo.HomepageFeaturedGroup;
import top.naccl.repository.HomepageFeaturedGroupRepository;
import top.naccl.util.markdown.MarkdownUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 首页精选文章组的完整性、可见性与排序用例。
 *
 * @author huangbingrui.awa
 */
@Service
public class HomepageFeaturedGroupService {
	static final int MAX_GROUP_COUNT = 3;
	static final int ARTICLE_COUNT_PER_GROUP = 3;
	static final int MAX_TITLE_CODE_POINTS = 100;
	static final int CANDIDATE_LIMIT = 50;

	private final HomepageFeaturedGroupRepository repository;
	private final TagService tagService;

	public HomepageFeaturedGroupService(HomepageFeaturedGroupRepository repository, TagService tagService) {
		this.repository = repository;
		this.tagService = tagService;
	}

	/**
	 * 公开页只返回当前仍由三篇公开已发布文章组成的完整分组。
	 */
	@Transactional(readOnly = true)
	public List<HomepageFeaturedGroup> listPublic() {
		return listAdmin().stream().filter(HomepageFeaturedGroup::complete).toList();
	}

	@Transactional(readOnly = true)
	public List<HomepageFeaturedGroup> listAdmin() {
		List<HomepageFeaturedGroupRepository.FeaturedRow> rows = repository.findAllRows();
		List<Long> articleIds = rows.stream()
				.map(HomepageFeaturedGroupRepository.FeaturedRow::articleId)
				.filter(java.util.Objects::nonNull)
				.distinct()
				.toList();
		Map<Long, List<Tag>> tagsByBlogId = articleIds.isEmpty()
				? Map.of()
				: tagService.getTagListsByBlogIds(articleIds);
		Map<Long, GroupAccumulator> groups = new LinkedHashMap<>();
		for (HomepageFeaturedGroupRepository.FeaturedRow row : rows) {
			GroupAccumulator group = groups.computeIfAbsent(row.groupId(), ignored ->
					new GroupAccumulator(row.groupId(), row.groupTitle(), row.groupSortOrder()));
			if (row.articleId() != null) {
				group.articles.add(new HomepageFeaturedArticle(
						row.articleId(),
						row.articleTitle(),
						renderDescription(row.articleDescription()),
						row.firstPicture(),
						row.createTime(),
						row.categoryName(),
						row.authorUsername(),
						row.authorNickname(),
						row.authorAvatar(),
						tagsByBlogId.getOrDefault(row.articleId(), List.of()),
						row.articleSortOrder(),
						row.available()));
			}
		}
		return groups.values().stream().map(GroupAccumulator::response).toList();
	}

	@Transactional(readOnly = true)
	public List<HomepageFeaturedCandidate> candidates(String query) {
		String normalizedQuery = query == null ? "" : query.strip();
		return repository.findCandidates(normalizedQuery, CANDIDATE_LIMIT).stream()
				.map(row -> new HomepageFeaturedCandidate(
						row.id(),
						row.title(),
						renderDescription(row.description()),
						row.firstPicture(),
						row.createTime(),
						row.categoryName(),
						row.authorUsername(),
						row.authorNickname(),
						row.authorAvatar(),
						row.sortOrder(),
						true,
						row.featuredGroupId()))
				.toList();
	}

	@Transactional(rollbackFor = Exception.class)
	public List<HomepageFeaturedGroup> create(HomepageFeaturedGroupUpsertRequest request) {
		NormalizedRequest normalized = validateRequest(request, null);
		if (repository.countGroups() >= MAX_GROUP_COUNT) {
			throw new BadRequestException("首页最多创建三个精选组");
		}
		try {
			long groupId = repository.insertGroup(normalized.title(), repository.findGroupIdsInOrder().size());
			repository.replaceArticles(groupId, normalized.articleIds());
		} catch (DataIntegrityViolationException exception) {
			throw new BadRequestException("精选组数量或文章选择已发生变化，请刷新后重试", exception);
		}
		return listAdmin();
	}

	@Transactional(rollbackFor = Exception.class)
	public List<HomepageFeaturedGroup> update(long id, HomepageFeaturedGroupUpsertRequest request) {
		if (!repository.existsGroup(id)) {
			throw new NotFoundException("首页精选组不存在");
		}
		NormalizedRequest normalized = validateRequest(request, id);
		try {
			if (repository.updateGroup(id, normalized.title()) != 1) {
				throw new NotFoundException("首页精选组不存在");
			}
			repository.replaceArticles(id, normalized.articleIds());
		} catch (DataIntegrityViolationException exception) {
			throw new BadRequestException("精选文章选择已发生变化，请刷新后重试", exception);
		}
		return listAdmin();
	}

	@Transactional(rollbackFor = Exception.class)
	public List<HomepageFeaturedGroup> delete(long id) {
		if (repository.deleteGroup(id) != 1) {
			throw new NotFoundException("首页精选组不存在");
		}
		repository.replaceGroupOrder(repository.findGroupIdsInOrder());
		return listAdmin();
	}

	@Transactional(rollbackFor = Exception.class)
	public List<HomepageFeaturedGroup> reorder(List<Long> ids) {
		List<Long> currentIds = repository.findGroupIdsInOrder();
		Set<Long> requestedIds = ids == null ? Set.of() : new HashSet<>(ids);
		if (ids == null || ids.size() != currentIds.size() || requestedIds.size() != ids.size()
				|| !requestedIds.equals(new HashSet<>(currentIds))) {
			throw new BadRequestException("排序必须包含全部精选组，且不能重复");
		}
		repository.replaceGroupOrder(ids);
		return listAdmin();
	}

	private NormalizedRequest validateRequest(HomepageFeaturedGroupUpsertRequest request, Long currentGroupId) {
		if (request == null || request.title() == null) {
			throw new BadRequestException("精选组标题不能为空");
		}
		String title = request.title().strip();
		if (title.isEmpty()) {
			throw new BadRequestException("精选组标题不能为空");
		}
		if (title.codePointCount(0, title.length()) > MAX_TITLE_CODE_POINTS) {
			throw new BadRequestException("精选组标题不能超过 100 个字符");
		}
		List<Long> articleIds = request.articleIds();
		if (articleIds == null || articleIds.size() != ARTICLE_COUNT_PER_GROUP
				|| articleIds.stream().anyMatch(java.util.Objects::isNull)
				|| new HashSet<>(articleIds).size() != ARTICLE_COUNT_PER_GROUP) {
			throw new BadRequestException("每个精选组必须选择三篇不同的文章");
		}
		Set<Long> requestedIds = new HashSet<>(articleIds);
		if (!repository.findAvailableBlogIds(articleIds).equals(requestedIds)) {
			throw new BadRequestException("精选组只能选择公开、已发布且未进入回收站的文章");
		}
		Map<Long, Long> assignments = repository.findAssignments(articleIds);
		boolean assignedElsewhere = assignments.values().stream()
				.anyMatch(groupId -> currentGroupId == null || !currentGroupId.equals(groupId));
		if (assignedElsewhere) {
			throw new BadRequestException("同一篇文章不能重复出现在多个首页精选组中");
		}
		return new NormalizedRequest(title, List.copyOf(articleIds));
	}

	private static String renderDescription(String description) {
		return MarkdownUtils.markdownToHtmlExtensions(description == null ? "" : description);
	}

	private record NormalizedRequest(String title, List<Long> articleIds) {
	}

	private static final class GroupAccumulator {
		private final Long id;
		private final String title;
		private final Integer sortOrder;
		private final List<HomepageFeaturedArticle> articles = new ArrayList<>(ARTICLE_COUNT_PER_GROUP);

		private GroupAccumulator(Long id, String title, Integer sortOrder) {
			this.id = id;
			this.title = title;
			this.sortOrder = sortOrder;
		}

		private HomepageFeaturedGroup response() {
			boolean complete = articles.size() == ARTICLE_COUNT_PER_GROUP
					&& articles.stream().allMatch(HomepageFeaturedArticle::available);
			return new HomepageFeaturedGroup(id, title, sortOrder, complete, List.copyOf(articles));
		}
	}
}
