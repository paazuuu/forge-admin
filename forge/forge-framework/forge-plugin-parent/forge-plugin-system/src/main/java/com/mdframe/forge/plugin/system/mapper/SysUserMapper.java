package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户Mapper接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 分页查询用户列表
     */
    IPage<SysUser> selectUserPage(Page<SysUser> page, @Param("query") SysUserQuery query);

    /**
     * 登录时按用户名和当前租户查询用户
     */
    SysUser selectByUsernameForLogin(@Param("username") String username, @Param("tenantId") Long tenantId);

    /**
     * 登录时按手机号和当前租户查询用户
     */
    SysUser selectByPhoneForLogin(@Param("phone") String phone, @Param("tenantId") Long tenantId);

    /**
     * 登录时按邮箱和当前租户查询用户
     */
    SysUser selectByEmailForLogin(@Param("email") String email, @Param("tenantId") Long tenantId);
}
