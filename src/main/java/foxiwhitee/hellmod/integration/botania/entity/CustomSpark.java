package foxiwhitee.hellmod.integration.botania.entity;

import baubles.common.lib.PlayerHandler;
import foxiwhitee.hellmod.integration.botania.BotaniaIntegration;
import foxiwhitee.hellmod.proxy.ClientProxy;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaItem;
import vazkii.botania.api.mana.spark.ISparkAttachable;
import vazkii.botania.api.mana.spark.ISparkEntity;
import vazkii.botania.api.mana.spark.SparkHelper;
import vazkii.botania.common.Botania;
import vazkii.botania.common.core.helper.Vector3;
import vazkii.botania.common.item.ModItems;

import java.awt.*;
import java.util.*;
import java.util.List;

public abstract class CustomSpark  extends Entity implements ISparkEntity {
    public static final int INVISIBILITY_DATA_WATCHER_KEY = 27;

    private static final String TAG_UPGRADE = "upgrade";

    private static final String TAG_INVIS = "invis";

    Set<ISparkEntity> transfers;

    int removeTransferants;

    boolean firstTick;

    public CustomSpark(World world) {
        super(world);
        this.transfers = Collections.newSetFromMap(new WeakHashMap<>());
        this.removeTransferants = 2;
        this.firstTick = false;
        this.isImmuneToFire = true;
    }

    public static void particleBeam(Entity e1, Entity e2) {
        if (e1 == null || e2 == null)
            return;
        Vector3 orig = new Vector3(e1.posX, e1.posY + 0.25D, e1.posZ);
        Vector3 end = new Vector3(e2.posX, e2.posY + 0.25D, e2.posZ);
        Vector3 diff = end.copy().sub(orig);
        Vector3 movement = diff.copy().normalize().multiply(0.1D);
        int iters = (int)(diff.mag() / movement.mag());
        float huePer = 1.0F / iters;
        float hueSum = (float)Math.random();
        Vector3 currentPos = orig.copy();
        for (int i = 0; i < iters; i++) {
            float hue = i * huePer + hueSum;
            Color color = Color.getHSBColor(hue, 1.0F, 1.0F);
            float r = Math.min(1.0F, color.getRed() / 255.0F + 0.4F);
            float g = Math.min(1.0F, color.getGreen() / 255.0F + 0.4F);
            float b = Math.min(1.0F, color.getBlue() / 255.0F + 0.4F);
            Botania.proxy.setSparkleFXNoClip(true);
            Botania.proxy.sparkleFX(e1.worldObj, currentPos.x, currentPos.y, currentPos.z, r, g, b, 1.0F, 12);
            Botania.proxy.setSparkleFXNoClip(false);
            currentPos.add(movement);
        }
    }

    public int getColor() {
        switch (this.getName()) {
            case "asgardSpark": return BotaniaIntegration.sparkColorAsgard;
            case "helhelmSpark": return BotaniaIntegration.sparkColorHelhelm;
            case "valhallaSpark": return BotaniaIntegration.sparkColorValhalla;
            default: return BotaniaIntegration.sparkColorMidgard;
        }
    }

    protected void entityInit() {
        setSize(0.1F, 0.5F);
        this.dataWatcher.addObject(27, Integer.valueOf(0));
        this.dataWatcher.addObject(28, Integer.valueOf(0));
        this.dataWatcher.setObjectWatched(27);
        this.dataWatcher.setObjectWatched(28);
    }

