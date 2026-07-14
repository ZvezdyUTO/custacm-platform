package top.naccl.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * 规范奖项语义；写入时映射到兼容的 level、scope 和 name 列。
 *
 * @author huangbingrui.awa
 */
public enum CompetitionAwardTier {
	MEDAL_GOLD("金牌", 1, null, Group.MEDAL),
	MEDAL_SILVER("银牌", 2, null, Group.MEDAL),
	MEDAL_BRONZE("铜牌", 3, null, Group.MEDAL),
	MEDAL_HONORABLE_MENTION("优胜奖", 4, null, Group.MEDAL),
	BAIDU_NATIONAL_FIRST("国赛一等奖", 1, CompetitionAwardScope.NATIONAL, Group.BAIDU),
	BAIDU_NATIONAL_SECOND("国赛二等奖", 2, CompetitionAwardScope.NATIONAL, Group.BAIDU),
	BAIDU_NATIONAL_THIRD("国赛三等奖", 3, CompetitionAwardScope.NATIONAL, Group.BAIDU),
	BAIDU_NATIONAL_FOURTH("国赛四等奖", 4, CompetitionAwardScope.NATIONAL, Group.BAIDU),
	BAIDU_PROVINCIAL_FIRST("省赛一等奖", 1, CompetitionAwardScope.PROVINCIAL, Group.BAIDU),
	BAIDU_PROVINCIAL_SECOND("省赛二等奖", 2, CompetitionAwardScope.PROVINCIAL, Group.BAIDU),
	BAIDU_PROVINCIAL_THIRD("省赛三等奖", 3, CompetitionAwardScope.PROVINCIAL, Group.BAIDU),
	FIRST_PRIZE("一等奖", 1, CompetitionAwardScope.NATIONAL, Group.ORDINARY),
	SECOND_PRIZE("二等奖", 2, CompetitionAwardScope.NATIONAL, Group.ORDINARY),
	THIRD_PRIZE("三等奖", 3, CompetitionAwardScope.NATIONAL, Group.ORDINARY);

	private static final Set<CompetitionAwardTier> MEDAL_TIERS = EnumSet.range(
			MEDAL_GOLD, MEDAL_HONORABLE_MENTION);
	private static final Set<CompetitionAwardTier> BAIDU_TIERS = EnumSet.range(
			BAIDU_NATIONAL_FIRST, BAIDU_PROVINCIAL_THIRD);
	private static final Set<CompetitionAwardTier> ORDINARY_TIERS = EnumSet.range(
			FIRST_PRIZE, THIRD_PRIZE);

	private final String label;
	private final int level;
	private final CompetitionAwardScope scope;
	private final Group group;

	CompetitionAwardTier(String label, int level, CompetitionAwardScope scope, Group group) {
		this.label = label;
		this.level = level;
		this.scope = scope;
		this.group = group;
	}

	public String label() {
		return label;
	}

	public int level() {
		return level;
	}

	public CompetitionAwardScope scope() {
		return scope;
	}

	public String storedName() {
		return label;
	}

	public boolean validFor(CompetitionCategory category) {
		if (category == null) return false;
		if (category.medalSystem()) return group == Group.MEDAL;
		if (category == CompetitionCategory.BAIDU_STAR) return group == Group.BAIDU;
		return (category == CompetitionCategory.GPLT_NATIONAL
				|| category == CompetitionCategory.LANQIAO_CUP_NATIONAL)
				&& group == Group.ORDINARY;
	}

	public static CompetitionAwardTier fromStored(CompetitionCategory category,
			CompetitionAwardScope scope, Integer level) {
		if (category == null || level == null) return null;
		Set<CompetitionAwardTier> candidates;
		if (category.medalSystem()) {
			candidates = MEDAL_TIERS;
		} else if (category == CompetitionCategory.BAIDU_STAR) {
			candidates = BAIDU_TIERS;
		} else if (category == CompetitionCategory.GPLT_NATIONAL
				|| category == CompetitionCategory.LANQIAO_CUP_NATIONAL) {
			candidates = ORDINARY_TIERS;
		} else {
			return null;
		}
		return candidates.stream()
				.filter(tier -> tier.level == level)
				.filter(tier -> category.medalSystem() || tier.scope == scope)
				.findFirst()
				.orElse(null);
	}

	private enum Group { MEDAL, BAIDU, ORDINARY }
}
