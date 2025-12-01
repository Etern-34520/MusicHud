package indi.etern.musichud.client.services;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.networking.NetworkManager;
import icyllis.modernui.mc.MuiModApi;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.Version;
import indi.etern.musichud.beans.login.LoginCookieInfo;
import indi.etern.musichud.beans.login.LoginType;
import indi.etern.musichud.beans.user.Profile;
import indi.etern.musichud.client.config.ClientConfigDefinition;
import indi.etern.musichud.client.config.ProfileConfigData;
import indi.etern.musichud.client.ui.components.AccountView;
import indi.etern.musichud.client.ui.components.QRLoginView;
import indi.etern.musichud.client.ui.pages.AccountBaseView;
import indi.etern.musichud.interfaces.ClientRegister;
import indi.etern.musichud.interfaces.ForceLoad;
import indi.etern.musichud.network.pushMessages.c2s.LogoutMessage;
import indi.etern.musichud.network.pushMessages.s2c.LoginResultMessage;
import indi.etern.musichud.network.requestResponseCycle.AnonymousLoginRequest;
import indi.etern.musichud.network.requestResponseCycle.ConnectRequest;
import indi.etern.musichud.network.requestResponseCycle.StartQRLoginResponse;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LoginService {
    private static volatile LoginService instance = null;
    @Setter
    private Consumer<StartQRLoginResponse> loginResponseHandler;
    @Getter
    private List<Runnable> loginCompleteListeners = new ArrayList<>();
    @Getter
    NetworkManager.NetworkReceiver<StartQRLoginResponse> qrLoginResponseReceiver = (qrLoginResponse, context) -> {
        if (loginResponseHandler != null)
            loginResponseHandler.accept(qrLoginResponse);
    };
    private static final Logger logger = MusicHud.getLogger(LoginService.class);
    @Getter
    NetworkManager.NetworkReceiver<LoginResultMessage> loginResultReceiver = (loginResult, context) -> {
        MusicHud.EXECUTOR.submit(() -> {
            LoginType type = loginResult.loginCookieInfo().type();
            if (type != LoginType.UNLOGGED && type != LoginType.ANONYMOUS && loginResult.success()) {
                loginResult.loginCookieInfo().setToClientCookie();
                Profile.setCurrent(loginResult.profile());
                loginCompleteListeners.forEach(Runnable::run);
            } else {
                logger.warn("Login failed");
            }
            AccountBaseView accountBaseView = AccountBaseView.getInstance();
            if (accountBaseView != null) {
                if (loginResult.success()) {
                    ProfileConfigData profileConfigData = ProfileConfigData.getInstance();
                    profileConfigData.setProfile(loginResult.profile());
                    profileConfigData.saveToConfig();
                    MuiModApi.postToUiThread(accountBaseView::refresh);
                } else {
                    MuiModApi.postToUiThread(() -> {
                        AccountView accountView = AccountView.getInstance();
                        if (accountView != null) {
                            accountView.refresh();
                        }
                        QRLoginView qrLoginView = QRLoginView.getInstance();
                        if (qrLoginView != null) {
                            qrLoginView.reset();
                            qrLoginView.errorText(loginResult.message());
                        }
                    });
                }
            }
        });
    };

    public static LoginService getInstance() {
        if (instance == null) {
            synchronized (LoginService.class) {
                if (instance == null) {
                    instance = new LoginService();
                }
            }
        }
        return instance;
    }

    public boolean isLogined() {
        return LoginCookieInfo.clientCurrentCookie().type() != LoginType.UNLOGGED &&
                LoginCookieInfo.clientCurrentCookie().type() != LoginType.ANONYMOUS &&
                MusicHud.isConnected();
    }

    public void logout() {
        NetworkManager.sendToServer(LogoutMessage.MESSAGE);
    }

    @ForceLoad
    public static final class RegisterImpl implements ClientRegister {
        @Override
        public void register() {
            ClientPlayerEvent.CLIENT_PLAYER_JOIN.register((player) -> {
                getInstance().sendConnectMessageToServer();
            });
            ClientPlayerEvent.CLIENT_PLAYER_QUIT.register((player) -> {
                getInstance().setDisconnected();
            });
        }
    }

    public void setDisconnected() {
        if (ClientConfigDefinition.enable.get()) {
            MusicHud.setConnected(false);
        }
    }

    public void sendConnectMessageToServer() {
        if (ClientConfigDefinition.enable.get()) {
            NetworkManager.sendToServer(new ConnectRequest(Version.current));
        }
    }

    public void loginToServer() {
        if (isLogined()) {
            logger.info("Previous cookie found");
            LoginCookieInfo.refreshIfNecessaryAndRegisterToServer();
        } else {
            logger.info("No previous cookie found, login as anonymous");
            NetworkManager.sendToServer(AnonymousLoginRequest.REQUEST);
        }
    }
}