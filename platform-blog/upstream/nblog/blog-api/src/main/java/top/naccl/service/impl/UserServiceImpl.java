package top.naccl.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import top.naccl.entity.User;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.UserMapper;
import top.naccl.service.RedisService;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.service.UserService;
import top.naccl.util.HashUtils;

/**
 * @Description: 用户业务层接口实现类
 * @Author: Naccl
 * @Date: 2020-07-19
 */
@Service
public class UserServiceImpl implements UserService, UserDetailsService {
	@Autowired
	private UserMapper userMapper;
	@Autowired
	private RedisService redisService;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userMapper.findByUsername(username);
		if (user == null) {
			throw new UsernameNotFoundException("用户不存在");
		}
		return user;
	}

	@Override
	public User findUserByUsernameAndPassword(String username, String password) {
		User user = userMapper.findByUsername(username);
		if (user == null) {
			throw new UsernameNotFoundException("用户不存在");
		}
		if (!HashUtils.matchBC(password, user.getPassword())) {
			throw new UsernameNotFoundException("密码错误");
		}
		return user;
	}

	@Override
	public User findUserById(Long id) {
		User user = userMapper.findById(id);
		if (user == null) {
			throw new NotFoundException("用户不存在");
		}
		return user;
	}

	@Override
	public User findUserByUsername(String username) {
		User user = userMapper.findByUsername(username);
		if (user == null) {
			throw new NotFoundException("用户不存在");
		}
		return user;
	}

	@Override
	public boolean updateNickname(String username, String nickname) {
		findUserByUsername(username);
		boolean updated = userMapper.updateNicknameByUsername(username, nickname) == 1;
		if (updated) {
			redisService.deleteCacheByKey(RedisKeyConstants.HOME_BLOG_INFO_LIST);
		}
		return updated;
	}

	@Override
	public boolean changePassword(String username, String oldPassword, String newPassword) {
		User user = findUserByUsername(username);
		if (!HashUtils.matchBC(oldPassword, user.getPassword())) {
			throw new BadCredentialsException("旧密码错误");
		}
		return userMapper.updatePasswordByUsername(username, HashUtils.getBC(newPassword)) == 1;
	}
}
