package top.naccl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.entity.User;
import top.naccl.enums.CommentOpenStateEnum;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.ForbiddenException;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.UserMapper;
import top.naccl.model.dto.Comment;
import top.naccl.util.StringUtils;
import top.naccl.util.comment.CommentUtils;

import java.util.Date;

/**
 * @author huangbingrui.awa
 */
@Service
public class PlayerCommentService {
	@Autowired private UserMapper userMapper;
	@Autowired private CommentService commentService;
	@Autowired private CommentUtils commentUtils;

	@Transactional(rollbackFor = Exception.class)
	public void create(String username, boolean admin, Comment comment, String ip) {
		String content = comment.getContent() == null ? null : comment.getContent().trim();
		if (StringUtils.isEmpty(content) || content.length() > 250
				|| comment.getPage() == null || comment.getParentCommentId() == null) {
			throw new BadRequestException("评论参数有误");
		}
		comment.setContent(content);
		if (comment.getParentCommentId() != -1L) {
			top.naccl.entity.Comment parent = commentService.getCommentById(comment.getParentCommentId());
			comment.setPage(parent.getPage());
			comment.setBlogId(parent.getPage() == 0 ? parent.getBlog().getId() : null);
		} else if (comment.getPage() != 0) {
			comment.setBlogId(null);
		}

		CommentOpenStateEnum state = commentUtils.judgeCommentState(comment.getPage(), comment.getBlogId());
		if (state == CommentOpenStateEnum.NOT_FOUND) {
			throw new NotFoundException("评论页面不存在");
		}
		if (state == CommentOpenStateEnum.CLOSE) {
			throw new ForbiddenException("评论已关闭");
		}
		User user = userMapper.findByUsername(username);
		if (user == null) {
			throw new NotFoundException("用户不存在");
		}
		comment.setUserId(user.getId());
		comment.setNickname(user.getNickname());
		comment.setAvatar(user.getAvatar());
		comment.setEmail(user.getEmail());
		comment.setWebsite("");
		comment.setQq(null);
		comment.setIp(ip);
		comment.setCreateTime(new Date());
		comment.setPublished(true);
		comment.setNotice(false);
		comment.setAdminComment(admin);
		commentService.saveComment(comment);
	}
}
