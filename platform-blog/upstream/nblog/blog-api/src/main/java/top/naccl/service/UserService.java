package top.naccl.service;

import top.naccl.entity.User;

public interface UserService {
	User findUserByUsernameAndPassword(String username, String password);

	User findUserByUsername(String username);

	boolean changePassword(String username, String oldPassword, String newPassword);
}
