# BodyPlan · 个性化健身管理系统

基于 Spring Boot 3.5.8 + Vue 3 + Android（Jetpack Compose）的毕设项目：用户填一份 5 要素建档问卷，
系统用「三步排除法」从动作库里排出 4 周训练计划，用户按天打卡，管理端看运营数据。

## 演示验收入口（2026-11-12）

详见 [演示验收清单](doc/演示验收清单.md)。本轮已补齐目标建议预览、建档/复测原子提交、
中断恢复、修改档案、自由打卡、实际训练反馈、月历、身体变化与历史计划、标签名称显示和异常日志。
现场彩排由使用者按清单执行，不能把编译及单元测试通过等同于三端现场验收通过。

## 新增运动、教练与饮食（2026-11-15）

第一版已接入 GPS 运动记录、距离与配速目标、教练学员管理与站内提醒，以及饮食营养记录。
查看 [新增功能验收说明](doc/新增功能验收说明.md) 获取安装包位置、专用账号、操作步骤、测试结果与限制。
本机本轮迁移已执行，旧账号与历史打卡没有重置；迁移前备份位于忽略目录 `.local/backups/`。

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
│   ├── sql/              基础表（01~05）、原演示（06）、动作快照（07）、新增模块（08）、专用演示（09）
│   └── src/main/java/com/bodyplan/
│       ├── common/       统一响应、异常、JWT、MyBatis-Plus 配置
│       └── module/       auth / user / profile / fitness / training / record / admin / activity / coaching / nutrition
├── BodyPlanApp/          Android 用户端（训练、运动路线、饮食、消息、档案）
├── BodyPlanWeb/          Vue 3 管理员与教练工作台（端口 5174）
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
mysql -uroot -p < BodyPlanApi/sql/06_demo_acceptance_data.sql
mysql -uroot -p < BodyPlanApi/sql/07_checkin_exercise.sql
mysql -uroot -p < BodyPlanApi/sql/08_wellness_modules.sql
mysql -uroot -p < BodyPlanApi/sql/09_wellness_demo.sql
```

新环境按顺序执行，已有环境只执行未应用的迁移。06 是原演示可选数据，09 是本轮专用演示数据；
都不重置旧账号或已有打卡。
Windows 可在 MySQL 命令行用 `source` 执行绝对路径；执行前先备份并确认目标库。

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

真机无需改源码，构建时传入电脑局域网地址：

```powershell
.\gradlew.bat --no-daemon :app:assembleDebug "-PbodyplanApiUrl=http://192.168.1.10:8993/"
```

将示例 IP 换成电脑实际 IP。后端运行日志位于 `BodyPlanApi/logs/bodyplan-api.log`。

## 演示账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `123456` | 管理员（看管理端） |
| `demo` | `123456` | 原始演示用户，已建档；01~05 不预置计划 |
| `demo02` | `123456` | 原始对比用户，已建档；01~05 不预置计划 |
| `demo_ready` | `123456` | 执行 06 后新增：已建档，带 4 周演示计划和近期历史 |
| `demo_new` | `123456` | 执行 06 后新增：首次未建档，完成演示后不会自动重置 |
| `demo_compare` | `123456` | 执行 06 后新增：增肌对比档案与身体/打卡历史，无预置计划 |
| `wellness_demo` | `123456` | 执行 09 后新增：移动端新功能演示账号，带当天饮食与 7 天计划 |
| `coach_demo` | `123456` | 执行 09 后新增：教练工作台，已分配 wellness_demo |

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Spring Boot 3.5.8、MyBatis-Plus、MySQL 8、JWT（jjwt）、Knife4j |
| 管理端 | Vue 3、TypeScript、Element Plus、ECharts、Pinia、Vite |
| 用户端 | Kotlin、Jetpack Compose、Retrofit + OkHttp、SharedPreferences |

## 文档

- [调研与环境](doc/个性化健身管理系统_调研环境.md)
- [需求分析](doc/个性化健身管理系统_需求分析.md)
- [架构设计](doc/个性化健身管理系统_架构设计.md)
- [分阶段实现计划](doc/个性化健身管理系统_分阶段实现计划.md)
