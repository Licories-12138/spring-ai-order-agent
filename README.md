# spring-ai-order-agent

用 Spring AI 的 Function Calling，让大模型调用 Java 方法查订单。

> **这个仓库是学习 Function Calling 用的最小 demo（内存假数据）。**
> 最终形态已演进到真实业务项目：**若依外卖管理系统的自然语言点单 Agent**，`@Tool` 查真实 MySQL，见
> [RuoYi-Vue3 仓库的 ruoyi-merchant 分支](https://github.com/Licories-12138/RuoYi-Vue3/tree/ruoyi-merchant)。
> 建议两个仓库对照看：本仓库讲「机制怎么跑通」，那个分支讲「怎么接进真实项目」。

## 用了什么

- JDK 21，Spring Boot 4.1.1
- Spring AI 2.0.1（`spring-ai-starter-model-openai`）
- 阿里云百炼 `qwen3.7-flash`

百炼提供 OpenAI 兼容接口，所以不用引阿里 SDK，改 `base-url` 和 `api-key` 就能跑。

## 解决什么问题

大模型只能生成文本，执行不了代码，也拿不到业务数据。直接问它"13800138000 花了多少钱"，它会编一个数字出来。

Function Calling 把这件事拆成两段：模型负责判断该调哪个方法、参数是什么；Spring AI 负责真正执行，再把结果交回模型组织成人话。模型全程不碰代码也不碰数据库。

方法上加 `@Tool` 注解，模型就能看见它。`@Tool` 和 `@ToolParam` 的 description 是给模型看的说明书，不是给开发者看的注释 —— 写得含糊，模型就不会调用或者传错参数。

## 实现了什么

三个工具方法，都在 `OrderTools.java`：

| 方法 | 作用 |
|---|---|
| `queryOrdersByPhone` | 手机号查订单列表 |
| `queryOrderDetail` | 订单号查详情 |
| `queryOrderTotalAmount` | 统计该手机号历史总金额 |

实测：

```
GET /chat?message=13800138000这个手机号一共花了多少钱

手机号 13800138000 共 2 笔订单，总金额 113.5 元
```

同一时刻控制台打印：

```
>>> 模型调用了我的 Java 方法，参数是：13800138000
```

这行日志说明数字是 Java 算出来的，不是模型编的。问"今天天气怎么样"不会触发任何工具。

## 怎么跑

```bash
export DASHSCOPE_KEY=sk-xxxxxxxx
./mvnw spring-boot:run
curl "http://localhost:8080/chat?message=帮我查13800138000的订单"
```

密钥走环境变量，配置文件里只有 `${DASHSCOPE_KEY}` 占位。

## 几个注意点

- 端点要用 `https://dashscope.aliyuncs.com/compatible-mode/v1`。百炼控制台给的 workspace 专属端点会报 403。
- 金额求和用 `BigDecimal`，不能用 `double`。
- `temperature` 设 0.2。工具调用要参数稳定，调到 1.5 时模型会编参数。
- Spring AI 2.x 没有 `.options` 配置层，直接写 `spring.ai.openai.chat.model` 和 `.temperature`。
- 结构化输出用 `entity(Dto.class)` 就够，它内部会把 DTO 反射成 JSON Schema 塞进 prompt，不要额外配 `useProviderStructuredOutput()`：`qwen3.7-flash` 不在百炼严格 JSON Schema 支持名单里（只有 qwen3.7-Plus / Max、qwen3.8-Max），开了会返回 200 但抽取字段全部丢失 —— 服务端降级后格式指令既不生效也不回落到 prompt。
- DTO 必须有 getter/setter（或 Lombok `@Data`），否则 Jackson 写不进也读不出，接口返回 `{}`。字段用包装类型（`Boolean` 而非 `boolean`），模型给 null 才知道是"用户没提"。

## 演进

这里用内存假数据把 Function Calling 机制跑通，是学习用的。真实业务形态（`@Tool` 查 MySQL、自然语言点单）在
[RuoYi-Vue3 仓库的 ruoyi-merchant 分支](https://github.com/Licories-12138/RuoYi-Vue3/tree/ruoyi-merchant)。
