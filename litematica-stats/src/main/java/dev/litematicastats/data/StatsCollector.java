package dev.litematicastats.data;

import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.util.ItemUtils;
import fi.dy.masa.malilib.util.data.DataBlockUtils;
import fi.dy.masa.malilib.util.data.tag.BaseData;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.StringData;
import fi.dy.masa.malilib.util.nbt.NbtInventory;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatsCollector {
    private static final String[] SINGLE_ITEM_KEYS = {"RecordItem", "item", "Book"};
    private static final String[] ENTITY_SINGLE_ITEM_KEYS = {"Item", "item"};

    public static List<MaterialListEntry> collectBlocks(LitematicaSchematic schematic) {
        List<MaterialListEntry> list = MaterialListUtils.createMaterialListFor(schematic, schematic.getAreas().keySet());
        MaterialListUtils.updateAvailableCounts(list, Minecraft.getInstance().player);
        return list;
    }

    public static List<EntityCount> collectEntities(LitematicaSchematic schematic) {
        Object2IntOpenHashMap<String> counts = new Object2IntOpenHashMap<>();
        Set<String> withInventory = new HashSet<>();

        for (String region : schematic.getAreas().keySet()) {
            List<LitematicaSchematic.EntityInfo> entities = schematic.getEntityListForRegion(region);
            if (entities == null) {
                continue;
            }
            for (LitematicaSchematic.EntityInfo info : entities) {
                String id = info.nbt().getString("id");
                if (id.isEmpty()) {
                    continue;
                }
                id = resolveBoatType(id, info.nbt());
                counts.addTo(id, 1);
                if (isEntityContainerTag(id, info.nbt())) {
                    withInventory.add(id);
                }
            }
        }

        List<EntityCount> result = new ArrayList<>();
        for (Object2IntMap.Entry<String> entry : counts.object2IntEntrySet()) {
            String id = entry.getKey();
            EntityType<?> type = lookupEntityType(id);
            ItemStack icon = entityIcon(id, type);
            Component name = entityDisplayName(id, type, icon);
            result.add(new EntityCount(id, type, entry.getIntValue(), icon, name, withInventory.contains(id)));
        }
        result.sort(Comparator.comparing(e -> e.name().getString().toLowerCase()));
        return result;
    }

    private static String resolveBoatType(String id, CompoundData nbt) {
        boolean boat = id.equals("minecraft:boat");
        boolean chestBoat = id.equals("minecraft:chest_boat");
        if ((boat || chestBoat) && nbt.contains("Type", 8)) {
            String type = nbt.getString("Type");
            if (!type.isEmpty()) {
                return "minecraft:" + type + (chestBoat ? "_chest_boat" : "_boat");
            }
        }
        return id;
    }

    private static Component entityDisplayName(String id, @Nullable EntityType<?> type, ItemStack icon) {
        if (type != null && SpawnEggItem.byId(type) != null) {
            return type.getDescription();
        }
        if (!icon.isEmpty() && icon.getItem() != Items.BARRIER) {
            return icon.getHoverName();
        }
        if (type != null) {
            return type.getDescription();
        }
        return Component.literal(id);
    }

    public static List<ContainerInfo> collectContainers(LitematicaSchematic schematic) {
        RegistryAccess registry = getRegistryAccess();
        List<ContainerInfo> result = new ArrayList<>();

        for (String region : schematic.getAreas().keySet()) {
            collectBlockContainers(schematic, region, registry, result);
            collectEntityContainers(schematic, region, registry, result);
        }

        result.sort(Comparator.comparing((ContainerInfo c) -> c.typeName.toLowerCase())
                .thenComparing(c -> c.region)
                .thenComparingInt(c -> c.pos.getX())
                .thenComparingInt(c -> c.pos.getY())
                .thenComparingInt(c -> c.pos.getZ()));
        return result;
    }

    private static void collectBlockContainers(LitematicaSchematic schematic, String region,
                                               RegistryAccess registry, List<ContainerInfo> result) {
        LitematicaBlockStateContainer container = schematic.getSubRegionContainer(region);
        Map<BlockPos, CompoundData> blockEntities = schematic.getBlockEntityMapForRegion(region);
        if (container == null || blockEntities == null || blockEntities.isEmpty()) {
            return;
        }

        Set<BlockPos> processed = new HashSet<>();
        for (Map.Entry<BlockPos, CompoundData> entry : blockEntities.entrySet()) {
            BlockPos pos = entry.getKey();
            if (processed.contains(pos)) {
                continue;
            }
            CompoundData tag = entry.getValue();
            if (!isContainerTag(tag)) {
                continue;
            }

            BlockState state = container.get(pos.getX(), pos.getY(), pos.getZ());
            if (state.isAir()) {
                continue;
            }

            boolean doubleChest = false;
            BlockPos displayPos = pos;
            List<ContainerInfo.SlotStack> items;

            if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                BlockPos otherPos = pos.relative(ChestBlock.getConnectedDirection(state));
                CompoundData otherTag = blockEntities.get(otherPos);
                if (otherTag != null) {
                    processed.add(otherPos);
                    doubleChest = true;
                    ChestType type = state.getValue(ChestBlock.TYPE);
                    if (type == ChestType.RIGHT) {
                        displayPos = otherPos;
                    }
                    int thisOffset = type == ChestType.LEFT ? 0 : 27;
                    items = mergeChestHalves(tag, otherTag, thisOffset, registry);
                } else {
                    items = parseItems(tag, registry);
                }
            } else {
                items = parseItems(tag, registry);
            }

            ItemStack icon = ItemUtils.getItemForState(state);
            if (icon.isEmpty()) {
                icon = new ItemStack(state.getBlock().asItem());
            }
            String typeKey = String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
            String typeName = state.getBlock().getName().getString();
            Component customName = readCustomName(tag, registry);
            boolean singleSlot = !tag.contains("Items", 9);
            int[] grid = singleSlot ? new int[]{1, 1} : blockGrid(typeKey, doubleChest, maxSlot(items));

            result.add(new ContainerInfo(region, displayPos, false, null, state, typeKey, typeName,
                    icon, customName, doubleChest, items, grid[0], grid[1]));
        }
    }

    private static void collectEntityContainers(LitematicaSchematic schematic, String region,
                                                RegistryAccess registry, List<ContainerInfo> result) {
        List<LitematicaSchematic.EntityInfo> entities = schematic.getEntityListForRegion(region);
        if (entities == null) {
            return;
        }
        for (LitematicaSchematic.EntityInfo info : entities) {
            CompoundData tag = info.nbt();
            String id = tag.getString("id");
            if (id.isEmpty() || !isEntityContainerTag(id, tag)) {
                continue;
            }
            String iconId = resolveBoatType(id, tag);
            List<ContainerInfo.SlotStack> items = tag.contains("Items", 9)
                    ? parseItems(tag, registry)
                    : parseItems(tag, registry, ENTITY_SINGLE_ITEM_KEYS);
            EntityType<?> type = lookupEntityType(id);
            ItemStack icon = entityIcon(iconId, type);
            String typeName = entityDisplayName(iconId, type, icon).getString();
            Component customName = readCustomName(tag, registry);
            BlockPos pos = BlockPos.containing(info.posVec());
            int columns = entityGridColumns(id, tag);
            int slots = entityGridSlots(id, columns, items);

            result.add(new ContainerInfo(region, pos, true, id, null, id, typeName,
                    icon, customName, false, items, columns, slots));
        }
    }

    private static List<ContainerInfo.SlotStack> mergeChestHalves(CompoundData thisHalf, CompoundData otherHalf,
                                                                  int thisOffset, RegistryAccess registry) {
        List<ContainerInfo.SlotStack> merged = new ArrayList<>();
        for (ContainerInfo.SlotStack slot : parseItems(thisHalf, registry)) {
            merged.add(new ContainerInfo.SlotStack(slot.slot() + thisOffset, slot.stack()));
        }
        int otherOffset = 27 - thisOffset;
        for (ContainerInfo.SlotStack slot : parseItems(otherHalf, registry)) {
            merged.add(new ContainerInfo.SlotStack(slot.slot() + otherOffset, slot.stack()));
        }
        merged.sort(Comparator.comparingInt(ContainerInfo.SlotStack::slot));
        return merged;
    }

    private static boolean isContainerTag(CompoundData tag) {
        if (tag.contains("Items", 9)) {
            return true;
        }
        for (String key : SINGLE_ITEM_KEYS) {
            if (tag.contains(key, 10)) {
                return true;
            }
        }
        return false;
    }

    private static List<ContainerInfo.SlotStack> parseItems(CompoundData tag, RegistryAccess registry) {
        return parseItems(tag, registry, SINGLE_ITEM_KEYS);
    }

    private static List<ContainerInfo.SlotStack> parseItems(CompoundData tag, RegistryAccess registry, String[] singleKeys) {
        List<ContainerInfo.SlotStack> list = new ArrayList<>();
        if (tag.contains("Items", 9)) {
            ListData items = tag.getList("Items");
            for (int i = 0; i < items.size(); i++) {
                CompoundData element = items.getCompoundAt(i);
                NbtInventory.EntrySlot slot = NbtInventory.EntrySlot.fromData(element, registry);
                if (!slot.stack().isEmpty()) {
                    list.add(new ContainerInfo.SlotStack(slot.slot(), slot.stack()));
                }
            }
        } else {
            for (String key : singleKeys) {
                if (tag.contains(key, 10)) {
                    NbtInventory.EntrySlot slot = NbtInventory.EntrySlot.fromData(tag.getCompound(key), registry);
                    if (!slot.stack().isEmpty()) {
                        list.add(new ContainerInfo.SlotStack(0, slot.stack()));
                    }
                    break;
                }
            }
        }
        list.sort(Comparator.comparingInt(ContainerInfo.SlotStack::slot));
        return list;
    }

    private static boolean isEntityItemHolder(String id) {
        return id.equals("minecraft:item_frame") || id.equals("minecraft:glow_item_frame") || id.equals("minecraft:item_display");
    }

    private static boolean isEntityContainerTag(String id, CompoundData tag) {
        if (tag.contains("Items", 9)) {
            return true;
        }
        return isEntityItemHolder(id) && (tag.contains("Item", 10) || tag.contains("item", 10));
    }

    private static int[] blockGrid(String blockId, boolean doubleChest, int maxSlot) {
        return switch (blockId) {
            case "minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker" -> new int[]{3, 3};
            case "minecraft:hopper" -> new int[]{5, 5};
            case "minecraft:dispenser", "minecraft:dropper", "minecraft:crafter" -> new int[]{3, 9};
            case "minecraft:brewing_stand" -> new int[]{5, 5};
            case "minecraft:chiseled_bookshelf" -> new int[]{6, 6};
            case "minecraft:campfire", "minecraft:soul_campfire" -> new int[]{4, 4};
            case "minecraft:beacon" -> new int[]{1, 1};
            case "minecraft:chest", "minecraft:trapped_chest" -> new int[]{9, doubleChest ? 54 : 27};
            case "minecraft:barrel" -> new int[]{9, 27};
            default -> {
                if (blockId.endsWith("shulker_box")) {
                    yield new int[]{9, 27};
                }
                yield new int[]{9, roundUpToMultiple(maxSlot + 1, 9, 9)};
            }
        };
    }

    private static int maxSlot(List<ContainerInfo.SlotStack> items) {
        int max = -1;
        for (ContainerInfo.SlotStack slot : items) {
            max = Math.max(max, slot.slot());
        }
        return max;
    }

    private static int roundUpToMultiple(int value, int multiple, int minimum) {
        int result = Math.max(value, minimum);
        return ((result + multiple - 1) / multiple) * multiple;
    }

    private static int entityGridColumns(String id, CompoundData tag) {
        return switch (id) {
            case "minecraft:chest_minecart", "minecraft:chest_boat" -> 9;
            case "minecraft:hopper_minecart" -> 5;
            case "minecraft:donkey", "minecraft:mule" -> 5;
            // Llamas carry strength * 3 slots, arranged in 'strength' columns
            case "minecraft:llama", "minecraft:trader_llama" -> Math.max(1, Math.min(5, tag.getInt("Strength")));
            case "minecraft:item_frame", "minecraft:glow_item_frame", "minecraft:item_display" -> 1;
            default -> 9;
        };
    }

    private static int entityGridSlots(String id, int columns, List<ContainerInfo.SlotStack> items) {
        int known = switch (id) {
            case "minecraft:chest_minecart", "minecraft:chest_boat" -> 27;
            case "minecraft:hopper_minecart" -> 5;
            case "minecraft:donkey", "minecraft:mule" -> 15;
            case "minecraft:llama", "minecraft:trader_llama" -> columns * 3;
            case "minecraft:item_frame", "minecraft:glow_item_frame", "minecraft:item_display" -> 1;
            default -> 0;
        };
        return Math.max(known, roundUpToMultiple(maxSlot(items) + 1, columns, columns));
    }

    @Nullable
    public static Component readCustomName(CompoundData tag, RegistryAccess registry) {
        // With litematica's datafixer bypassed, old schematics keep the legacy JSON string form
        BaseData raw = tag.getData("CustomName").orElse(null);
        if (raw instanceof StringData stringData) {
            String value = stringData.getString();
            try {
                return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(value))
                        .result().orElse(Component.literal(value));
            } catch (Exception e) {
                return Component.literal(value);
            }
        }
        return DataBlockUtils.getCustomName(tag, registry, "CustomName");
    }

    @Nullable
    public static EntityType<?> lookupEntityType(String id) {
        Identifier rl = Identifier.tryParse(id);
        return rl != null ? BuiltInRegistries.ENTITY_TYPE.getValue(rl) : null;
    }

    public static ItemStack entityIcon(String id, @Nullable EntityType<?> type) {
        if (type != null) {
            SpawnEggItem egg = SpawnEggItem.byId(type);
            if (egg != null) {
                return new ItemStack(egg);
            }
        }
        Identifier rl = Identifier.tryParse(id);
        if (rl != null) {
            Item item = BuiltInRegistries.ITEM.getValue(rl);
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        return new ItemStack(Items.BARRIER);
    }

    public static RegistryAccess getRegistryAccess() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return mc.level.registryAccess();
        }
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }
}