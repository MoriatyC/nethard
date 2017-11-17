package com.cmh;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.cmh.service.Crawler;
import com.cmh.service.Crawler4Null;

@SpringBootApplication
@EnableScheduling
public class NethardApplication     {
//    implements CommandLineRunner
    @Autowired
    Crawler crawler;
    @Autowired
    Crawler4Null crawler4Null;
	public static void main(String[] args) {
	    SpringApplication app = new SpringApplication(NethardApplication.class);
//	    app.setWebEnvironment(false);
	    app.setBannerMode(Banner.Mode.OFF);
		app.run(NethardApplication.class, args);
	}
	
//	@Override
//	public void run(String... args) {
//	    crawler4Null.runSpider();
//	}
}
