package io.github.aiagent.demo.config;

import io.github.aiagent.demo.entity.User;
import io.github.aiagent.vectorizer.VectorSyncEntityProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Demo 向量同步实体注册。
 */
@Component
public class DemoVectorSyncEntityProvider implements VectorSyncEntityProvider {

    @Override
    public List<Class<?>> entities() {
        return List.of(User.class);
    }
}
