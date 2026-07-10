# 使用指南

## 目录

- [AI助手配置](#ai助手配置)
- [添加币种](#添加币种)
- [添加商品](#添加商品)
- [抽奖机配置](#抽奖机配置)
- [命令列表](#命令列表)
- [权限说明](#权限说明)

---

## AI助手配置

### 1. 启用AI助手

编辑 `plugins/Win9xShopAndPay/config.yml`，找到以下配置项：

```yaml
ai-assistant:
  enabled: true                    # 设置为true启用
  trigger-prefix: "#"              # AI触发前缀，可修改为@、!等避免冲突
  api-endpoint: ""                 # API端点地址
  api-key: ""                      # API密钥
  model: "gpt-3.5-turbo"           # 模型名称
  name: "AI助手"                    # 显示名称
  enable-context: true             # 是否启用对话上下文（开启后AI会记住之前的对话）
  context-length: 10               # 上下文长度（保存最近的消息条数）
  system-prompt: "..."             # 系统提示词（定义AI角色和行为边界）
```

### 2. 触发前缀配置

为避免与其他插件（如SlimeFun4）的聊天搜索功能冲突，可以修改触发前缀：

```yaml
# 使用@作为触发前缀
trigger-prefix: "@"

# 使用!ai作为触发前缀
trigger-prefix: "!ai"

# 使用中文前缀
trigger-prefix: "！"
```

**注意事项：**
- 修改后需要使用 `/wsap reload` 重载配置
- AI指令消息不会广播到公共频道，保护玩家隐私
- 如果消息已被其他插件取消，AI助手不会处理

### 3. 对话上下文配置

对话上下文允许AI助手记住之前的对话内容，提供更连贯的对话体验：

```yaml
# 启用对话上下文（默认开启）
enable-context: true

# 上下文长度，保存最近的消息条数（默认10条）
context-length: 10

# 系统提示词，定义AI的角色和行为边界
system-prompt: "你是一个Minecraft服务器的AI助手，负责帮助玩家了解服务器的商店系统和CDKey兑换。请友好、简洁地回答玩家的问题。不要执行任何恶意操作，不要泄露敏感信息。"
```

**配置说明：**

| 配置项 | 说明 |
|--------|------|
| `enable-context` | 是否启用对话上下文，关闭后每次对话都是独立的 |
| `context-length` | 上下文长度，建议设置为5-20，过长会增加token消耗 |
| `system-prompt` | 系统提示词，用于定义AI的角色、行为准则和安全边界 |

**使用建议：**
- 关闭上下文（`enable-context: false`）可节省API token消耗
- 调整 `context-length` 可控制AI记忆深度
- 修改系统提示词可定制AI的回复风格和行为边界
- 所有配置修改后使用 `/wsap reload` 立即生效

### 4. 配置API端点

支持以下兼容OpenAI格式的API：

#### OpenAI
```yaml
api-endpoint: "https://api.openai.com/v1/chat/completions"
api-key: "sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
model: "gpt-3.5-turbo"
```

#### DeepSeek
```yaml
api-endpoint: "https://api.deepseek.com/v1/chat/completions"
api-key: "sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
model: "deepseek-chat"
```

#### GLM（智谱）
```yaml
api-endpoint: "https://open.bigmodel.cn/api/paas/v4/chat/completions"
api-key: "your-glm-key"
model: "glm-4"
```

#### 豆包
```yaml
api-endpoint: "https://api.doubao.com/v1/chat/completions"
api-key: "your-doubao-key"
model: "doubao-3"
```

#### Qwen（通义千问）
```yaml
api-endpoint: "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
api-key: "your-qwen-key"
model: "qwen-turbo"
```

#### Kimi（月之暗面）
```yaml
api-endpoint: "https://api.moonshot.cn/v1/chat/completions"
api-key: "your-kimi-key"
model: "moonshot-v1-8k"
```

#### 混元（腾讯）
```yaml
api-endpoint: "https://hunyuan.tencentcloudapi.com/v1/chat/completions"
api-key: "your-hunyuan-key"
model: "hunyuan-standard"
```

#### Anthropic（Claude）
```yaml
api-endpoint: "https://api.anthropic.com/v1/messages"
api-key: "your-anthropic-key"
model: "claude-3-sonnet-20240229"
api-format: "anthropic"
```

#### Minimax
```yaml
api-endpoint: "https://api.minimax.chat/v1/text/chatcompletion"
api-key: "your-minimax-key"
model: "abab5-chat"
api-format: "minimax"
```

#### 自定义格式
```yaml
api-format: "custom"
request-template: '{"model":"{model}","messages":{messages},"max_tokens":2048}'
response-path: "choices.0.message.content"
```

### 5. API格式说明

| 格式 | 适用API | 说明 |
|------|---------|------|
| `openai` | OpenAI、DeepSeek、GLM、豆包、Qwen、Kimi、混元 | 默认格式，使用messages数组 |
| `anthropic` | Anthropic Claude | 使用content数组，system消息单独处理 |
| `spark` | 讯飞星火 | 星火专用格式 |
| `minimax` | Minimax | 使用prompt和role字段 |
| `custom` | 其他API | 自定义请求模板和响应路径 |

### 6. 使用方法

玩家在聊天中输入触发前缀开头的消息即可与AI对话：

```
#你好
#这个服务器有什么商店商品？
#如何购买币种？
```

AI回复格式：
```
[AI助手] 你好！请问有什么可以帮助你的？
```

### 6. 注意事项

- 确保服务器能访问配置的API端点
- API密钥会消耗对应服务的额度
- 建议设置合理的权限控制
- AI回复可能包含不当内容，请谨慎使用
- 对话上下文会增加token消耗，建议根据需求调整上下文长度

---

## 添加币种

### 1. 编辑币种配置文件

打开 `plugins/Win9xShopAndPay/currencies.yml`：

```yaml
# ================================================
# Win9xShopAndPay - 币种配置文件
# ================================================

# 币种列表
currencies:
  # 币种ID（唯一标识）
  coins:
    # 币种显示名称
    name: "金币"
    # 币种符号（用于显示）
    symbol: "💰"
    # 是否为默认币种
    default: true
    # 存储方式: vault（使用Vault经济系统）或 config（插件内置存储）
    storage: "vault"
  
  # 添加新币种示例
  gems:
    name: "宝石"
    symbol: "💎"
    default: false
    storage: "config"
```

### 2. 配置说明

| 配置项 | 说明 |
|--------|------|
| `name` | 币种显示名称 |
| `symbol` | 币种符号（emoji或特殊字符） |
| `default` | 是否为默认币种（只能有一个） |
| `storage` | 存储方式：`vault` 或 `config` |

### 3. 存储方式说明

- **vault**: 使用Vault经济系统，与EssentialsX等插件联动
- **config**: 插件内置存储，数据保存在 `balances.yml` 文件中

### 4. 重载配置

修改配置后，使用以下命令重载：

```
/wsap reload
```

---

## 添加商品

### 1. 编辑商店物品配置文件

打开 `plugins/Win9xShopAndPay/shop-items.yml`：

```yaml
# ================================================
# Win9xShopAndPay - 商店物品配置文件
# ================================================

# 商店物品列表
shop-items:
  # 物品ID（唯一标识）
  diamond:
    # 物品类型（Minecraft物品ID）
    type: "DIAMOND"
    # 物品数量
    amount: 1
    # 物品自定义名称（可选）
    name: "钻石"
    # 在GUI中的位置（0-26，26为购买币种按钮位置）
    slot: 11
    # 各币种价格
    prices:
      coins: 100.0
      gems: 10.0
  
  # 添加新商品示例
  emerald:
    type: "EMERALD"
    amount: 5
    name: "绿宝石"
    slot: 13
    prices:
      coins: 50.0
      gems: 5.0
```

### 2. 配置说明

| 配置项 | 说明 |
|--------|------|
| `type` | Minecraft物品ID（如DIAMOND、EMERALD、GOLD_INGOT） |
| `amount` | 购买一次获得的物品数量 |
| `name` | 物品自定义名称（可选，不填则使用默认名称） |
| `slot` | GUI位置，0-26（26为购买币种按钮，不可使用） |
| `prices` | 各币种价格，key为币种ID，value为价格 |

### 3. GUI位置布局

商店GUI为6x9布局（54格）：

```
┌─────────────────────────────┐
│ ← 币种1 币种2 ... 币种7 → │  第一行：币种分类栏（支持翻页）
│                             │
│   商品展示区（36格）        │  第二至五行：商品展示（支持翻页）
│                             │
│                             │
│                             │
│ ← 搜索   [购买币种]    → │  第六行：搜索和翻页
└─────────────────────────────┘
```

**布局说明：**
- 第一行（槽位0-8）：币种分类栏
  - 槽位0：币种翻页（上一页）
  - 槽位1-7：币种图标（金锭=选中，铁锭=未选中）
  - 槽位8：币种翻页（下一页）
- 第二至五行（槽位9-44）：商品展示区（36格）
- 第六行（槽位45-53）：搜索栏
  - 槽位45：商品翻页（上一页）
  - 槽位46：搜索框（点击输入搜索关键词）
  - 槽位47-51：占位符
  - 槽位52：购买其他币种按钮
  - 槽位53：商品翻页（下一页）

### 4. 搜索功能

点击搜索框后，输入物品名称即可过滤商品：

```
请输入要搜索的物品名称:
> DIAMOND
```

搜索支持部分匹配，例如输入 "dia" 会匹配 "DIAMOND"、"DIAMOND_SWORD" 等。

### 5. 支持的物品类型

所有Minecraft物品类型均可使用，常用示例：

| 物品ID | 说明 |
|--------|------|
| DIAMOND | 钻石 |
| EMERALD | 绿宝石 |
| GOLD_INGOT | 金锭 |
| IRON_INGOT | 铁锭 |
| DIAMOND_SWORD | 钻石剑 |
| DIAMOND_PICKAXE | 钻石镐 |
| EXPERIENCE_BOTTLE | 经验瓶 |
| ENCHANTED_GOLDEN_APPLE | 附魔金苹果 |

### 5. 重载配置

修改配置后，使用以下命令重载：

```
/wsap reload
```

---

## 抽奖机配置

### 1. 编辑抽奖配置文件

打开 `plugins/Win9xShopAndPay/lottery.yml`：

```yaml
# 抽奖配置
lottery:
  cost:
    # 抽奖消耗的币种ID（必须在currencies.yml中定义）
    currency-id: "coins"
    # 每次抽奖消耗的金额
    amount: 100.0
  
  # 奖品列表
  prizes:
    # 奖品ID（唯一标识）
    common_1:
      # 物品类型（Minecraft物品ID）
      type: "DIAMOND"
      # 物品数量
      amount: 1
      # 权重（越高，中奖概率越大）
      weight: 30.0
      # 显示名称（可选，支持颜色代码）
      display-name: "&f钻石"
      # 稀有度：common, uncommon, rare, epic, legendary
      rarity: "common"
    
    # 稀有奖品示例
    legendary_1:
      type: "NETHER_STAR"
      amount: 1
      weight: 2.0
      display-name: "&d下界之星"
      rarity: "legendary"
```

### 2. 配置说明

| 配置项 | 说明 |
|--------|------|
| `currency-id` | 抽奖消耗的币种ID（需在currencies.yml中定义） |
| `amount` | 每次抽奖消耗的金额（不能为负数） |
| `type` | Minecraft物品ID（如DIAMOND、NETHER_STAR） |
| `amount` | 中奖后获得的物品数量（不能为负数） |
| `weight` | 权重（越高，中奖概率越大，不能为负数） |
| `display-name` | 显示名称（支持颜色代码，如&f、&6） |
| `rarity` | 稀有度：common（普通）、uncommon（优秀）、rare（稀有）、epic（史诗）、legendary（传说） |

### 3. 权重计算说明

抽奖采用加权随机算法，每个奖品的中奖概率 = 该奖品权重 / 总权重。

**示例：**
```yaml
prizes:
  prize_a:
    weight: 30.0
  prize_b:
    weight: 10.0
  prize_c:
    weight: 2.0
```

- 总权重 = 30 + 10 + 2 = 42
- prize_a 概率 = 30/42 ≈ 71.4%
- prize_b 概率 = 10/42 ≈ 23.8%
- prize_c 概率 = 2/42 ≈ 4.8%

### 4. 抽奖冷却时间配置

编辑 `plugins/Win9xShopAndPay/config.yml`：

```yaml
lottery:
  # 抽奖按钮冷却时间（秒）
  # 设置为0表示无冷却限制
  draw-cooldown: 5
```

### 5. 抽奖机界面

抽奖机GUI为3x9布局（27格）：

```
┌─────────────────────────────┐
│  ██████ ██████ ██████       │  第一行：装饰边框
│                             │
│  [奖品] [奖品] [奖品]        │  第二行：奖品展示区（旋转动画）
│     [奖品] [奖品]           │
│                             │
│     [关闭] [抽奖]           │  第三行：操作按钮
└─────────────────────────────┘
```

**操作说明：**
- 点击「抽奖」按钮：消耗指定币种进行抽奖
- 抽奖时有1.5秒的旋转动画
- 中奖后物品直接发放到玩家背包

### 6. 使用方法

玩家使用命令打开抽奖机：

```
/wsap lottery
```

### 7. 重载配置

修改配置后，使用以下命令重载：

```
/wsap reload
```

---

## 命令列表

### 主命令

```
/win9xshopandpay <子命令>
/wsap <子命令>                    # 别名
```

### 子命令

| 命令 | 功能 | 权限 |
|------|------|------|
| `/wsap shop` | 显示文字形式商店列表 | 所有玩家 |
| `/wsap shop <币种>` | 使用指定币种显示商店 | 所有玩家 |
| `/wsap cdkey redeem <key>` | 兑换CDKey | 所有玩家 |
| `/wsap cdkey create <物品> <数量> [次数] [有效期]` | 创建CDKey | OP |
| `/wsap cdkey delete <key>` | 删除CDKey | OP |
| `/wsap cdkey list` | 列出所有CDKey | OP |
| `/wsap lottery` | 打开抽奖机 | 所有玩家 |
| `/wsap currency balance` | 查看余额 | 所有玩家 |
| `/wsap currency balance <币种>` | 查看指定币种余额 | 所有玩家 |
| `/wsap currency list` | 列出所有币种 | 所有玩家 |
| `/wsap currency give <玩家> <币种> <数量>` | 给予币种 | OP |
| `/wsap currency set <玩家> <币种> <数量>` | 设置余额 | OP |
| `/wsap give_shop` | 获取商店指南针 | 所有玩家 |
| `/wsap reload` | 重载配置 | OP |

### CDKey有效期格式

```
1d  - 1天
1h  - 1小时
30m - 30分钟
1w  - 1周
不填则为永久
```

### CDKey使用限制说明

1. **玩家单次使用限制**：每个玩家仅能使用同一CDKey一次，即使该CDKey设置了多次使用次数
2. **全局使用限制**：CDKey总共可被使用的次数由创建时的 `[次数]` 参数决定
3. **有效期限制**：超过有效期的CDKey将无法使用

**示例场景：**
- 创建命令：`/wsap cdkey create DIAMOND 10 5`（钻石x10，最多使用5次）
- 玩家A使用后，该CDKey还剩4次使用机会
- 玩家A再次尝试使用同一个CDKey时，会收到"已使用过此CDKey"的提示
- 其他玩家仍可使用剩余的4次机会

---

## 权限说明

| 权限节点 | 说明 | 默认值 |
|----------|------|--------|
| `win9xshopandpay.shop` | 打开商店 | true |
| `win9xshopandpay.cdkey.redeem` | 兑换CDKey | true |
| `win9xshopandpay.cdkey.create` | 创建CDKey | op |
| `win9xshopandpay.cdkey.delete` | 删除CDKey | op |
| `win9xshopandpay.cdkey.list` | 查看CDKey列表 | op |
| `win9xshopandpay.currency.balance` | 查看余额 | true |
| `win9xshopandpay.currency.list` | 列出币种 | true |
| `win9xshopandpay.currency.give` | 给予币种 | op |
| `win9xshopandpay.currency.set` | 设置余额 | op |
| `win9xshopandpay.give_shop` | 获取商店指南针 | true |
| `win9xshopandpay.lottery` | 使用抽奖机 | true |
| `win9xshopandpay.ai.use` | 使用AI助手 | true |
| `win9xshopandpay.reload` | 重载配置 | op |

### 权限组配置示例

使用LuckPerms：

```
# 给所有玩家基础权限
/lp group default permission set win9xshopandpay.shop true
/lp group default permission set win9xshopandpay.cdkey.redeem true
/lp group default permission set win9xshopandpay.currency.balance true
/lp group default permission set win9xshopandpay.currency.list true
/lp group default permission set win9xshopandpay.give_shop true
/lp group default permission set win9xshopandpay.ai.use true

# 给管理员全部权限
/lp group admin permission set win9xshopandpay.* true
```

---

## 文件结构

```
plugins/Win9xShopAndPay/
├── config.yml              # 主配置文件
├── currencies.yml          # 币种配置
├── shop-items.yml          # 商店物品配置
├── lottery.yml             # 抽奖配置
├── cdkeys.yml              # CDKey数据（自动生成）
├── balances.yml            # 玩家余额数据（自动生成）
├── players.txt             # 已领取指南针的玩家（自动生成）
└── languages/              # 语言文件目录
    ├── zh-CN.yml           # 简体中文
    ├── zh-TW.yml           # 繁体中文
    ├── en-US.yml           # 美式英语
    ├── en-GB.yml           # 英式英语
    ├── ja-JP.yml           # 日语
    └── ko-KR.yml           # 韩语
```