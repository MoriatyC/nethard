package com.cmh.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cmh.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, String>{
    Category findByCategoryName(String name);
}
