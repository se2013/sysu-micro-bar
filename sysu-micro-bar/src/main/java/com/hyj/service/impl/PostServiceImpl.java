package com.hyj.service.impl;

import com.alibaba.fastjson.JSON;
import com.hyj.constant.Constants;
import com.hyj.dao.*;
import com.hyj.dto.FloorData;
import com.hyj.dto.HistoryData;
import com.hyj.dto.PostData;
import com.hyj.entity.*;
import com.hyj.service.PostService;
import com.hyj.util.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2016/5/26 0026.
 */
@Service("postService")
public class PostServiceImpl implements PostService {
    @Resource
    private PostMapper postMapper;
    @Resource
    private FloorMapper floorMapper;
    @Resource
    private FloorFileMapper floorFileMapper;
    @Resource
    private AccountMapper accountMapper;
    @Resource
    private HistoryMessageMapper historyMessageMapper;


    private static final Logger logger = LoggerFactory
            .getLogger(PostServiceImpl.class);

    public List<PostData> getPostDataList(int currentPostNum) {
        List<Post> posts = postMapper.selectAllPost(currentPostNum);
        List<PostData> postDatas = new ArrayList<PostData>();
        for (Post post : posts) {
            PostData postData = new PostData(post.getId(),
                    post.getTitle(), Constants.POST_TAGS[post.getTag()],
                    DateTimeUtil.getDateTimeString(new Date()), floorMapper.selectCountByPostId(post.getId()));
            postDatas.add(postData);
        }
        return postDatas;
    }



    public List<PostData> searchPostData(String title, int tag) {
        List<Post> posts = postMapper.searchByTitleAndTag(title, tag);
        List<PostData> postDatas = new ArrayList<PostData>();
        for (Post post : posts) {
            PostData postData = new PostData(post.getId(),
                    post.getTitle(), Constants.POST_TAGS[post.getTag()],
                    DateTimeUtil.getDateTimeString(new Date()), floorMapper.selectCountByPostId(post.getId()));
            postDatas.add(postData);
        }
        return postDatas;
    }


    /**
     * 私有工具函数，返回一个文件的url数组
     * @param dirName
     * @param fileBaseUrl
     * @return
     */
    private String[] uploadFiles(int floorId, String dirName, String fileBaseUrl, MultipartFile[] files) {
        logger.info("上传文件");
        String fileUrl /*+ 文件名*/;
        String[] fileUrls = new String[files.length];/*用于保存*/
        int count = 0;
        for (MultipartFile file : files) {
            fileUrl = fileBaseUrl;
            logger.info("文件长度: " + file.getSize());
            logger.info("文件类型: " + file.getContentType());
            logger.info("文件名称: " + file.getName());
            logger.info("文件原名: " + file.getOriginalFilename());
            logger.info("========================================");
            try {
                byte[] bytes = file.getBytes();
                File dir = new File(dirName);
                if (!dir.exists())
                    dir.mkdirs();
                String filename = System.currentTimeMillis() + file.getOriginalFilename();
                File serverFile = new File(dir.getAbsolutePath() + "/" + filename);
                BufferedOutputStream stream = new BufferedOutputStream(
                        new FileOutputStream(serverFile));
                stream.write(bytes);
                stream.close();
                logger.info("Server File Location="
                        + serverFile.getAbsolutePath());
                // 将文件名添加到url中(用系统时间查重处理- -)
                fileUrl += filename;
                fileUrls[count++] = fileUrl;
                floorFileMapper.insertSelective(new FloorFile(floorId, fileUrl, Constants.TYPE_IMAGE));
            } catch (Exception e) {
                logger.error("You failed to upload => " + e.getMessage());
                return null;
            }
        }
        return fileUrls;
    }
    /**
     * 上传文件之后要把帖子的内容里的[img=uuid]替换成<img src="floor.getFileUrl()"></img>
     * */
    private void afterUploadFiles(String[] fileUrls, String content, Floor floor) {
        logger.info("上传文件");
        if (fileUrls != null) {
            String regex = "\\[img=\\w+-\\w+-\\w+-\\w+-\\w+\\]";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(content);
            int count = 0;
            while (matcher.find()) {
                content = content.replace(matcher.group(), "\n<img src=\"" + fileUrls[count] + "\"</img>");
                logger.info(content);
                ++count;
            }
            floor.setDetail(content);
            floorMapper.updateByPrimaryKeySelective(floor);
        }
    }

