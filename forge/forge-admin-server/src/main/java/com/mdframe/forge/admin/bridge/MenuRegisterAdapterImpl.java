package com.mdframe.forge.admin.bridge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.generator.service.MenuRegisterAdapter;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.entity.SysRoleResource;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleResourceMapper;
import com.mdframe.forge.plugin.system.service.ISysResourceService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MenuRegisterAdapterImpl implements MenuRegisterAdapter {

    private static final String DEFAULT_CLIENT_CODE = "pc";
    private static final String DOMAIN_MENU_PERMS_PREFIX = "ai:lowcode:domain-menu:";

    private final ISysResourceService resourceService;
    private final SysResourceMapper resourceMapper;
    private final SysRoleResourceMapper roleResourceMapper;

    @Override
    public Long registerMenu(String menuName, Long parentId, String configKey, Integer sort) {
        SysResource resource = new SysResource();
        resource.setTenantId(resolveTenantId());
        resource.setResourceName(menuName);
        resource.setParentId(parentId != null ? parentId : resolveDefaultLowcodeParentId());
        resource.setResourceType(2);
        resource.setSort(sort);
        resource.setPath("/ai/crud-page/" + configKey);
        resource.setComponent("ai/crud-page");
        resource.setIsExternal(0);
        resource.setIsPublic(0);
        resource.setMenuStatus(1);
        resource.setVisible(1);
        resource.setPerms("ai:crud:" + configKey);
        resource.setKeepAlive(0);
        resource.setAlwaysShow(0);
        resource.setClientCode(DEFAULT_CLIENT_CODE);
        resourceService.save(resource);

        Long menuId = resource.getId();
        log.info("[MenuRegisterAdapter] 注册菜单成功: menuName={}, configKey={}, menuId={}", menuName, configKey, menuId);
        return menuId;
    }

    @Override
    public void updateMenu(Long menuResourceId, String menuName, Long parentId, Integer sort) {
        SysResource resource = new SysResource();
        resource.setId(menuResourceId);
        resource.setResourceName(menuName);
        if (parentId != null) {
            resource.setParentId(parentId);
        }
        resource.setSort(sort);
        resourceService.updateById(resource);
        log.info("[MenuRegisterAdapter] 更新菜单成功: menuId={}, menuName={}, parentId={}", menuResourceId, menuName, parentId);
    }

    @Override
    public void deleteMenu(Long menuResourceId) {
        resourceService.removeById(menuResourceId);
        log.info("[MenuRegisterAdapter] 删除菜单成功: menuId={}", menuResourceId);
    }

    @Override
    public boolean hasRolePermission(Long menuResourceId) {
        if (menuResourceId == null) {
            return false;
        }
        LambdaQueryWrapper<SysRoleResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleResource::getResourceId, menuResourceId);
        return roleResourceMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Long resolveDefaultLowcodeParentId() {
        SysResource aiRoot = resourceMapper.selectOneByPath(resolveTenantId(), 1, "/ai");
        return aiRoot != null && aiRoot.getId() != null ? aiRoot.getId() : 0L;
    }

    @Override
    public Long resolveOrCreateDomainParentId(String domainCode, String domainName, Integer sort) {
        String normalizedCode = StringUtils.trimToNull(domainCode);
        if (normalizedCode == null) {
            return null;
        }
        Long tenantId = resolveTenantId();
        String perms = DOMAIN_MENU_PERMS_PREFIX + normalizedCode;
        SysResource existing = resourceMapper.selectOneByPerms(tenantId, 1, perms);
        if (existing != null && existing.getId() != null) {
            updateDomainParentIfNeeded(existing, domainName, sort);
            return existing.getId();
        }

        Long parentId = resolveDefaultLowcodeParentId();
        SysResource resource = new SysResource();
        resource.setTenantId(tenantId);
        resource.setResourceName(StringUtils.defaultIfBlank(domainName, normalizedCode));
        resource.setParentId(parentId);
        resource.setResourceType(1);
        resource.setSort(sort != null ? sort : 0);
        resource.setPath("/ai/lowcode-domain/" + normalizedCode);
        resource.setComponent(null);
        resource.setIsExternal(0);
        resource.setIsPublic(0);
        resource.setMenuStatus(1);
        resource.setVisible(1);
        resource.setPerms(perms);
        resource.setIcon("ionicons5:FolderOpenOutline");
        resource.setKeepAlive(0);
        resource.setAlwaysShow(1);
        resource.setClientCode(DEFAULT_CLIENT_CODE);
        resourceService.save(resource);
        log.info("[MenuRegisterAdapter] 创建领域菜单目录成功: domainCode={}, menuId={}", normalizedCode, resource.getId());
        return resource.getId();
    }

    private void updateDomainParentIfNeeded(SysResource existing, String domainName, Integer sort) {
        SysResource resource = new SysResource();
        resource.setId(existing.getId());
        boolean changed = false;
        if (StringUtils.isNotBlank(domainName) && !domainName.equals(existing.getResourceName())) {
            resource.setResourceName(domainName);
            changed = true;
        }
        if (sort != null && !sort.equals(existing.getSort())) {
            resource.setSort(sort);
            changed = true;
        }
        if (changed) {
            resourceService.updateById(resource);
        }
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}
