package com.cmh.service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cmh.dao.ArticleRepository;
import com.cmh.dao.CategoryRepository;
import com.cmh.dao.NewsRepository;
import com.cmh.dao.SourceRepository;
import com.cmh.model.Article;
import com.cmh.model.Category;
import com.cmh.model.News;
import com.cmh.model.Source;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;;

@Service
public class Crawler implements PageProcessor {
    public static  int indexCounter = 0;
    public static String prefix = "http://3g.163.com/touch/reconstruct/article/list/BAI6RHDKwangning/";
    public static String suffix = "-100.html";
    SimpleDateFormat sdf =   new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
    @Autowired
    ArticleRepository articleRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    NewsRepository newsRepository;
    @Autowired
    SourceRepository sourceRepository;
    
    public static String content;
    //1.抓取网站的相关配置，包括编码、抓取间隔、重试次数、
    private Site site = Site.me().setRetryTimes(0).setSleepTime(5000);
    public void process(Page page) {
          Pattern pattern = Pattern.compile("artiList\\((.*)\\)");
        Matcher matcher = pattern.matcher(page.getRawText());
        String json = null;
        if (matcher.find()) {
            json = matcher.group(1);
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(json);
            JsonNode mainJson = rootNode.path("BAI6RHDKwangning");
            //抽取对应标签下的所有json数据，即每一个小数据就是一个news实体。
            Iterator<JsonNode> iterator = mainJson.elements();
            String cur = null;
            JsonNode mark = null;
            List<Map<String, Object>> list = new ArrayList<>();
            while (iterator.hasNext()) {
                mark = iterator.next();
                cur = mark.toString();
                Map<String, Object> map = mapper.readValue(cur, Map.class);
                list.add(map);
            }
//            System.out.println("抓取到" + list.size() + "条新闻!!!!");
            //每一个iterator都是一个JsonNode节点，对应一个具体news实体。
            dbService(list);
            //进行数据存储操作
            indexCounter += 100;
            page.addTargetRequest(prefix + indexCounter + suffix); 
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    public void dbService(List<Map<String, Object>> list) {
        try {
            Category category = categoryRepository.findByCategoryName("game");
                for (Map<String, Object> map : list) {
//
//                    for (String s : map.keySet()) {
//                        System.out.println(s + "=" + map.get(s));
//                        System.out.println();
//                    }
                    if (newsRepository.findByDocid(map.get("docid").toString()) == null) {
//                        System.out.println("开始数据填充~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                        Article article = new Article();
                        String url = "http://3g.163.com/play/17/1113/09/" + map.get("docid").toString() + ".html";
                        article.setUrl(url);
                        News news = new News();
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
                        news.setHasImg(Integer.valueOf(map.get("hasImg").toString()));
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
                    } 
//                    else {
//                        System.out.println("已经有该条新闻=========================================================" + newsRepository.findByDocid(map.get("docid").toString()));
//                    }
                }
        } catch (ParseException e) {
            System.out.println(e.getMessage() );
            e.printStackTrace();
        } catch (NullPointerException e) {
//            System.out.println(news);
        }
    }


    public Site getSite() {
        return site;
    }
    public void runSpider() {
        Spider.create(this)
//        .addUrl("http://3g.163.com/touch/reconstruct/article/list/BAI6RHDKwangning/20-5.html")
        .addUrl(prefix + indexCounter + suffix)
        //开启5个线程抓取
        .thread(5)
        //启动
        .run();
    }
}
