package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.BaseCategory1Mapper;
import com.atguigu.tingshu.album.mapper.BaseCategory2Mapper;
import com.atguigu.tingshu.album.mapper.BaseCategory3Mapper;
import com.atguigu.tingshu.album.mapper.BaseCategoryViewMapper;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;


    /**
     * 查询所有分类（1、2、3级分类）
     *
     * @return 业务数据：[{"categoryId":1,"categoryName":"分类",categoryChild:[..]},{其他1级分类}]
     */
    @Override
    public List<JSONObject> gerBaseCategoryList() {
        /*List<JSONObject> returnList = new ArrayList<>();
        List<BaseCategoryView> allCategoryList = baseCategoryViewMapper.selectList(null);
        Map<Long, List<BaseCategoryView>> category1Map = allCategoryList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("categoryId", entry1.getKey());
            jsonObject1.put("categoryName", entry1.getValue().get(0).getCategory1Name());
            ArrayList<JSONObject> arrayList1 = new ArrayList();
            Map<Long, List<BaseCategoryView>> category2Map = entry1.getValue().stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                JSONObject jsonObject2 = new JSONObject();
                jsonObject2.put("categoryId", entry2.getKey());
                jsonObject2.put("categoryName", entry2.getValue().get(0).getCategory2Name());
                List<BaseCategoryView> value = entry2.getValue();
                ArrayList<JSONObject> arrayList2 = new ArrayList();
                for (BaseCategoryView baseCategoryView : value) {
                    JSONObject jsonObject3 = new JSONObject();
                    jsonObject3.put("categoryId", baseCategoryView.getCategory3Id());
                    jsonObject3.put("categoryName", baseCategoryView.getCategory3Name());
                    arrayList2.add(jsonObject3);
                }
                jsonObject2.put("categoryChild", arrayList2);
                arrayList1.add(jsonObject2);
            }
            jsonObject1.put("categoryChild", arrayList1);
            returnList.add(jsonObject1);
        }
        return returnList;*/

        //1.创建响应结果集合对象-用于封装所有一级分类对象
        List<JSONObject> returnList = new ArrayList<>();
        //2.查询所有分类数据-查询视图即可 共计401条记录
        List<BaseCategoryView> allCategoryList = baseCategoryViewMapper.selectList(null);
        //3.处理一级分类数据
        //3.1 对所有分类集合列表进行分组按照1级分类ID进行分组 得到Map<分组ID，一级分类列表>
        Map<Long, List<BaseCategoryView>> category1Map = allCategoryList.stream()
                .collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
            //3.2 封装一级分类对象
            JSONObject jsonObject1 = new JSONObject();
            //3.2.1 封装1级分类ID
            Long category1Id = entry1.getKey();
            jsonObject1.put("categoryId", category1Id);
            //3.2.2 封装1级分类名称
            String category1Name = entry1.getValue().get(0).getCategory1Name();
            jsonObject1.put("categoryName", category1Name);

            //4. 处理二级分类数据
            List<JSONObject> jsonObject2List = new ArrayList<>();
            //4.1 对"1级"分类集合按照二级分类ID进行分组
            Map<Long, List<BaseCategoryView>> category2Map = entry1.getValue()
                    .stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            //4.2 遍历"2级"分类Map
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                //4.3 封装二级分类对象
                JSONObject jsonObject2 = new JSONObject();
                //4.3.1 封装2级分类ID
                Long category2Id = entry2.getKey();
                jsonObject2.put("categoryId", category2Id);
                //4.3.2 封装2级分类名称
                String category2Name = entry2.getValue().get(0).getCategory2Name();
                jsonObject2.put("categoryName", category2Name);
                //4.4 将2级分类对象放入二级分类集合中
                jsonObject2List.add(jsonObject2);
                //5. 处理三级分类数据
                List<JSONObject> jsonObject3List = new ArrayList<>();
                //5.1 对"2级"分类列表进行遍历
                for (BaseCategoryView baseCategoryView : entry2.getValue()) {
                    //5.2 封装三级分类JSONOBject对象
                    JSONObject jsonObject3 = new JSONObject();
                    //5.2.1 封装3级分类ID
                    jsonObject3.put("categoryId", baseCategoryView.getCategory3Id());
                    //5.2.2 封装3级分类名称
                    jsonObject3.put("categoryName", baseCategoryView.getCategory3Name());
                    //5.3 将3级分类对象放入集合中
                    jsonObject3List.add(jsonObject3);
                }
                //5.4 将3级分类对象集合加入到二级分类对象"categoryChild"属性中
                jsonObject2.put("categoryChild", jsonObject3List);
            }
            //4.5 将二级分类集合封装在一级分类对象中"categoryChild"属性中
            jsonObject1.put("categoryChild", jsonObject2List);
            returnList.add(jsonObject1);
        }

        return returnList;
    }
}
