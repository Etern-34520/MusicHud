package indi.etern.musichud.client.ui;

import icyllis.modernui.R;
import icyllis.modernui.core.Context;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;

import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class Theme {
    public static final int BASE_BACKGROUND_COLOR = 0xB0000000;

    public static final int PRIMARY_COLOR = 0xFFE0BFB7;

    public static final int EMPHASIZE_TEXT_COLOR = 0xFFFFFFFF;
    public static final int NORMAL_TEXT_COLOR = 0xFFE0E0E0;
    public static final int SECONDARY_TEXT_COLOR = 0xFFA0A0A0;

    public static final int ERROR_TEXT_COLOR = 0xFFFF4F4F;

    public static final int GHOST_BUTTON_BACKGROUND = 0x00FFFFFF;
    public static final int GHOST_BUTTON_BACKGROUND_PRESSED = 0x04FFFFFF;
    public static final int GHOST_BUTTON_BACKGROUND_HOVERED = 0x05FFFFFF;
    public static final int GHOST_BUTTON_BACKGROUND_CHECKED = 0x06FFFFFF;

    public static final ColorStateList GHOST_BUTTON_STATES = new ColorStateList(
            new int[][]{
                    new int[]{R.attr.state_pressed},
                    new int[]{R.attr.state_checked},
                    new int[]{R.attr.state_hovered},
                    StateSet.WILD_CARD
            },
            new int[]{
                    Theme.GHOST_BUTTON_BACKGROUND_PRESSED,
                    Theme.GHOST_BUTTON_BACKGROUND_CHECKED,
                    Theme.GHOST_BUTTON_BACKGROUND_HOVERED,
                    Theme.GHOST_BUTTON_BACKGROUND
            }
    );

    public static final ColorStateList ITEM_RIPPLE_COLOR_STATES = new ColorStateList(
            new int[][]{
                    new int[]{R.attr.state_pressed},
                    new int[]{R.attr.state_focused},
                    new int[]{R.attr.state_hovered},
                    StateSet.WILD_CARD
            },
            new int[]{
                    0x0A000000,
                    0x0A000000,
                    0x00000000,
                    0x08000000,
            }
    );

    public static TextView getNotificationTextView(Context context, boolean enabled) {
        TextView textView = new TextView(context);
        textView.setTextSize(textView.dp(8f));
        int color = EMPHASIZE_TEXT_COLOR;
        textView.setTextColor(color);
        if (enabled) {
            textView.setText("需要安装了 Music Hud 的服务器支持");
        } else {
            textView.setText("Music Hud 已禁用");
        }
        textView.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        return textView;
    }
}
