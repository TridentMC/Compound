package com.tridevmc.compound.ui.element;

final class TextEditing {
    private TextEditing() {
    }

    static boolean isPrintable(int codePoint) {
        return Character.isValidCodePoint(codePoint) && codePoint >= 32 && codePoint != 127
                && codePoint != 167 && (codePoint < 0xD800 || codePoint > 0xDFFF);
    }

    static String truncate(String text, int maxLength) {
        int end = Math.clamp(maxLength, 0, text.length());
        if (end > 0 && end < text.length()
                && Character.isHighSurrogate(text.charAt(end - 1))
                && Character.isLowSurrogate(text.charAt(end))) {
            end--;
        }
        return text.substring(0, end);
    }
}
