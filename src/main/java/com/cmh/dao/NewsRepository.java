package com.cmh.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cmh.domain.News;

public interface NewsRepository extends JpaRepository<News, String>{
    News findByDocid(String docid);
}
