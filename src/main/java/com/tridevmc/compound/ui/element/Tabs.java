/*
 * Copyright 2018 - 2026 TridentMC
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
import com.tridevmc.compound.ui.CompoundCursors;
import com.tridevmc.compound.ui.cursor.UICursor;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.state.State;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * A tabbed container that composes only the selected tab's body.
 * Retain the Tabs instance to preserve its selection; retain stateful body elements
 * separately if their state should survive switching tabs. Disabled tabs cannot be selected.
 */
public class Tabs extends Element implements IComposableElement {

    private static final int TAB_HEIGHT = 20;
    private static final int TAB_PADDING = 8;
    private static final int TAB_SPACING = 2;

    private static final IScreenSprite TAB_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/tab"));
    private static final IScreenSprite TAB_SELECTED_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/tab_selected"));
    private static final IScreenSprite TAB_HIGHLIGHTED_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/tab_highlighted"));
    private static final IScreenSprite TAB_SELECTED_HIGHLIGHTED_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/tab_selected_highlighted"));

    private final State<Integer> selectedIndex = State.of(0);
    private final State<Boolean> enabled = State.of(true);
    private final List<Tab> tabs = Lists.newArrayList();
    private Consumer<Integer> onTabChanged;

    /**
     * Creates an empty, enabled tab container.
     */
    public Tabs() {
    }

    /** {@inheritDoc} */
    @Override
    public void compose(ICompositionScope scope) {
        scope.e(new Column(), column -> {
            column.layout().fillMax().spacing(0);

            column.e(new Row(), tabRow -> {
                tabRow.layout().spacing(TAB_SPACING);

                for (int i = 0; i < this.tabs.size(); i++) {
                    final int tabIndex = i;
                    Tab tab = this.tabs.get(i);

                    tabRow.e(tab.header, button -> {
                        button.layout().fixedSize(Minecraft.getInstance().font.width(tab.label) + TAB_PADDING * 2, TAB_HEIGHT);
                        button.getElement().setEnabled(this.enabled.get() && tab.enabled);
                        button.fillSlot(Button.CONTENT_SLOT, content -> {
                            content.e(new Label(tab.label,
                                    () -> this.enabled.get() && tab.enabled ? 0xFFFFFF : 0xA0A0A0,
                                    () -> true), label -> label.layout().deferred(layout -> {
                                layout.bind(this.selectedIndex);
                                layout.layout().margin(0, this.selectedIndex.get() == tabIndex ? 0 : 4, 0, 0);
                            }));
                        });
                    });
                }
            });

            column.e(new TabContent(), content -> content.layout().fillMaxWidth().weight(1));
        });
    }

    private void selectTab(int index) {
        if (index < 0 || index >= this.tabs.size()) return;
        if (!this.tabs.get(index).enabled) return;

        int oldIndex = this.selectedIndex.get();
        if (oldIndex != index) {
            this.selectedIndex.set(index);

            if (this.onTabChanged != null) {
                this.onTabChanged.accept(index);
            }
        }
    }

    /**
     * Appends an enabled tab. Its body is composed only when selected.
     *
     * @param label the tab label.
     * @param content the callback that composes the tab body.
     */
    public void addTab(String label, Consumer<ICompositionScope> content) {
        this.tabs.add(new Tab(Component.literal(label), content, true));
        this.invalidate();
    }

    /**
     * Appends an enabled tab. Its body is composed only when selected.
     *
     * @param label the tab label.
     * @param content the callback that composes the tab body.
     */
    public void addTab(Component label, Consumer<ICompositionScope> content) {
        this.tabs.add(new Tab(label, content, true));
        this.invalidate();
    }

    /**
     * Appends a tab with the given enabled state.
     *
     * @param label the tab label.
     * @param content the callback that composes the selected body.
     * @param enabled whether the tab can be selected.
     */
    public void addTab(String label, Consumer<ICompositionScope> content, boolean enabled) {
        this.tabs.add(new Tab(Component.literal(label), content, enabled));
        this.invalidate();
    }

    /**
     * Removes a tab and keeps an enabled selection where possible, without a change callback.
     *
     * @param index the zero-based index; invalid indices are ignored.
     */
    public void removeTab(int index) {
        if (index >= 0 && index < this.tabs.size()) {
            this.tabs.remove(index);
            int selected = this.selectedIndex.get();
            if (index < selected) selected--;
            this.selectedIndex.set(Math.min(selected, this.tabs.size() - 1));
            this.ensureEnabledSelection();
            this.invalidate();
        }
    }

    /**
     * Selects an enabled tab and invokes the callback when the selection changes.
     *
     * @param index the zero-based index; absent or disabled tabs are ignored.
     */
    public void setSelectedTab(int index) {
        if (index < 0 || index >= this.tabs.size()) return;
        if (!this.tabs.get(index).enabled) return;
        if (this.selectedIndex.get() != index) {
            this.selectedIndex.set(index);
            if (this.onTabChanged != null) {
                this.onTabChanged.accept(index);
            }
        }
    }

