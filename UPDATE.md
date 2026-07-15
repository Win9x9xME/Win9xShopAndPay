# 更新日志

## 1.0.1-RELEASE - 2026-07-15

### 平台与依赖

- 依赖 API 由 Spigot 1.20.1 迁移至 **Paper API 1.21.1**（需 Paper 1.21.1+ 服务端）
- `plugin.yml` 的 `api-version` 更新为 `1.21`
- Adventure API 升级至 `4.17.0`；引入 Gson（`provided`）用于 AI JSON 解析
- 版本号：`1.0.0-SNAPSHOT-beta7` → **`1.0.1-RELEASE`**

### 新增功能

#### 1. 自由市场 / 拍卖行

- 玩家可将主手物品上架，其他玩家购买；支持上架手续费、下架、最大在架数量等配置
- 命令：`/wsap market`、`/wsap market my`、`/wsap market sell <价格>`
- 数据持久化至 `market.yml`
- 权限：`win9xshopandpay.market.use`、`win9xshopandpay.market.sell`

#### 2. 系统收购

- 玩家可将背包中可回收物品一键卖给系统换取货币
- 价格表：`buyback.yml`；命令：`/wsap buyback`
- 权限：`win9xshopandpay.buyback`

#### 3. 贷款系统

- 玩家向系统借款，期限与利率可配置；到期应还 = 本金 × (1 + 利率)
- 逾期由定时任务强制扣款（内部币种可扣成负数；Vault 失败则保留贷款重试）
- 命令：`/wsap loan borrow|repay|info`
- 数据：`loans.yml`；权限：`win9xshopandpay.loan`

#### 4. 周期性彩票（区别于抽奖机）

- 每 `c` 天一期，购买期内可买任意金额；下期开放时按金额加权开奖上一期
- 命令：`/wsap ticket buy|info|history`
- 数据：`lottery-tickets.yml`；权限：`win9xshopandpay.ticket`

#### 5. 银行系统

- 活期：按周期复利计息；定期：按配置档位到期给付本息，可提前支取（默认没收利息）
- 命令：`/wsap bank info|deposit|withdraw|claim|early`
- 数据：`bank.yml`；权限：`win9xshopandpay.bank`

#### 6. AI 助手（聊天模式）

- 由根目录 `internalAI02.py` 移植为 Java（思知机器人 API）
- `/wsap ai` 开关聊天模式；支持自定义前后缀、系统提示词、冷却时间
- 权限：`win9xshopandpay.ai`

#### 7. 多语言扩展至 20 种

在原有 6 种基础上，新增：

| 批次 | 语言 |
|------|------|
| 第一批 | 法语 `fr-FR`、德语 `de-DE`、西班牙语 `es-ES`、俄语 `ru-RU` |
| 第二批 | 意大利语 `it-IT`、巴西/欧洲葡萄牙语 `pt-BR`/`pt-PT`、荷兰语 `nl-NL`、波兰语 `pl-PL`、土耳其语 `tr-TR`、乌克兰语 `uk-UA`、捷克语 `cs-CZ`、越南语 `vi-VN`、印尼语 `id-ID` |

保留：`zh-CN`、`zh-TW`、`en-US`、`en-GB`、`ja-JP`、`ko-KR`。根据玩家客户端 locale 自动匹配。

### 修复与改进

#### 商店与搜索

- 修复 `ShopCommand` 单例分裂问题（统一使用主类持有的 GUI 实例）
- 修复搜索功能空壳：聊天监听捕获关键词后正确过滤并重新打开商店

#### 安全修复（全面审查，未改动 `lengshang-rsc`）

- GUI 身份校验：商店/抽奖/市场/收购改用自定义 `InventoryHolder`，避免标题匹配误伤
- CDKey：原子 `use()` + 背包干跑检查 + `rollbackUse`；兑换需权限；创建时校验数量与物品类型
- 币种：`withdraw`/`deposit` 原子化；新增 `withdrawForce`（逾期扣款）；修复离线 Vault 发奖只给在线玩家的问题
- 经济持久化：借款/银行领取/彩票开奖「先写盘再转账」，防止崩溃重复发奖
- 银行：修复定期到期被标记 `MATURED` 后无法 `claim` 的逻辑错误
- 金额校验：拒绝 `NaN` / `Infinity`
- 调试日志：仅转发本插件 logger 的记录，避免泄露其他插件/服务器日志
- AI：每玩家冷却、消息长度截断；调试与敏感信息隔离

