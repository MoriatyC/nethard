package com.cmh.service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
import com.cmh.domain.Category;
import com.cmh.domain.News;
import com.cmh.domain.Source;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

@Slf4j
@Service
public class Crawler implements PageProcessor, Runnable {
    public static Category category;
    public static  int indexCounter = 0;
    public static final String PREFIX = "http://3g.163.com/touch/reconstruct/article/list/";
    public static final String SUFFIX = "-20.html";
    public static ObjectMapper mapper = new ObjectMapper();
    public static  int categoryIndex = 0;
    SimpleDateFormat sdf =   new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
    public List<Category> categoryList;
    public List<String> urlList = new LinkedList<>();
    public BlockingQueue<String> producer = new LinkedBlockingQueue<>();
    @Autowired
    RedisDao redisDao;
    private final String WAITING_LIST = "waiting:";
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
          parserNews(page);
    }
    public void addTargetNewsURL(Page page) {
        if (400 != indexCounter) {
            page.addTargetRequest(PREFIX + category.getCategoryCode() + "/" + indexCounter + SUFFIX); 
        } else {
            indexCounter = 0;
            categoryIndex++;
            if (categoryIndex < categoryList.size()) {
                category = categoryList.get(categoryIndex);
                page.addTargetRequest(PREFIX + category.getCategoryCode() + "/" + indexCounter + SUFFIX);
            } else {
                categoryIndex = 0;
            }
        }
    }
    public void parserNews(Page page) {
        Pattern pattern = Pattern.compile("artiList\\((.*)\\)");
        Matcher matcher = pattern.matcher(page.getRawText());
        String json = null;
        if (matcher.find()) {
            json = matcher.group(1);
        }
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(json);
            JsonNode mainJson = rootNode.path(category.getCategoryCode());
            //抽取对应标签下的所有json数据，即每一个小数据就是一个news实体。
            Iterator<JsonNode> iterator = mainJson.elements();
            String cur = null;
            JsonNode mark = null;
            List<Map<String, Object>> list = new ArrayList<>();
            
            while (iterator.hasNext()) {
                mark = iterator.next();
                cur = mark.toString();
                try {
                    Map<String, Object> map = mapper.readValue(cur, Map.class);
                    list.add(map);
                } catch(Exception e) {
                    log.debug(cur);
                }
            }
            //每一个iterator都是一个JsonNode节点，对应一个具体news实体。
            
            dbServiceNews(list, category);
            indexCounter += 20;
            addTargetNewsURL(page);
            //进行数据存储操作

        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            log.debug(json);
        }
    }
    public void dbServiceNews(List<Map<String, Object>> list, Category category) {
        try {
                for (Map<String, Object> map : list) {
                    if (map.containsKey("skipType")) {
                        continue;
                    }
                    if (newsRepository.findByDocid(map.get("docid").toString()) == null) {
                        log.info("开始数据填充~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                        Article article = new Article();
                        String url = "http://3g.163.com/" + category.getCategoryName() + "/article/"
                                + map.get("docid").toString() + ".html";
                        article.setUrl(url);
                        
//                        urlList.add(url);
                        News news = new News();
                        Map<String, String> hash = new HashMap<>();
                        hash.put("voted", map.get("commentCount").toString());
                        hash.put("title", map.get("title").toString());
                        hash.put("url", url);
                        Source source = sourceRepository.findBySourceName(map.get("source").toString());
                        if (source == null) {
                            source = new Source();
                            source.setSourceName(map.get("source").toString());
                            source.setPublishCount(1);
                        } else {
                            source.setPublishCount(source.getPublishCount() + 1);
                        }
                        news.setDocid(map.get("docid").toString());
                        news.setCommentCount(Integer.valueOf(map.get("commentCount").toString()));
                        news.setDigest(map.get("digest").toString());
                        if (map.containsKey("hasImg")) {
                            news.setHasImg(Integer.valueOf(map.get("hasImg").toString()));
                        }
                        
                        news.setImgsrc(map.get("imgsrc").toString());
                        news.setPriority(Integer.valueOf(map.get("priority").toString()));
                        news.setPtime(sdf.parse(map.get("ptime").toString()));
                        news.setTitle(map.get("title").toString());
                        news.setArticleId(article);
                        news.setCategoryCode(category);
                        news.setArticleId(article);
                        news.setSourceId(source);
                        
                        articleRepository.save(article);
                        sourceRepository.save(source);
                        newsRepository.save(news);
                        StringBuilder sb = new StringBuilder();
                        sb.append("news:").append(map.get("docid").toString());
                        
                        redisDao.saddWithoutDuplication(WAITING_LIST, url);
                        synchronized (redisDao) {
                            redisDao.notify();
                        }
                        String member = sb.toString();
                        redisDao.hsetWithExpired(member, hash);
                        redisDao.zset("time:", System.currentTimeMillis() / 1000, member);
                    } 
                    
                    else {
                        log.info("已经有该条新闻");
                    }
                }
        } catch (ParseException e) {
            System.out.println(e.getMessage() );
            e.printStackTrace();
        } catch (NullPointerException e) {
            log.warn("该打断点了！！！！！！！！！！！！！！！！！！！！！！！");
        }
    }


    public Site getSite() {
        return site;
    }
    public void runSpider() {
        categoryList = categoryRepository.findAll();
        category = categoryList.get(categoryIndex);
//        System.out.println(prefix + category.getCategoryCode() + "/" + indexCounter + suffix);
        Spider.create(this)
        .addUrl(PREFIX + category.getCategoryCode() + "/" + indexCounter + SUFFIX)
        .thread(16)
        .run();
    }
    @Override
    public void run() {
        runSpider();
    }
}
