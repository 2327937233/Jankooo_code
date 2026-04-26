智能旅行行程规划 App
这是一个面向旅行场景的 Android 应用。用户只需要输入想去的城市、旅行日期和偏好类型，应用就可以调用 AI 行程规划服务，生成一份按天安排的旅行计划，并结合高德地图展示目的地位置、天气信息和行程详情。

简单来说，它想解决的问题是：

当你想去一座城市旅行，却不知道怎么安排行程时，让 App 帮你快速生成一份清晰、可保存、可查看地图的旅行方案。

项目预览
应用主要围绕「找目的地 - 生成计划 - 保存行程 - 地图查看」展开：

打开 App
  ↓
搜索或选择目的地
  ↓
填写旅行日期和偏好
  ↓
AI 生成每日行程
  ↓
保存到本地行程列表
  ↓
在地图和详情页中查看
核心功能
1. 首页推荐
首页展示热门旅行专题，例如济南、上海、北京、南京、武汉等城市。用户可以直接点击推荐卡片进入对应城市的旅行详情。

同时首页会展示当前位置天气，让用户在规划行程时可以快速了解当前环境。

2. 目的地搜索
搜索页支持输入城市或地点关键词。项目结合了：

本地城市数据 city_list.json
高德地图输入提示能力
这样用户可以更快找到想去的目的地，并直接进入行程创建页面。

3. AI 智能行程生成
用户可以填写：

旅行目的地
出发日期
结束日期
旅行偏好，例如美食、文化、自然风光、Citywalk 等
应用会把这些信息发送给后端 AI 接口，由后端返回结构化行程数据。生成结果会按照天数展示，方便用户快速了解每天去哪里、什么时候去、为什么推荐。

4. 行程保存与管理
生成后的行程可以保存到本地。保存后，用户可以在首页的行程列表中再次打开查看。

目前行程数据使用 Android SharedPreferences 存储，适合轻量级本地保存。

5. 地图查看
行程详情页集成了高德地图，可以根据目标城市定位到对应区域。页面底部使用行程面板展示每日安排，让地图和文字计划结合起来，查看体验更直观。

6. 附近地图
应用也提供附近地图入口，可以查看当前位置，或者根据指定城市、坐标进入地图页面。

技术栈
类型	技术
开发语言	Java
平台	Android 原生应用
构建工具	Gradle Kotlin DSL
UI	AndroidX、Material Components、ConstraintLayout
地图服务	高德地图 SDK、定位 SDK、搜索 SDK
网络请求	Retrofit、OkHttp、Gson Converter
本地存储	SharedPreferences
最低版本	Android 9.0, API 28
目标版本	Android API 34
项目结构
.
├── app/
│   ├── libs/
│   │   └── AMap3DMap_...aar              # 高德地图相关 SDK
│   └── src/main/
│       ├── assets/
│       │   └── city_list.json            # 本地城市数据
│       ├── java/com/example/myapplication/
│       │   ├── MainActivity.java         # 首页、天气、推荐、行程列表
│       │   ├── SearchActivity.java       # 目的地搜索
│       │   ├── CreateItineraryActivity.java
│       │   ├── AIResultActivity.java
│       │   ├── ItineraryDetailActivity.java
│       │   ├── NearbyActivity.java
│       │   └── network/                  # 后端接口请求
│       └── res/
│           ├── layout/                   # 页面布局
│           ├── drawable/                 # 图片、图标、背景资源
│           └── values/                   # 主题、颜色、字符串
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
页面说明
页面	作用
MainActivity	应用首页，展示天气、推荐城市和已保存行程
SearchActivity	搜索目的地，支持联想搜索
CreateItineraryActivity	填写旅行信息并请求 AI 生成行程
AIResultActivity	展示 AI 生成结果，并支持保存
ItineraryDetailActivity	使用地图和底部面板展示行程详情
NearbyActivity	展示当前位置或指定地点地图
NewsActivity	旅行资讯页面入口
如何运行
1. 克隆项目
git clone https://github.com/2327937233/Jankooo_code.git
cd Jankooo_code
2. 使用 Android Studio 打开
用 Android Studio 打开项目根目录，等待 Gradle 同步完成。

建议环境：

Android Studio Iguana 或更新版本
JDK 17
Android SDK Platform 34
3. 配置高德地图 Key
在 app/src/main/AndroidManifest.xml 中配置你的高德 Android Key：

<meta-data
    android:name="com.amap.api.v2.apikey"
    android:value="YOUR_AMAP_ANDROID_KEY" />
天气接口还需要高德 Web 服务 Key，目前代码中位于 MainActivity.java。

公开上传到 GitHub 前，建议把真实 Key 替换成自己的配置方式，例如 local.properties 或后端代理，避免密钥泄露。

4. 配置 AI 后端地址
后端接口地址位于：

app/src/main/java/com/example/myapplication/network/RetrofitClient.java
默认接口路径：

POST /api/v1/travel/plan
接口请求内容包含目的地、日期、天数和旅行偏好。后端返回的 JSON 中建议包含：

trip_overview：行程总览
daily_itinerary：每日行程安排
5. 构建项目
Windows：

gradlew.bat assembleDebug
macOS / Linux：

./gradlew assembleDebug
也可以直接在 Android Studio 中点击运行。

权限说明
应用会使用以下权限：

网络权限：访问地图、天气和 AI 后端接口
定位权限：获取当前位置、展示附近地图和天气
存储权限：兼容部分旧版本 Android 的资源访问需求
项目特色
把旅行规划从「手动搜索攻略」变成「输入需求后自动生成」
使用地图承载行程，让计划不只是文字列表
首页有城市专题推荐，适合快速发现旅行灵感
本地保存行程，方便用户之后继续查看
项目结构清晰，适合作为 Android 课程设计、毕业设计或个人作品展示
后续可以优化的方向
将 API Key、后端地址改为更安全的配置方式
使用 Room 数据库存储行程，替代 SharedPreferences
增加用户登录和云端同步
优化 AI 返回内容的解析和异常处理
增加路线规划、交通方式、预算估算等旅行辅助功能
修复部分中文资源或注释的编码显示问题
适用场景
这个项目适合作为：

Android 原生开发练习项目
智能旅行规划类课程设计
AI + 地图服务结合的移动端作品
个人 GitHub 项目展示
License
本项目目前主要用于学习、课程设计和个人作品展示。如需开源发布，可以根据需要补充 MIT、Apache-2.0 等开源协议。
