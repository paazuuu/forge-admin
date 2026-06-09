package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserOrg;
import com.mdframe.forge.plugin.system.entity.SysUserPost;
import com.mdframe.forge.plugin.system.entity.SysUserRole;
import com.mdframe.forge.plugin.system.entity.SysUserTenant;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.plugin.system.dto.UserTenantBindDTO;
import com.mdframe.forge.plugin.system.vo.SysUserTenantVO;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserOrgMapper userOrgMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysUserTenantMapper userTenantMapper;
    private final SysTenantMapper tenantMapper;
    private final SysRoleMapper roleMapper;

    @Override
    public IPage<SysUser> selectUserPage(SysUserQuery query) {
        normalizeUserQueryTenant(query);
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        return TenantContextHolder.executeIgnore(() -> userMapper.selectUserPage(page, query));
    }

    @Override
    public SysUser selectUserById(Long id) {
        assertCanManageUser(id);
        return TenantContextHolder.executeIgnore(() -> userMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertUser(SysUserDTO dto) {
        Long tenantId = resolveWriteTenantId(dto.getTenantId());
        validateUserTypeForWrite(dto);
        SysUser user = new SysUser();
        BeanUtil.copyProperties(dto, user);
        user.setTenantId(tenantId);
        user.setUserType(resolveWriteUserType(dto.getUserType()));
        user.setPassword(PasswordUtil.encrypt(dto.getPassword()));
        boolean inserted = userMapper.insert(user) > 0;
        if (inserted) {
            upsertUserTenant(user.getId(), tenantId, user.getUserType(), true);
            // 同步绑定岗位
            if (dto.getPostIds() != null && !dto.getPostIds().isEmpty()) {
                bindUserPosts(user.getId(), dto.getPostIds(), dto.getPostIds().get(0));
            }
        }
        return inserted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(SysUserDTO dto) {
        assertCanManageUser(dto.getId());
        validateUserTypeForWrite(dto);
        LoginUser loginUser = requireLoginUser();
        SysUser user = new SysUser();
        BeanUtil.copyProperties(dto, user);
        // 修改时不更新密码
        user.setPassword(null);

        Long tenantId = null;
        if (loginUser.isAdmin()) {
            tenantId = resolveWriteTenantId(dto.getTenantId());
            user.setTenantId(tenantId);
            user.setUserType(resolveWriteUserType(dto.getUserType()));
        } else {
            // 租户管理员只维护当前租户成员资料，不改变用户默认租户和全局用户类型。
            user.setTenantId(null);
            user.setUserType(null);
        }

        boolean updated = TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);
        if (updated && loginUser.isAdmin()) {
            upsertUserTenant(user.getId(), tenantId, user.getUserType(), true);
        }
        // 同步绑定岗位
        if (updated && dto.getPostIds() != null) {
            bindUserPosts(user.getId(), dto.getPostIds(), !dto.getPostIds().isEmpty() ? dto.getPostIds().get(0) : null);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserById(Long id) {
        assertCanManageUser(id);
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin()) {
            return removeUserFromTenant(id, loginUser.getTenantId());
        }
        return deleteUserGlobally(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserByIds(Long[] ids) {
        for (Long id : ids) {
            deleteUserById(id);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserRoles(Long userId, Long[] roleIds) {
        if (userId == null || roleIds == null || roleIds.length == 0) {
            return false;
        }
        
        // 防止权限溢出校验：非管理员只能分配自己拥有的角色
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser != null && !loginUser.isAdmin()) {
            List<Long> currentUserRoleIds = loginUser.getRoleIds();
            for (Long roleId : roleIds) {
                if (!currentUserRoleIds.contains(roleId)) {
                    throw new RuntimeException("权限溢出：不能分配自己没有的角色");
                }
            }
        }
        
        // 获取用户信息以获取租户ID
        assertCanManageUser(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }
        Long tenantId = resolveRoleBindTenantId(user);
        validateRoleTenant(roleIds, tenantId);
        
        // 批量插入用户角色关联
        List<SysUserRole> userRoles = new ArrayList<>();
        for (Long roleId : roleIds) {
            // 检查是否已存在
            LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserRole::getUserId, userId)
                    .eq(SysUserRole::getTenantId, tenantId)
                    .eq(SysUserRole::getRoleId, roleId);
            Long count = userRoleMapper.selectCount(wrapper);
            
            if (count == 0) {
                SysUserRole userRole = new SysUserRole();
                userRole.setTenantId(tenantId);
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoles.add(userRole);
            }
        }
        
        if (!userRoles.isEmpty()) {
            userRoles.forEach(userRoleMapper::insert);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindUserRoles(Long userId, Long[] roleIds) {
        if (userId == null || roleIds == null || roleIds.length == 0) {
            return false;
        }
        assertCanManageUser(userId);
        
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getTenantId, resolveTenantScopedOperationTenantId(userId))
                .in(SysUserRole::getRoleId, Arrays.asList(roleIds));
        return userRoleMapper.delete(wrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserOrg(Long userId, Long orgId, Integer isMain) {
        if (userId == null || orgId == null) {
            return false;
        }
        
        // 获取用户信息以获取租户ID
        assertCanManageUser(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }
        Long tenantId = resolveTenantScopedOperationTenantId(user);
        
        // 检查是否已存在
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, tenantId)
                .eq(SysUserOrg::getOrgId, orgId);
        Long count = userOrgMapper.selectCount(wrapper);
        
        if (count > 0) {
            return false;
        }
        
        // 如果是主组织，先取消其他主组织
        if (isMain != null && isMain == 1) {
            LambdaQueryWrapper<SysUserOrg> updateWrapper = new LambdaQueryWrapper<>();
            updateWrapper.eq(SysUserOrg::getUserId, userId)
                    .eq(SysUserOrg::getTenantId, tenantId)
                    .eq(SysUserOrg::getIsMain, 1);
            SysUserOrg updateOrg = new SysUserOrg();
            updateOrg.setIsMain(0);
            userOrgMapper.update(updateOrg, updateWrapper);
        }
        
        SysUserOrg userOrg = new SysUserOrg();
        userOrg.setTenantId(tenantId);
        userOrg.setUserId(userId);
        userOrg.setOrgId(orgId);
        userOrg.setIsMain(isMain != null ? isMain : 0);
        return userOrgMapper.insert(userOrg) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindUserOrg(Long userId, Long orgId) {
        if (userId == null || orgId == null) {
            return false;
        }
        assertCanManageUser(userId);
        
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, resolveTenantScopedOperationTenantId(userId))
                .eq(SysUserOrg::getOrgId, orgId);
        return userOrgMapper.delete(wrapper) > 0;
    }

    @Override
    public List<Long> selectUserRoleIds(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        assertCanManageUser(userId);
        
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getTenantId, resolveTenantScopedOperationTenantId(userId))
                .select(SysUserRole::getRoleId);
        return userRoleMapper.selectList(wrapper)
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> selectUserOrgIds(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        assertCanManageUser(userId);
        
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, resolveTenantScopedOperationTenantId(userId))
                .select(SysUserOrg::getOrgId);
        return userOrgMapper.selectList(wrapper)
                .stream()
                .map(SysUserOrg::getOrgId)
                .collect(Collectors.toList());
    }

    @Override
    public List<SysUserTenantVO> selectUserTenants(Long userId) {
        assertCanManageUser(userId);
        return TenantContextHolder.executeIgnore(() -> userTenantMapper.selectUserTenants(userId, false));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserTenants(Long userId, UserTenantBindDTO dto) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null || !loginUser.isAdmin()) {
            throw new RuntimeException("只有超级管理员可以绑定用户租户");
        }
        if (userId == null || dto == null || dto.getTenantIds() == null || dto.getTenantIds().isEmpty()) {
            return false;
        }
        Set<Long> tenantIds = new HashSet<>(dto.getTenantIds());
        Long defaultTenantId = dto.getDefaultTenantId();
        if (defaultTenantId == null || !tenantIds.contains(defaultTenantId)) {
            defaultTenantId = dto.getTenantIds().get(0);
        }
        Integer memberType = normalizeMemberType(dto.getMemberType());

        LambdaQueryWrapper<SysUserTenant> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserTenant::getUserId, userId)
                .notIn(SysUserTenant::getTenantId, tenantIds);
        List<Long> removedTenantIds = TenantContextHolder.executeIgnore(() -> userTenantMapper.selectList(deleteWrapper))
                .stream()
                .map(SysUserTenant::getTenantId)
                .collect(Collectors.toList());
        if (!removedTenantIds.isEmpty()) {
            TenantContextHolder.executeIgnore(() -> {
                userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
                        .in(SysUserRole::getTenantId, removedTenantIds));
                userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>()
                        .eq(SysUserOrg::getUserId, userId)
                        .in(SysUserOrg::getTenantId, removedTenantIds));
            });
        }
        TenantContextHolder.executeIgnore(() -> userTenantMapper.delete(deleteWrapper));

        for (Long tenantId : tenantIds) {
            upsertUserTenant(userId, tenantId, memberType, Objects.equals(tenantId, defaultTenantId));
        }

        SysUser user = new SysUser();
        user.setId(userId);
        user.setTenantId(defaultTenantId);
        TenantContextHolder.executeIgnore(() -> userMapper.updateById(user));
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserOrgs(Long userId, List<Long> orgIds, Long mainOrgId) {
        if (userId == null || orgIds == null || orgIds.isEmpty()) {
            return false;
        }
        
        // 获取用户信息以获取租户ID
        assertCanManageUser(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }
        Long tenantId = resolveTenantScopedOperationTenantId(user);
        
        // 验证主组织是否在组织列表中
        if (mainOrgId != null && !orgIds.contains(mainOrgId)) {
            mainOrgId = null;
        }
        
        // 获取用户当前的所有组织
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, tenantId);
        List<SysUserOrg> existingOrgs = userOrgMapper.selectList(wrapper);
        List<Long> existingOrgIds = existingOrgs.stream()
                .map(SysUserOrg::getOrgId)
                .collect(Collectors.toList());
        
        // 删除不再需要的组织
        List<Long> toDelete = existingOrgIds.stream()
                .filter(orgId -> !orgIds.contains(orgId))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            LambdaQueryWrapper<SysUserOrg> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SysUserOrg::getUserId, userId)
                    .eq(SysUserOrg::getTenantId, tenantId)
                    .in(SysUserOrg::getOrgId, toDelete);
            userOrgMapper.delete(deleteWrapper);
        }
        
        // 添加新的组织或更新现有组织
        for (Long orgId : orgIds) {
            SysUserOrg userOrg = existingOrgs.stream()
                    .filter(org -> org.getOrgId().equals(orgId))
                    .findFirst()
                    .orElse(null);
            
            if (userOrg == null) {
                // 插入新组织
                userOrg = new SysUserOrg();
                userOrg.setTenantId(tenantId);
                userOrg.setUserId(userId);
                userOrg.setOrgId(orgId);
                userOrg.setIsMain(orgId.equals(mainOrgId) ? 1 : 0);
                userOrgMapper.insert(userOrg);
            } else {
                // 更新现有组织的主组织标记
                int newIsMain = orgId.equals(mainOrgId) ? 1 : 0;
                if (!userOrg.getIsMain().equals(newIsMain)) {
                    userOrg.setIsMain(newIsMain);
                    userOrgMapper.updateById(userOrg);
                }
            }
        }
        
        return true;
    }
    
    @Override
    public void doUntieDisable(Long userId) {
        StpUtil.untieDisable(userId);
        // 同时更新数据库状态为正常
        this.updateUserStatus(userId, 1);
    }

    @Override
    public boolean resetPassword(Long userId, String newPassword) {
        assertCanManageUser(userId);
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(PasswordUtil.encrypt(newPassword));
        return TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);
    }

    @Override
    public boolean updateUserStatus(Long userId, Integer status) {
        assertCanManageUser(userId);
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin()) {
            SysUserTenant member = new SysUserTenant();
            member.setStatus(status != null && status == 1 ? 1 : 0);
            LambdaQueryWrapper<SysUserTenant> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserTenant::getUserId, userId)
                    .eq(SysUserTenant::getTenantId, loginUser.getTenantId());
            return TenantContextHolder.executeIgnore(() -> userTenantMapper.update(member, wrapper) > 0);
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUserStatus(status);
        return TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);
    }

    @Override
    public boolean updateUserProfile(SysUserDTO dto) {
        Long currentUserId = SessionHelper.getUserId();
        if (currentUserId == null) {
            throw new RuntimeException("用户未登录");
        }

        SysUser user = new SysUser();
        user.setId(currentUserId);
        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());

        boolean updated = TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);

        // 同步更新 Session 中的 LoginUser，确保 /auth/userInfo 返回最新数据
        if (updated) {
            com.mdframe.forge.starter.core.session.LoginUser loginUser = SessionHelper.getLoginUser();
            if (loginUser != null) {
                if (dto.getUsername() != null) loginUser.setUsername(dto.getUsername());
                if (dto.getRealName() != null) loginUser.setRealName(dto.getRealName());
                if (dto.getPhone() != null) loginUser.setPhone(dto.getPhone());
                if (dto.getEmail() != null) loginUser.setEmail(dto.getEmail());
                if (dto.getAvatar() != null) loginUser.setAvatar(dto.getAvatar());
                SessionHelper.setLoginUser(loginUser);
            }
        }

        return updated;
    }

    private void normalizeUserQueryTenant(SysUserQuery query) {
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin()) {
            query.setTenantId(loginUser.getTenantId());
        }
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        return loginUser;
    }

    private Long resolveWriteTenantId(Long requestedTenantId) {
        LoginUser loginUser = requireLoginUser();
        Long tenantId = loginUser.isAdmin()
                ? (requestedTenantId != null ? requestedTenantId : loginUser.getTenantId())
                : loginUser.getTenantId();
        validateTenantEnabled(tenantId);
        return tenantId;
    }

    private Long resolveCurrentTenantIdForNonAdmin() {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.getTenantId() == null) {
            throw new RuntimeException("用户未登录");
        }
        return loginUser.getTenantId();
    }

    private Long resolveRoleBindTenantId(SysUser user) {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            return user.getTenantId() != null ? user.getTenantId() : loginUser.getTenantId();
        }
        return resolveCurrentTenantIdForNonAdmin();
    }

    private void validateUserTypeForWrite(SysUserDTO dto) {
        LoginUser loginUser = requireLoginUser();
        Integer userType = dto.getUserType();
        if (loginUser.isAdmin()) {
            return;
        }
        if (userType != null && userType != 2) {
            throw new RuntimeException("租户管理员只能维护普通用户");
        }
    }

    private Integer resolveWriteUserType(Integer requestedUserType) {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            return requestedUserType != null ? requestedUserType : 2;
        }
        return 2;
    }

    private void validateTenantEnabled(Long tenantId) {
        if (tenantId == null) {
            throw new RuntimeException("租户不能为空");
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                tenantMapper.selectCount(new LambdaQueryWrapper<com.mdframe.forge.plugin.system.entity.SysTenant>()
                        .eq(com.mdframe.forge.plugin.system.entity.SysTenant::getId, tenantId)
                        .eq(com.mdframe.forge.plugin.system.entity.SysTenant::getTenantStatus, 1)));
        if (count == null || count == 0) {
            throw new RuntimeException("租户不存在或已禁用");
        }
    }

    private void assertCanManageUser(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            return;
        }
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getUserType() != null && user.getUserType() == 0) {
            throw new RuntimeException("无权操作超级管理员");
        }
        if (!isUserInTenant(userId, loginUser.getTenantId())) {
            throw new RuntimeException("无权操作非本租户用户");
        }
    }

    private Long resolveTenantScopedOperationTenantId(Long userId) {
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return resolveTenantScopedOperationTenantId(user);
    }

    private Long resolveTenantScopedOperationTenantId(SysUser user) {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            return user.getTenantId() != null ? user.getTenantId() : loginUser.getTenantId();
        }
        return resolveCurrentTenantIdForNonAdmin();
    }

    private boolean isUserInTenant(Long userId, Long tenantId) {
        if (userId == null || tenantId == null) {
            return false;
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                userTenantMapper.selectCount(new LambdaQueryWrapper<SysUserTenant>()
                        .eq(SysUserTenant::getUserId, userId)
                        .eq(SysUserTenant::getTenantId, tenantId)
                        .eq(SysUserTenant::getStatus, 1)));
        return count != null && count > 0;
    }

    private void validateRoleTenant(Long[] roleIds, Long tenantId) {
        if (roleIds == null || roleIds.length == 0) {
            return;
        }
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, Arrays.asList(roleIds))
                .eq(SysRole::getTenantId, tenantId));
        if (count == null || count != roleIds.length) {
            throw new RuntimeException("角色不属于当前操作租户");
        }
    }

    private void upsertUserTenant(Long userId, Long tenantId, Integer memberType, boolean defaultTenant) {
        if (userId == null || tenantId == null) {
            return;
        }
        if (defaultTenant) {
            SysUserTenant update = new SysUserTenant();
            update.setIsDefault(0);
            LambdaQueryWrapper<SysUserTenant> defaultWrapper = new LambdaQueryWrapper<>();
            defaultWrapper.eq(SysUserTenant::getUserId, userId);
            TenantContextHolder.executeIgnore(() -> userTenantMapper.update(update, defaultWrapper));
        }

        LambdaQueryWrapper<SysUserTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserTenant::getUserId, userId)
                .eq(SysUserTenant::getTenantId, tenantId);
        SysUserTenant existing = TenantContextHolder.executeIgnore(() -> userTenantMapper.selectOne(wrapper));
        if (existing == null) {
            SysUserTenant member = new SysUserTenant();
            member.setUserId(userId);
            member.setTenantId(tenantId);
            member.setMemberType(normalizeMemberType(memberType));
            member.setIsDefault(defaultTenant ? 1 : 0);
            member.setStatus(1);
            TenantContextHolder.executeIgnore(() -> userTenantMapper.insert(member));
            return;
        }

        existing.setMemberType(normalizeMemberType(memberType));
        existing.setIsDefault(defaultTenant ? 1 : 0);
        existing.setStatus(1);
        TenantContextHolder.executeIgnore(() -> userTenantMapper.updateById(existing));
    }

    private Integer normalizeMemberType(Integer memberType) {
        return memberType != null && memberType == 1 ? 1 : 2;
    }

    private boolean removeUserFromTenant(Long userId, Long tenantId) {
        return TenantContextHolder.executeIgnore(() -> {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, userId)
                    .eq(SysUserRole::getTenantId, tenantId));
            userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>()
                    .eq(SysUserOrg::getUserId, userId)
                    .eq(SysUserOrg::getTenantId, tenantId));

            int deleted = userTenantMapper.delete(new LambdaQueryWrapper<SysUserTenant>()
                    .eq(SysUserTenant::getUserId, userId)
                    .eq(SysUserTenant::getTenantId, tenantId));

            Long remainingTenants = userTenantMapper.selectCount(new LambdaQueryWrapper<SysUserTenant>()
                    .eq(SysUserTenant::getUserId, userId)
                    .eq(SysUserTenant::getStatus, 1));
            if (remainingTenants == null || remainingTenants == 0) {
                userMapper.deleteById(userId);
            }
            return deleted > 0;
        });
    }

    private boolean deleteUserGlobally(Long userId) {
        return TenantContextHolder.executeIgnore(() -> {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
            userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>().eq(SysUserOrg::getUserId, userId));
            userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId));
            userTenantMapper.delete(new LambdaQueryWrapper<SysUserTenant>().eq(SysUserTenant::getUserId, userId));
            return userMapper.deleteById(userId) > 0;
        });
    }

    @Override
    public List<Long> selectUserPostIds(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        assertCanManageUser(userId);

        LambdaQueryWrapper<SysUserPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserPost::getUserId, userId)
                .eq(SysUserPost::getTenantId, resolveTenantScopedOperationTenantId(userId))
                .select(SysUserPost::getPostId);
        return userPostMapper.selectList(wrapper)
                .stream()
                .map(SysUserPost::getPostId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserPosts(Long userId, List<Long> postIds, Long mainPostId) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return false;
        }

        assertCanManageUser(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }
        Long tenantId = resolveTenantScopedOperationTenantId(user);

        // 验证主岗位是否在岗位列表中
        if (mainPostId != null && !postIds.contains(mainPostId)) {
            mainPostId = null;
        }

        // 获取当前所有岗位
        LambdaQueryWrapper<SysUserPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserPost::getUserId, userId)
                .eq(SysUserPost::getTenantId, tenantId);
        List<SysUserPost> existingPosts = userPostMapper.selectList(wrapper);
        List<Long> existingPostIds = existingPosts.stream()
                .map(SysUserPost::getPostId)
                .collect(Collectors.toList());

        // 删除不再需要的岗位
        List<Long> toDelete = existingPostIds.stream()
                .filter(postId -> !postIds.contains(postId))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            LambdaQueryWrapper<SysUserPost> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SysUserPost::getUserId, userId)
                    .eq(SysUserPost::getTenantId, tenantId)
                    .in(SysUserPost::getPostId, toDelete);
            userPostMapper.delete(deleteWrapper);
        }

        // 添加新岗位或更新现有岗位
        for (Long postId : postIds) {
            SysUserPost userPost = existingPosts.stream()
                    .filter(p -> p.getPostId().equals(postId))
                    .findFirst()
                    .orElse(null);

            if (userPost == null) {
                userPost = new SysUserPost();
                userPost.setTenantId(tenantId);
                userPost.setUserId(userId);
                userPost.setPostId(postId);
                userPost.setIsMain(postId.equals(mainPostId) ? 1 : 0);
                userPostMapper.insert(userPost);
            } else {
                int newIsMain = postId.equals(mainPostId) ? 1 : 0;
                if (!userPost.getIsMain().equals(newIsMain)) {
                    userPost.setIsMain(newIsMain);
                    userPostMapper.updateById(userPost);
                }
            }
        }

        return true;
    }
}
