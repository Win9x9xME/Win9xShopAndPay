# 使用指南

## 目录

- [添加币种](#添加币种)
- [添加商品](#添加商品)
- [抽奖机配置](#抽奖机配置)
- [彩蛋系统](#彩蛋系统)
- [调试功能](#调试功能)
- [命令列表](#命令列表)
- [权限说明](#权限说明)
- [安全说明](#安全说明)

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

## 彩蛋系统

### 1. 编辑彩蛋配置文件

打开 `plugins/Win9xShopAndPay/easter-egg.yml`：

```yaml
# ================================================
# Win9xShopAndPay - 彩蛋配置文件
# ================================================
# 
# 这里包含各种隐藏的彩蛋功能
# 请勿随意开启，可能会影响服务器平衡
# 
# GitHub仓库: https://github.com/Win9x9xME/Win9xShopAndPay
# ================================================

# lengshang-rsc彩蛋
# 当玩家拥有水下呼吸效果时，自动获得永久的op权限
# 默认关闭，开启后请谨慎使用
lengshang-rsc:
  enabled: false
```

### 2. lengshang-rsc彩蛋说明

**功能描述：**
- 当彩蛋开启且玩家获得水下呼吸药水效果时，自动授予玩家永久OP权限
- 玩家获得OP后会收到提示消息：`[lengshang-rsc] 恭喜你获得了永久OP权限！`
- 仅对非OP玩家生效，已拥有OP权限的玩家不会重复授予

**触发方式：**
- 玩家喝下水下呼吸药水
- 玩家使用带有水下呼吸附魔的头盔
- 其他任何方式获得水下呼吸效果

**注意事项：**
- 此功能会严重影响服务器平衡，请谨慎使用
- 建议仅在测试服务器或私人服务器中开启
- OP权限为永久权限，除非手动撤销

### 3. 重载配置

修改配置后，使用以下命令重载：

```
/wsap reload
```

---

## 调试功能

### 1. 启用调试日志

编辑 `plugins/Win9xShopAndPay/config.yml`：

```yaml
de-bug:
  # 开启后，控制台输出会同时发送到游戏内所有OP玩家的聊天框
  # 仅用于调试目的，生产环境请关闭
  enabled: false
```

### 2. 功能说明

**作用：**
- 开启后，插件输出的所有日志（info、warning、severe）会同时显示在所有在线OP玩家的聊天框中
- 消息格式：`§7[DEBUG] §f日志内容`
- 方便管理员在游戏内实时查看插件运行状态

**使用场景：**
- 调试商店购买流程
- 排查CDKey兑换问题
- 监控币种余额变化
- 查看抽奖结果日志

**注意事项：**
- 大量日志会刷屏，请仅在调试时开启
- 生产环境务必关闭此功能
- 仅OP玩家能看到调试消息

### 3. 重载配置

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
| `/wsap currency give <玩家> <币种> <数量>` | 给予币种（支持离线玩家） | OP |
| `/wsap currency set <玩家> <币种> <数量>` | 设置余额（支持离线玩家） | OP |
| `/wsap give_shop` | 获取商店指南针 | 所有玩家 |
| `/wsap reload` | 重载配置 | OP |

### CDKey复制功能

创建CDKey成功后，会自动发送可点击的复制消息：

```
[点击复制CDKey] ABC123-XYZ789
```

点击消息即可将CDKey复制到剪贴板，方便分享给玩家。

### 离线玩家支持

管理员现在可以给离线玩家调整币种余额：

**命令示例：**
```
# 给离线玩家增加币种
/wsap currency give Player1 coins 1000

# 设置离线玩家的余额
/wsap currency set Player1 gems 50
```

**存储方式兼容性：**

| 存储方式 | 在线玩家 | 离线玩家 | 说明 |
|----------|----------|----------|------|
| `vault` | ✅ 支持 | ❌ 不支持 | Vault API需要在线玩家对象 |
| `config` | ✅ 支持 | ✅ 支持 | 数据存储在 `balances.yml` 文件中 |

**注意事项：**

1. **Vault存储限制**：使用 Vault 存储的币种（如默认的金币）只有在玩家在线时才能调整
2. **内置存储支持**：使用 config 存储的币种无论玩家是否在线都可以调整
3. **玩家验证**：如果玩家从未在服务器上登录过，会显示"未找到该玩家"提示
4. **在线通知**：如果玩家在线，会收到余额变化的提示消息；离线玩家上线后会看到更新后的余额

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
├── easter-egg.yml          # 彩蛋配置
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

---

## 安全说明

### 1. 并发安全

插件使用以下线程安全机制：

| 组件 | 线程安全措施 |
|------|-------------|
| `CDKeyManager` | 使用 `ConcurrentHashMap` 存储CDKey |
| `CDKey` | 使用 `AtomicInteger` 计数，`ConcurrentHashMap.newKeySet()` 存储已使用玩家 |
| `PlayerJoinListener` | 使用 `ConcurrentHashMap.newKeySet()` 存储已领取玩家 |
| `CurrencyManager` | 使用 `ConcurrentHashMap` 存储玩家余额，`ReadWriteLock` 保护保存操作 |
| `LotteryGUI` | 使用 `AtomicBoolean` 防止并发抽奖 |
| `LotteryManager` | 使用 `CopyOnWriteArrayList` 存储奖品列表 |
| `ShopManager` | 使用 `CopyOnWriteArrayList` 存储商店物品 |

### 2. 文件路径安全

- 内置HTTP服务器仅允许访问插件数据目录内的文件
- 使用 `getCanonicalPath()` 验证路径，防止路径遍历攻击
- 文件名白名单机制，仅允许指定扩展名的文件访问

### 3. API安全

- API密钥存储在配置文件中，插件启动时自动设置文件权限为仅所有者可读
- API请求添加超时设置（连接超时10秒，读取超时30秒）
- 自定义请求头验证，防止HTTP头注入攻击

### 4. 权限控制

- 所有管理员命令需要相应的权限节点
- 玩家无法直接修改他人余额（需管理员权限）
- CDKey兑换有玩家单次使用限制

### 5. 依赖安全

- 使用官方 VaultAPI 版本（1.7.1），通过 JitPack 仓库获取
- 依赖配置使用 `provided` 范围，避免冲突

### 6. 配置验证

- 抽奖消耗金额、权重、数量不能为负数
- Material类型无效时自动回退到默认值

### 7. 彩蛋安全

- 彩蛋功能默认关闭，需手动开启
- `lengshang-rsc` 彩蛋会授予OP权限，请谨慎使用
- 建议仅在测试环境中开启彩蛋功能