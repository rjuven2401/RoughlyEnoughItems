/*
 * This file is licensed under the MIT License, part of Roughly Enough Items.
 * Copyright (c) 2018, 2019, 2020 shedaniel
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

package me.shedaniel.rei.plugin;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import me.shedaniel.architectury.hooks.FluidStackHooks;
import me.shedaniel.architectury.platform.Platform;
import me.shedaniel.architectury.utils.NbtType;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.REIHelper;
import me.shedaniel.rei.api.favorites.FavoriteEntry;
import me.shedaniel.rei.api.favorites.FavoriteEntryType;
import me.shedaniel.rei.api.fluid.FluidSupportProvider;
import me.shedaniel.rei.api.ingredient.EntryIngredient;
import me.shedaniel.rei.api.ingredient.entry.comparison.ItemComparator;
import me.shedaniel.rei.api.ingredient.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.ingredient.util.EntryIngredients;
import me.shedaniel.rei.api.ingredient.util.EntryStacks;
import me.shedaniel.rei.api.plugins.BuiltinPlugin;
import me.shedaniel.rei.api.plugins.REIPlugin;
import me.shedaniel.rei.api.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.registry.screen.DisplayBoundsProvider;
import me.shedaniel.rei.api.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.subsets.SubsetsRegistry;
import me.shedaniel.rei.api.util.CollectionUtils;
import me.shedaniel.rei.plugin.autocrafting.DefaultRecipeBookHandler;
import me.shedaniel.rei.plugin.beacon.base.DefaultBeaconBaseCategory;
import me.shedaniel.rei.plugin.beacon.base.DefaultBeaconBaseDisplay;
import me.shedaniel.rei.plugin.beacon.payment.DefaultBeaconPaymentCategory;
import me.shedaniel.rei.plugin.beacon.payment.DefaultBeaconPaymentDisplay;
import me.shedaniel.rei.plugin.blasting.DefaultBlastingDisplay;
import me.shedaniel.rei.plugin.brewing.DefaultBrewingCategory;
import me.shedaniel.rei.plugin.brewing.DefaultBrewingDisplay;
import me.shedaniel.rei.plugin.brewing.RegisteredBrewingRecipe;
import me.shedaniel.rei.plugin.campfire.DefaultCampfireCategory;
import me.shedaniel.rei.plugin.campfire.DefaultCampfireDisplay;
import me.shedaniel.rei.plugin.composting.DefaultCompostingCategory;
import me.shedaniel.rei.plugin.composting.DefaultCompostingDisplay;
import me.shedaniel.rei.plugin.cooking.DefaultCookingCategory;
import me.shedaniel.rei.plugin.crafting.DefaultCraftingCategory;
import me.shedaniel.rei.plugin.crafting.DefaultCustomDisplay;
import me.shedaniel.rei.plugin.crafting.DefaultShapedDisplay;
import me.shedaniel.rei.plugin.crafting.DefaultShapelessDisplay;
import me.shedaniel.rei.plugin.favorites.GameModeFavoriteEntry;
import me.shedaniel.rei.plugin.favorites.WeatherFavoriteEntry;
import me.shedaniel.rei.plugin.fuel.DefaultFuelCategory;
import me.shedaniel.rei.plugin.fuel.DefaultFuelDisplay;
import me.shedaniel.rei.plugin.information.DefaultInformationCategory;
import me.shedaniel.rei.plugin.information.DefaultInformationDisplay;
import me.shedaniel.rei.plugin.pathing.DefaultPathingCategory;
import me.shedaniel.rei.plugin.pathing.DefaultPathingDisplay;
import me.shedaniel.rei.plugin.pathing.DummyShovelItem;
import me.shedaniel.rei.plugin.smelting.DefaultSmeltingDisplay;
import me.shedaniel.rei.plugin.smithing.DefaultSmithingCategory;
import me.shedaniel.rei.plugin.smithing.DefaultSmithingDisplay;
import me.shedaniel.rei.plugin.smoking.DefaultSmokingDisplay;
import me.shedaniel.rei.plugin.stonecutting.DefaultStoneCuttingCategory;
import me.shedaniel.rei.plugin.stonecutting.DefaultStoneCuttingDisplay;
import me.shedaniel.rei.plugin.stripping.DefaultStrippingCategory;
import me.shedaniel.rei.plugin.stripping.DefaultStrippingDisplay;
import me.shedaniel.rei.plugin.stripping.DummyAxeItem;
import me.shedaniel.rei.plugin.tilling.DefaultTillingCategory;
import me.shedaniel.rei.plugin.tilling.DefaultTillingDisplay;
import me.shedaniel.rei.plugin.tilling.DummyHoeItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagCollection;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static me.shedaniel.rei.impl.Internals.attachInstance;

@Environment(EnvType.CLIENT)
@ApiStatus.Internal
public class DefaultPlugin implements REIPlugin, BuiltinPlugin {
    private static final Logger LOGGER = LogManager.getFormatterLogger("REI/DefaultPlugin");
    public static final ResourceLocation CRAFTING = BuiltinPlugin.CRAFTING;
    public static final ResourceLocation SMELTING = BuiltinPlugin.SMELTING;
    public static final ResourceLocation SMOKING = BuiltinPlugin.SMOKING;
    public static final ResourceLocation BLASTING = BuiltinPlugin.BLASTING;
    public static final ResourceLocation CAMPFIRE = BuiltinPlugin.CAMPFIRE;
    public static final ResourceLocation STONE_CUTTING = BuiltinPlugin.STONE_CUTTING;
    public static final ResourceLocation STRIPPING = BuiltinPlugin.STRIPPING;
    public static final ResourceLocation BREWING = BuiltinPlugin.BREWING;
    public static final ResourceLocation COMPOSTING = BuiltinPlugin.COMPOSTING;
    public static final ResourceLocation FUEL = BuiltinPlugin.FUEL;
    public static final ResourceLocation SMITHING = BuiltinPlugin.SMITHING;
    public static final ResourceLocation BEACON = BuiltinPlugin.BEACON;
    public static final ResourceLocation BEACON_PAYMENT = BuiltinPlugin.BEACON_PAYMENT;
    public static final ResourceLocation TILLING = BuiltinPlugin.TILLING;
    public static final ResourceLocation PATHING = BuiltinPlugin.PATHING;
    public static final ResourceLocation INFO = BuiltinPlugin.INFO;
    private static final ResourceLocation DISPLAY_TEXTURE = new ResourceLocation("roughlyenoughitems", "textures/gui/display.png");
    private static final ResourceLocation DISPLAY_TEXTURE_DARK = new ResourceLocation("roughlyenoughitems", "textures/gui/display_dark.png");
    
    public static ResourceLocation getDisplayTexture() {
        return REIHelper.getInstance().getDefaultDisplayTexture();
    }
    
    public DefaultPlugin() {
        attachInstance(this, BuiltinPlugin.class);
    }
    
    public static void registerBrewingRecipe(RegisteredBrewingRecipe recipe) {
        DisplayRegistry.getInstance().registerDisplay(new DefaultBrewingDisplay(recipe.input, recipe.ingredient, recipe.output));
    }
    
    public static void registerInfoDisplay(DefaultInformationDisplay display) {
        DisplayRegistry.getInstance().registerDisplay(display);
    }
    
    @Override
    public void registerBrewingRecipe(Ingredient input, Ingredient ingredient, ItemStack output) {
        registerBrewingRecipe(new RegisteredBrewingRecipe(input, ingredient, output));
    }
    
    @Override
    public void registerInformation(EntryIngredient ingredient, Component name, UnaryOperator<List<Component>> textBuilder) {
        registerInfoDisplay(DefaultInformationDisplay.createFromEntries(ingredient, name).lines(textBuilder.apply(Lists.newArrayList())));
    }
    
    @Override
    public void registerEntries(EntryRegistry registry) {
        for (Item item : Registry.ITEM) {
            try {
                registry.registerEntries(EntryStacks.ofItemStacks(registry.appendStacksForItem(item)));
            } catch (Exception ignored) {
                registry.registerEntry(EntryStacks.of(item));
            }
        }
        for (Fluid fluid : Registry.FLUID) {
            FluidState state = fluid.defaultFluidState();
            if (!state.isEmpty() && state.isSource()) {
                registry.registerEntry(EntryStacks.of(fluid));
            }
        }
    }
    
    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        ToLongFunction<net.minecraft.nbt.Tag> nbtHasher = ItemComparator.nbtHasher();
        Function<ItemStack, ListTag> enchantmentTag = stack -> {
            CompoundTag tag = stack.getTag();
            if (tag == null) return null;
            if (!tag.contains("Enchantments", NbtType.LIST)) return null;
            return tag.getList("Enchantments", NbtType.COMPOUND);
        };
        registry.register(stack -> nbtHasher.applyAsLong(enchantmentTag.apply(stack)), Items.ENCHANTED_BOOK);
        registry.registerNbt(Items.POTION);
        registry.registerNbt(Items.SPLASH_POTION);
        registry.registerNbt(Items.LINGERING_POTION);
        registry.registerNbt(Items.TIPPED_ARROW);
    }
    
    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.register(
                new DefaultCraftingCategory(),
                new DefaultCookingCategory(SMELTING, EntryStacks.of(Items.FURNACE), "category.rei.smelting"),
                new DefaultCookingCategory(SMOKING, EntryStacks.of(Items.SMOKER), "category.rei.smoking"),
                new DefaultCookingCategory(BLASTING, EntryStacks.of(Items.BLAST_FURNACE), "category.rei.blasting"), new DefaultCampfireCategory(),
                new DefaultStoneCuttingCategory(),
                new DefaultFuelCategory(),
                new DefaultBrewingCategory(),
                new DefaultCompostingCategory(),
                new DefaultStrippingCategory(),
                new DefaultSmithingCategory(),
                new DefaultBeaconBaseCategory(),
                new DefaultBeaconPaymentCategory(),
                new DefaultTillingCategory(),
                new DefaultPathingCategory(),
                new DefaultInformationCategory()
        );
        
        registry.removePlusButton(FUEL);
        registry.removePlusButton(COMPOSTING);
        registry.removePlusButton(BEACON);
        registry.removePlusButton(BEACON_PAYMENT);
        registry.removePlusButton(INFO);
        registry.removePlusButton(STRIPPING);
        registry.removePlusButton(TILLING);
        registry.removePlusButton(PATHING);
        
        registry.addWorkstations(CRAFTING, EntryStacks.of(Items.CRAFTING_TABLE));
        registry.addWorkstations(SMELTING, EntryStacks.of(Items.FURNACE));
        registry.addWorkstations(SMOKING, EntryStacks.of(Items.SMOKER));
        registry.addWorkstations(BLASTING, EntryStacks.of(Items.BLAST_FURNACE));
        registry.addWorkstations(CAMPFIRE, EntryStacks.of(Items.CAMPFIRE), EntryStacks.of(Items.SOUL_CAMPFIRE));
        registry.addWorkstations(FUEL, EntryStacks.of(Items.FURNACE), EntryStacks.of(Items.SMOKER), EntryStacks.of(Items.BLAST_FURNACE));
        registry.addWorkstations(BREWING, EntryStacks.of(Items.BREWING_STAND));
        registry.addWorkstations(STONE_CUTTING, EntryStacks.of(Items.STONECUTTER));
        registry.addWorkstations(COMPOSTING, EntryStacks.of(Items.COMPOSTER));
        registry.addWorkstations(SMITHING, EntryStacks.of(Items.SMITHING_TABLE));
        registry.addWorkstations(BEACON, EntryStacks.of(Items.BEACON));
        registry.addWorkstations(BEACON_PAYMENT, EntryStacks.of(Items.BEACON));
        
        Set<Item> axes = Sets.newHashSet(), hoes = Sets.newHashSet(), shovels = Sets.newHashSet();
        EntryRegistry.getInstance().getEntryStacks().filter(stack -> stack.getValueType() == ItemStack.class).map(stack -> ((ItemStack) stack.getValue()).getItem()).forEach(item -> {
            if (item instanceof AxeItem && axes.add(item)) {
                registry.addWorkstations(STRIPPING, EntryStacks.of(item));
            }
            if (item instanceof HoeItem && hoes.add(item)) {
                registry.addWorkstations(TILLING, EntryStacks.of(item));
            }
            if (item instanceof ShovelItem && shovels.add(item)) {
                registry.addWorkstations(PATHING, EntryStacks.of(item));
            }
        });
        TagCollection<Item> itemTagCollection = Minecraft.getInstance().getConnection().getTags().getItems();
        Tag<Item> axesTag = itemTagCollection.getTag(new ResourceLocation("c", "axes"));
        if (axesTag != null) {
            for (Item item : axesTag.getValues()) {
                if (axes.add(item)) registry.addWorkstations(STRIPPING, EntryStacks.of(item));
            }
        }
        Tag<Item> hoesTag = itemTagCollection.getTag(new ResourceLocation("c", "hoes"));
        if (hoesTag != null) {
            for (Item item : hoesTag.getValues()) {
                if (hoes.add(item)) registry.addWorkstations(TILLING, EntryStacks.of(item));
            }
        }
        Tag<Item> shovelsTag = itemTagCollection.getTag(new ResourceLocation("c", "shovels"));
        if (shovelsTag != null) {
            for (Item item : shovelsTag.getValues()) {
                if (shovels.add(item)) registry.addWorkstations(PATHING, EntryStacks.of(item));
            }
        }
    }
    
    @Override
    public void registerDisplays(DisplayRegistry registry) {
        registry.registerFiller(ShapelessRecipe.class, DefaultShapelessDisplay::new);
        registry.registerFiller(ShapedRecipe.class, DefaultShapedDisplay::new);
        registry.registerFiller(SmeltingRecipe.class, DefaultSmeltingDisplay::new);
        registry.registerFiller(SmokingRecipe.class, DefaultSmokingDisplay::new);
        registry.registerFiller(BlastingRecipe.class, DefaultBlastingDisplay::new);
        registry.registerFiller(CampfireCookingRecipe.class, DefaultCampfireDisplay::new);
        registry.registerFiller(StonecutterRecipe.class, DefaultStoneCuttingDisplay::new);
        registry.registerFiller(UpgradeRecipe.class, DefaultSmithingDisplay::new);
        for (Map.Entry<Item, Integer> entry : AbstractFurnaceBlockEntity.getFuel().entrySet()) {
            registry.registerDisplay(new DefaultFuelDisplay(EntryIngredients.of(entry.getKey()), entry.getValue()));
        }
        EntryIngredient arrowStack = EntryIngredient.of(EntryStacks.of(Items.ARROW));
        ReferenceSet<Potion> registeredPotions = new ReferenceOpenHashSet<>();
        EntryRegistry.getInstance().getEntryStacks().filter(entry -> entry.getValue() == Items.LINGERING_POTION).forEach(entry -> {
            ItemStack itemStack = (ItemStack) entry.getValue();
            Potion potion = PotionUtils.getPotion(itemStack);
            if (registeredPotions.add(potion)) {
                List<EntryIngredient> input = new ArrayList<>();
                for (int i = 0; i < 4; i++)
                    input.add(arrowStack);
                input.add(EntryIngredient.of(EntryStacks.of(itemStack)));
                for (int i = 0; i < 4; i++)
                    input.add(arrowStack);
                ItemStack outputStack = new ItemStack(Items.TIPPED_ARROW, 8);
                PotionUtils.setPotion(outputStack, potion);
                PotionUtils.setCustomEffects(outputStack, PotionUtils.getCustomEffects(itemStack));
                EntryIngredient output = EntryIngredient.of(EntryStacks.of(outputStack));
                registry.registerDisplay(new DefaultCustomDisplay(null, input, output));
            }
        });
        if (ComposterBlock.COMPOSTABLES.isEmpty())
            ComposterBlock.bootStrap();
        Object2FloatMap<ItemLike> compostables = ComposterBlock.COMPOSTABLES;
        int i = 0;
        Iterator<List<Object2FloatMap.Entry<ItemLike>>> iterator = Iterators.partition(compostables.object2FloatEntrySet().stream().sorted(Map.Entry.comparingByValue()).iterator(), 48);
        while (iterator.hasNext()) {
            List<Object2FloatMap.Entry<ItemLike>> entries = iterator.next();
            registry.registerDisplay(new DefaultCompostingDisplay(i, entries, compostables, new ItemStack(Items.BONE_MEAL)));
            i++;
        }
        DummyAxeItem.getStrippedBlocksMap().entrySet().stream().sorted(Comparator.comparing(b -> Registry.BLOCK.getKey(b.getKey()))).forEach(set -> {
            registry.registerDisplay(new DefaultStrippingDisplay(EntryStacks.of(set.getKey()), EntryStacks.of(set.getValue())));
        });
        DummyHoeItem.getTilledBlocksMap().entrySet().stream().sorted(Comparator.comparing(b -> Registry.BLOCK.getKey(b.getKey()))).forEach(set -> {
            registry.registerDisplay(new DefaultTillingDisplay(EntryStacks.of(set.getKey()), EntryStacks.of(set.getValue().getBlock())));
        });
        DummyShovelItem.getPathBlocksMap().entrySet().stream().sorted(Comparator.comparing(b -> Registry.BLOCK.getKey(b.getKey()))).forEach(set -> {
            registry.registerDisplay(new DefaultPathingDisplay(EntryStacks.of(set.getKey()), EntryStacks.of(set.getValue().getBlock())));
        });
        registry.registerDisplay(new DefaultBeaconBaseDisplay(CollectionUtils.map(Lists.newArrayList(BlockTags.BEACON_BASE_BLOCKS.getValues()), ItemStack::new)));
        registry.registerDisplay(new DefaultBeaconPaymentDisplay(CollectionUtils.map(Lists.newArrayList(ItemTags.BEACON_PAYMENT_ITEMS.getValues()), ItemStack::new)));
        if (Platform.isFabric()) {
            Set<Potion> potions = Sets.newLinkedHashSet();
            for (Ingredient container : PotionBrewing.ALLOWED_CONTAINERS) {
                for (PotionBrewing.Mix<Potion> mix : PotionBrewing.POTION_MIXES) {
                    Potion from = mix.from;
                    Ingredient ingredient = mix.ingredient;
                    Potion to = mix.to;
                    Ingredient base = Ingredient.of(Arrays.stream(container.getItems())
                            .map(ItemStack::copy)
                            .map(stack -> PotionUtils.setPotion(stack, from)));
                    ItemStack output = Arrays.stream(container.getItems())
                            .map(ItemStack::copy)
                            .map(stack -> PotionUtils.setPotion(stack, to))
                            .findFirst().orElse(ItemStack.EMPTY);
                    registerBrewingRecipe(base, ingredient, output);
                    potions.add(from);
                    potions.add(to);
                }
            }
            for (Potion potion : potions) {
                for (PotionBrewing.Mix<Item> mix : PotionBrewing.CONTAINER_MIXES) {
                    Item from = mix.from;
                    Ingredient ingredient = mix.ingredient;
                    Item to = mix.to;
                    Ingredient base = Ingredient.of(PotionUtils.setPotion(new ItemStack(from), potion));
                    ItemStack output = PotionUtils.setPotion(new ItemStack(to), potion);
                    registerBrewingRecipe(base, ingredient, output);
                }
            }
        }
    }
    
    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(EffectRenderingInventoryScreen.class, new DefaultPotionEffectExclusionZones());
        zones.register(RecipeUpdateListener.class, new DefaultRecipeBookExclusionZones());
    }
    
    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerDecider(new DisplayBoundsProvider<AbstractContainerScreen<?>>() {
            @Override
            public Rectangle getScreenBounds(AbstractContainerScreen<?> screen) {
                return new Rectangle(screen.leftPos, screen.topPos, screen.imageWidth, screen.imageHeight);
            }
    
            @Override
            public <R extends Screen> boolean isHandingScreen(Class<R> screen) {
                return AbstractContainerScreen.class.isAssignableFrom(screen);
            }
        });
        
        registry.registerContainerClickArea(new Rectangle(88, 32, 28, 23), CraftingScreen.class, CRAFTING);
        registry.registerContainerClickArea(new Rectangle(137, 29, 10, 13), InventoryScreen.class, CRAFTING);
        registry.registerContainerClickArea(new Rectangle(97, 16, 14, 30), BrewingStandScreen.class, BREWING);
        registry.registerContainerClickArea(new Rectangle(78, 32, 28, 23), FurnaceScreen.class, SMELTING);
        registry.registerContainerClickArea(new Rectangle(78, 32, 28, 23), SmokerScreen.class, SMOKING);
        registry.registerContainerClickArea(new Rectangle(78, 32, 28, 23), BlastFurnaceScreen.class, BLASTING);
    }
    
    @Override
    public void registerFluidSupport(FluidSupportProvider support) {
        support.register(entry -> {
            ItemStack stack = entry.getValue();
            if (stack.getItem() instanceof BucketItem)
                return InteractionResultHolder.success(Stream.of(EntryStacks.of(((BucketItem) stack.getItem()).content, FluidStackHooks.bucketAmount())));
            return InteractionResultHolder.pass(null);
        });
    }
    
    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        registry.register(new DefaultRecipeBookHandler());
    }
    
    @Override
    public void registerFavorites(FavoriteEntryType.Registry registry) {
        registry.register(GameModeFavoriteEntry.ID, GameModeFavoriteEntry.Type.INSTANCE);
        registry.getOrCrateSection(new TranslatableComponent(GameModeFavoriteEntry.TRANSLATION_KEY))
                .add(Stream.concat(
                        Arrays.stream(GameType.values()),
                        Stream.of((GameType) null)
                ).<FavoriteEntry>map(GameModeFavoriteEntry.Type.INSTANCE::fromArgs).toArray(FavoriteEntry[]::new));
        registry.register(WeatherFavoriteEntry.ID, WeatherFavoriteEntry.Type.INSTANCE);
        registry.getOrCrateSection(new TranslatableComponent(WeatherFavoriteEntry.TRANSLATION_KEY))
                .add(Stream.concat(
                        Arrays.stream(WeatherFavoriteEntry.Weather.values()),
                        Stream.of((WeatherFavoriteEntry.Weather) null)
                ).<FavoriteEntry>map(WeatherFavoriteEntry.Type.INSTANCE::fromArgs).toArray(FavoriteEntry[]::new));
    }
    
    @Override
    public void registerSubsets(SubsetsRegistry registry) {
//        registry.registerPathEntry("roughlyenoughitems:food", EntryStacks.of(Items.MILK_BUCKET));
//        registry.registerPathEntry("roughlyenoughitems:food/roughlyenoughitems:cookies", EntryStacks.of(Items.COOKIE));
    }
    
    @Override
    public int getPriority() {
        return -100;
    }
    
}