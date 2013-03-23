package org.bukkit.craftbukkit.inventory;

import static org.bukkit.craftbukkit.inventory.CraftMetaItem.ENCHANTMENTS;
import static org.bukkit.craftbukkit.inventory.CraftMetaItem.ENCHANTMENTS_ID;
import static org.bukkit.craftbukkit.inventory.CraftMetaItem.ENCHANTMENTS_LVL;

import java.util.Map;


import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import guava10.com.google.common.collect.ImmutableMap;

@DelegateDeserialization(ItemStack.class)
public final class CraftItemStack extends ItemStack {

    public static /*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ asNMSCopy(ItemStack original) {
        if (original instanceof CraftItemStack) {
            CraftItemStack stack = (CraftItemStack) original;
            return stack.handle == null ? null : stack.handle.copy/*was:cloneItemStack*/();
        }
        if (original == null || original.getTypeId() <= 0) {
            return null;
        }
        /*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ stack = new net.minecraft.item.ItemStack/*was:ItemStack*/(original.getTypeId(), original.getAmount(), original.getDurability());
        if (original.hasItemMeta()) {
            setItemMeta(stack, original.getItemMeta());
        }
        return stack;
    }

    public static /*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ copyNMSStack(/*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ original, int amount) {
        /*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ stack = original.copy/*was:cloneItemStack*/();
        stack.stackSize/*was:count*/ = amount;
        return stack;
    }

    /**
     * Copies the NMS stack to return as a strictly-Bukkit stack
     */
    public static ItemStack asBukkitCopy(/*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ original) {
        if (original == null) {
            return new ItemStack(Material.AIR);
        }
        ItemStack stack = new ItemStack(original.itemID/*was:id*/, original.stackSize/*was:count*/, (short) original.getItemDamage/*was:getData*/());
        if (hasItemMeta(original)) {
            stack.setItemMeta(getItemMeta(original));
        }
        return stack;
    }

    public static CraftItemStack asCraftMirror(/*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ original) {
        return new CraftItemStack(original);
    }

    public static CraftItemStack asCraftCopy(ItemStack original) {
        if (original instanceof CraftItemStack) {
            CraftItemStack stack = (CraftItemStack) original;
            return new CraftItemStack(stack.handle == null ? null : stack.handle.copy/*was:cloneItemStack*/());
        }
        return new CraftItemStack(original);
    }

    public static CraftItemStack asNewCraftStack(/*was:net.minecraft.server.*/net.minecraft.item.Item/*was:Item*/ item) {
        return asNewCraftStack(item, 1);
    }

    public static CraftItemStack asNewCraftStack(/*was:net.minecraft.server.*/net.minecraft.item.Item/*was:Item*/ item, int amount) {
        return new CraftItemStack(item.itemID/*was:id*/, amount, (short) 0, null);
    }

    /*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ handle;

    /**
     * Mirror
     */
    private CraftItemStack(/*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ item) {
        this.handle = item;
    }

    private CraftItemStack(ItemStack item) {
        this(item.getTypeId(), item.getAmount(), item.getDurability(), item.hasItemMeta() ? item.getItemMeta() : null);
    }

    private CraftItemStack(int typeId, int amount, short durability, ItemMeta itemMeta) {
        setTypeId(typeId);
        setAmount(amount);
        setDurability(durability);
        setItemMeta(itemMeta);
    }

    @Override
    public int getTypeId() {
        return handle != null ? handle.itemID/*was:id*/ : 0;
    }

    @Override
    public void setTypeId(int type) {
        if (getTypeId() == type) {
            return;
        } else if (type == 0) {
            handle = null;
        } else if (handle == null) {
            handle = new net.minecraft.item.ItemStack/*was:ItemStack*/(type, 1, 0);
        } else {
            handle.itemID/*was:id*/ = type;
            if (hasItemMeta()) {
                // This will create the appropriate item meta, which will contain all the data we intend to keep
                setItemMeta(handle, getItemMeta(handle));
            }
        }
        setData(null);
    }

    @Override
    public int getAmount() {
        return handle != null ? handle.stackSize/*was:count*/ : 0;
    }

    @Override
    public void setAmount(int amount) {
        if (handle == null) {
            return;
        }
        if (amount == 0) {
            handle = null;
        } else {
            handle.stackSize/*was:count*/ = amount;
        }
    }

    @Override
    public void setDurability(final short durability) {
        // Ignore damage if item is null
        if (handle != null) {
            handle.setItemDamage/*was:setData*/(durability);
        }
    }

    @Override
    public short getDurability() {
        if (handle != null) {
            return (short) handle.getItemDamage/*was:getData*/();
        } else {
            return -1;
        }
    }

    @Override
    public int getMaxStackSize() {
        return (handle == null) ? Material.AIR.getMaxStackSize() : handle.getItem/*was:getItem*/().getItemStackLimit/*was:getMaxStackSize*/();
    }

    @Override
    public void addUnsafeEnchantment(Enchantment ench, int level) {
        Validate.notNull(ench, "Cannot add null enchantment");

        if (!makeTag(handle)) {
            return;
        }
        net.minecraft.nbt.NBTTagList/*was:NBTTagList*/ list = getEnchantmentList(handle);
        if (list == null) {
            list = new net.minecraft.nbt.NBTTagList/*was:NBTTagList*/(ENCHANTMENTS.NBT);
            handle.stackTagCompound/*was:tag*/.setTag/*was:set*/(ENCHANTMENTS.NBT, list);
        }
        int size = list.tagCount/*was:size*/();

        for (int i = 0; i < size; i++) {
            net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/ tag = (net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/) list.tagAt/*was:get*/(i);
            short id = tag.getShort/*was:getShort*/(ENCHANTMENTS_ID.NBT);
            if (id == ench.getId()) {
                tag.setShort/*was:setShort*/(ENCHANTMENTS_LVL.NBT, (short) level);
                return;
            }
        }
        net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/ tag = new net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/();
        tag.setShort/*was:setShort*/(ENCHANTMENTS_ID.NBT, (short) ench.getId());
        tag.setShort/*was:setShort*/(ENCHANTMENTS_LVL.NBT, (short) level);
        list.appendTag/*was:add*/(tag);
    }

    static boolean makeTag(/*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ item) {
        if (item == null) {
            return false;
        }
        if (item.stackTagCompound/*was:tag*/ != null) {
            return true;
        }
        item.stackTagCompound/*was:tag*/ = new net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/();
        return true;
    }

    @Override
    public boolean containsEnchantment(Enchantment ench) {
        return getEnchantmentLevel(ench) > 0;
    }

    @Override
    public int getEnchantmentLevel(Enchantment ench) {
        Validate.notNull(ench, "Cannot find null enchantment");
        if (handle == null) {
            return 0;
        }
        return net.minecraft.enchantment.EnchantmentHelper/*was:EnchantmentManager*/.getEnchantmentLevel/*was:getEnchantmentLevel*/(ench.getId(), handle);
    }

    @Override
    public int removeEnchantment(Enchantment ench) {
        Validate.notNull(ench, "Cannot remove null enchantment");

        net.minecraft.nbt.NBTTagList/*was:NBTTagList*/ list = getEnchantmentList(handle), listCopy;
        if (list == null) {
            return 0;
        }
        int index = Integer.MIN_VALUE;
        int level = Integer.MIN_VALUE;
        int size = list.tagCount/*was:size*/();

        for (int i = 0; i < size; i++) {
            net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/ enchantment = (net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/) list.tagAt/*was:get*/(i);
            int id = 0xffff & enchantment.getShort/*was:getShort*/(ENCHANTMENTS_ID.NBT);
            if (id == ench.getId()) {
                index = i;
                level = 0xffff & enchantment.getShort/*was:getShort*/(ENCHANTMENTS_LVL.NBT);
                break;
            }
        }

        if (index == Integer.MIN_VALUE) {
            return 0;
        }
        if (size == 1) {
            handle.stackTagCompound/*was:tag*/.removeTag/*was:remove*/(ENCHANTMENTS.NBT);
            if (handle.stackTagCompound/*was:tag*/.hasNoTags/*was:isEmpty*/()) {
                handle.stackTagCompound/*was:tag*/ = null;
            }
            return level;
        }

        // This is workaround for not having an index removal
        listCopy = new net.minecraft.nbt.NBTTagList/*was:NBTTagList*/(ENCHANTMENTS.NBT);
        for (int i = 0; i < size; i++) {
            if (i != index) {
                listCopy.appendTag/*was:add*/(list.tagAt/*was:get*/(i));
            }
        }
        handle.stackTagCompound/*was:tag*/.setTag/*was:set*/(ENCHANTMENTS.NBT, listCopy);

        return level;
    }

    @Override
    public Map<Enchantment, Integer> getEnchantments() {
        return getEnchantments(handle);
    }

    static Map<Enchantment, Integer> getEnchantments(/*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ item) {
        ImmutableMap.Builder<Enchantment, Integer> result = ImmutableMap.builder();
        net.minecraft.nbt.NBTTagList/*was:NBTTagList*/ list = (item == null) ? null : item.getEnchantmentTagList/*was:getEnchantments*/();

        if (list == null) {
            return result.build();
        }

        for (int i = 0; i < list.tagCount/*was:size*/(); i++) {
            int id = 0xffff & ((net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/) list.tagAt/*was:get*/(i)).getShort/*was:getShort*/(ENCHANTMENTS_ID.NBT);
            int level = 0xffff & ((net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/) list.tagAt/*was:get*/(i)).getShort/*was:getShort*/(ENCHANTMENTS_LVL.NBT);

            result.put(Enchantment.getById(id), level);
        }

        return result.build();
    }

    static net.minecraft.nbt.NBTTagList/*was:NBTTagList*/ getEnchantmentList(/*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ item) {
        return item == null ? null : item.getEnchantmentTagList/*was:getEnchantments*/();
    }

    @Override
    public CraftItemStack clone() {
        CraftItemStack itemStack = (CraftItemStack) super.clone();
        if (this.handle != null) {
            itemStack.handle = this.handle.copy/*was:cloneItemStack*/();
        }
        return itemStack;
    }

    @Override
    public ItemMeta getItemMeta() {
        return getItemMeta(handle);
    }

    static ItemMeta getItemMeta(/*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ item) {
        if (!hasItemMeta(item)) {
            return CraftItemFactory.instance().getItemMeta(getType(item));
        }
        switch (getType(item)) {
            case WRITTEN_BOOK:
            case BOOK_AND_QUILL:
                return new CraftMetaBook(item.stackTagCompound/*was:tag*/);
            case SKULL_ITEM:
                return new CraftMetaSkull(item.stackTagCompound/*was:tag*/);
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
                return new CraftMetaLeatherArmor(item.stackTagCompound/*was:tag*/);
            case POTION:
                return new CraftMetaPotion(item.stackTagCompound/*was:tag*/);
            case MAP:
                return new CraftMetaMap(item.stackTagCompound/*was:tag*/);
            case FIREWORK:
                return new CraftMetaFirework(item.stackTagCompound/*was:tag*/);
            case FIREWORK_CHARGE:
                return new CraftMetaCharge(item.stackTagCompound/*was:tag*/);
            case ENCHANTED_BOOK:
                return new CraftMetaEnchantedBook(item.stackTagCompound/*was:tag*/);
            default:
                return new CraftMetaItem(item.stackTagCompound/*was:tag*/);
        }
    }

    static Material getType(/*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ item) {
        Material material = Material.getMaterial(item == null ? 0 : item.itemID/*was:id*/);
        return material == null ? Material.AIR : material;
    }

    @Override
    public boolean setItemMeta(ItemMeta itemMeta) {
        return setItemMeta(handle, itemMeta);
    }

    static boolean setItemMeta(/*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ item, ItemMeta itemMeta) {
        if (item == null) {
            return false;
        }
        if (itemMeta == null) {
            item.stackTagCompound/*was:tag*/ = null;
            return true;
        }
        if (!CraftItemFactory.instance().isApplicable(itemMeta, getType(item))) {
            return false;
        }

        net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/ tag = new net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/();
        item.setTagCompound/*was:setTag*/(tag);

        ((CraftMetaItem) itemMeta).applyToItem(tag);
        return true;
    }

    @Override
    public boolean isSimilar(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        if (stack == this) {
            return true;
        }
        if (!(stack instanceof CraftItemStack)) {
            return stack.getClass() == ItemStack.class && stack.isSimilar(this);
        }

        CraftItemStack that = (CraftItemStack) stack;
        if (handle == that.handle) {
            return true;
        }
        if (handle == null || that.handle == null) {
            return false;
        }
        if (!(that.getTypeId() == getTypeId() && getDurability() == that.getDurability())) {
            return false;
        }
        return hasItemMeta() ? that.hasItemMeta() && handle.stackTagCompound/*was:tag*/.equals/*was:equals*/(that.handle.stackTagCompound/*was:tag*/) : !that.hasItemMeta();
    }

    @Override
    public boolean hasItemMeta() {
        return hasItemMeta(handle);
    }

    static boolean hasItemMeta(/*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ item) {
        return !(item == null || item.stackTagCompound/*was:tag*/ == null || item.stackTagCompound/*was:tag*/.hasNoTags/*was:isEmpty*/());
    }
}