package com.cmh.service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
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
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;;

@Service
public class Crawler implements PageProcessor {
    public static Category category;
    public static  int indexCounter = 0;
    public static String prefix = "http://3g.163.com/touch/reconstruct/article/list/";
    public static String suffix = "-100.html";
    public static ObjectMapper mapper = new ObjectMapper();
    public static  int categoryIndex = 0;
    SimpleDateFormat sdf =   new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
    public List<Category> categoryList;
    public List<String> urlList = new LinkedList<>();
    
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
    public void process(Page page) {
        if (urlList.size() != 0) {
            urlList.remove(urlList.size() - 1);
            parserArticle(page);
            if (urlList.size() == 0) {
                addTargetNewsURL(page);
            }
        } else {
            parserNews(page);
        }
        
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
//        System.out.println(urlList.size());
 
        
    }
    public void addTargetNewsURL(Page page) {
        if (400 != indexCounter) {
            page.addTargetRequest(prefix + category.getCategoryCode() + "/" + indexCounter + suffix); 
        } else {
            indexCounter = 0;
            categoryIndex++;
            if (categoryIndex < categoryList.size()) {
                category = categoryList.get(categoryIndex);
                page.addTargetRequest(prefix + category.getCategoryCode() + "/" + indexCounter + suffix);
            } else {
                categoryIndex = 0;
            }
        }
    }
    private void dbServiceArticle(String url, String content) {
        Article article = articleRepository.findByUrl(url);
        article.setContent(content);
        articleRepository.save(article);
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
                Map<String, Object> map = mapper.readValue(cur, Map.class);
                list.add(map);
            }
//            System.out.println("抓取到" + list.size() + "条新闻!!!!");
            //每一个iterator都是一个JsonNode节点，对应一个具体news实体。
            
            dbServiceNews(list, category);
            indexCounter += 100;
            if (urlList.size() != 0) {
                page.addTargetRequests(urlList);
            } else {
                addTargetNewsURL(page);
            }
            //进行数据存储操作

        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    public void dbServiceNews(List<Map<String, Object>> list, Category category) {
        try {
                for (Map<String, Object> map : list) {
//                    for (String s : map.keySet()) {
//                        System.out.println(s + "=" + map.get(s));
//                        System.out.println();
//                    }
                    if (map.containsKey("skipType")) {
                        continue;
                    }
                    if (newsRepository.findByDocid(map.get("docid").toString()) == null) {
                        System.out.println("开始数据填充~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                        Article article = new Article();
                        String url = "http://3g.163.com/" + category.getCategoryName() +"/article/" + map.get("docid").toString() + ".html";
                        article.setUrl(url);
                        urlList.add(url);
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
                    } 
                    
                    else {
                        System.out.println("已经有该条新闻");
                    }
                }
        } catch (ParseException e) {
            System.out.println(e.getMessage() );
            e.printStackTrace();
        } catch (NullPointerException e) {
//            System.out.println(news);
            System.out.println("该打断点了！！！！！！！！！！！！！！！！！！！！！！！");
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
        .addUrl(prefix + category.getCategoryCode() + "/" + indexCounter + suffix)
        //开启5个线程抓取
        .thread(1)
        //启动
        .run();
    }
}
