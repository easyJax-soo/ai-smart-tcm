package com.bobo.aismartcloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ai.ai_chat_memory 表数据访问层
 *
 * 继承 BaseMapper 获得 selectById / deleteById / updateById / selectList 等
 * 类型安全 API；自定义方法（selectAll / selectAllChatIds / upsert）见同名 XML。
 */
@Mapper
public interface ChatMemoryMapper extends BaseMapper<ChatMemoryEntity> {

    /** 全量查询（按 updated_at DESC），侧边栏列表用 */
    List<ChatMemoryEntity> selectAll();

    /** 全量查询 chat_id（Spring AI ChatMemoryRepository 接口要求） */
    List<String> selectAllChatIds();

    /** UPSERT：主键冲突则更新 messages + updated_at */
    int upsert(ChatMemoryEntity entity);
}
