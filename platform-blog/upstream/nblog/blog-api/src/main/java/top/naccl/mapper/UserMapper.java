package top.naccl.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.naccl.entity.User;

import java.util.List;

/**
 * @Description: 用户持久层接口
 * @Author: Naccl
 * @Date: 2020-07-19
 */
@Mapper
@Repository
public interface UserMapper {
	User findByUsername(String username);

	User findById(Long id);

	List<User> findAll();

	int insert(User user);

	int updateNicknameByUsername(String username, String nickname);

	int updateProfileByUsername(@Param("username") String username, @Param("nickname") String nickname,
			@Param("signature") String signature);

	int updateAvatarByUsername(String username, String avatar);

	int updatePasswordByUsername(String username, String password);

	int updateAdminFields(@Param("user") User user, @Param("oldUsername") String oldUsername);

	int deleteByUsername(String username);

	int anonymizeCommentsByUserId(Long userId);

	List<Long> lockAdminIds();
}
