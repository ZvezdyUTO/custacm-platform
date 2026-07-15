package top.naccl.service;

import top.naccl.entity.Tag;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TagService {
	List<Tag> getTagList();

	List<Tag> getTagListNotId();

	Map<Long, List<Tag>> getTagListsByBlogIds(Collection<Long> blogIds);

	void saveTag(Tag tag);

	Tag getTagById(Long id);

	void deleteTagById(Long id);

}
