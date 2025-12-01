package indi.etern.musichud.client.ui.pages;

import icyllis.modernui.core.Context;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.client.config.ClientConfigDefinition;
import indi.etern.musichud.client.services.LoginService;
import indi.etern.musichud.client.ui.Theme;
import indi.etern.musichud.client.ui.components.AccountView;
import indi.etern.musichud.client.ui.components.QRLoginView;
import lombok.Getter;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class AccountBaseView extends LinearLayout {
    @Getter
    static volatile AccountBaseView instance;

    public AccountBaseView(Context context) {
        super(context);
        try {
            instance = this;
            var baseParams = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
            setLayoutParams(baseParams);
            setOrientation(LinearLayout.HORIZONTAL);

            refresh();
        } catch (Exception e) {
            instance = null;
            throw e;
        }
    }

    public void refresh() {
        Context context = getContext();
        removeAllViews();
        boolean enabled = ClientConfigDefinition.enable.get();
        if (!MusicHud.isConnected() || !enabled) {
            setGravity(Gravity.CENTER);
            TextView textView = new TextView(context);
            textView.setTextSize(textView.dp(8f));
            int color = Theme.EMPHASIZE_TEXT_COLOR;
            textView.setTextColor(color);
            if (enabled) {
                textView.setText("需要安装了 Music Hud 的服务器支持");
            } else {
                textView.setText("Music Hud 已禁用");
            }
            textView.setLayoutParams(new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
            addView(textView);
        } else {
            setGravity(Gravity.CENTER_HORIZONTAL);
            if (LoginService.getInstance().isLogined()) {
                AccountView accountView = new AccountView(context);
                addView(accountView);
            } else {
                QRLoginView qrLoginView = new QRLoginView(context);
                LayoutParams loginParams = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                loginParams.setMargins(0, qrLoginView.dp(120), 0, 0);
                qrLoginView.setLayoutParams(loginParams);
                addView(qrLoginView);
            }
        }
    }
}