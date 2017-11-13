package com.cmh.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cmh.model.Source;

public interface SourceRepository extends JpaRepository<Source, String>{

    Source findBySourceName(String sourceName);

}
