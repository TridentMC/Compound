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

package com.tridevmc.compound.ui.element;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.tridevmc.compound.ui.CompoundCursors;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * A tabbed container component for organizing content into multiple tabs.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new Tabs(), tabs -> {
 *     tabs.getElement().addTab("General", tabScope -> {
 *         tabScope.e(new Label(Component.literal("General settings")));
 *     });
 *     tabs.getElement().addTab("Advanced", tabScope -> {
 *         tabScope.e(new Label(Component.literal("Advanced settings")));
 *     });
 *     tabs.getElement().setSelectedTab(0);
 * });
 * </pre>
 */
public class Tabs extends BaseElement implements IComposableElement {

    private static final int TAB_HEIGHT = 20;
    private static final int TAB_PADDING = 8;
    private static final int TAB_SPACING = 2;

    private static final IScreenSprite TAB_SPRITE = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/button"));
    private static final IScreenSprite TAB_SELECTED_SPRITE = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/button_highlighted"));
    private static final IScreenSprite TAB_DISABLED_SPRITE = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/button_disabled"));

    private final State<Integer> selectedIndex = new StateImpl<>(0);
    private final State<Boolean> enabled = new StateImpl<>(true);
    private final List<Tab> tabs = Lists.newArrayList();
    private Consumer<Integer> onTabChanged;

    public Tabs() {
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.enabled);
        scope.bind(this.selectedIndex);

        scope.e(new Column(), column -> {
            column.layout().fillMax().spacing(0);

            column.e(new Row(), tabRow -> {
                tabRow.layout().spacing(TAB_SPACING);

                for (int i = 0; i < this.tabs.size(); i++) {
                    final int tabIndex = i;
                    Tab tab = this.tabs.get(i);

                    tabRow.e(new Button(), button -> {
                        button.layout().fixedHeight(TAB_HEIGHT);
                        button.getElement().setEnabled(this.enabled.get() && tab.enabled);

                        button.fillSlot(Button.CONTENT_SLOT, content -> {
                            content.e(new Label(tab.label, 0xFFFFFF, false));
                        });

                        button.getElement().addPressListener((x, y) -> {
                            if (tab.enabled) {
                                this.selectTab(tabIndex);
                            }
                        });
                    });
                }
            });

            if (this.selectedIndex.get() >= 0 && this.selectedIndex.get() < this.tabs.size()) {
                Tab selectedTab = this.tabs.get(this.selectedIndex.get());
                column.e(new Panel(), contentPanel -> {
                    contentPanel.layout().fillMax();
                    contentPanel.fillSlot(Panel.CONTENT_SLOT, content -> {
                        content.e(new Box(), box -> {
                            box.layout().padding(8).fillMax();
                            selectedTab.content.accept(box);
                        });
                    });
                });
            }
        });
    }

    private void selectTab(int index) {
        if (index < 0 || index >= this.tabs.size()) return;
        if (!this.tabs.get(index).enabled) return;

        int oldIndex = this.selectedIndex.get();
        if (oldIndex != index) {
            this.selectedIndex.set(index);

            SoundManager soundManager = Minecraft.getInstance().getSoundManager();
            soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

            if (this.onTabChanged != null) {
                this.onTabChanged.accept(index);
            }
        }
    }

    public void addTab(String label, Consumer<ICompositionScope> content) {
        this.tabs.add(new Tab(Component.literal(label), content, true));
    }

    public void addTab(Component label, Consumer<ICompositionScope> content) {
        this.tabs.add(new Tab(label, content, true));
    }

    public void addTab(String label, Consumer<ICompositionScope> content, boolean enabled) {
        this.tabs.add(new Tab(Component.literal(label), content, enabled));
    }

    public void removeTab(int index) {
        if (index >= 0 && index < this.tabs.size()) {
            this.tabs.remove(index);
            if (this.selectedIndex.get() >= this.tabs.size()) {
                this.selectedIndex.set(Math.max(0, this.tabs.size() - 1));
            }
        }
    }

    public void setSelectedTab(int index) {
        this.selectTab(index);
    }

    public int getSelectedTab() {
        return this.selectedIndex.get();
    }

    public void setTabEnabled(int index, boolean enabled) {
        if (index >= 0 && index < this.tabs.size()) {
            this.tabs.get(index).enabled = enabled;
        }
    }

    public void setOnTabChanged(Consumer<Integer> onTabChanged) {
        this.onTabChanged = onTabChanged;
    }

    public int getTabCount() {
        return this.tabs.size();
    }

    public boolean isEnabled() {
        return this.enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(constraints.maxWidth(), constraints.maxHeight());
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }

    @Override
    public CursorType getCursor(int x, int y) {
        return this.enabled.get() ? CompoundCursors.HAND : null;
    }

    private static class Tab {
        Component label;
        Consumer<ICompositionScope> content;
        boolean enabled;

        Tab(Component label, Consumer<ICompositionScope> content, boolean enabled) {
            this.label = label;
            this.content = content;
            this.enabled = enabled;
        }
    }
}
