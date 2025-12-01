package indi.etern.musichud.utils;

import dev.architectury.platform.Platform;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.interfaces.*;
import io.github.classgraph.*;
import net.fabricmc.api.EnvType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

public class ClassGraphRegistrationManager {
    // 指定你要扫描的基础包名
    private static final String BASE_PACKAGE = "indi.etern.musichud";
    private static final HashSet<ClassInfo> registeredSet = new HashSet<>();

    private static String getClassName(Class<?> clazz) {
        String fullName = clazz.getName();
        int lastDotIndex = fullName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            return fullName.substring(lastDotIndex + 1);
        } else {
            return fullName;
        }
    }

    public static void performAutoRegistration(EnvType envType) {
        MusicHud.LOGGER.info("Starting auto-registration with ClassGraph in environment: {}", envType);

        try (ScanResult scanResult = new ClassGraph()
                .enableAllInfo()
                .acceptPackages(BASE_PACKAGE)
                .scan()) {

            forceLoadClasses(scanResult);
            // 根据环境注册特定接口
            if (envType == EnvType.CLIENT) {
                registerClassesImplementing(ClientRegister.class, scanResult, "client");
            } else {
                registerClassesImplementing(ServerRegister.class, scanResult, "server");
            }
            registerClassesImplementing(CommonRegister.class, scanResult, "common");
        } // ScanResult 会自动关闭
    }

    private static <T extends Register> void registerClassesImplementing(Class<T> interfaceType, ScanResult scanResult, String typeName) {
        // 获取所有实现了目标接口的类信息
        var allClasses = scanResult.getClassesImplementing(interfaceType.getName());

        MusicHud.LOGGER.info("Found {} {} registries", allClasses.size(), typeName);

        for (ClassInfo classInfo : allClasses) {
            if (!registeredSet.contains(classInfo)) {
                try {
                    // 加载类并实例化
                    @SuppressWarnings("unchecked")
                    Class<? extends T> clazz = (Class<? extends T>) Class.forName(classInfo.getName());
                    if (!clazz.isInterface()) {
                        T instance = clazz.getDeclaredConstructor().newInstance();
                        // 调用 register 方法
                        instance.register();
                        registeredSet.add(classInfo);
                        MusicHud.LOGGER.info("Successfully registered (ClassGraph): {}", getClassName(clazz));
                    }
                } catch (Exception e) {
                    MusicHud.LOGGER.error("Failed to register: {}", classInfo.getName(), e);
                }
            }
        }
    }

    private static void forceLoadClasses(ScanResult scanResult) {
        var allClasses = scanResult.getClassesWithAnnotation(ForceLoad.class.getName());

        MusicHud.LOGGER.info("Found {} classes with @{}", allClasses.size(), ForceLoad.class.getSimpleName());

        for (ClassInfo classInfo : allClasses) {
            AnnotationInfo annotationInfo = classInfo.getAnnotationInfo(ForceLoad.class);
            Object value = annotationInfo.getParameterValues().getValue("value");
            boolean b1 =
                    value instanceof EnvType envType &&
                            envType == Platform.getEnv();
            boolean b2 =
                    value instanceof Object[] envTypes &&
                            Arrays.stream(envTypes)
                                    .anyMatch(o ->
                                            o instanceof AnnotationEnumValue enumValue &&
                                                    Objects.equals(enumValue.getValueName(), Platform.getEnv().name())
                                    );
            if (!b1 && !b2) {
                continue;
            }
            try {
                // 加载类
                Class<?> clazz = classInfo.loadClass();
                classInfo.getMethodInfo().forEach(methodInfo -> {
                    boolean annotationPresent = methodInfo.hasAnnotation(OnClassLoaded.class);
                    if (annotationPresent) {
                        if (methodInfo.getParameterInfo().length == 0) {
                            try {
                                Method method = methodInfo.loadClassAndGetMethod();
                                method.invoke(null);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            throw new RuntimeException("@OnClassLoaded method must be static, have no parameters:" + methodInfo);
                        }
                    }
                });
                MusicHud.LOGGER.info("Successfully loaded (ClassGraph): {}", clazz.getCanonicalName());
            } catch (Exception e) {
                MusicHud.LOGGER.error("Failed to load: {}", classInfo.getName(), e);
            }
        }
    }
}