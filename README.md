> 特别说明：源码、JDK、数据库、Redis等安装或存放路径禁止包含中文、空格、特殊字符等

## 一 技术栈

- 编辑语言：`Java8/11`、`Java17/21`
- 主框架：`Spring Boot 2.7/3.2` + `Spring Framework`
- 持久层框架：`MyBatis-Plus`
- 数据库连接池：`Alibaba Druid`
- 多数据源：`Dynamic-Datasource`
- 数据库兼容： `MySQL`(默认)、`SQLServer`、`Oracle`、`PostgreSQL`、`达数据库`、`人大金仓数据库`
- 分库分表解决方案：`Apache ShardingSphere`
- 权限认证框架：`Sa-Token`+`JWT`
- 代码生成器：`MyBatis-Plus-Generator`
- 流程引擎：`Flowable 6.7`
- 模板引擎：`Velocity`
- 任务调度：`XXL-JOB`
- 分布式锁：`Lock4j`
- JSON序列化: `Jackson`&`Fastjson`
- 缓存数据库：`Redis`
- 校验框架：`Validation`
- 分布式文件存储：兼容`MinIO`及多个云对象存储，如阿里云 OSS、华为云 OBS、七牛云 Kodo、腾讯云 COS等
- 工具类框架：`Hutool`、`Lombok`
- 接口文档：`Knife4j`
- 项目构建：`Maven`

## 二 环境要求

### 2.1 开发环境

| 类目 | 版本说明或建议           |
| --- |------------------|
| 硬件 | 开发电脑建议使用I3及以上CPU，16G及以上内存  |
| 操作系统 | Windows 10/11，MacOS |
| JDK   | 默认使用JDK 21，如需要切换JDK 8/11/17版本请参考文档调整代码，推荐使用 `OpenJDK`，如 `Liberica JDK`、`Eclipse Temurin`、`Alibaba Dragonwell`、`BiSheng` 等发行版； |
| Maven | 依赖管理工具，推荐使用 `3.6.3` 及以上版本  |
| Redis | 数据缓存，推荐使用 `5.0` 及以上版本 |
| 数据库 | 兼容 `MySQL 5.7.x/8.x`、`SQLServer 2012+`、`Oracle 11g`、`PostgreSQL 12+`、`达梦数据库(DM8)`、`人大金仓数据库(KingbaseES_V8R6)` |
| IDE   | 代码集成开发环境，推荐使用 `IDEA2024` 及以上版本，兼容 `Eclipse`、 `Spring Tool Suite` 等IDE工具 |
| 文件存储 | 默认使用本地存储，兼容 `MinIO` 及多个云对象存储，如 `阿里云 OSS`、`华为云 OBS`、`七牛云 Kodo`、`腾讯云 COS` 等； |

### 2.2 运行环境

> 适用于测试或生产环境

| 类目 | 版本说明或建议    |
| --- |--------------|
| 服务器配置 | 建议至少在 `4C/16G/50G`  的机器配置下运行；  |
| 操作系统 | 建议使用 `Windows Server 2019` 及以上版本或主流 `Linux` 发行版本，推荐使用 `Linux` 环境；兼容 `统信UOS`，`OpenEuler`，`麒麟服务器版` 等信创环境；    |
| JRE | 默认使用JRE 21，如需要切换JRE 8/11/17版本请参考文档调整代码；推荐使用 `OpenJDK`，如 `Liberica JDK`、`Eclipse Temurin`、`Alibaba Dragonwell`、`BiSheng` 等发行版；   |
| Redis | 数据缓存，推荐使用 `5.0` 及以上版本 |
| 数据库 | 兼容 `MySQL 5.7.x/8.x`、`SQLServer 2012+`、`Oracle 11g`、`PostgreSQL 12+`、`达梦数据库(DM8)`、`人大金仓数据库(KingbaseES_V8R6)` |
| 中间件(兼容)) | 东方通 `Tong-web`、金蝶天燕-应用服务器`AAS` v10； |
| 文件存储 | 默认使用本地存储，兼容 `MinIO` 及多个云对象存储，如 `阿里云 OSS`、`华为云 OBS`、`七牛云 Kodo`、`腾讯云 COS` 等； |

## 三 IDEA插件

