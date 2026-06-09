package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.RoleUserQuery;
import com.mdframe.forge.plugin.system.dto.SysRoleQuery;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色Mapper接口
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 分页查询角色列表
     */
    IPage<SysRole> selectRolePage(Page<SysRole> page, @Param("query") SysRoleQuery query);

    /**
     * 查询角色详情
     */
    SysRole selectRoleById(@Param("id") Long id);

    /**
     * 分页查询角色下的用户
     */
    IPage<SysUser> selectRoleUsers(Page<SysUser> page, @Param("query") RoleUserQuery query);
}
