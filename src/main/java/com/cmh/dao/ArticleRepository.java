package com.cmh.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cmh.model.Article;

public interface ArticleRepository extends JpaRepository<Article, String>{

}
