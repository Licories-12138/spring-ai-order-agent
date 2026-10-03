# spring-ai-order-agent

> 用 Spring AI 的 **Function Calling（Tool Calling）** 让大模型自主调用 Java 方法查询订单 —— 从"聊天"走向"能干活"的最小可运行示例。

核心问题不是"怎么调大模型 API"，而是：**怎么让模型自己决定调哪个 Java 方法、自己从人话里抽出参数**。这是自然语言点单、智能客服、Agent 工具链的共同地基。

---

## 效果实测

问一句人话：

```
GET http://localhost:8080/chat?message=13800138000这个手机号一共花了多少钱
```

模型回答：

```
手机号 13800138000 共 2 笔订单，总金额 113.5 元
```

同一时刻控制台打印：

```
>>> 模型调用了我的 Java 方法，参数是：13800138000
```

**这行日志是关键证据** —— 数字不是模型编的，是它决定调用 `queryOrderTotalAmount("13800138000")`，由 Spring AI 真正执行 Java 代码算出来的。

问 "今天天气怎么样" 则不会触发任何工具（负面测试通过），模型不会乱调方法。

---

## 技术栈

| 项 | 版本 |
|---|---|
| JDK | 21 |
| Spring Boot | 4.1.1 |
| Spring AI | 2.0.1（`spring-ai-starter-model-openai`） |
| 模型 | 阿里云百炼 `qwen3.7-flash`（OpenAI 兼容端点） |

百炼提供 OpenAI 兼容接口，所以直接用 `spring-ai-starter-model-openai`，只改 `base-url` 和 `api-key` 即可，无需引阿里 SDK。

---

## 快速开始

```bash
# 1. 设置密钥（从阿里云百炼控制台获取，不要写进配置文件）
export DASHSCOPE_KEY=sk-xxxxxxxx

# 2. 启动
./mvnw spring-boot:run

# 3. 调用
curl "http://localhost:8080/chat?message=帮我查一下13800138000的订单"
```

`application.yml` 里只写 `${DASHSCOPE_KEY}` 占位，真实密钥走环境变量 —— 这也是本项目能直接公开的原因。

---

## 三个工具

`OrderTools.java` 里三个普通 Java 方法，加上 `@Tool` 注解后模型就能"看见"它们：

| 方法 | 作用 | 参数抽取示例 |
|---|---|---|
| `queryOrdersByPhone` | 按手机号查订单列表 | "138 的订单" → `phone=13800138000` |
| `queryOrderDetail` | 按订单号查详情 | "SK20261001001 到哪了" → `orderNo=SK20261001001` |
| `queryOrderTotalAmount` | 统计该手机号历史总金额 | "一共花了多少钱" → 同上手机号 |

注册方式（`ChatController`）：

```java
this.chatClient = builder
    .defaultSystem("你是一个简洁的中文助手，回答控制在三句话以内")
    .defaultTools(orderTools)
    .build();
```

---

## 机制：模型到底做了什么

一个常见误解是"模型会执行代码"。**不会**。真实流程是四步：

1. 模型收到你的问题 + 三个工具的**签名和 description**
2. 模型决定："我要调 `queryOrderTotalAmount`，参数 `phone=13800138000`" —— 它只返回这个意图
3. **Spring AI 执行**真正的 Java 方法，拿到返回值
4. 返回值回灌给模型，模型组织成人话输出给你

所以 `@Tool` 的 `description` 和 `@ToolParam` 的 `description` 不是注释，**是给模型看的说明书**，写不清楚模型就不会调或者传错参数。

---

## 踩过的坑（真实记录）

**1. 403 `Workspace endpoint access denied`**
百炼控制台给的可能是 workspace 专属端点（形如 `ws-xxx.maas.aliyuncs.com/compatible-mode/v1`），用它报 403。必须换成通用兼容端点：

```
https://dashscope.aliyuncs.com/compatible-mode/v1
```

**2. 金额求和用 `double` 会丢精度**
`45.5 + 68.0` 看似没问题，但金额计算必须用 `BigDecimal`：

```java
BigDecimal total = list.stream()
    .map(o -> BigDecimal.valueOf((Double) o.get("amount")))
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```

**3. `temperature` 要按场景调**
工具调用场景下参数解析必须稳定，本项目设为 **0.2**。做过对照实验：0.1 时三次回答几乎一致，1.5 时发散明显甚至编造参数。聊天场景可以调高，工具场景必须压低。

**4. Spring AI 2.x 配置结构变了**
没有 `spring.ai.openai.chat.options.*` 这一层，直接写 `spring.ai.openai.chat.model` / `.temperature`。

---

## 下一步

- [ ] 内存假数据换成**真 MySQL**（对接苍穹外卖订单表）
- [ ] 加结构化输出，把"来两份宫保鸡丁不要辣"解析成下单参数 JSON
- [ ] 接 RAG，让模型基于菜品文档回答而不是凭空编
- [ ] 补上观测：每次调用的 token 消耗与耗时

---

## 目录结构

```
src/main/java/com/example/llmdemo/
├── LlmDemoApplication.java
├── controller/ChatController.java   # ChatClient 装配 + /chat 接口
└── tool/OrderTools.java             # @Tool 工具定义（本项目的核心）
```