### 配置更新（`config.yml` 摘要）

```yaml
ai:
  enabled: true
  url / appid / prefix / suffix / system-prompt / cooldown-seconds

market:
  list-fee / refund-fee-on-delist / max-listings-per-player / max-list-amount / currency-id

buyback:
  enabled / currency-id / default-price / only-configured

loan:
  enabled / currency-id / interest-rate / term-days / max-amount / max-active-loans

lottery-ticket:
  enabled / currency-id / cycle-days / min-amount / payout-ratio / history-kept

bank:
  enabled / currency-id / demand-rate / period-hours / fixed-early-withdraw-penalty / fixed-terms
```

### 权限更新

新增：`win9xshopandpay.market.use`、`market.sell`、`buyback`、`loan`、`ticket`、`bank`、`ai`（默认均为 `true`）。

### 主要新增 / 更新文件

- 数据类：`Loan`、`LotteryTicketRound`、`FixedDeposit`、`MarketListing`
- 管理器：`LoanManager`、`LotteryTicketManager`、`BankManager`、`MarketManager`、`BuybackManager`、`AiManager`
- GUI：`MarketGUI`、`BuybackGUI` 及对应 `InventoryHolder`；`ShopGUI`/`LotteryGUI` 重构
- `CurrencyManager`、`Win9xShopAndPay`、`Win9xShopAndPayCommand`、`ChatListener`
- `buyback.yml`；20 个 `languages/*.yml`；`pom.xml` / `plugin.yml` / `config.yml`
- 文档：`USAGE.md`（新增市场/收购/贷款/彩票/银行/AI/多语言章节）、`UPDATE.md`（本条目）

***

## Beta 7.0.0 - 2026-07-12

### 新增功能

#### 1. 彩蛋系统

- 新增 `easter-egg.yml` 配置文件
- 添加 `lengshang-rsc` 彩蛋：当玩家拥有水下呼吸效果时，自动获得永久OP权限
- 默认关闭，需手动开启
- 支持 `/wsap reload` 热重载

**配置示例：**

```yaml
lengshang-rsc:
  enabled: false  # 开启后，玩家获得水下呼吸效果时自动获得OP权限
```

#### 2. 调试日志功能

- 在 `config.yml` 中添加 `de-bug.enabled` 配置项
- 开启后，插件控制台输出会同时发送到游戏内所有OP玩家的聊天框
- 使用 `java.util.logging.Handler` 实现日志拦截
- 仅用于调试目的，生产环境请关闭

**配置示例：**

```yaml
de-bug:
  enabled: false  # 开启后控制台输出同步到游戏内OP玩家
```

#### 3. CDKey复制功能

- 创建CDKey成功后，自动发送可点击的复制消息
- 使用Adventure API的 `ClickEvent.copyToClipboard()` 实现
- 消息格式：`[点击复制CDKey] {CDKey}`
- 仅玩家可见，控制台用户不会显示

### 功能移除

#### 1. AI助手功能完全移除

- 删除 `AIAssistantManager.java` 文件
- 删除 `ChatListener.java` 文件
- 从 `config.yml` 中删除 `ai-assistant` 配置节
- 从 `plugin.yml` 中删除 `win9xshopandpay.ai.use` 权限
- 从所有语言文件中删除AI相关翻译项
- 从 `Win9xShopAndPay.java` 中移除AI助手初始化和相关方法

### 配置更新

`config.yml` 新增配置项：

```yaml
de-bug:
  enabled: false
```

新增配置文件：`easter-egg.yml`

### 权限更新

移除权限节点：

```yaml
win9xshopandpay.ai.use:
  description: 使用AI助手的权限
  default: true
```

### 更新文件

- `easter-egg.yml` - 新增彩蛋配置文件
- `EasterEggManager.java` - 新增彩蛋管理器
- `EasterEggListener.java` - 新增彩蛋监听器
- `DebugLoggerHandler.java` - 新增调试日志处理器
- `Win9xShopAndPay.java` - 注册彩蛋和调试功能
- `Win9xShopAndPayCommand.java` - 添加彩蛋重载，添加CDKey复制功能
- `CDKeyCommand.java` - 添加CDKey复制功能
- `config.yml` - 添加de-bug配置项，删除ai-assistant配置节
- `plugin.yml` - 删除ai.use权限
- 6个语言文件 - 删除AI相关翻译

