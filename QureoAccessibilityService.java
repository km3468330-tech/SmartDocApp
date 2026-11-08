package com.egyptianhacker.qureo;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.SharedPreferences;

public class QureoAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        StringBuilder visible = new StringBuilder();
        collectText(root, visible, 0);
        String text = visible.toString().trim();
        if (!text.isEmpty()) {
            SharedPreferences p = getSharedPreferences("qureo_local", MODE_PRIVATE);
            p.edit().putString("last_visible_page_text",
                    text.length() > 12000 ? text.substring(0, 12000) : text).apply();
        }
    }

    private void collectText(AccessibilityNodeInfo node, StringBuilder out, int depth) {
        if (node == null || depth > 35 || out.length() > 12000) return;
        CharSequence t = node.getText();
        if (t != null && t.length() > 0) {
            if (out.length() > 0) out.append('\n');
            out.append(t);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectText(node.getChild(i), out, depth + 1);
        }
    }

    @Override public void onInterrupt() { }
}
