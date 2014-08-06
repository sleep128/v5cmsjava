package cn.v5cn.v5cms.biz.impl;

import cn.v5cn.v5cms.biz.AdvBiz;
import cn.v5cn.v5cms.dao.AdvDao;
import cn.v5cn.v5cms.entity.Adv;
import cn.v5cn.v5cms.entity.AdvPos;
import cn.v5cn.v5cms.entity.wrapper.AdvWrapper;
import cn.v5cn.v5cms.util.PropertyUtils;
import cn.v5cn.v5cms.util.SystemUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * Created by ZYW on 2014/7/28.
 */
@Service("advBiz")
public class AdvBizImpl implements AdvBiz {

    @Autowired
    private AdvDao advDao;

    @Override
    @Transactional
    public Adv save(AdvWrapper advWrapper) throws JsonProcessingException {
        Adv adv = advWrapper.getAdv();
        String advTypeJson = advTypeJson(advWrapper.getAti(), adv.getAdvType());

        adv.setAdvTypeInfo(advTypeJson);
        adv.setCreateDate(DateTime.now().toDate());
        return advDao.save(adv);
    }

    @Override
    @Transactional
    public Adv update(AdvWrapper advWrapper) throws JsonProcessingException
    ,IllegalAccessException, InvocationTargetException {
        Adv fontAdv = advWrapper.getAdv();
        String advTypeJson = advTypeJson(advWrapper.getAti(), fontAdv.getAdvType());
        fontAdv.setAdvTypeInfo(advTypeJson);

        Adv dbAdv = advDao.findOne(fontAdv.getAdvId());
        SystemUtils.copyProperties(dbAdv,fontAdv);
        return dbAdv;
    }

    @Override
    public void deleteAdvs(Long[] advIds) {
        List<Adv> advs = Lists.newArrayList();
        Adv adv = null;
        for(Long advId : advIds){
            adv = new Adv();
            adv.setAdvId(advId);
            advs.add(adv);
        }
        advDao.delete(advs);
    }

    @Override
    public Page<Adv> findAdvByAdvNamePageable(final Adv adv, Integer currPage) {
        int pageSize = Integer.valueOf(PropertyUtils.getValue("page.size").or("0"));

        return advDao.findAll(new Specification<Adv>() {

            @Override
            public javax.persistence.criteria.Predicate toPredicate(Root<Adv> advRoot, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                Join<Adv,AdvPos> advPos = advRoot.join("advPos", JoinType.INNER);
                List<javax.persistence.criteria.Predicate> ps = Lists.newArrayList();
                if(StringUtils.isNotBlank(adv.getAdvName())){
                    Path<String> advName = advRoot.get("advName");
                    ps.add(criteriaBuilder.like(advName, "%" + adv.getAdvName() + "%"));
                }
                if(adv.getAdvPos()!= null && adv.getAdvPos().getAdvPosId() != null){
                    Path<Integer> advPosId = advPos.get("advPosId");
                    ps.add(criteriaBuilder.equal(advPosId, adv.getAdvPos().getAdvPosId()));
                }
                return criteriaBuilder.and(ps.toArray(new javax.persistence.criteria.Predicate[0]));
            }
        }, new PageRequest(currPage,pageSize,new Sort(Sort.Direction.DESC,new String[]{"createDate"})));
    }

    @Override
    public Adv findOne(Long advId) {
        return advDao.findOne(advId);
    }

    private Map<String,Object> filterMapKeys(Map<String, Object> reqMap,final String key) {
        Map<String, Object> filterAfterMap = Maps.filterKeys(reqMap, new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return StringUtils.contains(input,key);
            }
        });
        return filterAfterMap;
    }
    private String advTypeJson(Map<String,Object> reqMap, int advType) throws JsonProcessingException {
        Map<String,Object> filterAfterMap = null;
        if(advType == 1){
            filterAfterMap = filterMapKeys(reqMap,"adv_image_");
        }else if(advType == 2){
            filterAfterMap = filterMapKeys(reqMap,"adv_flash_");
        }else if(advType == 3){
            filterAfterMap = filterMapKeys(reqMap,"adv_text_");
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(filterAfterMap);
    }
}
