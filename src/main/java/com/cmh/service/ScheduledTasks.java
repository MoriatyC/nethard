package com.cmh.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTasks {

    @Autowired
    Crawler crawler;
    @Autowired
    Crawler4Null crawler4Null;
    @Scheduled(cron = "0 0 0/3 * * ?")
    public void execute() {
         crawler.runSpider();
    }
    @Scheduled(cron = "0 40 0/5 * * ?")
    public void checkNullContent() {
        crawler4Null.runSpider();
    }
}
