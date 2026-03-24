# Hedwig Design Files

本目录存放 [Pencil](https://pencil.dev) 设计文件 (`.pen`)。

## 文件说明

| 文件 | 描述 |
|------|------|
| `hedwig-ui.pen` | 主设计文件 - 包含所有页面和组件的 UI 设计 |

## 使用方式

### 在 Cursor / VS Code 中打开
1. 确保已安装 Pencil 扩展
2. 双击 `.pen` 文件即可在可视化画布中打开

### 使用 Pencil CLI
```bash
# 修改设计
pencil --in design/hedwig-ui.pen --out design/hedwig-ui.pen --prompt "Add a new page"

# 从设计生成代码
# 在 Cursor 中打开 .pen 文件，按 Cmd+K 打开 AI 聊天，输入：
# "Generate React TypeScript code using Ant Design for this design"
```

### 导入现有组件到设计
在 Cursor 中打开 `.pen` 文件，按 `Cmd+K`，然后输入:
```
Recreate the DashboardPage from frontend-react/src/pages/DashboardPage.tsx
```

## 设计规范

- **主色**: `#c8a44e` (金色)
- **辅助色**: `#4ecdc4` (青色)  
- **错误色**: `#e05c5c`
- **警告色**: `#e0a84b`
- **深色背景**: `#0c1117`
- **卡片背景(深色)**: `#141a23`
- **字体**: DM Sans
- **圆角**: 8px
