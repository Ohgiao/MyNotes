<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="app">
    <header class="app-header">
      <span class="brand">学习记录</span>
      <nav>
        <RouterLink to="/">记录</RouterLink>
        <RouterLink to="/timeline">时间线</RouterLink>
        <RouterLink to="/topic">主题</RouterLink>
        <RouterLink to="/review">复习</RouterLink>
        <RouterLink to="/search">搜索</RouterLink>
        <RouterLink to="/ask">问答</RouterLink>
        <RouterLink to="/trash">回收站</RouterLink>
      </nav>
    </header>
    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<style>
:root {
  --ink: #1f2329;
  --muted: #8a919f;
  --line: #e6e8eb;
}
* { box-sizing: border-box; }
body {
  margin: 0;
  color: var(--ink);
  background: #fafafa;
  font-family: -apple-system, 'Segoe UI', 'Microsoft YaHei', sans-serif;
}
.app-header {
  display: flex;
  align-items: baseline;
  gap: 24px;
  padding: 14px 24px;
  background: #fff;
  border-bottom: 1px solid var(--line);
}
.brand { font-weight: 600; }
.app-header nav { display: flex; gap: 16px; }
.app-header a {
  color: var(--muted);
  text-decoration: none;
  font-size: 14px;
}
.app-header a.router-link-active { color: var(--ink); font-weight: 600; }
.app-main {
  max-width: 780px;
  margin: 0 auto;
  padding: 24px 16px 80px;
}
</style>
