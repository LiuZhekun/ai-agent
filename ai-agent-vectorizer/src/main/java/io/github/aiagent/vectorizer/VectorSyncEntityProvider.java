package io.github.aiagent.vectorizer;

import java.util.List;

/**
 * 提供需要参与向量同步的实体类型。
 */
public interface VectorSyncEntityProvider {

    List<Class<?>> entities();
}
