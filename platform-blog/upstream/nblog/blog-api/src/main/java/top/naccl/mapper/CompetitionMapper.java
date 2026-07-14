package top.naccl.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.naccl.entity.Competition;
import top.naccl.entity.CompetitionArticle;
import top.naccl.entity.CompetitionAward;
import top.naccl.entity.CompetitionAwardFlatProjection;
import top.naccl.entity.CompetitionAwardRecipient;
import top.naccl.entity.CompetitionParticipant;
import top.naccl.entity.CompetitionTypeTag;
import top.naccl.enums.CompetitionType;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 比赛、参赛人、奖项和文章绑定的 MyBatis 持久层接口。
 *
 * @author huangbingrui.awa
 */
@Mapper
@Repository
public interface CompetitionMapper {
	List<Competition> findActiveCompetitions(@Param("yearFrom") Integer yearFrom,
			@Param("yearTo") Integer yearTo,
			@Param("categoryTypes") Collection<CompetitionType> categoryTypes,
			@Param("categoryTypeCount") Integer categoryTypeCount);

	List<Competition> findRecycleBinCompetitions(@Param("yearFrom") Integer yearFrom,
			@Param("yearTo") Integer yearTo,
			@Param("categoryTypes") Collection<CompetitionType> categoryTypes,
			@Param("categoryTypeCount") Integer categoryTypeCount,
			@Param("cutoff") Date cutoff);

	Competition findActiveCompetitionById(Long id);

	Competition findCompetitionByIdForUpdate(Long id);

	Competition findActiveCompetitionByFullName(String fullName);

	int insertCompetition(Competition competition);

	int insertTypeTags(@Param("competitionId") Long competitionId,
			@Param("types") Collection<CompetitionType> types);

	int moveCompetitionToRecycleBin(@Param("id") Long id, @Param("deletedAt") Date deletedAt);

	int restoreCompetition(@Param("id") Long id, @Param("cutoff") Date cutoff);

	List<Long> findExpiredCompetitionIdsForUpdate(Date cutoff);

	int deleteCompetitionById(Long id);

	List<CompetitionTypeTag> findTypeTagsByCompetitionIds(
			@Param("competitionIds") Collection<Long> competitionIds);

	List<CompetitionParticipant> findParticipantsByCompetitionIds(
			@Param("competitionIds") Collection<Long> competitionIds);

	List<CompetitionAward> findAwardsByCompetitionIds(
			@Param("competitionIds") Collection<Long> competitionIds);

	List<CompetitionAwardRecipient> findAwardRecipientsByCompetitionIds(
			@Param("competitionIds") Collection<Long> competitionIds);

	List<CompetitionArticle> findPublicArticlesByCompetitionIds(
			@Param("competitionIds") Collection<Long> competitionIds);

	List<CompetitionParticipant> findParticipantsByCompetitionIdAndUsernames(
			@Param("competitionId") Long competitionId, @Param("usernames") Collection<String> usernames);

	CompetitionParticipant findParticipantByIdForUpdate(Long id);

	int insertParticipants(@Param("participants") List<CompetitionParticipant> participants);

	int deleteParticipant(@Param("competitionId") Long competitionId, @Param("participantId") Long participantId);

	int countAwardReferencesByParticipantId(Long participantId);

	CompetitionAward findAwardByIdForUpdate(Long id);

	int insertAward(CompetitionAward award);

	int deleteAward(@Param("competitionId") Long competitionId, @Param("awardId") Long awardId);

	int insertAwardRecipients(@Param("recipients") List<CompetitionAwardRecipient> recipients);

	int bindOwnedPublicArticle(@Param("participantId") Long participantId,
			@Param("username") String username, @Param("blogId") Long blogId);

	int unbindOwnedArticle(@Param("participantId") Long participantId,
			@Param("username") String username, @Param("blogId") Long blogId);

	CompetitionAwardRecipient findAchievementProfileStateForUpdate(@Param("competitionId") Long competitionId,
			@Param("awardId") Long awardId, @Param("username") String username);

	int updateAchievementProfileVisibility(@Param("competitionId") Long competitionId,
			@Param("awardId") Long awardId, @Param("username") String username,
			@Param("visible") boolean visible, @Param("profileSortOrder") Long profileSortOrder);

	List<CompetitionAwardRecipient> findVisibleAchievementOrdersForUpdate(String username);

	int updateAchievementProfileOrder(@Param("username") String username,
			@Param("awardId") Long awardId, @Param("profileSortOrder") Long profileSortOrder);

	List<CompetitionAwardFlatProjection> findActiveAwardProjectionsByUsername(String username);
}
