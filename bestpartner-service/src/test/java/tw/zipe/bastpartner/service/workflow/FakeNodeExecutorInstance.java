package tw.zipe.bastpartner.service.workflow;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * WorkflowEngineTest 專用的最小 CDI {@link Instance} 假實作。
 *
 * 以 Java 撰寫的原因：jakarta.enterprise.inject.Instance 對同名多載 vararg 泛型方法
 * （select 三個多載），Kotlin 編譯器目前無法正確辨識覆寫對應關係（"overrides nothing"），
 * 屬已知 Kotlin/Java 互通限制；改以 Java 撰寫可正常覆寫，供 Kotlin 測試呼叫。
 * 專案未使用 mockito/mockk，其餘 repository 皆以 Kotlin 手寫 fake 子類別替代。
 *
 * @author Gary
 * @created 2026/7/10
 */
public class FakeNodeExecutorInstance implements Instance<NodeExecutor> {

    private final List<NodeExecutor> executors;

    public FakeNodeExecutorInstance(List<NodeExecutor> executors) {
        this.executors = new ArrayList<>(executors);
    }

    @Override
    public Iterator<NodeExecutor> iterator() {
        return executors.iterator();
    }

    @Override
    public NodeExecutor get() {
        return executors.isEmpty() ? null : executors.get(0);
    }

    @Override
    public Instance<NodeExecutor> select(Annotation... qualifiers) {
        return this;
    }

    @Override
    public <U extends NodeExecutor> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException("not used in test");
    }

    @Override
    public <U extends NodeExecutor> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException("not used in test");
    }

    @Override
    public boolean isUnsatisfied() {
        return executors.isEmpty();
    }

    @Override
    public boolean isAmbiguous() {
        return false;
    }

    @Override
    public void destroy(NodeExecutor instance) {
        // no-op
    }

    @Override
    public Handle<NodeExecutor> getHandle() {
        throw new UnsupportedOperationException("not used in test");
    }

    @Override
    public Iterable<? extends Handle<NodeExecutor>> handles() {
        throw new UnsupportedOperationException("not used in test");
    }
}