- `Lombok`(必须)
- `Alibaba Java Coding Guidelines`
- `MybatisX`

## 四 Maven私服配置

> 建议使用 Apache Maven 3.6.3 及以上版本<br>以解决依赖无法从公共Maven仓库下载的问题<br>通过官方私服下载依赖完成后，由于IDEA的缓存可能会出现部分报红，重启IDEA即可

打开Maven安装目录中的 `conf/settings.xml` 文件，<br>
在 `<servers></servers>` 中添加如下内容

```xml
<server>
  <id>maven-releases</id>
  <username>您的账号</username>
  <password>您的密码</password>
</server>
```

在 `<mirrors></mirrors>` 中添加

```xml
<mirror>
  <id>maven-releases</id>
  <mirrorOf>*</mirrorOf>
  <name>maven-releases</name>
  <url>https://repository.jnpfsoft.com/repository/maven-public/</url>
</mirror>
```

## 五 配套项目

### 5.1 后端项目

| 项目 | 分支            | 说明                |
| --- |---------------|-------------------|
| jnpf-common | v6.1.x-stable | Java基础依赖项目源码      |
| jnpf-datareport | v6.1.x-stable | 报表后端项目源码          |
| jnpf-file-core-starter | v6.1.x-stable | 文件基础依赖项目源码        |
| jnpf-file-preview | v6.1.x-stable  | 本地文档预览项目源码        |
| jnpf-java-datareport-univer | v6.1.x-stable | Java Univer报表源码 |
| jnpf-java-datareport-univer-core | v6.1.x-stable | Java Univer报表核心依赖源码，不同销售版本交付有所差异，以实际交付为准 |
| jnpf-java-tenant | v6.1.x-stable | 多租户后端源码，不同销售版本交付有所差异，以实际交付为准 |
| jnpf-scheduletask | v6.1.x-stable | 任务调度客户端依赖及服务端项目源码 |
| jnpf-workflow | v1.0.0-stable  | 流程引擎后端项目          |
| jnpf-workflow-core | v1.0.0-stable | Flowable流程引擎基础依赖，不同销售版本交付有所差异，以实际交付为准  |

### 5.2 前端项目

| 项目 | 分支 |  说明 |
| --- | --- | --- |
| jnpf-web-apps-main |  v6.1.x-stable | 前端主项目源码，不同销售版本交付有所差异，以实际交付为准 |
| jnpf-web-apps-main-npm |  v6.1.x-stable | 前端主项目源码(集成流程设计器、Univer报表设计器npm包)，不同销售版本交付有所差异，以实际交付为准 |
| jnpf-web-datareport |  v6.1.x-stable | 报表前端项目源码 |
| jnpf-web-datascreen-vue3 |  v6.1.x-stable | 前端大屏项目源码（Vue3） |
| jnpf-web-monorepo-framework |  v6.1.x-stable | 前端核心项目源码|
| jnpf-web-tenant-vue3 |  v6.1.x-stable | 多租户前端项目源码（Vue3），不同销售版本交付有所差异，以实际交付为准 |
| jnpf-bpmn | v1.1.x-stable | 前端流程设计器源代码，不同销售版本交付有所差异，以实际交付为准 |
| jnpf-univer | v1.0.x-stable | 前端Univer报表设计器源代码，不同销售版本交付有所差异，以实际交付为准 |

### 5.3 移动端

| 项目 | 分支 |  说明 |
| --- | --- | --- |
| jnpf-app-vue3 | v6.1.x-stable | 移动端项目源码(Vue3) |

### 5.4 静态资源

| 项目 | 分支 |  说明 |
| --- | --- | --- |
| jnpf-resources | v6.1.x-stable | 静态资源 |

### 5.5 数据库

| 项目 | 分支 |  说明 |
| --- | --- | --- |
| jnpf-database | v6.1.x-stable | 数据库脚本或文件 |

## 六 开发环境

### 6.1 导入数据库脚本

> 以 MySQL数据库为例<br>字符集：`utf8mb4` <br/>排序规则：`utf8mb4_general_ci`

#### 6.1.1 创建平台数据库

