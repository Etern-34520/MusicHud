package indi.etern.musichud.utils;

import indi.etern.musichud.MusicHud;
import indi.etern.musichud.interfaces.*;
import net.fabricmc.api.EnvType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class RegistrationManager {
    private static final String[] CLIENT_REGISTRIES = new String[]{
            "indi.etern.musichud.client.config.Keybinds",
            "indi.etern.musichud.client.services.LoginService$RegisterImpl",
            "indi.etern.musichud.client.services.MusicService$RegisterImpl"
    };

    private static final String[] SERVER_REGISTRIES = new String[]{
            "indi.etern.musichud.server.api.LoginApiService$Register",
            "indi.etern.musichud.server.api.MusicPlayerServerService$Register"
    };

    private static final String[] COMMON_REGISTRIES = new String[]{
            "indi.etern.musichud.network.requestResponseCycle.GetPlaylistDetailRequest$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.GetPlaylistDetailResponse$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.GetUserPlaylistRequest$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.GetUserPlaylistResponse$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.SearchRequest$Register",
            "indi.etern.musichud.network.requestResponseCycle.SearchResponse$Register",
            "indi.etern.musichud.network.requestResponseCycle.StartQRLoginRequest$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.StartQRLoginResponse$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.ConnectRequest$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.ConnectResponse$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.CancelQRLoginRequest$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.AnonymousLoginRequest$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.CookieLoginRequest$RegisterImpl",
            "indi.etern.musichud.network.pushMessages.s2c.RefreshMusicQueueMessage$Register",
            "indi.etern.musichud.network.pushMessages.s2c.SwitchMusicMessage$RegisterImpl",
            "indi.etern.musichud.network.pushMessages.s2c.LoginResultMessage$RegisterImpl",
            "indi.etern.musichud.network.pushMessages.s2c.SyncCurrentPlayingMessage$RegisterImpl",
            "indi.etern.musichud.network.pushMessages.c2s.RemovePlaylistFromIdlePlaySourceMessage$RegisterImpl",
            "indi.etern.musichud.network.pushMessages.c2s.ClientPushMusicToQueueMessage$RegisterImpl",
            "indi.etern.musichud.network.pushMessages.c2s.ClientRemoveMusicFromQueueMessage$RegisterImpl",
            "indi.etern.musichud.network.pushMessages.c2s.LogoutMessage$Register"
    };

    // 列出需要强制加载的类（之前由 ClassGraph 通过 @ForceLoad 找到）
    private static final String[] FORCELOAD_CLASSES = new String[]{
            "indi.etern.musichud.server.api.MusicPlayerServerService$Register",
            "indi.etern.musichud.server.api.LoginApiService$Register",
            "indi.etern.musichud.network.requestResponseCycle.ConnectRequest",
            "indi.etern.musichud.network.requestResponseCycle.ConnectRequest$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.ConnectResponse$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.GetPlaylistDetailRequest$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.GetPlaylistDetailResponse$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.GetUserPlaylistRequest$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.GetUserPlaylistResponse$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.SearchRequest$Register",
            "indi.etern.musichud.network.requestResponseCycle.SearchResponse$Register",
            "indi.etern.musichud.network.requestResponseCycle.StartQRLoginRequest$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.StartQRLoginResponse$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.CancelQRLoginRequest$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.AnonymousLoginRequest$RegisterImpl",
            "indi.etern.musichud.network.requestResponseCycle.CookieLoginRequest$RegisterImpl"
    };

    private static final Set<Class<?>> registeredSet = new HashSet<>();
    private static final Set<Class<?>> forceLoadedSet = new HashSet<>();

    public static void performAutoRegistration(EnvType envType) {
        MusicHud.LOGGER.info("Starting explicit auto-registration (call-based) in environment: {}", envType);

        // 先处理 @ForceLoad 的显式类
        processForceLoad(envType);

        // 根据环境注册特定接口
        if (envType == EnvType.CLIENT) {
            registerClassesFromList(CLIENT_REGISTRIES, "client");
        } else {
            registerClassesFromList(SERVER_REGISTRIES, "server");
        }
        registerClassesFromList(COMMON_REGISTRIES, "common");
    }

    private static void processForceLoad(EnvType envType) {
        MusicHud.LOGGER.info("Processing {} explicit @ForceLoad classes", FORCELOAD_CLASSES.length);
        for (String className : FORCELOAD_CLASSES) {
            try {
                Class<?> clazz = Class.forName(className);
                if (forceLoadedSet.contains(clazz)) continue;

                // 检查注解的环境限制
                ForceLoad forceLoad = clazz.getAnnotation(ForceLoad.class);
                if (forceLoad != null) {
                    boolean matched = false;
                    for (EnvType et : forceLoad.value()) {
                        if (et == envType) {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        continue;
                    }
                }

                // 调用所有被 @OnClassLoaded 注解的方法（必须是 static 且无参数）
                Method[] methods = clazz.getDeclaredMethods();
                for (Method m : methods) {
                    if (m.isAnnotationPresent(OnClassLoaded.class)) {
                        if (!Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 0) {
                            throw new RuntimeException("@OnClassLoaded method must be static and have no parameters: " + m);
                        }
                        m.setAccessible(true);
                        try {
                            m.invoke(null);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                        MusicHud.LOGGER.debug("Invoked @OnClassLoaded method {} on {}", m.getName(), clazz.getCanonicalName());
                    }
                }

                forceLoadedSet.add(clazz);
                MusicHud.LOGGER.debug("Successfully force-loaded (call-based): {}", clazz.getCanonicalName());
            } catch (Throwable e) {
                MusicHud.LOGGER.error("Failed to force-load: {}", className, e);
            }
        }
    }

    private static void registerClassesFromList(String[] classNames, String typeName) {
        MusicHud.LOGGER.info("Registering {} {} registries", classNames.length, typeName);
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                if (registeredSet.contains(clazz)) continue;
                if (Register.class.isAssignableFrom(clazz)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Register> regClass = (Class<? extends Register>) clazz;
                    if (!regClass.isInterface()) {
                        Register instance = regClass.getDeclaredConstructor().newInstance();
                        instance.register();
                        registeredSet.add(clazz);
                        MusicHud.LOGGER.debug("Successfully registered (call-based): {}", clazz.getCanonicalName());
                    }
                } else {
                    MusicHud.LOGGER.warn("Class {} does not implement Register, skipping", className);
                }
            } catch (Throwable e) {
                MusicHud.LOGGER.error("Failed to register: {}", className, e);
            }
        }
    }
}

