package top.naccl.enums;

/**
 * 比赛类型标签。一场比赛可同时拥有多个标签。
 *
 * @author huangbingrui.awa
 */
public enum CompetitionType {
	ICPC("ICPC"),
	CCPC("CCPC"),
	PROVINCIAL("省赛"),
	INVITATIONAL("邀请赛"),
	NATIONAL_SITE("CCPC 全国分站赛"),
	ASIA_REGIONAL("ICPC 亚洲区域赛"),
	ASIA_EAST_CONTINENT_FINAL("ICPC 亚洲东大陆总决赛（EC-Final）"),
	NATIONAL_FINAL("全国总决赛"),
	WORLD_FINAL("世界总决赛"),
	LANQIAO_CUP("蓝桥杯"),
	BAIDU_STAR("百度之星"),
	GPLT("团体程序设计天梯赛"),
	OTHER("其他");

	private final String label;

	CompetitionType(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
