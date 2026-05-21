package com.mdframe.forge.plugin.generator.service;

public interface MenuRegisterAdapter {

    Long registerMenu(String menuName, Long parentId, String configKey, Integer sort);

    default void updateMenu(Long menuResourceId, String menuName, Integer sort) {
        updateMenu(menuResourceId, menuName, null, sort);
    }

    void updateMenu(Long menuResourceId, String menuName, Long parentId, Integer sort);

    void deleteMenu(Long menuResourceId);

    /**
     * 检查指定菜单资源是否已被某个角色赋权
     *
     * @param menuResourceId 菜单资源 ID
     * @return true 表示已有角色赋权，不能删除
     */
    default boolean hasRolePermission(Long menuResourceId) {
        return false;
    }

    /**
     * 低代码发布菜单默认挂载的 AI 管理目录。
     */
    default Long resolveDefaultLowcodeParentId() {
        return 0L;
    }

    /**
     * 解析或创建某个业务领域下低代码应用默认挂载的菜单目录。
     */
    default Long resolveOrCreateDomainParentId(String domainCode, String domainName, Integer sort) {
        return null;
    }
}