    public boolean createPost(int accountId, String title, int tag, String detail, MultipartFile[] files, String rootPath, String contextPath) {
        Account account = accountMapper.selectByPrimaryKey(accountId);
        Post post = new Post(account, title, tag, new Date(), new Date());
        postMapper.insertSelective(post);
        Floor floor = new Floor(post, account, null, false, new Date(), detail);
        logger.info("postId" + post.getId());
        floorMapper.insertSelective(floor);
        logger.info("floorId= =" + floor.getId());
        if (files != null && files.length != 0) {
            String dirName = rootPath + "/post" + post.getId() + "/floor" + floor.getId();
            String fileBaseUrl = contextPath + "/post" + post.getId() + "/floor" + floor.getId() + "/";
            String[] fileUrls = this.uploadFiles(floor.getId(), dirName, fileBaseUrl, files);
            for (String fileUrl : fileUrls) {
                logger.info("134123431241324" + fileUrl);
            }
            this.afterUploadFiles(fileUrls, detail, floor);
        }
        return true;
    }

    public boolean createFloor(int accountId, int postId, String detail, MultipartFile[] files, String rootPath, String contextPath) {
        Account account = accountMapper.selectByPrimaryKey(accountId);
        Post post = postMapper.selectByPrimaryKey(postId);
        Floor floor = new Floor(post, account, null, false, new Date(), detail);
        floorMapper.insertSelective(floor);
        String dirName = rootPath + "/post" + postId + "/floor" + floor.getId();
        String fileBaseUrl = contextPath + "/post" + postId + "/floor" + floor.getId() + "/";
        logger.info(dirName);
        logger.info(fileBaseUrl);
        if (files != null && files.length != 0) {
            String[] fileUrls = this.uploadFiles(floor.getId(), dirName, fileBaseUrl, files);
            afterUploadFiles(fileUrls, detail, floor);
        }
        // 盖楼：如果楼是我盖的话就不需要插入历史信息
        logger.info("创楼人" + floor.getAccount().getId());
        logger.info("创建人" + post.getCreator().getId());
        // 发布新的楼，并通知帖子的创建者
        if (floor.getAccount().getId() != post.getCreator().getId()) {
            logger.info("？？？" + floor.getId());
            HistoryMessage historyMessage = new HistoryMessage(post.getCreator(), floor, false, true);
            historyMessageMapper.insertSelective(historyMessage);
        }
        return true;
    }

    public boolean createReply(int accountId, int postId, int replyFloorId, String detail, MultipartFile[] files, String rootPath, String contextPath) {
        Account account = accountMapper.selectByPrimaryKey(accountId);
        Post post = postMapper.selectByPrimaryKey(postId);
        Floor replyFloor = floorMapper.selectByPrimaryKey(replyFloorId);
        Floor floor = new Floor(post, account, replyFloor, true, new Date(), detail);
        floorMapper.insertSelective(floor);
        String dirName = rootPath + "/post" + postId + "/floor" + floor.getId();
        String fileBaseUrl = contextPath + "/post" + postId + "/floor" + floor.getId() + "/";
        if (files != null && files.length != 0) {
            String[] fileUrls = this.uploadFiles(floor.getId(), dirName, fileBaseUrl, files);
            afterUploadFiles(fileUrls, detail, floor);

        }
        // 回复某个人的时候，需要往历史消息表里插入两条记录
        // 一个是通知帖子的创始人
        // 另一个是通知回复的人
        // 如果回复的那个帖子的创始人是accountId的话就不用提醒
        if (post.getCreator().getId() != accountId) {
            HistoryMessage historyMessage1 = new HistoryMessage(post.getCreator(), floor, false, true);
            historyMessageMapper.insertSelective(historyMessage1);
        }
        // 如果回复的那层楼的帐号是accountId的话就不用提醒
        if (replyFloor.getAccount().getId() != post.getCreator().getId()) {
            HistoryMessage historyMessage2 = new HistoryMessage(replyFloor.getAccount(), floor, false, false);
            historyMessageMapper.insertSelective(historyMessage2);
        }
        return true;
    }






    public List<FloorData> getAllFloorDatas(int postId) {
        List<Floor> floors = floorMapper.selectByPostId(postId);
        List<FloorData> floorDatas = new ArrayList<FloorData>();
        for (Floor floor : floors) {
            /*楼层id--->账号*/
            Account account = floor.getAccount();
            /*如果有回复，就增加replyAccount= =有可能空指针错误需要注意*/
            boolean isReply = floor.getIsReply();
            Floor replyFloor = floor.getReplyFloor();
            Account replyAccount = isReply ? replyFloor.getAccount() : null;
            FloorData floorData = new FloorData(floor.getId(), account.getHeadImageUrl(), account.getNickname(),
                    DateTimeUtil.getDateTimeString(floor.getCreateTime()), floor.getDetail(),
                    isReply, isReply == true ? replyAccount.getNickname() : null, isReply == true ? replyFloor.getId() : null);
            floorDatas.add(floorData);
        }
        return floorDatas;
    }



}
