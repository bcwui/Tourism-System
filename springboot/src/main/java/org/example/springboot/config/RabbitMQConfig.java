package org.example.springboot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ==================== Exchange ====================
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String SECKILL_EXCHANGE = "seckill.exchange";

    // ==================== Queue ====================
    public static final String ORDER_QUEUE = "order.queue";
    public static final String EMAIL_QUEUE = "email.queue";
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";

    // ==================== Routing Key ====================
    public static final String ORDER_ROUTING_KEY = "order";
    public static final String EMAIL_ROUTING_KEY = "email";
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order";

    // ==================== Exchange Beans ====================
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE);
    }

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE);
    }

    // ==================== Queue Beans ====================
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE).build();
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE).build();
    }

    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE).build();
    }

    // ==================== Binding Beans ====================
    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue()).to(orderExchange()).with(ORDER_ROUTING_KEY);
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(emailExchange()).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue()).to(seckillExchange()).with(SECKILL_ORDER_ROUTING_KEY);
    }

    // ==================== Message Converter ====================
    // 必须注入 Spring 的 ObjectMapper（含 JavaTimeModule），
    // 否则 Jackson2JsonMessageConverter 默认构造器不注册 jsr310，LocalDate/LocalDateTime 反序列化会失败
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // 确保 Listener 容器使用 JSON 转换器
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }

    // 确保 RabbitTemplate 使用 JSON 转换器
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
