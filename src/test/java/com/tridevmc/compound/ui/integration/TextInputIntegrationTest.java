/*
 * Copyright 2018 - 2024 TridentMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridevmc.compound.ui.integration;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.TextInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MinecraftMockExtension.class)
class TextInputIntegrationTest {
    @Test
    void publicEditingMethodsPreserveSelectionValidationAndLengthLimits() {
        var input = new TextInput("Hello World");
        input.moveCursorTo(5, false);
        input.moveCursorTo(11, true);
        assertEquals(" World", input.getSelectedText());
        input.insertText(" There");
        assertEquals("Hello There", input.getValue());
        input.deleteText(-1, false);
        assertEquals("Hello Ther", input.getValue());
        input.moveCursorTo(5, false);
        input.deleteText(1, false);
        assertEquals("HelloTher", input.getValue());

        input.setMaxLength(3);
        assertEquals("Hel", input.getValue());
        input.setMaxLength(10);
        input.setFilter(value -> value.matches("[0-9]*"));
        input.setValue("abc123");
        assertEquals("Hel", input.getValue());
        input.setValue("12345");
        assertEquals("12345", input.getValue());
        assertEquals(5, input.getCursorPosState().get());
    }
}
