package com.cmh.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cmh.dao.ArticleRepository;
import com.cmh.dao.CategoryRepository;
import com.cmh.dao.NewsRepository;
import com.cmh.dao.RedisDao;
import com.cmh.dao.SourceRepository;
import com.cmh.domain.Article;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

@Slf4j
@Service
public class Crawler4Detail implements PageProcessor, Runnable {
    private final Jedis conn = new Jedis();
    private final String WAITING_LIST = "waiting:";
    private final String DONE_LIST = "done:";
    @Autowired
    RedisDao redisDao;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    ArticleRepository articleRepository;
    @Autowired
    NewsRepository newsRepository;
    @Autowired
    SourceRepository sourceRepository;
    
    public static String content;
    //1.抓取网站的相关配置，包括编码、抓取间隔、重试次数、
    private Site site = Site.me().setRetryTimes(3).setSleepTime(5000);
    @Override
    public void process(Page page) {
        parserArticle(page);
    }
    public void parserArticle(Page page) {
        Selectable article = page.getHtml().xpath("article");
        String content = article.toString();
        Pattern p = Pattern.compile("<div class=\"footer\">(.*)</div>", Pattern.DOTALL);
        Matcher m = p.matcher(content);
        String footer = "";
        if (m.find()) {
            footer = m.group(0);
        }
        content = content.replace(footer, "").replace("data-src", "src");
        String url = page.getUrl().toString();
        dbServiceArticle(url, content);
        String start = redisDao.spop(WAITING_LIST);
        if (start == null) {
            try {
                synchronized(redisDao) {
                    redisDao.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        start = redisDao.spop(WAITING_LIST);
        page.addTargetRequest(start);
    }
    
    private void dbServiceArticle(String url, String content) {
        log.info("开始存储文章具体内容！！！！！！！！！！！！！！！！！！");
        Article article = articleRepository.findByUrl(url);
        if (content == null || content == "") {
            redisDao.sadd(WAITING_LIST, url);
        } else {
            redisDao.sadd(DONE_LIST, url);
        }
        article.setContent(content);
        articleRepository.save(article);
    }
  
  

    public Site getSite() {
        return site;
    }
    public void runSpider() {
//        System.out.println(prefix + category.getCategoryCode() + "/" + indexCounter + suffix);
        String start = redisDao.spop(WAITING_LIST);
        if (start == null) {
            try {
                synchronized (redisDao) {
                    redisDao.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        start = redisDao.spop(WAITING_LIST);
        Spider.create(this)
        .addUrl(start)
        //开启5个线程抓取
        .thread(16)
        //启动
        .run();
    }
    @Override
    public void run() {
        runSpider();
    }
}