    /**
     * Returns the stored selected index.
     *
     * @return the zero-based index, or -1 after selection repair finds no enabled tab.
     */
    public int getSelectedTab() {
        return this.selectedIndex.get();
    }

    /**
     * Changes a tab's availability, repairing selection without invoking the change callback.
     *
     * @param index the zero-based index; invalid indices are ignored.
     * @param enabled whether the tab can be selected.
     */
    public void setTabEnabled(int index, boolean enabled) {
        if (index >= 0 && index < this.tabs.size()) {
            var tab = this.tabs.get(index);
            tab.enabled = enabled;
            tab.header.setEnabled(this.enabled.get() && enabled);
            this.ensureEnabledSelection();
        }
    }

    /**
     * Replaces the callback invoked when {@link #setSelectedTab(int)} changes the selection.
     *
     * @param onTabChanged the callback receiving the new index, or null to remove it.
     */
    public void setOnTabChanged(Consumer<Integer> onTabChanged) {
        this.onTabChanged = onTabChanged;
    }

    /**
     * Returns the number of tabs.
     *
     * @return the tab count.
     */
    public int getTabCount() {
        return this.tabs.size();
    }

    /**
     * Returns whether user interaction is enabled.
     *
     * @return true when enabled.
     */
    public boolean isEnabled() {
        return this.enabled.get();
    }

    /**
     * Changes whether user interaction is enabled.
     *
     * @param enabled whether to accept user interaction.
     */
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        for (var tab : this.tabs) {
            tab.header.setEnabled(enabled && tab.enabled);
        }
    }

    private void invalidate() {
        this.ensureEnabledSelection();
        this.invalidateComposition();
    }

    private void ensureEnabledSelection() {
        int selected = this.selectedIndex.get();
        if (selected >= 0 && selected < this.tabs.size() && this.tabs.get(selected).enabled) return;
        for (int i = 0; i < this.tabs.size(); i++) {
            if (this.tabs.get(i).enabled) {
                this.selectedIndex.set(i);
                return;
            }
        }
        this.selectedIndex.set(-1);
    }

    /** {@inheritDoc} */
    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(constraints.maxWidth(), constraints.maxHeight());
    }

    /** {@inheritDoc} */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }

    /** {@inheritDoc} */
    @Override
    public UICursor getCursor(int x, int y) {
        return this.enabled.get() ? CompoundCursors.HAND : null;
    }

    private final class TabContent extends Element implements IComposableElement {
        @Override
        public void compose(ICompositionScope scope) {
            scope.bindComposition(Tabs.this.selectedIndex);
            int selected = Tabs.this.selectedIndex.get();
            if (selected < 0 || selected >= Tabs.this.tabs.size()) return;
            var tab = Tabs.this.tabs.get(selected);
            scope.e(new Panel(), contentPanel -> {
                contentPanel.layout().fillMax();
                contentPanel.fillSlot(Panel.CONTENT_SLOT, content -> {
                    content.e(new Box(), box -> {
                        box.layout().padding(8).fillMax();
                        box.e(new Column(), tabColumn -> {
                            tabColumn.layout().fillMax().spacing(4);
                            tab.content.accept(tabColumn);
                        });
                    });
                });
            });
        }

        @Override
        public Size measure(Constraints constraints, LayoutProperties props, List<Size> children) {
            return children.isEmpty() ? Size.ZERO : children.getFirst();
        }

        @Override
        public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> children) {
            return children.isEmpty() ? List.of() : List.of(bounds);
        }
    }

    private final class Tab {
        private final Component label;
        private final Consumer<ICompositionScope> content;
        private final Button header = new Button();
        private boolean enabled;

        private Tab(Component label, Consumer<ICompositionScope> content, boolean enabled) {
            this.label = label;
            this.content = content;
            this.enabled = enabled;
            this.header.setSprites(
                    () -> this.isSelected() ? TAB_SELECTED_SPRITE : TAB_SPRITE,
                    () -> this.isSelected() ? TAB_SELECTED_HIGHLIGHTED_SPRITE : TAB_HIGHLIGHTED_SPRITE,
                    () -> this.isSelected() ? TAB_SELECTED_SPRITE : TAB_SPRITE);
            this.header.addPressListener((x, y) -> Tabs.this.selectTab(Tabs.this.tabs.indexOf(this)));
        }

        private boolean isSelected() {
            int selected = Tabs.this.selectedIndex.get();
            return selected >= 0 && selected < Tabs.this.tabs.size() && Tabs.this.tabs.get(selected) == this;
        }
    }
}
