package com.zyb.backend.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Kryo Redis序列化器
 */
@Component
@Slf4j
public class KryoRedisSerializer implements RedisSerializer<Object> {

    /**
     * Kryo不是线程安全的，每个线程需要独立的实例
     */
    private final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();

        // 不要求预先注册类，允许动态注册
        kryo.setRegistrationRequired(false);

        // 设置实例化策略，支持无参构造函数的类
        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        // 启用引用跟踪，处理循环引用
        kryo.setReferences(true);

        log.debug("创建新的Kryo实例用于线程: {}", Thread.currentThread().getName());
        return kryo;
    });

    @Override
    public byte[] serialize(Object obj) throws SerializationException {
        if (obj == null) {
            return null;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Output output = new Output(baos)) {

            Kryo kryo = kryoThreadLocal.get();
            kryo.writeClassAndObject(output, obj);
            output.flush();

            byte[] result = baos.toByteArray();
            log.debug("Kryo序列化成功: {}",
                    obj.getClass().getSimpleName());
            return result;

        } catch (Exception e) {
            log.error("Kryo序列化失败: {}", e.getMessage());
            throw new SerializationException("Kryo序列化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             Input input = new Input(bais)) {

            Kryo kryo = kryoThreadLocal.get();
            Object result = kryo.readClassAndObject(input);

            log.debug("Kryo反序列化成功: {}",
                    result.getClass().getSimpleName());
            return result;

        } catch (Exception e) {
            log.error("Kryo反序列化失败: {}", e.getMessage());
            throw new SerializationException("Kryo反序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理ThreadLocal资源，防止内存泄漏
     */
    public void cleanup() {
        kryoThreadLocal.remove();
        log.debug("🗑️ 清理线程本地Kryo实例: {}", Thread.currentThread().getName());
    }
}

