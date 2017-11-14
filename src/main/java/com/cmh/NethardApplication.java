package com.cmh;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.cmh.service.Crawler;

@SpringBootApplication
//@EnableScheduling
public class NethardApplication     implements CommandLineRunner{
 
    @Autowired
    Crawler crawler;
	public static void main(String[] args) {
	    SpringApplication app = new SpringApplication(NethardApplication.class);
//	    app.setWebEnvironment(false);
	    app.setBannerMode(Banner.Mode.OFF);
		app.run(NethardApplication.class, args);
	}
	
	@Override
	public void run(String... args) {
	    crawler.runSpider();
	}
}
