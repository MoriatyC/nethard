package com.cmh.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cmh.dao.ArticleRepository;
import com.cmh.dao.CategoryRepository;
import com.cmh.dao.NewsRepository;
import com.cmh.dao.SourceRepository;
import com.cmh.domain.Article;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;;

@Service
public class Crawler4Null implements PageProcessor {
    
    @Autowired
    Crawler crawler;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    ArticleRepository articleRepository;
    @Autowired
    NewsRepository newsRepository;
    @Autowired
    SourceRepository sourceRepository;
    
    //1.抓取网站的相关配置，包括编码、抓取间隔、重试次数、
    private Site site = Site.me().setRetryTimes(3).setSleepTime(5000);
    public void process(Page page) {
//        crawler.parserArticle(page);
    }


    public Site getSite() {
        return site;
    }
    public void runSpider() {
        List<Article> list = articleRepository.findByContentIsNull();
        String[] requests = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            requests[i] = list.get(i).getUrl();
        }
        Spider.create(this)
        .addUrl(requests)
        //开启5个线程抓取
        .thread(5)
        //启动
        .run();
    }
}
