package top.naccl.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.entity.User;
import top.naccl.mapper.UserMapper;
import top.naccl.model.dto.Comment;
import top.naccl.model.dto.PlayerCommentCreateRequest;
import top.naccl.util.comment.CommentUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
	void bindsCommentAuthorFromAuthenticatedUser() {
		User user = new User();
		user.setId(7L);
		user.setUsername("player1");
		user.setNickname("真实昵称");
		user.setAvatar("/real.png");
		user.setEmail("real@example.com");
		user.setRole("ROLE_player");
		when(userMapper.findByUsername("player1")).thenReturn(user);
		when(commentUtils.judgeCommentState(0, 10L)).thenReturn(OPEN);
		PlayerCommentCreateRequest request = new PlayerCommentCreateRequest("题解写得很好", -1L, 10L);

		playerCommentService.create("player1", false, request, "127.0.0.1");

		ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
		verify(commentService).saveComment(captor.capture());
		Comment comment = captor.getValue();
		assertEquals(7L, comment.getUserId());
		assertEquals("真实昵称", comment.getNickname());
		assertEquals("/real.png", comment.getAvatar());
		assertEquals("real@example.com", comment.getEmail());
		assertEquals("", comment.getWebsite());
		assertNull(comment.getQq());
		assertEquals(0, comment.getPage());
		assertFalse(comment.getAdminComment());
		assertFalse(comment.getNotice());
	}

	@Test
	void exposesOnlyContentAndArticleCoordinatesAsClientInput() {
		Set<String> components = Arrays.stream(PlayerCommentCreateRequest.class.getRecordComponents())
				.map(component -> component.getName())
				.collect(Collectors.toSet());

		assertEquals(Set.of("content", "parentCommentId", "blogId"), components);
	}
}
