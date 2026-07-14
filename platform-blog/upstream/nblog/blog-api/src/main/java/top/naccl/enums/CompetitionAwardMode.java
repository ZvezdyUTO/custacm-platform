package top.naccl.enums;

/**
 * 奖项归属形态。
 *
 * @author huangbingrui.awa
 */
public enum CompetitionAwardMode {
	INDIVIDUAL("个人"),
	TEAM("团队");

	private final String label;

	CompetitionAwardMode(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
