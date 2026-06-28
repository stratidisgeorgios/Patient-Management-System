package com.patientsystem.treatmentservice.service;
import java.util.List;
import org.springframework.stereotype.Service;
import com.patientsystem.treatmentservice.model.Category;
import com.patientsystem.treatmentservice.repository.CategoryRepository;
import com.patientsystem.treatmentservice.repository.TreatmentRepository;
@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final TreatmentRepository treatmentRepository;

    public CategoryService(CategoryRepository categoryRepository, TreatmentRepository treatmentRepository) {
        this.categoryRepository = categoryRepository;
        this.treatmentRepository = treatmentRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category createCategory(Category category) {
        if (categoryRepository.existsByName(category.getName())) {
            throw new RuntimeException("Category with name " + category.getName() + " already exists.");
        }
        return categoryRepository.save(category);
    }

    public Category updateCategory(String categoryId, Category category) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException("Category not found for ID: " + categoryId);
        }
        category.setId(categoryId);
        return categoryRepository.save(category);
    }

    public void deleteCategory(String categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException("Category not found for ID: " + categoryId);
        }
        if (treatmentRepository.existsByCategory_Id(categoryId)) {
            throw new RuntimeException("Cannot delete category: treatments are still assigned to it.");
        }
        categoryRepository.deleteById(categoryId);
    }
}