    public void onUpdate() {
        super.onUpdate();
        ISparkAttachable tile = getAttachedTile();
        if (tile == null) {
            if (!this.worldObj.isRemote)
                setDead();
            return;
        }
        boolean first = (this.worldObj.isRemote && !this.firstTick);
        int upgrade = getUpgrade();
        java.util.List<ISparkEntity> allSparks = null;
        if (first || upgrade == 2 || upgrade == 3)
            allSparks = SparkHelper.getSparksAround(this.worldObj, this.posX, this.posY, this.posZ);
        if (first)
            first = true;
        Collection<ISparkEntity> transfers = getTransfers();
        if (upgrade != 0) {
            java.util.List<EntityPlayer> players;
            java.util.List<ISparkEntity> validSparks;
            Map<EntityPlayer, Map<ItemStack, Integer>> receivingPlayers;
            ItemStack input;
            switch (upgrade) {
                case 1:
                    players = SparkHelper.getEntitiesAround(EntityPlayer.class, this.worldObj, this.posX, this.posY, this.posZ);
                    receivingPlayers = new HashMap<>();
                    Item item;
                    switch (this.getName()) {
                        case "asgardSpark": item = BotaniaIntegration.ASGARD_SPARK; break;
                        case "helhelmSpark": item = BotaniaIntegration.HELHELM_SPARK; break;
                        case "valhallaSpark": item = BotaniaIntegration.VALHALLA_SPARK; break;
                        case "midgardSpark": item = BotaniaIntegration.MIDGARD_SPARK; break;
                        default: item = null;
                    }
                    input = new ItemStack(item);
                    for (EntityPlayer player : players) {
                        java.util.List<ItemStack> stacks = new ArrayList<>();
                        stacks.addAll(Arrays.asList(player.inventory.mainInventory));
                        stacks.addAll(Arrays.asList(player.inventory.armorInventory));
                        stacks.addAll(Arrays.asList((PlayerHandler.getPlayerBaubles(player)).stackList));
                        for (ItemStack stack : stacks) {
                            Map<ItemStack, Integer> receivingStacks;
                            if (stack == null ||
                                    !(stack.getItem() instanceof IManaItem))
                                continue;
                            IManaItem manaItem = (IManaItem)stack.getItem();
                            if (!manaItem.canReceiveManaFromItem(stack, input))
                                continue;
                            boolean add = false;
                            if (!receivingPlayers.containsKey(player)) {
                                add = true;
                                receivingStacks = (Map)new HashMap<>();
                            } else {
                                receivingStacks = receivingPlayers.get(player);
                            }
                            int recv = Math.min(getAttachedTile().getCurrentMana(), Math.min(250000, manaItem.getMaxMana(stack) - manaItem.getMana(stack)));
                            if (recv <= 0)
                                continue;
                            receivingStacks.put(stack, Integer.valueOf(recv));
                            if (!add)
                                continue;
                            receivingPlayers.put(player, receivingStacks);
                        }
                    }
                    if (!receivingPlayers.isEmpty()) {
                        java.util.List<EntityPlayer> keys = new ArrayList<>(receivingPlayers.keySet());
                        Collections.shuffle(keys);
                        EntityPlayer player = keys.iterator().next();
                        Map<ItemStack, Integer> items = receivingPlayers.get(player);
                        ItemStack stack2 = items.keySet().iterator().next();
                        int cost = ((Integer)items.get(stack2)).intValue();
                        int manaToPut = Math.min(getAttachedTile().getCurrentMana(), cost);
                        ((IManaItem)stack2.getItem()).addMana(stack2, manaToPut);
                        getAttachedTile().recieveMana(-manaToPut);
                        particlesTowards((Entity)player);
                    }
                    break;
                case 2:
                    validSparks = new ArrayList<>();
                    for (ISparkEntity spark : allSparks) {
                        if (spark == this)
                            continue;
                        int upgrade_ = spark.getUpgrade();
                        if (upgrade_ != 0 || !(spark.getAttachedTile() instanceof vazkii.botania.api.mana.IManaPool))
                            continue;
                        validSparks.add(spark);
                    }
                    if (validSparks.size() > 0)
                        ((ISparkEntity)validSparks.get(this.worldObj.rand.nextInt(validSparks.size()))).registerTransfer(this);
                    break;
                case 3:
                    for (ISparkEntity spark2 : allSparks) {
                        if (spark2 == this)
                            continue;
                        int upgrade_2 = spark2.getUpgrade();
                        if (upgrade_2 == 2 || upgrade_2 == 3 || upgrade_2 == 4)
                            continue;
                        transfers.add(spark2);
                    }
                    break;
            }
        }
        if (!transfers.isEmpty()) {
            int manaTotal = Math.min(250000 * transfers.size(), tile.getCurrentMana());
            int manaForEach = manaTotal / transfers.size();
            int manaSpent = 0;
            if (manaForEach > transfers.size()) {
                for (ISparkEntity spark3 : transfers) {
                    if (spark3.getAttachedTile() == null || spark3.getAttachedTile().isFull() || spark3.areIncomingTransfersDone()) {
                        manaTotal -= manaForEach;
                        continue;
                    }
                    ISparkAttachable attached = spark3.getAttachedTile();
                    int spend = Math.min(attached.getAvailableSpaceForMana(), manaForEach);
                    attached.recieveMana(spend);
                    manaSpent += spend;
                    particlesTowards((Entity)spark3);
                }
                tile.recieveMana(-manaSpent);
            }
        }
        if (this.removeTransferants > 0)
            this.removeTransferants--;
        getTransfers();
    }

    void particlesTowards(Entity e) {
        Vector3 thisVec = Vector3.fromEntityCenter(this).add(0.0D, 0.0D, 0.0D);
        Vector3 receiverVec = Vector3.fromEntityCenter(e).add(0.0D, 0.0D, 0.0D);
        double rc = 0.45D;
        thisVec.add((Math.random() - 0.5D) * rc, (Math.random() - 0.5D) * rc, (Math.random() - 0.5D) * rc);
        receiverVec.add((Math.random() - 0.5D) * rc, (Math.random() - 0.5D) * rc, (Math.random() - 0.5D) * rc);
        Vector3 motion = receiverVec.copy().sub(thisVec);
        motion.multiply(0.03999999910593033D);
        float r = 0.4F + 0.3F * (float)Math.random();
        float g = 0.4F + 0.3F * (float)Math.random();
        float b = 0.4F + 0.3F * (float)Math.random();
        float size = 0.125F + 0.125F * (float)Math.random();
        Botania.proxy.wispFX(this.worldObj, thisVec.x, thisVec.y, thisVec.z, r, g, b, size, (float)motion.x, (float)motion.y, (float)motion.z);
    }

