package top.naccl.enums;

/**
 * 比赛允许的参赛形态。
 *
 * @author huangbingrui.awa
 */
public enum CompetitionParticipationMode {
	INDIVIDUAL("个人"),
	TEAM("团队"),
	MIXED("混合");

	private final String label;

	CompetitionParticipationMode(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
