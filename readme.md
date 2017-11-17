# nethard
一个基于webmagic框架的爬虫，用于爬取网易新闻手机端的新闻内容。

## 兼容性
基础依赖:

* JAVA8
*  Maven3.3.9
*  spring-boot 1.5.8
*  mysql

第三方库:

其余第三方库详见pom.xml。

该程序在Win7上开发并测试有效。

## 使用

克隆本项目后根据```application.properties``` 中的数据库source配置本地数据库信息。通过手机端网易新闻，获取相应新闻分类的分类码和缩写，存入`category`表中即可。

例如：

category_code | category_name
---|---
BA10TA81wangning | ent
BA8E6OEOwangning | sports


即可爬取娱乐和体育板块。

##TODO:
主爬虫暂时使用单线程，所以爬取速度有待提高，等主线做完，回来改善。