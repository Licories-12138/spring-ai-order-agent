package com.example.llmdemo.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 第一个工具集：订单查询
 * 关键认知：这个类里的普通 Java 方法，加上 @Tool 之后，
 * 大模型就能"看见"它，并自己决定要不要调、传什么参数。
 * 模型不执行任何代码 —— 它只返回"我想调 queryOrdersByPhone，参数是 13800138000"，
 * 真正执行的是 Spring AI，执行完把结果再喂回模型，模型才组织成自然语言回答你。
 */
@Component
public class OrderTools {

    // 今天先用内存假数据，把机制跑通；明天再换成真 MySQL
    private static final List<Map<String, Object>> ORDERS = List.of(
            Map.of("orderNo", "SK20261001001", "phone", "13800138000", "status", "已完成", "amount", 45.5),
            Map.of("orderNo", "SK20261001002", "phone", "13800138000", "status", "配送中", "amount", 68.0),
            Map.of("orderNo", "SK20261002001", "phone", "13900139000", "status", "待付款", "amount", 23.0)
    );

    @Tool(description = "根据手机号查询该用户的订单列表，返回订单号、订单状态和金额")
    // @Tool 的 description 是给大模型看的，不是给 Java 开发者看的。
    public String queryOrdersByPhone(
            @ToolParam(description = "用户手机号，11 位数字") String phone) { // 告诉大模型：这个参数叫 phone，含义是“用户手机号，11 位数字”。

        System.out.println(">>> 模型调用了我的 Java 方法，参数是：" + phone);

        /*
        遍历 ORDERS
        保留 phone 字段等于传入手机号的订单
        收集成新的 List
        var 是 Java 10+ 的局部变量类型推断，这里 list 实际类型是 List<Map<String, Object>>。
         */
        var list = ORDERS.stream()
                .filter(o -> phone.equals(o.get("phone")))
                .toList();

        if (list.isEmpty()) {
            return "手机号 " + phone + " 名下没有订单";
        }
        // 返回值要是"模型能读懂的字符串"，别直接扔对象
        return list.stream()
                .map(o -> "订单号 " + o.get("orderNo") + "，状态 " + o.get("status") + "，金额 " + o.get("amount") + " 元")
                .reduce((a, b) -> a + "；" + b)
                .orElse("");
    }

    @Tool(description = "根据订单号查询单个订单的详细信息")
    public String queryOrderDetail(
            @ToolParam(description = "订单号，例如 SK20261001001") String orderNo) {

        System.out.println(">>> 模型调用了我的 Java 方法，订单号是：" + orderNo);

        // 流式处理，找到第一个匹配的订单
        /*
        流程：
        遍历订单列表；
        找到订单号匹配的第一条；
        如果找到，拼接详细信息；
        如果没找到，返回“没有找到订单号为 xxx 的订单”
         */
        return ORDERS.stream()
                .filter(o -> orderNo.equals(o.get("orderNo")))
                .findFirst()
                .map(o -> "订单号 " + o.get("orderNo") + "，手机号 " + o.get("phone")
                        + "，状态 " + o.get("status") + "，金额 " + o.get("amount") + " 元")
                .orElse("没有找到订单号为 " + orderNo + " 的订单");
    }

    /**
     * 模型读到你的问题 → 自己决定调 queryOrderTotalAmount →
     * 自己从“13800138000这个手机号一共花了多少钱”里抽出参数 phone=13800138000 →
     * 你的 Java 方法执行 →
     * 结果回灌 →
     * 模型组织成人话
     */
    @Tool(description = "根据手机号统计某个手机号的历史订单总金额")
    public String queryOrderTotalAmount(
            @ToolParam(description = "用户手机号，11 位数字") String phone) {

        System.out.println(">>> 模型调用了我的 Java 方法，参数是：" + phone);

        var list = ORDERS.stream()
                .filter(o -> phone.equals(o.get("phone")))
                .toList();

        if (list.isEmpty()) {
            return "手机号 " + phone + " 名下没有订单";
        }

        // 订单总金额
        BigDecimal total = list.stream()
                .map(o -> BigDecimal.valueOf((Double) o.get("amount")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return "手机号 " + phone + " 共 " + list.size() + " 笔订单，总金额 " + total + " 元";
    }
}
