package com.cmh.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTasks {

    @Autowired
    Crawler crawler;
    @Scheduled(cron = "* 28/30 * * * ?")
    public void execute() {
         crawler.runSpider();
    }
}
