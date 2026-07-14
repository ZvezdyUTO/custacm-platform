package top.naccl.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.entity.Competition;
import top.naccl.entity.CompetitionArticle;
import top.naccl.entity.CompetitionAward;
import top.naccl.entity.CompetitionAwardFlatProjection;
import top.naccl.entity.CompetitionAwardRecipient;
import top.naccl.entity.CompetitionParticipant;
import top.naccl.entity.CompetitionTypeTag;
import top.naccl.entity.User;
import top.naccl.enums.CompetitionAwardMode;
import top.naccl.enums.CompetitionAwardScope;
import top.naccl.enums.CompetitionParticipationMode;
import top.naccl.enums.CompetitionType;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.ForbiddenException;
import top.naccl.exception.NotFoundException;
import top.naccl.exception.PersistenceException;
import top.naccl.mapper.CompetitionMapper;
import top.naccl.mapper.UserMapper;
import top.naccl.model.dto.CompetitionAchievementVisibilityRequest;
import top.naccl.model.dto.CompetitionAwardCreateRequest;
import top.naccl.model.dto.CompetitionCreateRequest;
import top.naccl.model.dto.CompetitionParticipantsCreateRequest;
import top.naccl.model.vo.CompetitionAchievement;
import top.naccl.model.vo.CompetitionPageResponse;
import top.naccl.model.vo.CompetitionResponse;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 比赛记录聚合用例。比赛根进入七天回收站，参赛人和奖项只允许添加或删除。
 *
 * @author huangbingrui.awa
 */
@Service
public class CompetitionService {
	public static final Duration RETENTION = Duration.ofDays(7);

	private static final int MIN_YEAR = 1900;
	private static final int MAX_YEAR = 9999;
	private static final int MAX_PAGE_SIZE = 100;
	private static final int MAX_FULL_NAME_LENGTH = 255;
	private static final int MAX_PARTICIPANTS_PER_REQUEST = 100;
	private static final int MAX_AWARD_TEXT_LENGTH = 255;
	private static final Pattern USERNAME = Pattern.compile("[\\p{L}\\p{N}._-]{1,128}");

	private final CompetitionMapper competitionMapper;
	private final UserMapper userMapper;
	private final Clock clock;

	@Autowired
	public CompetitionService(CompetitionMapper competitionMapper, UserMapper userMapper) {
		this(competitionMapper, userMapper, Clock.systemUTC());
	}

	CompetitionService(CompetitionMapper competitionMapper, UserMapper userMapper, Clock clock) {
		this.competitionMapper = competitionMapper;
		this.userMapper = userMapper;
		this.clock = clock;
	}

	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public CompetitionPageResponse list(Integer startYear, Integer endYear, String type,
			Integer pageNum, Integer pageSize) {
		Query query = query(startYear, endYear, type, pageNum, pageSize);
		PageHelper.startPage(query.pageNum(), query.pageSize());
		List<Competition> competitions = competitionMapper.findActiveCompetitions(
				query.startYear(), query.endYear(), query.type());
		return page(new PageInfo<>(competitions), assemble(competitions));
	}

	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public CompetitionPageResponse listRecycleBin(Integer startYear, Integer endYear, String type,
			Integer pageNum, Integer pageSize) {
		Query query = query(startYear, endYear, type, pageNum, pageSize);
		PageHelper.startPage(query.pageNum(), query.pageSize());
		List<Competition> competitions = competitionMapper.findRecycleBinCompetitions(
				query.startYear(), query.endYear(), query.type(), cutoff());
		return page(new PageInfo<>(competitions), assemble(competitions));
	}

	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public CompetitionResponse get(Long id) {
		Competition competition = id == null ? null : competitionMapper.findActiveCompetitionById(id);
		if (competition == null) {
			throw new NotFoundException("比赛不存在");
		}
		return assemble(List.of(competition)).getFirst();
	}

