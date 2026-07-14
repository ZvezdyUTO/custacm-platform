package top.naccl.enums;

/**
 * 奖项级别范围。
 *
 * @author huangbingrui.awa
 */
public enum CompetitionAwardScope {
	PROVINCIAL("省级"),
	NATIONAL("国家级");

	private final String label;

	CompetitionAwardScope(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
