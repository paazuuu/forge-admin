package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 代码生成表字段配置Mapper接口
 */
@Mapper
public interface GenTableColumnMapper extends BaseMapper<GenTableColumn> {

    /**
     * 根据表名查询数据库表字段信息
     */
    @Select("SELECT " +
            "    column_name AS columnName, " +
            "    column_comment AS columnComment, " +
            "    column_type AS columnType, " +
            "    (CASE WHEN column_key = 'PRI' THEN 1 ELSE 0 END) AS isPk, " +
            "    (CASE WHEN extra = 'auto_increment' THEN 1 ELSE 0 END) AS isIncrement, " +
            "    (CASE WHEN is_nullable = 'NO' AND column_key != 'PRI' THEN 1 ELSE 0 END) AS isRequired " +
            "FROM information_schema.columns " +
            "WHERE table_schema = (SELECT DATABASE()) " +
            "AND table_name = #{tableName} " +
            "ORDER BY ordinal_position")
    List<GenTableColumn> selectDbTableColumnsByName(@Param("tableName") String tableName);

    /**
     * 按数据模型字段配置同步表模型字段必填状态。
     */
    int updateRequiredByTableRef(@Param("tableId") Long tableId,
                                 @Param("tableName") String tableName,
                                 @Param("columns") List<Map<String, Object>> columns);
}
