package foxiwhitee.hellmod.integration.draconic.entity;

import foxiwhitee.hellmod.integration.draconic.items.ItemArialHeart;
import foxiwhitee.hellmod.integration.draconic.items.ItemChaoticHeart;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemExpireEvent;

public class EntityHeart extends EntityItem {
    //Class taken from Draconic Evolution

    public EntityHeart(World par1World, double par2, double par4, double par6) {
        super(par1World, par2, par4, par6);
        this.isImmuneToFire = true;
        this.lifespan = 72000;
    }

    public EntityHeart(World par1World, double par2, double par4, double par6, ItemStack par8ItemStack) {
        this(par1World, par2, par4, par6);
        this.setEntityItemStack(par8ItemStack);
        this.lifespan = 72000;
    }

    public EntityHeart(World par1World) {
        super(par1World);
        this.isImmuneToFire = true;
        this.lifespan = 72000;
    }

    public EntityHeart(World world, Entity original, ItemStack stack) {
        this(world, original.posX, original.posY, original.posZ);
        if (original instanceof EntityItem) {
            this.delayBeforeCanPickup = ((EntityItem)original).delayBeforeCanPickup;
        } else {
            this.delayBeforeCanPickup = 20;
        }

        this.motionX = original.motionX;
        this.motionY = original.motionY;
        this.motionZ = original.motionZ;
        this.setEntityItemStack(stack);
        this.lifespan = 72000;
    }

    public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
        if (!worldObj.isRemote) {
            if (this.getEntityItem().getItem() instanceof ItemChaoticHeart && par1DamageSource.isExplosion() && par2 > 10.0F && !this.isDead) {
                this.setDead();
                this.worldObj.spawnEntityInWorld(new EntityChaoticHeart(this.worldObj, this.posX, this.posY, this.posZ));
            }
            if (this.getEntityItem().getItem() instanceof ItemArialHeart && par1DamageSource.isExplosion() && par2 > 10.0F && !this.isDead) {
                this.setDead();
                this.worldObj.spawnEntityInWorld(new EntityArialHeart(this.worldObj, this.posX, this.posY, this.posZ));
            }

        }
        return true;
    }

    public boolean isInRangeToRenderDist(double p_70112_1_) {
        double d1 = this.boundingBox.getAverageEdgeLength();
        d1 *= 256.0D;
        return p_70112_1_ < d1 * d1;
    }

    public boolean isInRangeToRender3d(double p_145770_1_, double p_145770_3_, double p_145770_5_) {
        return super.isInRangeToRender3d(p_145770_1_, p_145770_3_, p_145770_5_);
    }

    public void onUpdate() {
        if (this.age + 10 >= this.lifespan) {
            this.age = 0;
        }

        boolean flag2 = false;
        if (this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY - 1.0D), MathHelper.floor_double(this.posZ)) == Blocks.end_portal) {
            flag2 = true;
        }

        ItemStack stack = this.getDataWatcher().getWatchableObjectItemStack(10);
        if (stack == null || stack.getItem() == null || !stack.getItem().onEntityItemUpdate(this)) {
            if (this.getEntityItem() == null) {
                this.setDead();
            } else {
                super.onEntityUpdate();
                if (this.delayBeforeCanPickup > 0) {
                    --this.delayBeforeCanPickup;
                }

                this.prevPosX = this.posX;
                this.prevPosY = this.posY;
                this.prevPosZ = this.posZ;
                this.motionY -= 0.03999999910593033D;
                if (flag2) {
                    this.motionX = 0.0D;
                    this.motionY = 0.0D;
                    this.motionZ = 0.0D;
                }

                this.noClip = this.func_145771_j(this.posX, (this.boundingBox.minY + this.boundingBox.maxY) / 2.0D, this.posZ);
                this.moveEntity(this.motionX, this.motionY, this.motionZ);
                boolean flag = (int)this.prevPosX != (int)this.posX || (int)this.prevPosY != (int)this.posY || (int)this.prevPosZ != (int)this.posZ;
                if ((flag || this.ticksExisted % 25 == 0) && this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ)).getMaterial() == Material.lava) {
                    this.motionY = 0.20000000298023224D;
                    this.motionX = (double)((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
                    this.motionZ = (double)((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
                    this.playSound("random.fizz", 0.4F, 2.0F + this.rand.nextFloat() * 0.4F);
                }

                float f = 0.98F;
                if (this.onGround) {
                    f = this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.boundingBox.minY) - 1, MathHelper.floor_double(this.posZ)).slipperiness * 0.98F;
                }

                this.motionX *= (double)f;
                this.motionY *= 0.9800000190734863D;
                this.motionZ *= (double)f;
                if (flag2) {
                    this.motionX = 0.0D;
                    this.motionY = 0.0D;
                    this.motionZ = 0.0D;
                }

                if (this.onGround) {
                    this.motionY *= -0.5D;
                }

                ++this.age;
                ItemStack item = this.getDataWatcher().getWatchableObjectItemStack(10);
                if (!this.worldObj.isRemote && this.age >= this.lifespan) {
                    if (item != null) {
                        ItemExpireEvent event = new ItemExpireEvent(this, item.getItem() == null ? 6000 : item.getItem().getEntityLifespan(item, this.worldObj));
                        if (MinecraftForge.EVENT_BUS.post(event)) {
                            this.lifespan += event.extraLife;
                        } else {
                            this.setDead();
                        }
                    } else {
                        this.setDead();
                    }
                }

                if (item != null && item.stackSize <= 0) {
                    this.setDead();
                }
            }
        }
    }
}

