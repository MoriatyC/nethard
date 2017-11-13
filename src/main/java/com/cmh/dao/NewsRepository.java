package com.cmh.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cmh.model.News;

public interface NewsRepository extends JpaRepository<News, String>{

}
