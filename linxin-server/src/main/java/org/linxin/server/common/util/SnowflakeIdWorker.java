package org.linxin.server.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 雪花算法 ID 生成器 (Snowflake) 格式：1位符号(0) | 41位时间戳 | 10位机器ID | 12位序列号
 */
@Component
public class SnowflakeIdWorker {

    // 起始时间戳 (2024-01-01)
    private final long twepoch = 1704067200000L;

    // 机器标识占用的位数
    private final long workerIdBits = 10L;

    // 序列在 id 中占用的位数
    private final long sequenceBits = 12L;

    // 机器 ID 向左移 12 位
    private final long workerIdShift = sequenceBits;

    // 时间截向左移 22 位 (10+12)
    private final long timestampLeftShift = sequenceBits + workerIdBits;

    // 生成序列的掩码，这里为 4095
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    // 工作机器 ID (0~1023)
    @Value("${snowflake.worker-id:1}")
    private long workerId;

    // 毫秒内序列 (0~4095)
    private long sequence = 0L;

    // 上次生成 ID 的时间截
    private long lastTimestamp = -1L;

    public synchronized long nextId() {
        long timestamp = timeGen();

        // 如果当前时间小于上一次 ID 生成的时间戳，说明系统时钟回退过，抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }

        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // 毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 时间戳改变，毫秒内序列重置
            sequence = 0L;
        }

        // 上次生成 ID 的时间截
        lastTimestamp = timestamp;

        // 移位并通过或运算拼到一起组成 64 位的 ID
        return ((timestamp - twepoch) << timestampLeftShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }
}
