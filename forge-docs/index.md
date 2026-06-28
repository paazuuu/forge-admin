---
layout: home

hero:
  name: "Forge Admin"
  text: "企业级后台管理框架"
  tagline: 基于 Vue3 + Spring Boot 的前后端分离解决方案
  image:
    src: /logo.png
    alt: Forge Admin Logo
  actions:
    - theme: brand
      text: 开发指南
      link: /guide/sdd-workflow
    - theme: alt
      text: 前端文档
      link: /frontend/components/overview
    - theme: alt
      text: 后端文档
      link: /backend/modules/overview
    - theme: alt
      text: 🔗 在线演示
      link: http://www.dlforgelab.com:8084/forge/login

features:
  - title: 📝 渐进式 SDD 开发
    details: Spec 驱动开发（No Spec No Code），7 步命令式工作流，代码质量有保障
  - title: 🚀 智能组件
    details: AI 表单、AI 表格、AI CRUD 页面，通过 JSON 配置快速生成
  - title: 🤖 AI 插件
    details: 集成 Spring AI，支持通义千问、OpenAI、智谱等多提供商，Agent 管理 + 流式对话
  - title: 🔐 安全机制
    details: RSA/AES/SM4 数据加密、分布式幂等、无感刷新、请求拦截
  - title: 🎨 主题定制
    details: 可切换颜色风格，支持亮色/暗色模式，适配不同场景
  - title: 📦 后端模块化
    details: 动态配置、MyBatis-Plus、社交登录、API 配置管理等 20+ 开箱即用模块
  - title: 🏢 多租户支持
    details: 基于 MyBatis Plus 的多租户隔离，支持租户级数据隔离
  - title: 🔄 动态配置中心
    details: 数据库驱动的配置中心，@RefreshScope 运行时刷新，无需重启服务
  - title: 🔗 流程引擎集成
    details: 注解驱动的流程集成（@FlowBind/@FlowStart/@FlowCallback），BPMN 可视化设计器
  - title: 🛡️ 数据权限
    details: 支持全部/本人/本组织/自定义等多种数据权限范围
  - title: 📢 消息推送
    details: 站内信、短信、WebSocket 实时推送，支持消息模板
  - title: ⏰ 定时任务
    details: 基于 Quartz 的分布式定时任务，支持动态管理和集群部署
  - title: 🌐 社交登录
    details: 支持微信、钉钉、GitHub、Gitee 等 18+ 第三方平台一键登录
---

<script setup>
import { withBase } from 'vitepress'
</script>

## 📺 在线演示

> **后台管理**：[http://www.dlforgelab.com:8084/forge/login](http://www.dlforgelab.com:8084/forge/login)
>
> 账号：`admin` / 密码：`123456`

## 社区与支持 {#community-support}

<div class="docs-community-panel">
  <div class="docs-community-copy">
    <div class="docs-community-eyebrow">ForgeAdmin 社区</div>
    <h3>添加维护者微信，把问题聊清楚</h3>
    <p>
      低代码建模、CRUD 配置、流程审批、插件扩展和二开落地，很多细节适合结合你的业务场景一起看。
      扫码添加维护者，备注 <strong>ForgeAdmin</strong>，可以交流使用经验、排查思路和后续版本建议。
    </p>
    <div class="docs-community-tags">
      <span>问题排查</span>
      <span>二开建议</span>
      <span>版本动态</span>
    </div>
  </div>
  <div class="docs-community-cards">
    <article class="docs-community-card">
      <div class="docs-community-card-title">
        <span>添加维护者微信</span>
        <em>交流咨询</em>
      </div>
      <div class="docs-community-qr-duo">
        <a class="docs-community-qr" :href="withBase('/images/community/forge-wechat-group.png')" target="_blank" rel="noopener">
          <img :src="withBase('/images/community/forge-wechat-group.png')" alt="ForgeAdmin 维护者微信二维码">
          <span>维护者 A</span>
        </a>
        <a class="docs-community-qr" :href="withBase('/images/community/forge-wechat-group1.png')" target="_blank" rel="noopener">
          <img :src="withBase('/images/community/forge-wechat-group1.png')" alt="ForgeAdmin 维护者微信二维码">
          <span>维护者 B</span>
        </a>
      </div>
      <p>添加任一维护者即可，说明你的使用场景，沟通会更高效。</p>
    </article>
    <article class="docs-community-card support">
      <div class="docs-community-card-title">
        <span>支持维护</span>
        <em>随心支持</em>
      </div>
      <a class="docs-community-support-qr" :href="withBase('/images/community/forge-wechat-support.png')" target="_blank" rel="noopener">
        <img :src="withBase('/images/community/forge-wechat-support.png')" alt="ForgeAdmin 支持维护二维码">
      </a>
      <p>如果 ForgeAdmin 帮你节省了时间，可以请维护者喝杯咖啡，支持文档、组件和低代码能力继续打磨。</p>
    </article>
  </div>
</div>
