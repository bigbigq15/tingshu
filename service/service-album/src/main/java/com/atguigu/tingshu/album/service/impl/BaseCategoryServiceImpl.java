package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.shaded.com.google.gson.JsonObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    @Autowired
    private BaseAttributeMapper baseAttributeMapper;


    /**
     * 查询所有分类（1、2、3级分类）
     *
     * @return 业务数据：[{"categoryId":1,"categoryName":"分类",categoryChild:[..]},{其他1级分类}]
     */
    @Override
    public List<JSONObject> getBaseCategoryList() {
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


    /**
     * 根据一级分类Id获取分类属性以及属性值（标签名，标签值）列表
     *
     * @param category1Id
     * @return
     */
    @Override
    public List<BaseAttribute> findAttribute(Long category1Id) {
        //1.获取持久层接口，调用持久层动态SQL
        return baseAttributeMapper.getAttributesByCategory1Id(category1Id);
    }

    /**
     * 根据3级分类ID查询分类视图对象
     * @param category3Id
     * @return
     */
    @Override
    public BaseCategoryView getCategoryView(Long category3Id) {
        return baseCategoryViewMapper.selectById(category3Id);
    }

    /**
     * 查询1级分类下包含所有2,3级分类列表
     *
     * @param category1Id
     * @return 1级分类对象 包含2,3级分类列表
     */
    @Override
    public JSONObject getBaseCategoryListByCategory1Id(Long category1Id) {
        //1.处理1级分类JSON对象
        //1.1 创建1级分类JSON对象
        JSONObject jsonObject1 = new JSONObject();
        //1.2 根据1级分类ID查询分类视图得到"1级"分类列表
        List<BaseCategoryView> category1ViewList = baseCategoryViewMapper.selectList(
                new LambdaQueryWrapper<BaseCategoryView>()
                        .eq(BaseCategoryView::getCategory1Id, category1Id)
        );

        //1.3. 封装1级JSON对象 1级分类ID跟名称
        jsonObject1.put("categoryId", category1ViewList.get(0).getCategory1Id());
        jsonObject1.put("categoryName", category1ViewList.get(0).getCategory1Name());

        //2.处理2级分类
        //2.1 创建2级分类JSON集合
        ArrayList<JSONObject> jsonObject2List = new ArrayList<>();
        //2.2 对"1级"分类列表按照2级分类ID进行分组 得到 二级分类Map key="二级分类ID" value="'2级'分类列表"
        Map<Long, List<BaseCategoryView>> map2 = category1ViewList
                .stream()
                .collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
        //2.3 遍历Map 封装二级分类对象
        for (Map.Entry<Long, List<BaseCategoryView>> entry2 : map2.entrySet()) {
            JSONObject jsonObject2 = new JSONObject();
            jsonObject2.put("categoryId", entry2.getKey());
            jsonObject2.put("categoryName", entry2.getValue().get(0).getCategory2Name());
            jsonObject2List.add(jsonObject2);
            //3.处理3级分类
            //3.1 创建3级分类JSON集合
            ArrayList<JSONObject> jsonObject3List = new ArrayList<>();
            //3.2 遍历"2级分类列表"
            for (BaseCategoryView baseCategoryView : entry2.getValue()) {
                //3.3 封装三级分类JSON对象
                JSONObject jsonObject3 = new JSONObject();
                jsonObject3.put("categoryId", baseCategoryView.getCategory3Id());
                jsonObject3.put("categoryName", baseCategoryView.getCategory3Name());
                //3.4 将3级分类JSON对象加入3级分类集合中
                jsonObject3List.add(jsonObject3);
            }
            //3.5 将3级分类列表加入到2级分类对象"categoryChild"中
            jsonObject2.put("categoryChild", jsonObject3List);
        }
        //2.4 将二级列表加入一级分类对象"categoryChild"中
        jsonObject1.put("categoryChild", jsonObject2List);

        //4.响应1级分类JSON对象
        return jsonObject1;
    }

    @Override
    public List<BaseCategory3> findTop7BaseCategory3(Long category1Id) {
        /*ArrayList<BaseCategory3> result = new ArrayList<>();
        List<BaseCategory2> baseCategory2List = baseCategory2Mapper
                .selectList(new LambdaQueryWrapper<BaseCategory2>().eq(BaseCategory2::getCategory1Id, category1Id));
        for (BaseCategory2 baseCategory2 : baseCategory2List) {
            List<BaseCategory3> baseCategory3List = baseCategory3Mapper.selectList(new LambdaQueryWrapper<BaseCategory3>()
                    .eq(BaseCategory3::getCategory2Id, baseCategory2.getId()).eq(BaseCategory3::getIsTop, 1));
            result.addAll(baseCategory3List);
        }
        result.subList(0,7);
        return result;*/

        //1.根据1级分类ID查询二级分类ID列表
        List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(
                new LambdaQueryWrapper<BaseCategory2>()
                        .eq(BaseCategory2::getCategory1Id, category1Id)
                        .select(BaseCategory2::getId)
        );
        //2.根据二级分类ID列表+置顶标识+排序+数量限制 查询三级分类列表
        if (CollUtil.isNotEmpty(baseCategory2List)) {
            List<Long> category2IdList = baseCategory2List.stream().map(BaseCategory2::getId).collect(Collectors.toList());
            LambdaQueryWrapper<BaseCategory3> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(BaseCategory3::getCategory2Id, category2IdList);
            queryWrapper.eq(BaseCategory3::getIsTop, 1);
            queryWrapper.orderByAsc(BaseCategory3::getOrderNum);
            queryWrapper.last("limit 7");
            List<BaseCategory3> baseCategory3List = baseCategory3Mapper.selectList(queryWrapper);
            return baseCategory3List;
        }
        return null;
    }



}