在MySQL中创建 `jnpf_init` 数据库，并将 `jnpf-database/MySQL/jnpf_db_init.sql` 方式导入。<br/>
若需要使用纯净数据库（不含Demo示例），则以【新建查询】方式导入 `jnpf_dbnull_init.sql` 。
若有更新脚本(Update目录下)，按日期顺序执行更新

#### 6.1.2 创建系统调度数据库

在MySQL创建 `jnpf_xxljob` 数据库，并将 `jnpf-database/MySQL/jnpf_xxljob_init.sql` 导入；

#### 6.1.3 创建流程数据库

在MySQL创建 `jnpf_flow` 数据库，并将 `jnpf-database/MySQL/jnpf_flow_init.sql` 导入；

### 6.2 导入依赖

#### 6.2.1 基础依赖

详见 `jnpf-common` 项目中的 `README.md` 文档说明

#### 6.2.2 文件基础依赖

详见 `jnpf-file-starter` 项目中的 `README.md` 文档说明

#### 6.2.3 导入系统调度服务端

详见 `jnpf-scheduletask` 项目中的 `README.md` 文档说明

### 6.3 配套项目

#### 6.3.1 jnpf-datareport 报表后端项目

详见 `jnpf-datareport` 项目中的 `README.md` 文档说明

#### 6.3.2 jnpf-java-datareport-univer Java Univer报表后端项目

详见 `jnpf-java-datareport-univer` 项目中的 `README.md` 文档说明

#### 6.3.3 jnpf-workflow 工作流引擎后端项目

详见 `jnpf-workflow` 项目中的 `README.md` 文档说明

### 6.4 项目配置

打开编辑 `jnpf-admin/src/main/resources/application.yml`

#### 6.4.1 指定环境配置

环境说明：

- `application-dev.yml` 开发环境(默认)
- `application-test.yml` 测试环境
- `application-preview.yml` 预发布环境
- `application-prod.yml` 生产环境

> 以开发环境为例，根据实际需求修改

```yaml
# application.yml第6行,可选值：dev(开发环境-默认)、test(测试环境)、preview(预生产)、prod(生产环境)
active: dev
```

#### 6.4.2 配置域名

打开编辑 `jnpf-admin/src/main/resources/application.yml` ，修改以下配置

```yaml
  PreviewType: kkfile #文件预览方式 （1.yozo 2.kkfile）默认使用kkfile
  kkFileUrl: http://127.0.0.1:30090/FileServer/ #kkfile文件预览服务地址
  ApiDomain: http://127.0.0.1:30000 #后端域名(文档预览中使用)
  FrontDomain: http://127.0.0.1:3000 #前端域名(文档预览中使用)
  AppDomain: http://127.0.0.1:8080 #app/h5端域名配置(文档预览中使用)
  FlowDomain: http://127.0.0.1:31000 #流程引擎接口地址
```

#### 6.4.3 数据源配置

配置参数说明：

- `db-type`：数据库类型（可选值：`MySQL`、`SQLServer`、`Oracle`、`PostgreSQL`、`DM`、`KingbaseES`）
- `host`：数据库主机地址
- `port`：数据库端口
- `dbname`：平台初始库
- `username`：数据库用户名
- `password`：数据库密码
- `db-schema`：数据库模式
- `prepare-url`：自定义JDBC连接配置

打开编辑 `jnpf-admin/src/main/resources/application-dev.yml`，修改以下配置

##### 6.4.3.1 MySQL数据库

```yaml
  datasource:
    db-type: MySQL
    host: 127.0.0.1
    port: 3306
    db-name: jnpf_init
    username: dbuser
    password: dbpasswd
    db-schema:
    prepare-url:
```

##### 6.4.3.2 SQLServer数据库

```yaml
  datasource:
    db-type: SQLServer
    host: 127.0.0.1
    port: 1433
    db-name: jnpf_init
    username: dbuser
    password: dbpasswd
    db-schema:
    prepare-url:
```

##### 6.4.3.3 Oracle数据库

```yaml
  datasource:
    db-type: Oracle
    host: 127.0.0.1
    port: 1521
    db-name:
    username: JNPF_INIT
    password: dbpasswd
    db-schema:
    prepare-url: jdbc:oracle:thin:@127.0.0.1:1521:ORCL
```

##### 6.4.3.4 PostgreSQL数据库配置

