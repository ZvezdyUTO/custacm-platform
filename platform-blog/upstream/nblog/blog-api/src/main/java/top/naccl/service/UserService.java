package top.naccl.service;

import top.naccl.entity.User;

public interface UserService {
	User findUserByUsernameAndPassword(String username, String password);

	User findUserById(Long id);

	User findUserByUsername(String username);

	boolean updateNickname(String username, String nickname);

	boolean changePassword(String username, String oldPassword, String newPassword);
}
