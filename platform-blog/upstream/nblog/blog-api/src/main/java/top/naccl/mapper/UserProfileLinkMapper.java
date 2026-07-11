package top.naccl.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.naccl.entity.UserProfileLink;

import java.util.List;

/**
 * @author huangbingrui.awa
 */
@Mapper
@Repository
public interface UserProfileLinkMapper {
	List<UserProfileLink> findByUserId(@Param("userId") Long userId);

	int deleteByUserId(@Param("userId") Long userId);

	int insert(UserProfileLink link);
}