```yaml
  datasource:
    db-type: PostgreSQL
    host: 127.0.0.1
    port: 5432
    db-name: jnpf_init
    username: dbuser
    password: dbpasswd
    db-schema: public
    prepare-url:
```

##### 6.4.3.5 达梦（DM8）数据库

```yaml
  datasource:
    db-type: DM
    host: 127.0.0.1
    port: 5236
    db-name: JNPF_INIT
    username: DBUSER
    password: dbpasswd
    db-schema:
    prepare-url:
    tablespace: MAIN
```

##### 6.4.3.6 人大金仓（KingbaseES_V8R6）数据库

```yaml
  datasource:
    db-type: KingbaseES
    host: 127.0.0.1
    port: 54321
    db-name: jnpf_init
    username: dbuser
    password: dbpasswd
    db-schema:
    prepare-url:
```

#### 6.4.4 Redis配置

打开编辑 `jnpf-admin/src/main/resources/application-dev.yml`，修改以下配置
> 支持单机模式和集群模式，配置默认为单机模式

**若使用Redis单机模式**

```yaml
  redis:
    database: 1 #缓存库编号
    host: 127.0.0.1
    port: 6379
    password: 123456  # 密码为空时，请将本行注释
    timeout: 3000 #超时时间(单位：秒)
    lettuce: #Lettuce为Redis的Java驱动包
      pool:
        max-active: 8 # 连接池最大连接数
        max-wait: -1ms  # 连接池最大阻塞等待时间（使用负值表示没有限制）
        min-idle: 0 # 连接池中的最小空闲连接
        max-idle: 8 # 连接池中的最大空闲连接
```

**若使用Redis集群模式**

```yaml
 redis:
   cluster:
     nodes:
       - 192.168.0.225:6380
       - 192.168.0.225:6381
       - 192.168.0.225:6382
       - 192.168.0.225:6383
       - 192.168.0.225:6384
       - 192.168.0.225:6385
   password: 123456 # 密码为空时，请将本行注释
   timeout: 3000 # 超时时间(单位：秒)
   lettuce: #Lettuce为Redis的Java驱动包
     pool:
       max-active: 8 # 连接池最大连接数
       max-wait: -1ms  # 连接池最大阻塞等待时间（使用负值表示没有限制）
       min-idle: 0 # 连接池中的最小空闲连接
       max-idle: 8 # 连接池中的最大空闲连接
```

#### 6.4.5 静态资源配置

打开编辑 `jnpf-admin/src/main/resources/application-dev.yml` ，修改以下配置
> 默认使用本地存储，兼容 `MinIO` 及多个云对象存储，如阿里云 OSS、华为云 OBS、七牛云 Kodo、腾讯云 COS等

```yaml
  # ===================== 文件存储配置 =====================
  file-storage: #文件存储配置，不使用的情况下可以不写
    default-platform: local-plus-1 #默认使用的存储平台
    thumbnail-suffix: ".min.jpg" #缩略图后缀，例如【.min.jpg】【.png】
    local-plus: # 本地存储升级版
      - platform: local-plus-1 # 存储平台标识
        enable-storage: true  #启用存储
        enable-access: true #启用访问（线上请使用 Nginx 配置，效率更高）
        domain: "" # 访问域名，例如：“http://127.0.0.1:8030/”，注意后面要和 path-patterns 保持一致，“/”结尾，本地存储建议使用相对路径，方便后期更换域名
        base-path: D:/project/jnpf-resources/ # 基础路径
        path-patterns: /** # 访问路径
        storage-path:  # 存储路径
    aliyun-oss: # 阿里云 OSS ，不使用的情况下可以不写
      - platform: aliyun-oss-1 # 存储平台标识
        enable-storage: false  # 启用存储
        access-key: ??
        secret-key: ??
        end-point: ??
        bucket-name: ??
        domain: ?? # 访问域名，注意“/”结尾，例如：https://abc.oss-cn-shanghai.aliyuncs.com/
        base-path: hy/ # 基础路径
    qiniu-kodo: # 七牛云 kodo ，不使用的情况下可以不写
      - platform: qiniu-kodo-1 # 存储平台标识
        enable-storage: false  # 启用存储
        access-key: ??
        secret-key: ??
        bucket-name: ??
        domain: ?? # 访问域名，注意“/”结尾，例如：http://abc.hn-bkt.clouddn.com/
        base-path: base/ # 基础路径
    tencent-cos: # 腾讯云 COS
      - platform: tencent-cos-1 # 存储平台标识
        enable-storage: false  # 启用存储
        secret-id: ??
        secret-key: ??
        region: ?? #存仓库所在地域
        bucket-name: ??
        domain: ?? # 访问域名，注意“/”结尾，例如：https://abc.cos.ap-nanjing.myqcloud.com/
        base-path: hy/ # 基础路径
    minio: # MinIO，由于 MinIO SDK 支持 AWS S3，其它兼容 AWS S3 协议的存储平台也都可配置在这里
      - platform: minio-1 # 存储平台标识
        enable-storage: true  # 启用存储
        access-key: Q9jJs2b6Tv
        secret-key: Thj2WkpLu9DhmJyJ
        end-point: http://192.168.0.207:9000/
        bucket-name: jnpfsoftoss
        domain:  # 访问域名，注意“/”结尾，例如：http://minio.abc.com/abc/
        base-path:  # 基础路径
```

