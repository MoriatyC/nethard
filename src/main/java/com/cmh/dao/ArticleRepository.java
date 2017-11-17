package com.cmh.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cmh.model.Article;

public interface ArticleRepository extends JpaRepository<Article, String>{
    Article findByUrl(String url);
    List<Article> findByContentIsNull();
}
