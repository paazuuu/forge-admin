package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.system.dto.BatchUserRoleBindDTO;
import com.mdframe.forge.plugin.system.dto.BatchUserTenantBindDTO;
import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.dto.UserOrgBindDTO;
import com.mdframe.forge.plugin.system.dto.UserOrgRoleBindDTO;
import com.mdframe.forge.plugin.system.dto.UserPostBindDTO;
import com.mdframe.forge.plugin.system.dto.UserTenantBindDTO;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.plugin.system.vo.SysUserTenantVO;
import com.mdframe.forge.plugin.system.vo.UserOrgBindingVO;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.core.util.SensitiveDataUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理Controller
 */
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysUserController {

    private final ISysUserService userService;

    /**
     * 分页查询用户列表
     */
    @GetMapping("/page")
    public RespInfo<IPage<SysUser>> page(SysUserQuery query) {
        IPage<SysUser> page = userService.selectUserPage(query);
        sanitizeUserPage(page);
        return RespInfo.success(page);
    }

    /**
     * 根据ID查询用户详情
     */
    @PostMapping("/getById")
    public RespInfo<SysUser> getById(@RequestParam Long id) {
        SysUser user = userService.selectUserById(id);
        clearAuthFields(user);
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser != null && loginUser.isAdmin()) {
            List<SysUserTenantVO> userTenants = userService.selectUserTenants(id);
            List<SysUserTenantVO> enabledUserTenants = userTenants.stream()
                    .filter(item -> item.getStatus() == null || item.getStatus() != 0)
                    .toList();
            user.setTenantIds(enabledUserTenants.stream()
                    .map(SysUserTenantVO::getTenantId)
                    .toList());
            user.setTenantName(enabledUserTenants.stream()
                    .map(SysUserTenantVO::getTenantName)
                    .filter(name -> name != null && !name.isBlank())
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(",")));
        } else if (loginUser != null) {
            Long tenantId = loginUser.getTenantId();
            user.setTenantId(tenantId);
            user.setTenantIds(tenantId == null ? List.of() : List.of(tenantId));
        }
        // 填充用户岗位ID列表
        user.setPostIds(userService.selectUserPostIds(id));
        // 填充用户角色ID列表，支持新增/编辑表单直接维护角色
        user.setRoleIds(userService.selectUserRoleIds(id));
        return RespInfo.success(user);
    }

    /**
     * 新增用户
     */
    @PostMapping("/add")
    public RespInfo<Void> add(@RequestBody SysUserDTO dto) {
        boolean result = userService.insertUser(dto);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改用户
     */
    @PostMapping("/edit")
    public RespInfo<Void> edit(@RequestBody SysUserDTO dto) {
        boolean result = userService.updateUser(dto);
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除用户
     */
    @PostMapping("/remove")
    public RespInfo<Void> remove(@RequestParam Long id) {
        boolean result = userService.deleteUserById(id);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }
    
    @PostMapping("/doUntieDisable")
    public RespInfo<Void> doUntieDisable(@RequestParam Long id) {
        userService.doUntieDisable(id);
        return RespInfo.success();
    }

    /**
     * 批量删除用户
     */
    @PostMapping("/removeBatch")
    public RespInfo<Void> removeBatch(@RequestBody Long[] ids) {
        boolean result = userService.deleteUserByIds(ids);
        return result ? RespInfo.success() : RespInfo.error("批量删除失败");
    }

    /**
     * 给用户绑定角色
     */
    @PostMapping("/{userId}/roles")
    public RespInfo<Void> bindRoles(@PathVariable Long userId,
                                    @RequestBody Long[] roleIds,
                                    @RequestParam(required = false) Long tenantId) {
        boolean result = userService.bindUserRoles(userId, roleIds, tenantId);
        return result ? RespInfo.success() : RespInfo.error("绑定角色失败");
    }

    /**
     * 批量给用户追加角色。
     */
    @PostMapping("/batch/roles")
    public RespInfo<Void> batchBindRoles(@RequestBody BatchUserRoleBindDTO dto) {
        boolean result = userService.batchBindUserRoles(dto);
        return result ? RespInfo.success() : RespInfo.error("批量授权失败");
    }

    /**
     * 解除用户角色
     */
    @PostMapping("/{userId}/roles/unbind")
    public RespInfo<Void> unbindRoles(@PathVariable Long userId, @RequestBody Long[] roleIds) {
        boolean result = userService.unbindUserRoles(userId, roleIds);
        return result ? RespInfo.success() : RespInfo.error("解除角色失败");
    }

    /**
     * 给用户绑定组织
     */
    @PostMapping("/{userId}/org")
    public RespInfo<Void> bindOrg(@PathVariable Long userId, @RequestParam Long orgId, @RequestParam(required = false, defaultValue = "0") Integer isMain) {
        boolean result = userService.bindUserOrg(userId, orgId, isMain);
        return result ? RespInfo.success() : RespInfo.error("绑定组织失败");
    }

    /**
     * 解除用户组织
     */
    @PostMapping("/{userId}/org/unbind")
    public RespInfo<Void> unbindOrg(@PathVariable Long userId, @RequestParam Long orgId) {
        boolean result = userService.unbindUserOrg(userId, orgId);
        return result ? RespInfo.success() : RespInfo.error("解除组织失败");
    }

    /**
     * 查询用户的角色ID列表
     */
    @GetMapping("/{userId}/roles")
    public RespInfo<List<Long>> getUserRoleIds(@PathVariable Long userId,
                                               @RequestParam(required = false) Long tenantId) {
        List<Long> roleIds = userService.selectUserRoleIds(userId, tenantId);
        return RespInfo.success(roleIds);
    }

    /**
     * 查询用户的组织ID列表
     */
    @GetMapping("/{userId}/orgs")
    public RespInfo<List<Long>> getUserOrgIds(@PathVariable Long userId,
                                              @RequestParam(required = false) Long tenantId) {
        List<Long> orgIds = userService.selectUserOrgIds(userId, tenantId);
        return RespInfo.success(orgIds);
    }

    /**
     * 查询用户组织绑定详情。
     */
    @GetMapping("/{userId}/org-bindings")
    public RespInfo<List<UserOrgBindingVO>> getUserOrgBindings(@PathVariable Long userId,
                                                               @RequestParam(required = false) Long tenantId) {
        return RespInfo.success(userService.selectUserOrgBindings(userId, tenantId));
    }

    /**
     * 查询用户的租户绑定列表
     */
    @GetMapping("/{userId}/tenants")
    public RespInfo<List<SysUserTenantVO>> getUserTenants(@PathVariable Long userId) {
        return RespInfo.success(userService.selectUserTenants(userId));
    }

    /**
     * 批量绑定用户租户
     */
    @PostMapping("/{userId}/tenants")
    public RespInfo<Void> bindTenants(@PathVariable Long userId, @RequestBody UserTenantBindDTO dto) {
        boolean result = userService.bindUserTenants(userId, dto);
        return result ? RespInfo.success() : RespInfo.error("绑定租户失败");
    }

    /**
     * 批量将用户加入租户。
     */
    @PostMapping("/batch/tenants")
    public RespInfo<Void> batchBindTenant(@RequestBody BatchUserTenantBindDTO dto) {
        boolean result = userService.batchBindUserTenant(dto);
        return result ? RespInfo.success() : RespInfo.error("批量加入租户失败");
    }

    /**
     * 批量绑定用户组织
     */
    @PostMapping("/{userId}/orgs")
    public RespInfo<Void> bindOrgs(@PathVariable Long userId,
                                   @RequestBody UserOrgBindDTO dto,
                                   @RequestParam(required = false) Long tenantId) {
        boolean result = userService.bindUserOrgs(userId, dto.getOrgIds(), dto.getMainOrgId(), tenantId);
        return result ? RespInfo.success() : RespInfo.error("绑定组织失败");
    }

    /**
     * 查询用户在指定组织下的角色ID列表。
     */
    @GetMapping("/{userId}/org-roles")
    public RespInfo<List<Long>> getUserOrgRoleIds(@PathVariable Long userId,
                                                  @RequestParam Long orgId,
                                                  @RequestParam(required = false) Long tenantId) {
        return RespInfo.success(userService.selectUserOrgRoleIds(userId, orgId, tenantId));
    }

    /**
     * 保存用户在指定组织下的角色。
     */
    @PostMapping("/{userId}/org-roles")
    public RespInfo<Void> bindUserOrgRoles(@PathVariable Long userId,
                                           @RequestBody UserOrgRoleBindDTO dto) {
        boolean result = userService.bindUserOrgRoles(userId, dto.getOrgId(), dto.getRoleIds(), dto.getTenantId());
        return result ? RespInfo.success() : RespInfo.error("绑定组织角色失败");
    }

    /**
     * 查询用户的岗位ID列表
     */
    @GetMapping("/{userId}/posts")
    public RespInfo<List<Long>> getUserPostIds(@PathVariable Long userId,
                                               @RequestParam(required = false) Long tenantId) {
        List<Long> postIds = userService.selectUserPostIds(userId, tenantId);
        return RespInfo.success(postIds);
    }

    /**
     * 批量绑定用户岗位
     */
    @PostMapping("/{userId}/posts")
    public RespInfo<Void> bindPosts(@PathVariable Long userId,
                                    @RequestBody UserPostBindDTO dto,
                                    @RequestParam(required = false) Long tenantId) {
        boolean result = userService.bindUserPosts(userId, dto.getPostIds(), dto.getMainPostId(), tenantId);
        return result ? RespInfo.success() : RespInfo.error("绑定岗位失败");
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/resetPwd")
    public RespInfo<Void> resetPwd(@RequestParam Long id, @RequestParam String password) {
        boolean result = userService.resetPassword(id, password);
        return result ? RespInfo.success() : RespInfo.error("重置密码失败");
    }

    /**
     * 更新用户状态
     */
    @PostMapping("/updateStatus")
    public RespInfo<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        boolean result = userService.updateUserStatus(id, status);
        return result ? RespInfo.success() : RespInfo.error("操作失败");
    }

    /**
     * 更新用户资料
     */
    @PostMapping("/updateProfile")
    public RespInfo<Void> updateProfile(@RequestBody SysUserDTO dto) {
        boolean result = userService.updateUserProfile(dto);
        return result ? RespInfo.success() : RespInfo.error("更新资料失败");
    }

    /**
     * 获取当前登录用户的基本资料（直接查数据库，非缓存）
     */
    @GetMapping("/profile")
    public RespInfo<SysUser> profile() {
        Long userId = SessionHelper.getUserId();
        SysUser user = userService.getById(userId);
        clearAuthFields(user);
        return RespInfo.success(user);
    }

    private void sanitizeUserPage(IPage<SysUser> page) {
        if (page == null || page.getRecords() == null) {
            return;
        }
        page.getRecords().forEach(user -> {
            clearAuthFields(user);
            user.setPhone(SensitiveDataUtil.maskPhone(user.getPhone()));
            user.setIdCard(SensitiveDataUtil.maskIdCard(user.getIdCard()));
            user.setEmail(SensitiveDataUtil.maskEmail(user.getEmail()));
        });
    }

    private void clearAuthFields(SysUser user) {
        if (user == null) {
            return;
        }
        user.setPassword(null);
        user.setSalt(null);
    }
}
