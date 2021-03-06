package com.hyj.service.impl;

import com.alibaba.fastjson.JSON;
import com.hyj.service.PostService;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * Created by Administrator on 2016/5/27 0027.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring-mybatis.xml"})
public class PostServiceImplTest {



    private static Logger logger = Logger.getLogger(PostServiceImplTest.class);
    @Resource
    private PostService postService = null;
    @Test
    public void getAllFloorDatas() throws Exception {
        logger.info(JSON.toJSONString(postService.getAllFloorDatas(1)));
    }
    @Test
    public void getPostDataList() throws Exception {
        logger.info(JSON.toJSONString(postService.getPostDataList(0)));
    }


}