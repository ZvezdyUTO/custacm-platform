package top.naccl.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.entity.User;
import top.naccl.mapper.UserMapper;
import top.naccl.model.dto.Comment;
import top.naccl.util.comment.CommentUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static top.naccl.enums.CommentOpenStateEnum.OPEN;

@ExtendWith(MockitoExtension.class)
class PlayerCommentServiceTest {
	@Mock private UserMapper userMapper;
	@Mock private CommentService commentService;
	@Mock private CommentUtils commentUtils;
	@InjectMocks private PlayerCommentService playerCommentService;

	@Test
	void bindsCommentAuthorFromAuthenticatedUserAndIgnoresClientProfile() {
		User user = new User();
		user.setId(7L);
		user.setUsername("player1");
		user.setNickname("真实昵称");
		user.setAvatar("/real.png");
		user.setEmail("real@example.com");
		user.setRole("ROLE_player");
		when(userMapper.findByUsername("player1")).thenReturn(user);
		when(commentUtils.judgeCommentState(0, 10L)).thenReturn(OPEN);
		Comment comment = new Comment();
		comment.setContent("题解写得很好");
		comment.setPage(0);
		comment.setBlogId(10L);
		comment.setParentCommentId(-1L);
		comment.setNickname("伪造昵称");
		comment.setAvatar("/fake.png");

		playerCommentService.create("player1", false, comment, "127.0.0.1");

		assertEquals(7L, comment.getUserId());
		assertEquals("真实昵称", comment.getNickname());
		assertEquals("/real.png", comment.getAvatar());
		assertEquals("real@example.com", comment.getEmail());
		assertFalse(comment.getAdminComment());
		verify(commentService).saveComment(comment);
	}
}
