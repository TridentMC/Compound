package com.tridevmc.compound.ui.element;

final class TextEditing {
    private TextEditing() {
    }

    static boolean isPrintable(int codePoint) {
        return Character.isValidCodePoint(codePoint) && codePoint >= 32 && codePoint != 127
                && codePoint != 167 && (codePoint < 0xD800 || codePoint > 0xDFFF);
    }

}
