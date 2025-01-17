/*
 * This file is licensed under the MIT License, part of Roughly Enough Items.
 * Copyright (c) 2018, 2019, 2020, 2021, 2022 shedaniel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.shedaniel.rei.jeicompat.wrap;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.util.ClientEntryStacks;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.EntryType;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.CollectionUtils;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.stream.Collectors;

public class JEIDisplaySetup {
    public static class ShapelessData {
        public boolean shapeless;
        public Point pos;
    }
    
    public static class Result implements IRecipeSlotsView {
        public ShapelessData shapelessData = new ShapelessData();
        public List<JEIRecipeSlot> slots;
        public Map<RecipeIngredientRole, List<JEIRecipeSlot>> slotsRoled;
        
        @Override
        public List<IRecipeSlotView> getSlotViews() {
            return (List<IRecipeSlotView>) (List<? extends IRecipeSlotView>) slots;
        }
        
        @Override
        public List<IRecipeSlotView> getSlotViews(RecipeIngredientRole role) {
            return (List<IRecipeSlotView>) (List<? extends IRecipeSlotView>) slotsRoled.getOrDefault(role, Collections.emptyList());
        }
        
        @Override
        public Optional<IRecipeSlotView> findSlotByName(String slotName) {
            return (Optional<IRecipeSlotView>) (Optional<? extends IRecipeSlotView>) slots.stream().filter(slot -> Objects.equals(slot.getSlotName().orElse(null), slotName))
                    .findFirst();
        }
        
        public void setSlots(List<JEIRecipeSlot> slots) {
            this.slots = slots;
            this.slotsRoled = slots.stream().collect(Collectors.groupingBy(JEIRecipeSlot::getRole));
        }
    }
    
    public static <T> Result create(IRecipeCategory<T> category, JEIWrappedDisplay<T> display, IFocusGroup focuses) {
        Result result = new Result();
        JEIRecipeLayoutBuilder builder = new JEIRecipeLayoutBuilder(result.shapelessData);
        category.setRecipe(builder, display.getBackingRecipe(), focuses);
        result.setSlots(builder.slots);
        return result;
    }
    
    public static void addTo(List<Widget> widgets, Rectangle bounds, Result result) {
        for (JEIRecipeSlot slot : result.slots) {
            if (slot.isVisible()) {
                slot.slot.getBounds().translate(bounds.x + 4, bounds.y + 4);
                slot.slot.highlightEnabled(!slot.isEmpty());
                
                if (slot.background != null) {
                    widgets.add(slot.background);
                }
                
                RecipeIngredientRole role = slot.role;
                
                if (role == RecipeIngredientRole.INPUT) {
                    slot.slot.markInput();
                } else if (role == RecipeIngredientRole.OUTPUT) {
                    slot.slot.markOutput();
                }
                
                widgets.add(Widgets.withTranslate(slot.slot, 0, 0, 10));
                
                for (Map.Entry<EntryType<?>, IIngredientRenderer<?>> rendererEntry : slot.renderers.entrySet()) {
                    EntryType<?> type = rendererEntry.getKey();
                    
                    JEIEntryDefinition.Renderer<?> renderer = new JEIEntryDefinition.Renderer<>(rendererEntry.getValue());
                    for (EntryStack<?> entry : slot.slot.getEntries()) {
                        if (entry.getType() == type) {
                            ClientEntryStacks.setRenderer(entry, renderer);
                        }
                    }
                }
                
                if (slot.renderers.size() == 1) {
                    IIngredientRenderer<?> renderer = slot.renderers.values().iterator().next();
                    slot.slot.getBounds().setSize(renderer.getWidth() + 2, renderer.getHeight() + 2);
                }
                
                if (slot.capacityMb != null) {
                    for (EntryStack<?> entry : slot.slot.getEntries()) {
                        if (entry.getType() == VanillaEntryTypes.FLUID) {
                            ClientEntryStacks.setFluidRenderRatio(entry.cast(),
                                    entry.<dev.architectury.fluid.FluidStack>cast().getValue().getAmount() / (float) slot.capacityMb);
                        }
                    }
                }
                
                for (EntryStack<?> entry : slot.slot.getEntries()) {
                    if (entry.getType() == VanillaEntryTypes.FLUID) {
                        entry.setting(EntryStack.Settings.FLUID_AMOUNT_VISIBLE, false);
                    }
                }
                
                if (slot.overlay != null) {
                    widgets.add(slot.overlay);
                }
                
                for (EntryStack<?> entry : slot.slot.getEntries()) {
                    ClientEntryStacks.setTooltipProcessor(entry, (stack, tooltip) -> {
                        Object ingredient = null;
                        List<Component> components = CollectionUtils.filterAndMap(tooltip.entries(), Tooltip.Entry::isText, Tooltip.Entry::getAsText);
                        slot.tooltipCallback.onTooltip(slot, components);
                        tooltip.entries().removeIf(Tooltip.Entry::isText);
                        tooltip.addAllTexts(components);
                        return tooltip;
                    });
                }
            }
        }
    }
}
