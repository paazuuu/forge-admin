package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.dto.SysResourceDTO;
import com.mdframe.forge.plugin.system.dto.SysResourceQuery;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.entity.SysRoleResource;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.service.ISysResourceService;
import com.mdframe.forge.starter.auth.domain.UserResourceTreeVO;
import com.mdframe.forge.starter.auth.service.IMenuService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资源Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysResourceServiceImpl extends ServiceImpl<SysResourceMapper, SysResource> implements ISysResourceService, IMenuService {

    private final SysResourceMapper resourceMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleResourceMapper roleResourceMapper;

    @Override
    public IPage<SysResource> selectResourcePage(SysResourceQuery query) {
        assertSystemAdmin();
        LambdaQueryWrapper<SysResource> wrapper = buildQueryWrapper(query);
        Page<SysResource> page = new Page<>(query.getPageNum(), query.getPageSize());
        return resourceMapper.selectPage(page, wrapper);
    }

    @Override
    public List<SysResource> selectResourceTree(SysResourceQuery query) {
        assertSystemAdmin();
        List<SysResource> list = list(buildQueryWrapper(query));
        return buildEntityTree(list, 0L);
    }

    @Override
    public List<SysResource> selectAssignableResourceTree(SysResourceQuery query) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }

        LambdaQueryWrapper<SysResource> wrapper = buildQueryWrapper(query);
        applyUserTypeScope(wrapper, loginUser);
        if (!loginUser.isAdmin()) {
            List<Long> resourceIds = selectCurrentUserResourceIds();
            if (CollUtil.isEmpty(resourceIds)) {
                return new ArrayList<>();
            }
            wrapper.in(SysResource::getId, resourceIds);
        }

        List<SysResource> list = list(wrapper);
        return buildEntityTree(list, 0L);
    }

    @Override
    public SysResource selectResourceById(Long id) {
        assertSystemAdmin();
        return resourceMapper.selectById(id);
    }

    @Override
    public boolean insertResource(SysResourceDTO dto) {
        assertSystemAdmin();
        SysResource resource = new SysResource();
        BeanUtil.copyProperties(dto, resource);
        resource.setMinUserType(normalizeMinUserType(resource.getMinUserType()));
        validateParentUserTypeBoundary(resource.getParentId(), resource.getMinUserType());
        return resourceMapper.insert(resource) > 0;
    }

    @Override
    public boolean updateResource(SysResourceDTO dto) {
        assertSystemAdmin();
        SysResource existing = resourceMapper.selectById(dto.getId());
        if (existing == null) {
            throw new RuntimeException("资源不存在");
        }
        SysResource resource = new SysResource();
        BeanUtil.copyProperties(dto, resource);
        Integer minUserType = dto.getMinUserType() != null
                ? normalizeMinUserType(dto.getMinUserType())
                : normalizeMinUserType(existing.getMinUserType());
        resource.setMinUserType(dto.getMinUserType() != null ? minUserType : null);
        Long parentId = dto.getParentId() != null ? dto.getParentId() : existing.getParentId();
        validateParentUserTypeBoundary(parentId, minUserType);
        return resourceMapper.updateById(resource) > 0;
    }

    @Override
    public boolean deleteResourceById(Long id) {
        assertSystemAdmin();
        return resourceMapper.deleteById(id) > 0;
    }

    @Override
    public List<UserResourceTreeVO> selectCurrentUserResourceTree() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }

        List<SysResource> userResources = getUserResources(loginUser);
        List<UserResourceTreeVO> voList = userResources.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return buildVOTree(voList, 0L);
    }

    @Override
    public List<UserResourceTreeVO> selectCurrentUserMenuTree() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }

        List<SysResource> userResources = getUserResources(loginUser);
        List<SysResource> menuResources = userResources.stream()
                .filter(resource -> resource.getResourceType() != null
                        && (resource.getResourceType() == 1 || resource.getResourceType() == 2))
                .collect(Collectors.toList());

        List<UserResourceTreeVO> voList = menuResources.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return buildVOTree(voList, 0L);
    }

    @Override
    public List<String> selectCurrentUserPermissions() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }

        if (loginUser.isAdmin()) {
            List<String> allPermissions = new ArrayList<>();
            allPermissions.add("*:*:*");
            return allPermissions;
        }

        List<SysResource> userResources = getUserResources(loginUser);
        return userResources.stream()
                .filter(resource -> resource.getResourceType() != null && resource.getResourceType() == 3)
                .filter(resource -> StrUtil.isNotBlank(resource.getPerms()))
                .map(SysResource::getPerms)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> selectCurrentUserResourceIds() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            return new ArrayList<>();
        }

        if (loginUser.isAdmin()) {
            return resourceMapper.selectList(new LambdaQueryWrapper<SysResource>().select(SysResource::getId))
                    .stream().map(SysResource::getId).collect(Collectors.toList());
        }

        return getUserResources(loginUser).stream()
                .map(SysResource::getId)
                .collect(Collectors.toList());
    }

    private List<SysResource> getUserResources(LoginUser loginUser) {
        String clientCode = loginUser.getUserClient() != null ? loginUser.getUserClient() : "pc";
        
        if (loginUser.isAdmin()) {
            LambdaQueryWrapper<SysResource> wrapper = new LambdaQueryWrapper<>();
            applyClientScope(wrapper, clientCode)
                    .orderByAsc(SysResource::getSort)
                    .orderByDesc(SysResource::getCreateTime);
            return resourceMapper.selectList(wrapper);
        }

        List<Long> roleIds = loginUser.getRoleIds();
        if (CollUtil.isEmpty(roleIds)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<SysRoleResource> roleResourceWrapper = new LambdaQueryWrapper<>();
        roleResourceWrapper.in(SysRoleResource::getRoleId, roleIds)
                .eq(SysRoleResource::getTenantId, loginUser.getTenantId());
        List<SysRoleResource> roleResources = roleResourceMapper.selectList(roleResourceWrapper);

        if (CollUtil.isEmpty(roleResources)) {
            return new ArrayList<>();
        }

        List<Long> resourceIds = roleResources.stream()
                .map(SysRoleResource::getResourceId)
                .distinct()
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(resourceIds)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<SysResource> resourceWrapper = new LambdaQueryWrapper<>();
        resourceWrapper.in(SysResource::getId, resourceIds)
                .orderByAsc(SysResource::getSort)
                .orderByDesc(SysResource::getCreateTime);
        applyClientScope(resourceWrapper, clientCode);
        applyUserTypeScope(resourceWrapper, loginUser);
        return resourceMapper.selectList(resourceWrapper);
    }

    private List<SysResource> buildEntityTree(List<SysResource> list, Long parentId) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        Map<Long, List<SysResource>> groupMap = list.stream()
                .collect(Collectors.groupingBy(SysResource::getParentId));

        List<SysResource> children = groupMap.get(parentId);
        if (CollUtil.isEmpty(children)) {
            return new ArrayList<>();
        }

        children.forEach(node -> {
            List<SysResource> subChildren = buildEntityTree(list, node.getId());
            if (CollUtil.isNotEmpty(subChildren)) {
                node.setChildren(subChildren);
            }
        });

        return children;
    }

    private List<UserResourceTreeVO> buildVOTree(List<UserResourceTreeVO> list, Long parentId) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        Map<Long, List<UserResourceTreeVO>> groupMap = list.stream()
                .collect(Collectors.groupingBy(vo -> vo.getParentId() == null ? 0L : vo.getParentId()));

        List<UserResourceTreeVO> children = groupMap.get(parentId);
        if (CollUtil.isEmpty(children)) {
            return new ArrayList<>();
        }

        children.forEach(node -> {
            List<UserResourceTreeVO> subChildren = buildVOTree(list, node.getId());
            if (CollUtil.isNotEmpty(subChildren)) {
                node.setChildren(subChildren);
            }
        });

        return children;
    }

    private UserResourceTreeVO convertToVO(SysResource resource) {
        UserResourceTreeVO vo = new UserResourceTreeVO();
        vo.setId(resource.getId());
        vo.setParentId(resource.getParentId());
        vo.setResourceName(resource.getResourceName());
        vo.setResourceType(resource.getResourceType());
        vo.setPath(resource.getPath());
        vo.setComponent(resource.getComponent());
        vo.setIsExternal(resource.getIsExternal());
        vo.setSsoEnabled(resource.getSsoEnabled());
        vo.setSsoTargetClient(resource.getSsoTargetClient());
        vo.setOpenTarget(resource.getOpenTarget());
        vo.setIsPublic(resource.getIsPublic());
        vo.setMenuStatus(resource.getMenuStatus());
        vo.setVisible(resource.getVisible());
        vo.setPerms(resource.getPerms());
        vo.setIcon(resource.getIcon());
        vo.setClientCode(resource.getClientCode());
        vo.setApiMethod(resource.getApiMethod());
        vo.setApiUrl(resource.getApiUrl());
        vo.setKeepAlive(resource.getKeepAlive());
        vo.setAlwaysShow(resource.getAlwaysShow());
        vo.setRedirect(resource.getRedirect());
        vo.setRemark(resource.getRemark());
        vo.setSort(resource.getSort());
        return vo;
    }

    private LambdaQueryWrapper<SysResource> buildQueryWrapper(SysResourceQuery query) {
        LambdaQueryWrapper<SysResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getTenantId() != null, SysResource::getTenantId, query.getTenantId())
                .like(StringUtils.isNotBlank(query.getResourceName()), SysResource::getResourceName, query.getResourceName())
                .eq(query.getParentId() != null, SysResource::getParentId, query.getParentId())
                .eq(query.getResourceType() != null, SysResource::getResourceType, query.getResourceType())
                .eq(query.getVisible() != null, SysResource::getVisible, query.getVisible())
                .eq(query.getMinUserType() != null, SysResource::getMinUserType, query.getMinUserType())
                .orderByAsc(SysResource::getSort)
                .orderByDesc(SysResource::getCreateTime);
        applyClientScope(wrapper, query.getClientCode());
        return wrapper;
    }

    private void assertSystemAdmin() {
        SessionHelper.assertAdmin("只有超级管理员可以维护菜单和资源配置");
    }

    private void applyUserTypeScope(LambdaQueryWrapper<SysResource> wrapper, LoginUser loginUser) {
        int userType = normalizeUserType(loginUser == null ? null : loginUser.getUserType());
        wrapper.and(item -> item
                .isNull(SysResource::getMinUserType)
                .or()
                .ge(SysResource::getMinUserType, userType));
    }

    private int normalizeUserType(Integer userType) {
        if (userType == null) {
            return SystemConstants.UserType.NORMAL_USER;
        }
        if (userType < SystemConstants.UserType.SYSTEM_ADMIN || userType > SystemConstants.UserType.NORMAL_USER) {
            return SystemConstants.UserType.NORMAL_USER;
        }
        return userType;
    }

    private Integer normalizeMinUserType(Integer minUserType) {
        return normalizeUserType(minUserType);
    }

    private void validateParentUserTypeBoundary(Long parentId, Integer minUserType) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        SysResource parent = resourceMapper.selectById(parentId);
        if (parent == null) {
            throw new RuntimeException("上级资源不存在");
        }
        int parentMinUserType = normalizeMinUserType(parent.getMinUserType());
        if (minUserType > parentMinUserType) {
            throw new RuntimeException("子资源开放范围不能高于上级资源");
        }
    }

    private LambdaQueryWrapper<SysResource> applyClientScope(LambdaQueryWrapper<SysResource> wrapper, String clientCode) {
        if (StringUtils.isBlank(clientCode)) {
            return wrapper;
        }
        return wrapper.and(item -> item
                .eq(SysResource::getClientCode, clientCode)
                .or()
                .isNull(SysResource::getClientCode)
                .or()
                .eq(SysResource::getClientCode, ""));
    }
}
