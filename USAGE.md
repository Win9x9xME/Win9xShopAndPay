# 使用指南

## 目录

- [AI助手配置](#ai助手配置)
- [添加币种](#添加币种)
- [添加商品](#添加商品)
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
```

#### DeepSeek
```yaml
api-endpoint: "https://api.deepseek.com/v1/chat/completions"
api-key: "sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
model: "deepseek-chat"
```

#### 其他兼容API
```yaml
api-endpoint: "https://api.example.com/v1/chat/completions"
api-key: "your-api-key"
model: "your-model-name"
```

### 5. 使用方法

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

```
┌─────────────────────────────┐
│ 0  1  2  3  4  5  6  7  8 │
│ 9 10 11 12 13 14 15 16 17 │
│18 19 20 21 22 23 24 25 26 │  ← 26为购买币种按钮
└─────────────────────────────┘
```

### 4. 支持的物品类型

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