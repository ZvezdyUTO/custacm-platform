package top.naccl.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author huangbingrui.awa
 */
@Getter
@Setter
@NoArgsConstructor
public class PasswordUpdate {
	private String oldPassword;
	private String newPassword;
}
