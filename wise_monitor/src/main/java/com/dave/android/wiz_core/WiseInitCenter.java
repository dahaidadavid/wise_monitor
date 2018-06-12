package com.dave.android.wiz_core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import com.dave.android.wiz_core.services.concurrency.DependsOn;
import com.dave.android.wiz_core.services.concurrency.PriorityThreadPoolExecutor;
import com.dave.android.wiz_core.services.concurrency.UnmetDependencyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author rendawei
 * @since 2018/6/5
 */
public class WiseInitCenter {

    @SuppressLint("StaticFieldLeak")
    private static volatile WiseInitCenter INSTANCE;
    private static final Logger DEFAULT_LOGGER = new DefaultLogger();
    private final Context context;
    private final Map<Class<? extends Kit>, Kit> kits;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final InitializationCallback<WiseInitCenter> initializationCallback;
    private final InitializationCallback<?> kitInitializationCallback;
    private AtomicBoolean initialized;
    private final Logger logger;
    private final boolean debuggable;

    public static WiseInitCenter getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "Must Initialize WiseInitCenter before using INSTANCE()");
        } else {
            return INSTANCE;
        }
    }

    private WiseInitCenter(Context context, Map<Class<? extends Kit>, Kit> kits, PriorityThreadPoolExecutor threadPoolExecutor, Handler mainHandler, Logger logger,
            boolean debuggable, InitializationCallback callback) {
        this.context = context;
        this.kits = kits;
        this.executorService = threadPoolExecutor;
        this.mainHandler = mainHandler;
        this.logger = logger;
        this.debuggable = debuggable;
        this.initializationCallback = callback;
        this.initialized = new AtomicBoolean(false);
        this.kitInitializationCallback = this.createKitInitializationCallback(kits.size());
    }

    public static WiseInitCenter with(Context context, Kit... kits) {
        if (INSTANCE == null) {
            synchronized (WiseInitCenter.class) {
                if (INSTANCE == null) {
                    setWiseInitCenter((new WiseInitCenter.Builder(context)).kits(kits).build());
                }
            }
        }

        return INSTANCE;
    }

    public static WiseInitCenter with(WiseInitCenter wiseInitCenter) {
        if (INSTANCE == null) {
            synchronized (WiseInitCenter.class) {
                if (INSTANCE == null) {
                    setWiseInitCenter(wiseInitCenter);
                }
            }
        }

        return INSTANCE;
    }

    private static void setWiseInitCenter(WiseInitCenter wiseInitCenter) {
        INSTANCE = wiseInitCenter;
    }

    /**
     * 初始化
     */
    public void initializeKits(Context context) {
        if(isInitialized()){
            return;
        }
        //获取用户传递的需要初始化的插件
        List<Kit> kits = new ArrayList<>(getKits());
        Collections.sort(kits);

        //为所有需要初始化的插件设置必要的参数和回调监听
        for (Kit kit : kits) {
            kit.initializationTask.setCurrentRunningThread(kit.validateAnnotationThread());
            kit.injectParameters(context, this, kitInitializationCallback);
        }

        //为所有需要初始化的插件设置关联依赖并启动初始化
        for (Kit kit : kits) {
            addAnnotatedDependencies(this.kits, kit);
            kit.initialize();
        }
    }

    /**
     * 寻找当前任务的依赖项，并将它的初始化任务添加进来，组成一个关系链
     *
     * @param kits 所有的初始化插件
     * @param currentToBeRunKit 当前需要执行的插件
     */
    private void addAnnotatedDependencies(@NonNull Map<Class<? extends Kit>, Kit> kits, @NonNull Kit currentToBeRunKit) {
        DependsOn dependsOn = currentToBeRunKit.dependsOnAnnotation;
        if (dependsOn != null) {
            Class<?>[] dependencies = dependsOn.value();
            for (Class<?> dependency : dependencies) {
                if (dependency.isInterface()) {
                    for (Kit kit : kits.values()) {
                        if (dependency.isAssignableFrom(kit.getClass())) {
                            currentToBeRunKit.initializationTask.addDependency(kit.initializationTask);
                        }
                    }
                } else {
                    Kit kit = kits.get(dependency);
                    if (kit == null) {
                        throw new UnmetDependencyException("Referenced Kit was null, does the kit exist?");
                    }
                    currentToBeRunKit.initializationTask.addDependency(kit.initializationTask);
                }
            }
        }
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public Handler getMainHandler() {
        return this.mainHandler;
    }

    public Collection<Kit> getKits() {
        return kits.values();
    }

    public static <T extends Kit> T getKit(Class<T> cls) {
        return (T) getInstance().kits.get(cls);
    }

    public static Logger getLogger() {
        return INSTANCE == null ? DEFAULT_LOGGER : INSTANCE.logger;
    }

    public static boolean isDebuggable() {
        return INSTANCE != null && INSTANCE.debuggable;
    }

    public static boolean isInitialized() {
        return INSTANCE != null && INSTANCE.initialized.get();
    }

    /**
     * 将列表中的kit组装成Map
     *
     * @param kits kit集合
     * @return <code>Map<Class<? extends Kit>, Kit></>
     */
    private static Map<Class<? extends Kit>, Kit> getKitMap(Collection<? extends Kit> kits) {
        HashMap<Class<? extends Kit>, Kit> map = new HashMap<>(kits.size());
        addToKitMap(map, kits);
        return map;
    }

    private static void addToKitMap(Map<Class<? extends Kit>, Kit> map, Collection<? extends Kit> kits) {
        for (Kit kit : kits) {
            map.put(kit.getClass(), kit);
            if (kit instanceof KitGroup) {
                addToKitMap(map, ((KitGroup) kit).getKits());
            }
        }
    }

    /**
     * 创建插件初始化监听类，会在一个插件初始化失败或则全部成功地时候进行回调
     * @param size 初始化插件的数量
     * @return 插件初始化监听类
     */
    private InitializationCallback<?> createKitInitializationCallback(final int size) {
        return new InitializationCallback() {
            final CountDownLatch kitInitializedLatch = new CountDownLatch(size);

            public void success(Object o) {
                kitInitializedLatch.countDown();
                if (kitInitializedLatch.getCount() == 0L) {
                    initialized.set(true);
                    initializationCallback.success(WiseInitCenter.this);
                }
            }

            public void failure(Exception exception) {
                initializationCallback.failure(exception);
            }
        };
    }

    public static class Builder {

        private final Context context;
        private Kit[] kits;
        private Handler handler;
        private Logger logger;
        private boolean debuggable;
        private String appIdentifier;
        private String appInstallIdentifier;
        private PriorityThreadPoolExecutor threadPoolExecutor;
        private InitializationCallback<WiseInitCenter> initializationCallback;

        public Builder(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context must not be null.");
            } else {
                this.context = context;
            }
        }

        public WiseInitCenter.Builder kits(Kit... kits) {
            if (this.kits != null) {
                throw new IllegalStateException("Kits already set.");
            } else {
                this.kits = kits;
                return this;
            }
        }

        public WiseInitCenter.Builder threadPoolExecutor(PriorityThreadPoolExecutor threadPoolExecutor) {
            if (threadPoolExecutor == null) {
                throw new IllegalArgumentException("PriorityThreadPoolExecutor must not be null.");
            } else if (this.threadPoolExecutor != null) {
                throw new IllegalStateException("PriorityThreadPoolExecutor already set.");
            } else {
                this.threadPoolExecutor = threadPoolExecutor;
                return this;
            }
        }

        public WiseInitCenter.Builder logger(Logger logger) {
            if (logger == null) {
                throw new IllegalArgumentException("Logger must not be null.");
            } else if (this.logger != null) {
                throw new IllegalStateException("Logger already set.");
            } else {
                this.logger = logger;
                return this;
            }
        }

        public WiseInitCenter.Builder appIdentifier(String appIdentifier) {
            if (appIdentifier == null) {
                throw new IllegalArgumentException("appIdentifier must not be null.");
            } else if (this.appIdentifier != null) {
                throw new IllegalStateException("appIdentifier already set.");
            } else {
                this.appIdentifier = appIdentifier;
                return this;
            }
        }

        public WiseInitCenter.Builder appInstallIdentifier(String appInstallIdentifier) {
            if (appInstallIdentifier == null) {
                throw new IllegalArgumentException("appInstallIdentifier must not be null.");
            } else if (this.appInstallIdentifier != null) {
                throw new IllegalStateException("appInstallIdentifier already set.");
            } else {
                this.appInstallIdentifier = appInstallIdentifier;
                return this;
            }
        }

        public WiseInitCenter.Builder debuggable(boolean enabled) {
            this.debuggable = enabled;
            return this;
        }

        public WiseInitCenter.Builder initializationCallback(InitializationCallback<WiseInitCenter> initializationCallback) {
            if (initializationCallback == null) {
                throw new IllegalArgumentException("initializationCallback must not be null.");
            } else if (this.initializationCallback != null) {
                throw new IllegalStateException("initializationCallback already set.");
            } else {
                this.initializationCallback = initializationCallback;
                return this;
            }
        }

        public WiseInitCenter build() {

            if (this.threadPoolExecutor == null) {
                this.threadPoolExecutor = PriorityThreadPoolExecutor.create();
            }

            if (this.handler == null) {
                this.handler = new Handler(Looper.getMainLooper());
            }

            if (this.logger == null) {
                if (this.debuggable) {
                    this.logger = new DefaultLogger(3);
                } else {
                    this.logger = new DefaultLogger();
                }
            }

            if (this.appIdentifier == null) {
                this.appIdentifier = this.context.getPackageName();
            }

            if (this.initializationCallback == null) {
                this.initializationCallback = InitializationCallback.EMPTY;
            }

            Map kitMap;
            if (this.kits == null) {
                kitMap = new HashMap();
            } else {
                kitMap = WiseInitCenter.getKitMap(Arrays.asList(this.kits));
            }

            Context appContext = this.context.getApplicationContext();
            return new WiseInitCenter(appContext, kitMap, this.threadPoolExecutor, this.handler,
                    this.logger, this.debuggable, this.initializationCallback);
        }
    }
}
