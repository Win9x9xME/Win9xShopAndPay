# 更新日志

## Beta 3.0.0 - 2026-07-10

### 新增功能

#### 1. 抽奖机系统

- 新增 `LotteryManager` 管理抽奖逻辑
- 新增 `LotteryGUI` 抽奖图形界面（3x9布局）
- 新增 `lottery.yml` 奖品配置文件
- 支持加权概率抽奖（权重越高，中奖概率越大）
- 支持自定义抽奖消耗币种和金额
- 炫酷旋转动画效果（1.5秒）
- 稀有度系统：common、uncommon、rare、epic、legendary
- 抽奖按钮冷却时间配置（默认5秒）
- 命令：`/wsap lottery` 打开抽奖机

#### 2. 安全增强

- LotteryGUI 使用 UUID 而非玩家名称作为键
- 使用 `AtomicBoolean` 防止并发抽奖
- 使用 `CopyOnWriteArrayList` 确保奖品列表线程安全
- 配置验证：过滤负数消耗、负数权重、负数数量
- 抽奖冷却时间从配置读取，支持热重载

### 配置更新

`config.yml` 新增配置项：

```yaml
lottery:
  draw-cooldown: 5           # 抽奖按钮冷却时间（秒）
```

`lottery.yml` 配置示例：

```yaml
lottery:
  cost:
    currency-id: "coins"      # 抽奖消耗的币种ID
    amount: 100.0              # 每次抽奖消耗金额
  prizes:
    common_1:
      type: "DIAMOND"          # 物品类型
      amount: 1                # 数量
      weight: 30.0             # 权重（越高概率越大）
      display-name: "&f钻石"   # 显示名称
      rarity: "common"         # 稀有度
```

### 权限更新

新增权限节点：

```yaml
win9xshopandpay.lottery:
  description: 使用抽奖机的权限
  default: true
```

### 更新文件

- `config.yml` - 添加抽奖冷却配置
- `lottery.yml` - 新增抽奖配置文件
- `LotteryPrize.java` - 新增奖品数据类
- `LotteryManager.java` - 新增抽奖管理类
- `LotteryGUI.java` - 新增抽奖图形界面
- `Win9xShopAndPay.java` - 注册抽奖组件
- `Win9xShopAndPayCommand.java` - 添加抽奖命令
- 6个语言文件 - 添加抽奖相关消息

***

## Beta 2.0.0 - 2026-07-10

### 新增功能

#### 1. 更多AI API支持

- 添加 Kimi API 支持：`https://api.moonshot.cn/v1/chat/completions`
- 添加混元 API 支持：`https://hunyuan.tencentcloudapi.com/v1/chat/completions`
- 添加豆包 API 支持：`https://api.doubao.com/v1/chat/completions`
- 添加 Qwen API 支持：`https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`
- 添加 Minimax API 支持：`https://api.minimax.chat/v1/text/chatcompletion`
- 添加 Anthropic 格式支持（Claude）
- 添加讯飞星火格式支持
- 添加自定义请求模板功能

#### 2. 商店GUI重构（6x9布局）

- 界面大小改为 6x9（54格）
- 第一行：币种分类栏，支持翻页
- 中间四行：商品展示区，支持翻页
- 最后一行：搜索栏和购买币种按钮，支持翻页
- 搜索功能：点击搜索框后输入物品名称过滤

#### 3. 语言系统优化

- 添加语言文件热重载功能
- `/wsap reload` 命令现在会重新加载语言文件
- 修复商店指南针颜色代码显示问题
- 修复所有物品名称和描述的颜色代码处理

### 安全修复

#### 1. 权限与安全审查

- 修复 `CurrencyManager` 暴露 `playerBalances` 的安全问题
- 修复 `Win9xShopAndPayCommand` 绕过API直接修改余额
- 添加 `InventoryDragEvent` 处理，防止拖放物品到商店界面
- 修复线程安全问题：`playerContexts` 和 `giveShopCooldowns` 改为 `ConcurrentHashMap`
- 修复 `secureDataFolder()` 权限设置错误（改为所有者权限）

#### 2. 异常处理增强

- 添加 Material 无效类型的异常处理（CDKeyManager、ShopManager）
- 添加 API 端点格式验证
- 添加空值检查和边界检查

### 配置更新

`config.yml` 新增配置项：

```yaml
ai-assistant:
  api-format: "openai"          # API格式类型：openai, anthropic, spark, minimax, custom
  custom-headers: {}            # 自定义请求头
  request-template: ""          # 自定义请求模板（仅custom格式）
  response-path: ""             # 响应内容提取路径（仅custom格式）
```

### 更新文件

- `config.yml` - 添加更多AI API配置和说明
- `ShopGUI.java` - 重构商店GUI为6x9布局
- `AIAssistantManager.java` - 添加多种API格式支持
- `LanguageManager.java` - 添加语言热重载功能
- `CurrencyManager.java` - 修复安全问题，添加setBalance方法
- `CDKeyManager.java` - 添加Material异常处理
- `ShopManager.java` - 添加Material异常处理
- `ColorCodeConverter.java` - 优化颜色代码转换
- `ShopItemListener.java` - 修复指南针颜色代码
- `ChatListener.java` - 使用ColorCodeConverter发送消息
- `Win9xShopAndPayCommand.java` - 支持语言重载
- `Win9xShopAndPay.java` - 添加GitHub仓库地址

