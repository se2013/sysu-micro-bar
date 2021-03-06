package com.hyj.service;

import com.alibaba.fastjson.JSON;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * Created by Administrator on 2016/6/2 0002.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring-mybatis.xml"})
public class PostServiceTest {
    private static Logger logger = Logger.getLogger(PostServiceTest.class);
    @Resource
    private PostService postService = null;

    @Test
    public void createReply() throws Exception {
        postService.createReply(2, 24, 39, "66666你这个僚机", null, null, null);
    }

    @Test
    public void createFloor() throws Exception {
        postService.createFloor(2, 24, "你行你上", null, null, null);
    }


    @Test
        public void createPost() throws Exception {
//        postService.createPost(1, "测试1", 1, "好好干", null, null, null);
            postService.createPost(13331095, "测试啊发发飒飒3", 1, "6啊发发飒啊发发飒啊发发飒啊发发飒啊发发飒啊发发飒啊发发飒66", null, null, null);
//        postService.createPost(3, "测试3", 1, "666", null, null, null);
    }
    @Test
    public void getPostDataList() throws Exception {
        logger.info(JSON.toJSONString(postService.getPostDataList(0)));
    }

    @Test
    public void getAllFloorDatas() throws Exception {
        logger.info(JSON.toJSONString(postService.getAllFloorDatas(1)));
    }

}