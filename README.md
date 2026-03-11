# 便笺录 2.0

## 项目简介

本项目是一个基于 **Jetpack Compose + MVVM + Room + Hilt** 的笔记 / 待办应用。
当前模块化改造参考了 **Now in Android** 的分层思路，但没有照搬其工程结构，而是结合本项目现状进行了**渐进式、最小改动**的拆分。

当前目标是：
- 保持现有功能稳定
- 降低页面之间的耦合
- 让数据层、公共 UI、业务页面职责更清晰
- 为后续继续拆分功能与测试打基础

---

## 当前架构

整体采用分层 + 功能模块化：

- `app`：应用壳层，负责入口、导航、顶层组合
- `core:common`：通用常量等轻量公共能力
- `core:model`：实体模型层
- `core:data`：数据库、DAO、Repository、数据存储、DI
- `core:ui`：公共主题、通用组件
- `feature:notes`：笔记功能
- `feature:todo`：待办功能
- `feature:category`：分类管理功能
- `feature:calendar`：日历页面功能
- `feature:timeline`：时间轴页面功能

---

## 模块说明

### 1. `app`

`app` 目前保留以下职责：

- `MainActivity` 与应用入口
- 顶层导航与页面路由
- 抽屉、顶部栏等跨功能组合逻辑
- 应用级依赖聚合

这是当前阶段的“壳模块”，尽量不承载具体业务实现。

### 2. `core:common`

负责轻量公共内容，例如：

- 常量定义

该模块不放业务逻辑，避免被反向依赖污染。

### 3. `core:model`

负责领域模型 / 数据模型，目前主要包括：

- `Note`
- `Todo`
- `Category`
- 相关枚举与实体定义

该模块用于承载被多个功能共同依赖的数据结构。

### 4. `core:data`

负责数据访问与持久化，包括：

- `Room Database`
- `DAO`
- `Repository`
- `CategorySelectionStore`
- 数据库依赖注入模块

该模块是数据唯一来源，功能模块通过它获取和更新数据。

### 5. `core:ui`

负责跨模块复用的 UI 基础能力，例如：

- 应用主题
- 公共组件
- 通用弹窗
- 可复用交互组件

这样可以避免多个功能模块重复维护相同 UI。

### 6. `feature:notes`

负责笔记相关功能，包括：

- 笔记列表
- 新建 / 编辑笔记
- 笔记筛选逻辑
- 笔记页面状态管理

### 7. `feature:todo`

负责待办相关功能，包括：

- 待办列表
- 新建 / 编辑待办
- 待办筛选逻辑
- 待办状态管理

### 8. `feature:category`

负责分类管理相关功能，包括：

- 分类列表展示
- 分类新增 / 修改 / 删除 / 清空
- 分类排序
- 分类页面状态管理

### 9. `feature:calendar`

负责日历页面相关展示逻辑。
当前仍是轻量页面，但已独立成模块，方便后续继续扩展日历视图、日程聚合等能力。

### 10. `feature:timeline`

负责时间轴页面相关展示逻辑。
当前为轻量页面，已从 `app` 中拆出，后续可逐步承接时间线聚合展示、按时间排序浏览等功能。

---

## 当前模块依赖关系

当前推荐理解为：

- `app` 依赖所有 `feature` 模块与必要 `core` 模块
- `feature:*` 依赖 `core:*`
- `core` 模块之间仅保留必要依赖
- 业务功能模块之间尽量不直接依赖

可简化表示为：

```text
app
├─ core:model
├─ core:data
├─ core:ui
├─ feature:notes
├─ feature:todo
├─ feature:category
├─ feature:calendar
└─ feature:timeline

feature:* -> core:*
core:data -> core:model
core:ui -> core:model（按需）
```

> 说明：本项目采用“保持原包名不变、逐步迁移目录”的策略，以降低重构风险。

---

## 目录结构（当前实际情况）

```text
app/
core/
  common/
  model/
  data/
  ui/
feature/
  notes/
  todo/
  category/
  calendar/
  timeline/
```

---

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- MVVM
- Room
- Hilt
- Coroutines / Flow

---

## 当前模块化改造原则

本次改造遵循以下原则：

- **最小可行拆分**：先拆最清晰的业务边界，不一次性大改
- **保持现有包名**：降低 import 改动与迁移风险
- **壳层稳定**：导航与应用入口暂时保留在 `app`
- **公共能力下沉**：模型、数据、公共 UI 统一沉到 `core`
- **业务边界清晰**：笔记、待办、分类、日历、时间轴分别进入 `feature`

---

## 后续建议

后续如果继续演进，可以按优先级考虑：

1. 将顶层导航中与具体功能相关的组合逻辑进一步下沉
2. 继续收敛 `app` 中的页面装配逻辑
3. 为各 `feature` 模块补充单元测试 / UI 测试
4. 逐步规范模块对外暴露的 API，减少直接依赖 ViewModel 实现细节
5. 按模块补充文档与开发说明

---

## 构建验证

当前建议使用以下命令进行基础编译校验：

```bash
./gradlew :app:compileDebugKotlin
```

在本轮模块化改造后，以上命令已可以通过。
