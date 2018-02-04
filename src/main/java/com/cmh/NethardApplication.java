package com.cmh;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;
import com.cmh.service.Crawler;
import com.cmh.service.Crawler4Detail;
import com.cmh.service.Crawler4Null;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class NethardApplication   implements CommandLineRunner   {
   
    @Autowired
    Crawler crawler;
    @Autowired
    Crawler4Detail crawler4Detail;
	public static void main(String[] args) {
	    SpringApplication app = new SpringApplication(NethardApplication.class);
//	    app.setWebEnvironment(false);
	    app.setBannerMode(Banner.Mode.OFF);
		app.run(NethardApplication.class, args);
	}
	
	@Override
	public void run(String... args) {
	    log.info("开始测试爬虫！！！！！！！！！");
	    Thread t1 = new Thread(crawler);
	    Thread t2 = new Thread(crawler4Detail);
	    t1.start();
	    t2.start();
	    log.info("完成爬虫!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	}
}
