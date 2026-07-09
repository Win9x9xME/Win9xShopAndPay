# 更新日志

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

---

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

---

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

---

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