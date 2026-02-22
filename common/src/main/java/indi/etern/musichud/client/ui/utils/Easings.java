package indi.etern.musichud.client.ui.utils;

import icyllis.modernui.animation.TimeInterpolator;

public enum Easings implements TimeInterpolator{
    EASE_OUT_QUINT {
        @Override
        public float getInterpolation(float t) {
            return 1 - (float) Math.pow(1 - t, 5);
        }
    },
    EASE_IN_QUINT {
        @Override
        public float getInterpolation(float t) {
            return (float) Math.pow(t, 5);
        }
    },
    EASE_IN_OUT_QUINT {
        @Override
        public float getInterpolation(float t) {
            return t < 0.5 ? 16 * t * t * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 5) / 2;
        }
    }
}
