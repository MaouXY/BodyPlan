# BodyPlan · 个性化健身管理系统

基于 Spring Boot 3.5.8 + Vue 3 + Android（Jetpack Compose）的毕设项目：用户填一份 5 要素建档问卷，
系统用「三步排除法」从动作库里排出 4 周训练计划，用户按天打卡，管理端看运营数据。

## 图解导览（建议先看这个）

打开 **`doc/diagrams/index.html`**，里面有六张交互图的目录与阅读顺序：

| 页 | 文件 | 回答什么问题 |
| --- | --- | --- |
| 01 | `doc/diagrams/01-architecture.html` | 系统由哪几块组成，请求怎么流转 |
| 02 | `doc/diagrams/02-workflow-loop.html` | 一个用户从注册到打卡要走哪些步 |
| 03 | `doc/diagrams/03-recommend-algorithm.html` | 训练计划凭什么这么推 |
| 04 | `doc/diagrams/04-checkin-sequence.html` | 一次打卡在服务端发生了什么 |
| 05 | `doc/diagrams/05-plan-lifecycle.html` | 一条训练项会经历哪些状态 |
| 06 | `doc/diagrams/06-data-model.html` | 11 张表怎么分工、外键怎么连 |

每张图都能切换深色主题、按引导视图逐步走查、导出 PNG / SVG。

## 目录结构

```
BodyPlan/
├── BodyPlanApi/          Spring Boot 后端（端口 8993）
│   ├── sql/              建表与种子数据（01~05，按序执行）
│   └── src/main/java/com/bodyplan/
│       ├── common/       统一响应、异常、JWT、MyBatis-Plus 配置
│       └── module/       auth / user / profile / fitness / training / record / admin
├── BodyPlanApp/          Android 用户端（Jetpack Compose，4 个界面）
├── BodyPlanWeb/          Vue 3 管理端（端口 5174，4 个模块）
└── doc/
    ├── diagrams/         六张交互架构图 + index.html
    ├── screenshots/      App 界面截图
    └── *.md              调研 / 需求 / 架构 / 分阶段计划
```

## 快速启动

**1. 数据库**

```bash
mysql -uroot -p < BodyPlanApi/sql/01_schema_base.sql
mysql -uroot -p < BodyPlanApi/sql/02_schema_training.sql
mysql -uroot -p < BodyPlanApi/sql/03_schema_record.sql
mysql -uroot -p < BodyPlanApi/sql/04_seed_data.sql
mysql -uroot -p < BodyPlanApi/sql/05_schema_exercise_flags.sql
```

**2. 后端**

```bash
cd BodyPlanApi
mvn spring-boot:run          # http://127.0.0.1:8993 ，接口文档 /doc.html
```

**3. 管理端**

```bash
cd BodyPlanWeb
npm install
npm run dev                  # http://127.0.0.1:5174
```

**4. Android 用户端**

用 Android Studio 打开 `BodyPlanApp/`，启动模拟器后运行。模拟器通过 `10.0.2.2:8993` 访问宿主机后端。

## 演示账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `123456` | 管理员（看管理端） |
| `demo` | `123456` | 普通用户（已建档、有计划） |
| `demo02` | `123456` | 普通用户（新注册、未建档） |

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Spring Boot 3.5.8、MyBatis-Plus、MySQL 8、JWT（jjwt）、Knife4j |
| 管理端 | Vue 3、TypeScript、Element Plus、ECharts、Pinia、Vite |
| 用户端 | Kotlin、Jetpack Compose、Retrofit + OkHttp、DataStore |

## 文档

- [调研与环境](doc/个性化健身管理系统_调研环境.md)
- [需求分析](doc/个性化健身管理系统_需求分析.md)
- [架构设计](doc/个性化健身管理系统_架构设计.md)
- [分阶段实现计划](doc/个性化健身管理系统_分阶段实现计划.md)
