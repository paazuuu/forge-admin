package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.SysTenantDTO;
import com.mdframe.forge.plugin.system.dto.SysTenantQuery;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserTenant;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.plugin.system.service.ISysTenantService;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.plugin.system.vo.SysUserTenantVO;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 租户Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl extends ServiceImpl<SysTenantMapper, SysTenant> implements ISysTenantService {

    private final SysTenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final SysUserTenantMapper userTenantMapper;
    private final IUserLoadService userLoadService;

    @Override
    public IPage<SysTenant> selectTenantPage(SysTenantQuery query) {
        LoginUser loginUser = requireLoginUser();
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(query.getTenantName()), SysTenant::getTenantName, query.getTenantName())
                .like(StringUtils.isNotBlank(query.getContactPerson()), SysTenant::getContactPerson, query.getContactPerson())
                .eq(query.getTenantStatus() != null, SysTenant::getTenantStatus, query.getTenantStatus())
                .orderByDesc(SysTenant::getCreateTime);
        if (!loginUser.isAdmin()) {
            wrapper.eq(SysTenant::getId, loginUser.getTenantId());
        }

        Page<SysTenant> page = new Page<>(query.getPageNum(), query.getPageSize());
        return tenantMapper.selectPage(page, wrapper);
    }

    @Override
    public SysTenant selectTenantById(Long id) {
        assertCanAccessTenant(id);
        return tenantMapper.selectById(id);
    }
    
    @Override
    public SysTenant selectUserTenantConfig(Long tenantId) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        Long targetTenantId = tenantId != null ? tenantId : loginUser.getTenantId();
        assertCanAccessTenant(targetTenantId);
        return tenantMapper.selectById(targetTenantId);
    }

    @Override
    public List<SysUserTenantVO> selectCurrentUserTenants() {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            return TenantContextHolder.executeIgnore(() -> tenantMapper.selectList(
                            new LambdaQueryWrapper<SysTenant>()
                                    .eq(SysTenant::getTenantStatus, 1)
                                    .orderByAsc(SysTenant::getId)))
                    .stream()
                    .map(tenant -> {
                        SysUserTenantVO vo = new SysUserTenantVO();
                        vo.setUserId(loginUser.getUserId());
                        vo.setTenantId(tenant.getId());
                        vo.setTenantName(tenant.getTenantName());
                        vo.setTenantStatus(tenant.getTenantStatus());
                        vo.setExpireTime(tenant.getExpireTime());
                        vo.setMemberType(1);
                        vo.setIsDefault(tenant.getId().equals(loginUser.getTenantId()) ? 1 : 0);
                        vo.setStatus(1);
                        return vo;
                    })
                    .toList();
        }
        return TenantContextHolder.executeIgnore(() -> userTenantMapper.selectUserTenants(loginUser.getUserId(), true));
    }

    @Override
    public List<SysTenant> selectAssignableTenantOptions() {
        LoginUser loginUser = requireLoginUser();
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysTenant::getTenantStatus, 1)
                .orderByAsc(SysTenant::getId);
        if (!loginUser.isAdmin()) {
            wrapper.eq(SysTenant::getId, loginUser.getTenantId());
        }
        return TenantContextHolder.executeIgnore(() -> tenantMapper.selectList(wrapper));
    }

    @Override
    public IPage<SysUser> selectTenantUsers(Long tenantId, SysUserQuery query) {
        assertCanAccessTenant(tenantId);
        SysUserQuery userQuery = query == null ? new SysUserQuery() : query;
        userQuery.setTenantId(tenantId);
        Page<SysUser> page = new Page<>(userQuery.getPageNum(), userQuery.getPageSize());
        return TenantContextHolder.executeIgnore(() -> userMapper.selectUserPage(page, userQuery));
    }

    @Override
    public LoginUser switchTenant(Long tenantId) {
        LoginUser currentUser = requireLoginUser();
        validateTenantAvailable(tenantId);
        if (!currentUser.isAdmin() && !isUserBoundTenant(currentUser.getUserId(), tenantId)) {
            throw new RuntimeException("用户未绑定该租户");
        }
        LoginUser switchedUser = userLoadService.loadUserByUserId(currentUser.getUserId(), tenantId);
        switchedUser.setLoginTime(currentUser.getLoginTime());
        switchedUser.setLoginIp(currentUser.getLoginIp());
        switchedUser.setUserClient(currentUser.getUserClient());
        SessionHelper.setLoginUser(switchedUser);
        TenantContextHolder.setTenantId(tenantId);
        return switchedUser;
    }
    
    @Override
    public boolean insertTenant(SysTenantDTO dto) {
        assertSuperAdmin();
        SysTenant tenant = new SysTenant();
        BeanUtil.copyProperties(dto, tenant);
        return tenantMapper.insert(tenant) > 0;
    }

    @Override
    public boolean updateTenant(SysTenantDTO dto) {
        assertCanAccessTenant(dto.getId());
        SysTenant tenant = new SysTenant();
        BeanUtil.copyProperties(dto, tenant);
        return tenantMapper.updateById(tenant) > 0;
    }

    @Override
    public boolean deleteTenantById(Long id) {
        assertSuperAdmin();
        return tenantMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteTenantByIds(Long[] ids) {
        assertSuperAdmin();
        return tenantMapper.deleteBatchIds(Arrays.asList(ids)) > 0;
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        return loginUser;
    }

    private void assertSuperAdmin() {
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin()) {
            throw new RuntimeException("只有超级管理员可以操作租户");
        }
    }

    private void assertCanAccessTenant(Long tenantId) {
        if (tenantId == null) {
            throw new RuntimeException("租户ID不能为空");
        }
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            return;
        }
        if (!tenantId.equals(loginUser.getTenantId())) {
            throw new RuntimeException("无权访问该租户");
        }
    }

    private void validateTenantAvailable(Long tenantId) {
        SysTenant tenant = TenantContextHolder.executeIgnore(() -> tenantMapper.selectById(tenantId));
        if (tenant == null) {
            throw new RuntimeException("租户不存在");
        }
        if (tenant.getTenantStatus() == null || tenant.getTenantStatus() != 1) {
            throw new RuntimeException("租户已禁用");
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("租户已过期");
        }
    }

    private boolean isUserBoundTenant(Long userId, Long tenantId) {
        Long count = TenantContextHolder.executeIgnore(() ->
                userTenantMapper.selectCount(new LambdaQueryWrapper<SysUserTenant>()
                        .eq(SysUserTenant::getUserId, userId)
                        .eq(SysUserTenant::getTenantId, tenantId)
                        .eq(SysUserTenant::getStatus, 1)));
        return count != null && count > 0;
    }
}