***

## Beta 1.0.3 - 2026-07-09

### 优化与修复

#### 1. AI助手对话上下文支持

- 在 `config.yml` 中添加了 `enable-context` 配置项，默认开启
- 添加了 `context-length` 配置项，默认保存最近10条消息
- 添加了 `system-prompt` 系统提示词，防止提示词注入攻击
- AI线程池改为固定大小（5个线程），防止资源耗尽

#### 2. AI配置热重载支持

- `/wsap reload` 命令现在会重新加载AI助手配置
- 支持在线修改上下文设置、触发前缀等配置

### 配置更新

`config.yml` 新增配置项：

```yaml
ai-assistant:
  enable-context: true           # 是否启用对话上下文
  context-length: 10             # 上下文长度（保存最近消息条数）
  system-prompt: "..."           # 系统提示词（定义AI行为边界）
```

### 更新文件

- `config.yml` - 添加AI上下文配置和系统提示词
- `AIAssistantManager.java` - 实现上下文管理和系统提示词
- `Win9xShopAndPay.java` - 添加 `reloadAIConfig()` 方法
- `Win9xShopAndPayCommand.java` - 重载命令调用AI配置重载

***

## Beta 1.0.2 - 2026-07-09

### 优化与修复

#### 1. AI助手触发前缀可配置

- 在 `config.yml` 中添加了 `ai-assistant.trigger-prefix` 配置项
- 默认值为 `"#"`，可修改为其他字符避免与其他插件冲突
- 解决了与SlimeFun4等插件聊天搜索功能的冲突问题
- AI指令消息不再广播到公共频道，保护玩家隐私

#### 2. CDKey玩家单次使用限制

- 新增每位玩家仅能使用同一CDKey一次的限制
- 即使CDKey设置了多次使用次数，单个玩家也只能兑换一次
- 防止玩家重复刷取同一礼包
- 使用 `usedByPlayers` 集合记录已使用的玩家UUID

### 配置更新

`config.yml` 新增配置项：

```yaml
ai-assistant:
  trigger-prefix: "#"  # AI助手触发前缀，可修改为@、!等
```

### 更新文件

- `config.yml` - 添加触发前缀配置
- `ChatListener.java` - 读取配置前缀，取消事件广播
- `Win9xShopAndPay.java` - 添加获取触发前缀方法
- `CDKey.java` - 添加玩家使用记录集合
- `CDKeyManager.java` - 添加玩家使用检查逻辑
- `Win9xShopAndPayCommand.java` - 更新CDKey兑换提示
- `CDKeyCommand.java` - 更新CDKey兑换提示
- 6个语言文件 - 添加"已使用过此CDKey"提示

***

## Beta 1.0.1 - 2026-07-09

### 新增功能

#### 1. 购买币种按钮文字提示

- 在 `config.yml` 中添加了 `gui.buy-currency-message` 配置项
- 默认值为："请跳转到该网页以购买其他币种！"
- 点击按钮后会先显示自定义提示，再显示可点击链接

#### 2. AI助手功能

- 新增 `AIAssistantManager` 类处理AI对话
- 新增 `ChatListener` 监听玩家聊天消息
- 支持所有兼容OpenAI格式的API（OpenAI、DeepSeek等）
- 玩家输入 `#内容` 即可与AI对话
- AI回复格式：`[AI助手] 回复内容`
- 支持自定义AI显示名称

### 配置更新

`config.yml` 新增配置项：

```yaml
gui:
  buy-currency-message: "请跳转到该网页以购买其他币种！"

ai-assistant:
  enabled: false
  api-endpoint: ""
  api-key: ""
  model: "gpt-3.5-turbo"
  name: "AI助手"
```

### 权限更新

新增权限节点：

```yaml
win9xshopandpay.ai.use:
  description: 使用AI助手的权限
  default: true
```

### 新增文件

- `src/main/java/com/win9x/shopandpay/manager/AIAssistantManager.java`
- `src/main/java/com/win9x/shopandpay/listener/ChatListener.java`

### 更新文件

- `config.yml` - 添加AI配置和购买币种提示
- `ShopGUI.java` - 显示自定义提示文字
- `Win9xShopAndPay.java` - 注册AI组件
- `plugin.yml` - 添加AI权限
- 6个语言文件 - 添加AI相关消息

***

## Beta 1.0.0 - 2026-07-09

### 初始版本功能

#### 商店系统

- 图形化商店GUI界面
- 右键商店指南针打开GUI
- `/wsap shop` 文字形式商店列表
- 多币种支持

#### CDKey系统

- 创建/兑换/删除CDKey
- 支持使用次数限制
- 支持有效期设置

#### 币种系统

- 自定义币种配置
- Vault经济系统联动
- 内置存储支持

#### 多语种支持

- 简体中文、繁体中文
- 美式英语、英式英语
- 日语、韩语

#### 其他功能

- 玩家初次进入获得商店指南针
- `/wsap give_shop` 命令获取指南针
- 购买其他币种按钮（网页跳转）