#### 6.4.6 第三方登录配置

打开编辑 `jnpf-admin/src/main/resources/application-dev.yml` ，修改以下配置
> 配置默认关闭

```yaml
socials:
  # 第三方登录功能开关(false-关闭，true-开启)
  socials-enabled: false
  config:
    - # 微信
      provider: wechat_open
      client-id: your-client-id
      client-secret: your-client-secret
    - # qq
      provider: qq
      client-id: your-client-id
      client-secret: your-client-secret
    - # 企业微信
      provider: wechat_enterprise
      client-id: your-client-id
      client-secret: your-client-secret
      agentId: your-agentId
    - # 钉钉
      provider: dingtalk
      client-id: your-client-id
      client-secret: your-client-secret
      agentId: your-agentId
    - # 飞书
      provider: feishu
      client-id: your-client-id
      client-secret: your-client-secret
    - # 小程序
      provider: wechat_applets
      client-id: your-client-id
      client-secret: your-client-secret
```
#### 6.4.7 任务调度配置

打开编辑 `jnpf-admin/src/main/resources/application-dev.yml` ，修改以下配置，调整 xxl.job.admin.addresses 地址

```yaml
xxl:
  job:
    accessToken: '432e62f3b488bc861d91b0e274e850cc'
    i18n: zh_CN
    logretentiondays: 30
    triggerpool:
      fast:
        max: 200
      slow:
        max: 100
    # xxl-job服务端地址
    admin:
      addresses: http://127.0.0.1:30020/xxl-job-admin/
    executor:
      address: ''
      appname: xxl-job-executor-sample1
      ip: ''
      logpath: /data/applogs/xxl-job/jobhandler
      logretentiondays: 30
      port: 9999
  # rest调用xxl-job接口地址
  admin:
    register:
      handle-query-address: ${xxl.job.admin.addresses}api/handler/queryList
      job-info-address: ${xxl.job.admin.addresses}api/jobinfo
      log-query-address: ${xxl.job.admin.addresses}api/log
      task-list-address: ${xxl.job.admin.addresses}api/ScheduleTask/List
      task-info-address: ${xxl.job.admin.addresses}api/ScheduleTask/getInfo
      task-save-address: ${xxl.job.admin.addresses}api/ScheduleTask
      task-update-address: ${xxl.job.admin.addresses}api/ScheduleTask
      task-remove-address: ${xxl.job.admin.addresses}api/ScheduleTask/remove
      task-start-or-remove-address: ${xxl.job.admin.addresses}api/ScheduleTask/updateTask
```
## 七 启动项目

找到`jnpf-admin/src/main/java/JnpfAdminApplication.java`，右击运行即可。

## 八 项目发布

- 在IDEA中，双击右侧Maven中 `jnpf-java-boot` > `Lifecycle` > `clean` 清理项目
- 在IDEA中，双击右侧Maven中 `jnpf-java-boot` > `Lifecycle` > `package` 打包项目
- 打开 `jnpf-java-boot\jnpf-admin\target`，将 `jnpf-admin-6.1.0-RELEASE.jar` 上传至服务器

## 九 接口文档

- `http://localhost:30000/doc.html`