	@Transactional(rollbackFor = Exception.class)
	public CompetitionResponse create(CompetitionCreateRequest request) {
		if (request == null) {
			throw new BadRequestException("请求体不能为空");
		}
		String fullName = normalizeRequiredText(request.fullName(), MAX_FULL_NAME_LENGTH, "比赛全称");
		Integer year = requireYear(request.year(), "比赛年份");
		List<CompetitionType> types = normalizeTypes(request.types());
		CompetitionParticipationMode participationMode = participationMode(request.participationMode());
		if (competitionMapper.findActiveCompetitionByFullName(fullName) != null) {
			throw new BadRequestException("比赛全称已存在");
		}

		Competition competition = new Competition();
		competition.setFullName(fullName);
		competition.setActiveFullName(fullName);
		competition.setCompetitionYear(year);
		competition.setParticipationMode(participationMode);
		try {
			if (competitionMapper.insertCompetition(competition) != 1) {
				throw new PersistenceException("比赛创建失败");
			}
			if (competitionMapper.insertTypeTags(competition.getId(), types) != types.size()) {
				throw new PersistenceException("比赛类型保存失败");
			}
		} catch (DuplicateKeyException exception) {
			throw new BadRequestException("比赛全称已存在", exception);
		}
		return requireActiveResponse(competition.getId());
	}

