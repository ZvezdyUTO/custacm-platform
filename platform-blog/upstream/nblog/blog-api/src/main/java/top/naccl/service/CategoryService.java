package top.naccl.service;

import top.naccl.entity.Category;

import java.util.List;

public interface CategoryService {
	List<Category> getCategoryList();

	List<Category> getCategoryNameList();

	void saveCategory(Category category);

	Category getCategoryById(Long id);

	void deleteCategoryById(Long id);

	void updateCategory(Category category);
}
