package top.naccl.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 面向创建、筛选和展示的规范比赛分类；持久层继续保存稳定的多类型标签组合。
 *
 * @author huangbingrui.awa
 */
public enum CompetitionCategory {
	PROVINCIAL("省赛", null, CompetitionType.PROVINCIAL),
	ICPC_NATIONAL_INVITATIONAL("ICPC 全国邀请赛", CompetitionParticipationMode.TEAM,
			CompetitionType.ICPC, CompetitionType.INVITATIONAL),
	CCPC_NATIONAL_INVITATIONAL("CCPC 全国邀请赛", CompetitionParticipationMode.TEAM,
			CompetitionType.CCPC, CompetitionType.INVITATIONAL),
	ICPC_ASIA_REGIONAL("ICPC 亚洲区域赛", CompetitionParticipationMode.TEAM,
			CompetitionType.ICPC, CompetitionType.ASIA_REGIONAL),
	CCPC_REGIONAL("CCPC 区域赛", CompetitionParticipationMode.TEAM,
			CompetitionType.CCPC, CompetitionType.NATIONAL_SITE),
	EC_FINAL("EC-Final", CompetitionParticipationMode.TEAM,
			CompetitionType.ICPC, CompetitionType.ASIA_EAST_CONTINENT_FINAL),
	CCPC_FINAL("CCPC-Final", CompetitionParticipationMode.TEAM,
			CompetitionType.CCPC, CompetitionType.NATIONAL_FINAL),
	BAIDU_STAR("百度之星", CompetitionParticipationMode.INDIVIDUAL, CompetitionType.BAIDU_STAR),
	GPLT_NATIONAL("GPLT 团体程序设计天梯赛（国赛）", CompetitionParticipationMode.MIXED,
			CompetitionType.GPLT),
	LANQIAO_CUP_NATIONAL("蓝桥杯程序设计竞赛（国奖）", CompetitionParticipationMode.INDIVIDUAL,
			CompetitionType.LANQIAO_CUP);

	private final String label;
	private final CompetitionParticipationMode fixedParticipationMode;
	private final List<CompetitionType> types;

	CompetitionCategory(String label, CompetitionParticipationMode fixedParticipationMode,
			CompetitionType... types) {
		this.label = label;
		this.fixedParticipationMode = fixedParticipationMode;
		this.types = List.copyOf(Arrays.asList(types));
	}

	public String label() {
		return label;
	}

	public CompetitionParticipationMode fixedParticipationMode() {
		return fixedParticipationMode;
	}

	public List<CompetitionType> types() {
		return types;
	}

	public boolean medalSystem() {
		return ordinal() <= CCPC_FINAL.ordinal();
	}

	public static CompetitionCategory fromTypes(Iterable<CompetitionType> types) {
		java.util.LinkedHashSet<CompetitionType> normalized = new java.util.LinkedHashSet<>();
		types.forEach(normalized::add);
		Set<CompetitionType> actual = Set.copyOf(normalized);
		return Arrays.stream(values())
				.filter(category -> actual.equals(Set.copyOf(category.types)))
				.findFirst()
				.orElse(null);
	}
}