	@Transactional(rollbackFor = Exception.class)
	public void moveToRecycleBin(Long id) {
		requireActiveLocked(id);
		if (competitionMapper.moveCompetitionToRecycleBin(id, now()) != 1) {
			throw new PersistenceException("比赛移入回收站失败");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public CompetitionResponse restore(Long id) {
		Competition competition = requireLocked(id);
		Date cutoff = cutoff();
		if (competition.getDeletedAt() == null || !competition.getDeletedAt().after(cutoff)) {
			throw new NotFoundException("比赛不存在或已超过七天恢复期限");
		}
		if (competitionMapper.findActiveCompetitionByFullName(competition.getFullName()) != null) {
			throw new BadRequestException("已有同名比赛，无法恢复该回收站记录");
		}
		try {
			if (competitionMapper.restoreCompetition(id, cutoff) != 1) {
				throw new NotFoundException("比赛不存在或已超过七天恢复期限");
			}
		} catch (DuplicateKeyException exception) {
			throw new BadRequestException("已有同名比赛，无法恢复该回收站记录", exception);
		}
		return requireActiveResponse(id);
	}

	@Transactional(rollbackFor = Exception.class)
	public int purgeExpired() {
		List<Long> competitionIds = competitionMapper.findExpiredCompetitionIdsForUpdate(cutoff());
		for (Long competitionId : competitionIds) {
			if (competitionMapper.deleteCompetitionById(competitionId) != 1) {
				throw new PersistenceException("回收站比赛清理失败");
			}
		}
		return competitionIds.size();
	}

	@Transactional(rollbackFor = Exception.class)
	public CompetitionResponse addParticipants(Long competitionId,
			CompetitionParticipantsCreateRequest request) {
		requireActiveLocked(competitionId);
		List<String> usernames = normalizeUsernames(request == null ? null : request.usernames());
		List<User> users = userMapper.findByUsernames(usernames);
		Map<String, User> usersByUsername = users.stream()
				.collect(Collectors.toMap(User::getUsername, Function.identity()));
		List<String> missing = usernames.stream().filter(username -> !usersByUsername.containsKey(username)).toList();
		if (!missing.isEmpty()) {
			throw new BadRequestException("参赛用户不存在：" + String.join("、", missing));
		}
		List<CompetitionParticipant> existing = competitionMapper
				.findParticipantsByCompetitionIdAndUsernames(competitionId, usernames);
		if (!existing.isEmpty()) {
			throw new BadRequestException("用户已在该比赛的参赛名单中：" + existing.stream()
					.map(CompetitionParticipant::getUsername).collect(Collectors.joining("、")));
		}

		List<CompetitionParticipant> participants = usernames.stream().map(username -> {
			User user = usersByUsername.get(username);
			CompetitionParticipant participant = new CompetitionParticipant();
			participant.setCompetitionId(competitionId);
			participant.setUsername(user.getUsername());
			participant.setDisplayNameSnapshot(displayName(user));
			return participant;
		}).toList();
		try {
			if (competitionMapper.insertParticipants(participants) != participants.size()) {
				throw new PersistenceException("参赛队员添加失败");
			}
		} catch (DuplicateKeyException exception) {
			throw new BadRequestException("参赛队员已存在", exception);
		}
		return requireActiveResponse(competitionId);
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteParticipant(Long competitionId, Long participantId) {
		requireActiveLocked(competitionId);
		CompetitionParticipant participant = participantId == null
				? null : competitionMapper.findParticipantByIdForUpdate(participantId);
		if (participant == null || !competitionId.equals(participant.getCompetitionId())) {
			throw new NotFoundException("参赛队员不存在");
		}
		if (competitionMapper.countAwardReferencesByParticipantId(participantId) > 0) {
			throw new BadRequestException("请先删除绑定该队员的奖项");
		}
		if (competitionMapper.deleteParticipant(competitionId, participantId) != 1) {
			throw new PersistenceException("参赛队员删除失败");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public CompetitionResponse addAward(Long competitionId, CompetitionAwardCreateRequest request) {
		requireActiveLocked(competitionId);
		NormalizedAward normalized = normalizeAward(competitionId, request);
		List<CompetitionParticipant> recipients = competitionMapper
				.findParticipantsByCompetitionIdAndUsernames(competitionId, normalized.recipientUsernames());
		Map<String, CompetitionParticipant> recipientsByUsername = recipients.stream()
				.collect(Collectors.toMap(CompetitionParticipant::getUsername, Function.identity()));
		List<String> missing = normalized.recipientUsernames().stream()
				.filter(username -> !recipientsByUsername.containsKey(username)).toList();
		if (!missing.isEmpty()) {
			throw new BadRequestException("获奖人尚未加入该比赛：" + String.join("、", missing));
		}

		CompetitionAward award = new CompetitionAward();
		award.setCompetitionId(competitionId);
		award.setAwardMode(normalized.mode());
		award.setTeamName(normalized.teamName());
		award.setAwardScope(normalized.scope());
		award.setAwardLevel(normalized.level());
		award.setRankPosition(normalized.rankPosition());
		award.setRankTotal(normalized.rankTotal());
		award.setAwardName(normalized.awardName());
		if (competitionMapper.insertAward(award) != 1) {
			throw new PersistenceException("奖项添加失败");
		}
		List<CompetitionAwardRecipient> recipientRows = normalized.recipientUsernames().stream()
				.map(username -> {
					CompetitionAwardRecipient recipient = new CompetitionAwardRecipient();
					recipient.setCompetitionId(competitionId);
					recipient.setAwardId(award.getId());
					recipient.setParticipantId(recipientsByUsername.get(username).getId());
					return recipient;
				})
				.toList();
		if (competitionMapper.insertAwardRecipients(recipientRows) != recipientRows.size()) {
			throw new PersistenceException("奖项获奖人保存失败");
		}
		return requireActiveResponse(competitionId);
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteAward(Long competitionId, Long awardId) {
		requireActiveLocked(competitionId);
		CompetitionAward award = awardId == null ? null : competitionMapper.findAwardByIdForUpdate(awardId);
		if (award == null || !competitionId.equals(award.getCompetitionId())) {
			throw new NotFoundException("奖项不存在");
		}
		if (competitionMapper.deleteAward(competitionId, awardId) != 1) {
			throw new PersistenceException("奖项删除失败");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void bindArticle(String username, Long competitionId, Long blogId) {
		requireActiveLocked(competitionId);
		CompetitionParticipant participant = requireActiveParticipant(competitionId, username);
		try {
			if (competitionMapper.bindOwnedPublicArticle(participant.getId(), username, blogId) != 1) {
				throw new BadRequestException("只能绑定本人已发布、公开且未删除的文章");
			}
		} catch (DuplicateKeyException exception) {
			throw new BadRequestException("该文章已绑定到这场比赛", exception);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void unbindArticle(String username, Long competitionId, Long blogId) {
		requireActiveLocked(competitionId);
		CompetitionParticipant participant = requireActiveParticipant(competitionId, username);
		if (competitionMapper.unbindOwnedArticle(participant.getId(), username, blogId) != 1) {
			throw new NotFoundException("文章绑定不存在");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateAchievementVisibility(String username, Long competitionId, Long awardId,
			CompetitionAchievementVisibilityRequest request) {
		String normalizedUsername = normalizeUsername(username);
		if (request == null || request.visible() == null) {
			throw new BadRequestException("是否展示不能为空");
		}
		requireActiveLocked(competitionId);
		Boolean current = competitionMapper.findAchievementProfileVisibility(
				competitionId, awardId, normalizedUsername);
		if (current == null) {
			throw new NotFoundException("本人获奖记录不存在");
		}
		if (current.equals(request.visible())) {
			return;
		}
		if (competitionMapper.updateAchievementProfileVisibility(
				competitionId, awardId, normalizedUsername, request.visible()) != 1) {
			throw new PersistenceException("奖项展示状态更新失败");
		}
	}

	@Transactional(readOnly = true)
	public List<CompetitionAchievement> achievements(String username) {
		return achievements(username, false);
	}

	@Transactional(readOnly = true)
	public List<CompetitionAchievement> publicAchievements(String username) {
		return achievements(username, true);
	}

	private List<CompetitionAchievement> achievements(String username, boolean visibleOnly) {
		if (username == null || username.isBlank()) {
			return List.of();
		}
		Map<Long, AchievementAccumulator> byAward = new LinkedHashMap<>();
		for (CompetitionAwardFlatProjection row : competitionMapper.findActiveAwardProjectionsByUsername(username)) {
			if (visibleOnly && !Boolean.TRUE.equals(row.getProfileVisible())) {
				continue;
			}
			AchievementAccumulator accumulator = byAward.computeIfAbsent(row.getAwardId(), ignored ->
					new AchievementAccumulator(row));
			if (row.getCompetitionType() != null) {
				accumulator.types.add(row.getCompetitionType());
			}
		}
		return byAward.values().stream().map(AchievementAccumulator::response).toList();
	}

	private CompetitionResponse requireActiveResponse(Long id) {
		Competition competition = competitionMapper.findActiveCompetitionById(id);
		if (competition == null) {
			throw new PersistenceException("比赛读取失败");
		}
		return assemble(List.of(competition)).getFirst();
	}

	private Competition requireActiveLocked(Long id) {
		Competition competition = requireLocked(id);
		if (competition.getDeletedAt() != null) {
			throw new NotFoundException("比赛不存在或已在回收站");
		}
		return competition;
	}

	private Competition requireLocked(Long id) {
		Competition competition = id == null ? null : competitionMapper.findCompetitionByIdForUpdate(id);
		if (competition == null) {
			throw new NotFoundException("比赛不存在");
		}
		return competition;
	}

	private CompetitionParticipant requireActiveParticipant(Long competitionId, String username) {
		String normalizedUsername = normalizeUsername(username);
		List<CompetitionParticipant> participants = competitionMapper
				.findParticipantsByCompetitionIdAndUsernames(competitionId, List.of(normalizedUsername));
		if (participants.isEmpty()) {
			throw new ForbiddenException("只有该比赛的参赛队员可以管理文章绑定");
		}
		return participants.getFirst();
	}

	private NormalizedAward normalizeAward(Long competitionId, CompetitionAwardCreateRequest request) {
		if (request == null) {
			throw new BadRequestException("请求体不能为空");
		}
		CompetitionAwardMode mode = enumValue(request.awardMode(), CompetitionAwardMode.class, "奖项归属形态");
		CompetitionAwardScope scope = nullableEnumValue(
				request.awardScope(), CompetitionAwardScope.class, "奖项级别范围");
		Integer level = request.awardLevel();
		if (level == null || level < 1 || level > 4) {
			throw new BadRequestException("奖项等级只能是 1、2、3、4");
		}
		Integer rankPosition = request.rankPosition();
		Integer rankTotal = request.rankTotal();
		if (rankPosition == null || rankTotal == null || rankPosition <= 0 || rankTotal <= 0
				|| rankPosition > rankTotal) {
			throw new BadRequestException("rank 必须满足 1 <= x <= y");
		}
		List<String> recipients = normalizeUsernames(request.recipientUsernames());
		if (mode == CompetitionAwardMode.INDIVIDUAL && recipients.size() != 1) {
			throw new BadRequestException("个人奖项必须且只能绑定一名参赛队员");
		}
		if (mode == CompetitionAwardMode.TEAM && recipients.size() < 2) {
			throw new BadRequestException("团队奖项至少需要绑定两名参赛队员");
		}
		String teamName = normalizeOptionalText(request.teamName(), MAX_AWARD_TEXT_LENGTH, "队伍名称");
		if (mode == CompetitionAwardMode.INDIVIDUAL && teamName != null) {
			throw new BadRequestException("个人奖项不能填写队伍名称");
		}
		String awardName = normalizeOptionalText(request.awardName(), MAX_AWARD_TEXT_LENGTH, "奖项名称");

		Set<CompetitionType> types = competitionMapper.findTypeTagsByCompetitionIds(List.of(competitionId)).stream()
				.map(CompetitionTypeTag::getType).collect(Collectors.toSet());
		if (types.contains(CompetitionType.LANQIAO_CUP) && scope != CompetitionAwardScope.NATIONAL) {
			throw new BadRequestException("蓝桥杯仅记录国家级奖项");
		}
		if ((types.contains(CompetitionType.BAIDU_STAR) || types.contains(CompetitionType.GPLT))
				&& scope == null) {
			throw new BadRequestException("百度之星和团体程序设计天梯赛奖项必须区分省级或国家级");
		}
		return new NormalizedAward(mode, teamName, scope, level, awardName,
				rankPosition, rankTotal, recipients);
	}

	private List<CompetitionResponse> assemble(List<Competition> competitions) {
		if (competitions.isEmpty()) {
			return List.of();
		}
		List<Long> competitionIds = competitions.stream().map(Competition::getId).toList();
		Map<Long, List<CompetitionResponse.Type>> typesByCompetition = competitionMapper
				.findTypeTagsByCompetitionIds(competitionIds).stream()
				.sorted(Comparator.comparingInt(tag -> tag.getType().ordinal()))
				.collect(Collectors.groupingBy(CompetitionTypeTag::getCompetitionId, LinkedHashMap::new,
						Collectors.mapping(tag -> type(tag.getType()), Collectors.toList())));

		List<CompetitionParticipant> participants = competitionMapper.findParticipantsByCompetitionIds(competitionIds);
		List<Long> participantIds = participants.stream().map(CompetitionParticipant::getId).toList();
		Map<Long, List<CompetitionResponse.Article>> articlesByParticipant = participantIds.isEmpty()
				? Map.of()
				: competitionMapper.findPublicArticlesByCompetitionIds(competitionIds).stream()
						.collect(Collectors.groupingBy(CompetitionArticle::getParticipantId, LinkedHashMap::new,
								Collectors.mapping(article -> new CompetitionResponse.Article(
										article.getBlogId(), article.getTitle()), Collectors.toList())));
		Map<Long, CompetitionParticipant> participantById = participants.stream()
				.collect(Collectors.toMap(CompetitionParticipant::getId, Function.identity()));
		Map<Long, List<CompetitionResponse.Participant>> participantsByCompetition = participants.stream()
				.collect(Collectors.groupingBy(CompetitionParticipant::getCompetitionId, LinkedHashMap::new,
						Collectors.mapping(participant -> new CompetitionResponse.Participant(
								participant.getId(), participant.getUsername(), participantName(participant),
								List.copyOf(articlesByParticipant.getOrDefault(participant.getId(), List.of()))),
								Collectors.toList())));

		Map<Long, List<CompetitionResponse.Recipient>> recipientsByAward = competitionMapper
				.findAwardRecipientsByCompetitionIds(competitionIds).stream()
				.collect(Collectors.groupingBy(CompetitionAwardRecipient::getAwardId, LinkedHashMap::new,
						Collectors.mapping(recipient -> {
							CompetitionParticipant participant = participantById.get(recipient.getParticipantId());
							String username = recipient.getUsername();
							String displayName = recipient.getDisplayName();
							if (participant != null) {
								username = participant.getUsername();
								displayName = participantName(participant);
							}
							return new CompetitionResponse.Recipient(
									recipient.getParticipantId(), username, displayName);
						}, Collectors.toList())));
		Map<Long, List<CompetitionResponse.Award>> awardsByCompetition = competitionMapper
				.findAwardsByCompetitionIds(competitionIds).stream()
				.collect(Collectors.groupingBy(CompetitionAward::getCompetitionId, LinkedHashMap::new,
						Collectors.mapping(award -> award(award,
								recipientsByAward.getOrDefault(award.getId(), List.of())), Collectors.toList())));

		return competitions.stream().map(competition -> new CompetitionResponse(
				competition.getId(), competition.getFullName(), competition.getCompetitionYear(),
				competition.getParticipationMode().name(), competition.getParticipationMode().label(),
				List.copyOf(typesByCompetition.getOrDefault(competition.getId(), List.of())),
				competition.getCreateTime(), competition.getDeletedAt(),
				List.copyOf(participantsByCompetition.getOrDefault(competition.getId(), List.of())),
				List.copyOf(awardsByCompetition.getOrDefault(competition.getId(), List.of()))
		)).toList();
	}

	private static CompetitionResponse.Award award(CompetitionAward award,
			List<CompetitionResponse.Recipient> recipients) {
		CompetitionAwardScope scope = award.getAwardScope();
		return new CompetitionResponse.Award(
				award.getId(), award.getAwardMode().name(), award.getAwardMode().label(), award.getTeamName(),
				scope == null ? null : scope.name(), scope == null ? null : scope.label(), award.getAwardLevel(),
				award.getAwardName(), award.getRankPosition(), award.getRankTotal(),
				rank(award.getRankPosition(), award.getRankTotal()), List.copyOf(recipients));
	}

	private static CompetitionResponse.Type type(CompetitionType type) {
		return new CompetitionResponse.Type(type.name(), type.label());
	}

	private static CompetitionPageResponse page(PageInfo<Competition> page, List<CompetitionResponse> list) {
		return new CompetitionPageResponse(page.getPageNum(), page.getPageSize(), page.getTotal(),
				page.getPages(), List.copyOf(list));
	}

	private static Query query(Integer startYear, Integer endYear, String type, Integer pageNum, Integer pageSize) {
		if (startYear != null) {
			requireYear(startYear, "起始年份");
		}
		if (endYear != null) {
			requireYear(endYear, "结束年份");
		}
		if (startYear != null && endYear != null && startYear > endYear) {
			throw new BadRequestException("起始年份不能大于结束年份");
		}
		if (pageNum == null || pageNum < 1) {
			throw new BadRequestException("页码必须大于 0");
		}
		if (pageSize == null || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
			throw new BadRequestException("每页数量必须在 1 到 100 之间");
		}
		CompetitionType parsedType = type == null || type.isBlank()
				? null : enumValue(type, CompetitionType.class, "比赛类型");
		return new Query(startYear, endYear, parsedType, pageNum, pageSize);
	}

	private static List<CompetitionType> normalizeTypes(List<String> values) {
		if (values == null || values.isEmpty()) {
			throw new BadRequestException("比赛类型至少需要选择一项");
		}
		LinkedHashSet<CompetitionType> types = values.stream()
				.map(value -> enumValue(value, CompetitionType.class, "比赛类型"))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		if (types.contains(CompetitionType.LANQIAO_CUP)
				&& types.contains(CompetitionType.PROVINCIAL)) {
			throw new BadRequestException("蓝桥杯不支持省赛类型");
		}
		return List.copyOf(types);
	}

	private static List<String> normalizeUsernames(List<String> values) {
		if (values == null || values.isEmpty()) {
			throw new BadRequestException("用户列表不能为空");
		}
		if (values.size() > MAX_PARTICIPANTS_PER_REQUEST) {
			throw new BadRequestException("单次最多处理 100 名用户");
		}
		LinkedHashSet<String> usernames = values.stream()
				.map(CompetitionService::normalizeUsername)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		if (usernames.size() != values.size()) {
			throw new BadRequestException("用户列表不能重复");
		}
		return List.copyOf(usernames);
	}

	private static String normalizeUsername(String value) {
		String username = value == null ? "" : value.trim();
		if (!USERNAME.matcher(username).matches()) {
			throw new BadRequestException("用户名格式不正确");
		}
		return username;
	}

	private static CompetitionParticipationMode participationMode(String value) {
		return enumValue(value, CompetitionParticipationMode.class, "参赛形态");
	}

	private static <E extends Enum<E>> E enumValue(String value, Class<E> type, String field) {
		String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
		try {
			return Enum.valueOf(type, normalized);
		} catch (IllegalArgumentException exception) {
			throw new BadRequestException(field + "不正确");
		}
	}

	private static <E extends Enum<E>> E nullableEnumValue(String value, Class<E> type, String field) {
		return value == null || value.isBlank() ? null : enumValue(value, type, field);
	}

	private static Integer requireYear(Integer year, String field) {
		if (year == null || year < MIN_YEAR || year > MAX_YEAR) {
			throw new BadRequestException(field + "必须在 1900 到 9999 之间");
		}
		return year;
	}

	private static String normalizeRequiredText(String value, int maxLength, String field) {
		String normalized = value == null ? "" : value.trim();
		if (normalized.isEmpty() || normalized.length() > maxLength) {
			throw new BadRequestException(field + "长度必须在 1 到 " + maxLength + " 个字符之间");
		}
		return normalized;
	}

	private static String normalizeOptionalText(String value, int maxLength, String field) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw new BadRequestException(field + "不能超过 " + maxLength + " 个字符");
		}
		return normalized;
	}

	private static String displayName(User user) {
		return user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
	}

	private static String participantName(CompetitionParticipant participant) {
		return participant.getDisplayName() == null || participant.getDisplayName().isBlank()
				? participant.getDisplayNameSnapshot() : participant.getDisplayName();
	}

	private static String rank(Integer position, Integer total) {
		return "(" + position + "/" + total + ")";
	}

	private Date now() {
		return Date.from(clock.instant());
	}

	private Date cutoff() {
		return Date.from(clock.instant().minus(RETENTION));
	}

	private record Query(Integer startYear, Integer endYear, CompetitionType type, int pageNum, int pageSize) {
	}

	private record NormalizedAward(
			CompetitionAwardMode mode,
			String teamName,
			CompetitionAwardScope scope,
			Integer level,
			String awardName,
			Integer rankPosition,
			Integer rankTotal,
			List<String> recipientUsernames
	) {
	}

	private static final class AchievementAccumulator {
		private final CompetitionAwardFlatProjection row;
		private final Set<CompetitionType> types = new LinkedHashSet<>();

		private AchievementAccumulator(CompetitionAwardFlatProjection row) {
			this.row = row;
		}

		private CompetitionAchievement response() {
			List<CompetitionResponse.Type> responseTypes = types.stream()
					.sorted(Comparator.comparingInt(Enum::ordinal))
					.map(CompetitionService::type)
					.toList();
			CompetitionAwardScope scope = row.getAwardScope();
			return new CompetitionAchievement(
					row.getCompetitionId(), row.getCompetitionFullName(), row.getCompetitionYear(), responseTypes,
					row.getAwardId(), row.getAwardMode().name(), row.getAwardMode().label(), row.getTeamName(),
					scope == null ? null : scope.name(), scope == null ? null : scope.label(), row.getAwardLevel(),
					row.getAwardName(), row.getRankPosition(), row.getRankTotal(),
					rank(row.getRankPosition(), row.getRankTotal()),
					Boolean.TRUE.equals(row.getProfileVisible()));
		}
	}
}