    public boolean canBeCollidedWith() {
        return true;
    }

    public boolean interactFirst(EntityPlayer player) {
        if (this.isDead)
            return false;
        ItemStack stack = player.getCurrentEquippedItem();
        if (stack != null) {
            int upgrade = getUpgrade();
            if (stack.getItem() == ModItems.twigWand) {
                if (player.isSneaking()) {
                    if (upgrade > 0) {
                        if (!this.worldObj.isRemote)
                            entityDropItem(new ItemStack(ModItems.sparkUpgrade, 1, upgrade - 1), 0.0F);
                        setUpgrade(0);
                        this.transfers.clear();
                        this.removeTransferants = 2;
                    } else {
                        setDead();
                    }
                    if (player.worldObj.isRemote)
                        player.swingItem();
                    return true;
                }
                List<ISparkEntity> allSparks = SparkHelper.getSparksAround(this.worldObj, this.posX, this.posY, this.posZ);
                for (ISparkEntity spark : allSparks)
                    particleBeam(this, (Entity)spark);
                return true;
            }
            if (stack.getItem() == ModItems.sparkUpgrade && upgrade == 0) {
                int newUpgrade = stack.getItemDamage() + 1;
                setUpgrade(newUpgrade);
                ItemStack itemStack = stack;
                itemStack.stackSize--;
                if (player.worldObj.isRemote)
                    player.swingItem();
                return true;
            }
        }
        return doPhantomInk(stack);
    }

    public boolean doPhantomInk(ItemStack stack) {
        if (stack != null && stack.getItem() == ModItems.phantomInk && !this.worldObj.isRemote) {
            int invis = this.dataWatcher.getWatchableObjectInt(27);
            this.dataWatcher.updateObject(27, Integer.valueOf((invis ^ 0xFFFFFFFF) & 0x1));
            return true;
        }
        return false;
    }

    protected void readEntityFromNBT(NBTTagCompound cmp) {
        setUpgrade(cmp.getInteger("upgrade"));
        this.dataWatcher.updateObject(27, Integer.valueOf(cmp.getInteger("invis")));
    }

    protected void writeEntityToNBT(NBTTagCompound cmp) {
        cmp.setInteger("upgrade", getUpgrade());
        cmp.setInteger("invis", this.dataWatcher.getWatchableObjectInt(27));
    }

    public void setDead() {
        super.setDead();
        if (!this.worldObj.isRemote) {
            int upgrade = getUpgrade();
            Item item;
            switch (this.getName()) {
                case "asgardSpark": item = BotaniaIntegration.ASGARD_SPARK; break;
                case "helhelmSpark": item = BotaniaIntegration.HELHELM_SPARK; break;
                case "valhallaSpark": item = BotaniaIntegration.VALHALLA_SPARK; break;
                case "midgardSpark": item = BotaniaIntegration.MIDGARD_SPARK; break;
                default: item = null;
            }
            entityDropItem(new ItemStack(item), 0.0F);
            if (upgrade > 0)
                entityDropItem(new ItemStack(ModItems.sparkUpgrade, 1, upgrade - 1), 0.0F);
        }
    }

    public ISparkAttachable getAttachedTile() {
        int x = MathHelper.floor_double(this.posX);
        int y = MathHelper.floor_double(this.posY) - 1;
        int z = MathHelper.floor_double(this.posZ);
        TileEntity tile = this.worldObj.getTileEntity(x, y, z);
        if (tile instanceof ISparkAttachable)
            return (ISparkAttachable)tile;
        return null;
    }

    public Collection<ISparkEntity> getTransfers() {
        Collection<ISparkEntity> removals = new ArrayList<>();
        for (ISparkEntity spark : this.transfers) {
            ISparkEntity e = spark;
            int upgr = getUpgrade();
            int supgr = spark.getUpgrade();
            ISparkAttachable atile = spark.getAttachedTile();
            if (spark == this || spark.areIncomingTransfersDone() || atile == null || atile.isFull() || ((upgr != 0 || supgr != 2) && (upgr != 3 || (supgr != 0 && supgr != 1)) && atile instanceof vazkii.botania.api.mana.IManaPool))
                removals.add(e);
        }
        if (!removals.isEmpty())
            this.transfers.removeAll(removals);
        return this.transfers;
    }

    private boolean hasTransfer(ISparkEntity entity) {
        return this.transfers.contains(entity);
    }

    public void registerTransfer(ISparkEntity entity) {
        if (hasTransfer(entity))
            return;
        this.transfers.add(entity);
    }

    public int getUpgrade() {
        return this.dataWatcher.getWatchableObjectInt(28);
    }

    public void setUpgrade(int upgrade) {
        this.dataWatcher.updateObject(28, Integer.valueOf(upgrade));
    }

    public boolean areIncomingTransfersDone() {
        ISparkAttachable tile = getAttachedTile();
        if (tile instanceof vazkii.botania.api.mana.IManaPool)
            return (this.removeTransferants > 0);
        return (tile != null && tile.areIncomingTranfersDone());
    }

    abstract public String getName();
}