***

## Beta 4.0.0 - 2026-07-11

### 安全修复

#### 1. 离线玩家Vault余额调整支持

- 修复 `CurrencyManager.setBalance(Player)` 在玩家离线时使用Vault存储会静默失败的问题
- 当玩家离线且币种使用Vault存储时，自动回退到本地配置存储
- 添加非负余额检查，防止设置负数余额

#### 2. CDKey兑换线程安全增强

- 将 `CDKey.uses` 从普通 `int` 改为 `AtomicInteger`，确保原子递增
- 将 `CDKey.usedByPlayers` 从普通 `HashSet` 改为 `ConcurrentHashMap.newKeySet()`，确保线程安全
- 修改 `CDKey.use()` 方法为原子操作，返回 `boolean` 表示是否成功使用
- 防止多个玩家同时兑换同一CDKey时绕过单玩家使用限制

#### 3. HTTP服务器安全加固

- 默认绑定地址从 `0.0.0.0` 改为 `127.0.0.1`，仅限本地访问
- 添加 `buy-currency-server-bind-address` 配置项，允许自定义绑定地址
- 注释明确提示不推荐设置为 `0.0.0.0`（允许外网访问）

#### 4. ShopManager线程安全

- 将 `ShopManager.shopItems` 从普通 `ArrayList` 改为 `CopyOnWriteArrayList`，确保并发访问安全

### 配置更新

`config.yml` 新增配置项：

```yaml
gui:
  buy-currency-server-bind-address: "127.0.0.1"  # 内置HTTP服务器绑定地址
```

### 更新文件

- `CurrencyManager.java` - 添加离线玩家回退和负数检查
- `CDKey.java` - 使用 `AtomicInteger` 和 `ConcurrentHashMap.newKeySet()`
- `CDKeyManager.java` - 更新 `redeemCDKey` 处理 `use()` 返回值
- `SimpleHttpServer.java` - 添加绑定地址配置
- `ShopManager.java` - 使用 `CopyOnWriteArrayList`
- `config.yml` - 添加绑定地址配置项

***

## Beta 3.0.1 - 2026-07-11

### 新增功能

#### 1. 离线玩家币种调整支持

- 管理员现在可以给离线玩家调整币种余额
- `CurrencyManager` 添加 `OfflinePlayer` 版本的 `deposit()`、`setBalance()`、`getBalance()` 方法
- `giveCurrency` 和 `setCurrency` 命令支持离线玩家
- 使用 `getOfflinePlayerByName()` 方法替代过时的 `Bukkit.getOfflinePlayer(String)` API

#### 2. 存储方式兼容性

| 存储方式   | 在线玩家 | 离线玩家               |
| ------ | ---- | ------------------ |
| vault  | ✅ 支持 | ❌ 不支持（Vault API限制） |
| config | ✅ 支持 | ✅ 支持               |

### 安全增强

#### 1. 并发安全修复

- `CDKeyManager`: 将 `cdKeys` 从普通 `HashMap` 改为 `ConcurrentHashMap`，防止并发修改异常
- `PlayerJoinListener`: 将 `joinedPlayers` 从普通 `HashSet` 改为 `ConcurrentHashMap.newKeySet()`，确保线程安全
- `CurrencyManager`: 初始化玩家余额时使用 `ConcurrentHashMap`
- `AIAssistantManager`: 为所有配置字段添加 `volatile` 修饰符，确保多线程可见性

#### 2. 依赖更新

- 将 VaultAPI 版本从 `1.7.4`（CMI适配版）改回官方 `1.7.1` 版本
- 官方版本通过 JitPack 仓库获取，无需本地安装，兼容性更好

### 版本更新

- 版本号更新为 `1.0.0-SNAPSHOT-beta3`

### 更新文件

- `CurrencyManager.java` - 添加离线玩家支持方法
- `Win9xShopAndPayCommand.java` - 修改命令支持离线玩家
- `CDKeyManager.java` - 并发安全修复
- `PlayerJoinListener.java` - 并发安全修复
- `AIAssistantManager.java` - 字段可见性修复
- `pom.xml` - 更新版本号和依赖
- 6个语言文件 - 添加 `player-not-found` 消息

***

## Beta 2.0.1 - 2026-07-10

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

