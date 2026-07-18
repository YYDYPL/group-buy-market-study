package com.hjs.study.infrastructure.dao;

import com.hjs.study.infrastructure.dao.po.GroupBuyOrderList;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface IGroupBuyOrderListDao {

    void insert(GroupBuyOrderList groupBuyOrderListReq);

    GroupBuyOrderList queryGroupBuyOrderRecordByOutTradeNo(GroupBuyOrderList groupBuyOrderListReq);

}
