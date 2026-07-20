package com.hjs.study.infrastructure.dao.po.base;

/**
 * DAO 查询对象的分页/数量基类。
 * <p>
 * 当前项目里没有使用常见的 pageNo、pageSize 结构，而是只保留了一个 count 字段，
 * 主要用于告诉 Mapper 本次最多取多少条记录，例如“查询我参与中的 N 条拼团记录”。
 * 因此它更像是“查询条数限制参数”，而不是完整的分页模型。
 */
public class Page {

    /**
     * 查询数量上限。
     * <p>
     * 由上层仓储在构造查询条件时设置，最终通常会映射到 SQL 的 limit 参数，
     * 用来控制本次查询返回的数据条数。
     */
    private Integer count;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
