package io.github.farms4life2016.vixen_maid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.ClimbOnTopOfPowderSnowGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class Braixen extends Animal implements VariantHolder<Braixen.Type> {
    private static final EntityDataAccessor<Integer> DATA_TYPE_ID = SynchedEntityData.defineId(Braixen.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(Braixen.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<UUID>> DATA_TRUSTED_ID_0 = SynchedEntityData.defineId(Braixen.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_TRUSTED_ID_1 = SynchedEntityData.defineId(Braixen.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final byte EATING_PARTICLES_EVENT = -45;
    private static final byte EATING_ANIMATION_EVENT = -46;
    private static final int FLAG_DEFENDING = 1;
    private static final int MIN_TICKS_BEFORE_EAT = 200; // was 600
    private static final int EATING_ANIMATION_TICKS = 60;
    private static final int EATING_ARM_RAISE_TICKS = 10;
    private static final int EATING_CONSUME_TICK = 50;
    private static final int EATING_PARTICLE_INTERVAL_TICKS = 5;
    private static final int EATING_PARTICLES_SPAWNED = 8; // vanilla fox spawns 8 food particles
    private static final Vec3 DEFAULT_LEASH_OFFSET = new Vec3(0.0D, 26.0D / 16.0D, 2.0D / 16.0D);
    private static final Vec3 DEFAULT_MOUTH_OFFSET = new Vec3(0.0D, 29.0D / 16.0D, 7.0D / 16.0D);
    private static final Predicate<ItemEntity> ALLOWED_ITEMS = item -> !item.hasPickUpDelay() && item.isAlive();
    private static final Predicate<Entity> TRUSTED_TARGET_SELECTOR = entity -> entity instanceof LivingEntity livingEntity
            && livingEntity.getLastHurtMob() != null
            && livingEntity.getLastHurtMobTimestamp() < livingEntity.tickCount + 600;

    private final List<ResourceKey<DamageType>> immuneTo = List.of(DamageTypes.SWEET_BERRY_BUSH);
    private int ticksSinceEaten;
    private int eatingAnimationTicks = -1;
    private int pendingClientEatingParticles; // if you want to be 100% correct, then this should be a queue
    private ItemStack pendingClientEatingParticleItem = ItemStack.EMPTY;
    private float clientEatingAnimationStartTicks = -1.0F;
    private Vec3 clientLeashOffset = DEFAULT_LEASH_OFFSET;
    private Vec3 clientMouthOffset = DEFAULT_MOUTH_OFFSET;

    public Braixen(EntityType<? extends Braixen> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(PathType.DANGER_OTHER, 0.0F);
        this.setPathfindingMalus(PathType.DAMAGE_OTHER, 0.0F);
        this.setCanPickUpLoot(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TYPE_ID, Type.YELLOW.getId());
        builder.define(DATA_FLAGS_ID, (byte) 0);
        builder.define(DATA_TRUSTED_ID_0, Optional.empty());
        builder.define(DATA_TRUSTED_ID_1, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new BraixenFloatGoal());
        this.goalSelector.addGoal(0, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
        this.goalSelector.addGoal(2, new BraixenPanicGoal(1.25D));
        this.goalSelector.addGoal(7, new BraixenMeleeAttackGoal(1.2D, true));
        this.goalSelector.addGoal(8, new BraixenFollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(10, new BraixenEatBerriesGoal(1.2D, 12, 2));
        this.goalSelector.addGoal(11, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(11, new BraixenSearchForItemsGoal());
        this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.targetSelector.addGoal(3, new DefendTrustedTargetGoal(
                LivingEntity.class,
                false,
                false,
                target -> TRUSTED_TARGET_SELECTOR.test(target) && !this.trusts(target.getUUID())
        ));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3F)
                .add(Attributes.MAX_HEALTH, 20.0D)  // vanilla fox has 10hp, player has 20hp, tamed wolf has 40hp
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.SAFE_FALL_DISTANCE, 5.0D);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (spawnType == MobSpawnType.SPAWN_EGG) {
            this.setVariant(this.random.nextBoolean() ? Type.YELLOW : Type.LAVENDER);
        }

        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide && this.isAlive() && this.isEffectiveAi()) {
            this.ticksSinceEaten++;
            this.updateEatingAnimation();
        }

        super.aiStep();
        if (this.isDefending() && this.random.nextFloat() < 0.05F) {
            this.playSound(SoundEvents.FOX_AGGRO, 1.0F, 1.0F);
        }
    }

    private void updateEatingAnimation() {
        ItemStack heldItem = this.getItemBySlot(EquipmentSlot.MAINHAND);
        boolean canEat = this.canEat(heldItem);
        int eatingAnimationTicks = this.getEatingAnimationTicks();
        if (eatingAnimationTicks >= 0) {
            if (eatingAnimationTicks >= EATING_ANIMATION_TICKS) {
                this.stopEatingAnimation();
            } else if (eatingAnimationTicks < EATING_CONSUME_TICK && !canEat) {
                this.stopEatingAnimation();
            } else {
                if (canEat && eatingAnimationTicks >= EATING_ARM_RAISE_TICKS && eatingAnimationTicks < EATING_CONSUME_TICK
                        && (eatingAnimationTicks - EATING_ARM_RAISE_TICKS) % EATING_PARTICLE_INTERVAL_TICKS == 0) {
                    this.playSound(this.getEatingSound(heldItem), 1.0F, 1.0F);
                    this.level().broadcastEntityEvent(this, EATING_PARTICLES_EVENT);
                }

                if (canEat && eatingAnimationTicks == EATING_CONSUME_TICK) {
                    ItemStack remainingItem = heldItem.finishUsingItem(this.level(), this);
                    this.setItemSlot(EquipmentSlot.MAINHAND, remainingItem);
                    this.ticksSinceEaten = 0;
                }

                this.advanceEatingAnimation(eatingAnimationTicks);
            }
        } else if (canEat && this.ticksSinceEaten > MIN_TICKS_BEFORE_EAT - EATING_CONSUME_TICK) {
            this.startEatingAnimation();
        }
    }

    @Override
    protected boolean isImmobile() {
        return this.isDeadOrDying();
    }

    private boolean canEat(ItemStack stack) {
        return stack.has(DataComponents.FOOD) && this.getTarget() == null && this.onGround();
    }

    public float getEatingAnimationSeconds(float ageInTicks) {
        if (this.level().isClientSide) {
            if (this.clientEatingAnimationStartTicks < 0.0F) {
                return -1.0F;
            }

            float elapsedTicks = ageInTicks - this.clientEatingAnimationStartTicks;
            if (elapsedTicks < 0.0F || elapsedTicks > EATING_ANIMATION_TICKS) {
                return -1.0F;
            }
            return elapsedTicks / 20.0F;
        }

        int animationTicks = this.getEatingAnimationTicks();
        return animationTicks >= 0 && animationTicks <= EATING_ANIMATION_TICKS ? animationTicks / 20.0F : -1.0F;
    }

    private int getEatingAnimationTicks() {
        return this.eatingAnimationTicks;
    }

    private void startEatingAnimation() {
        this.eatingAnimationTicks = 0;
        this.level().broadcastEntityEvent(this, EATING_ANIMATION_EVENT);
    }

    private void advanceEatingAnimation(int eatingAnimationTicks) {
        this.eatingAnimationTicks = eatingAnimationTicks + 1;
    }

    private void stopEatingAnimation() {
        this.eatingAnimationTicks = -1;
    }

    void setClientAnchorOffsets(@Nullable Vec3 leashOffset, @Nullable Vec3 mouthOffset) {
        if (leashOffset != null) {
            this.clientLeashOffset = leashOffset;
        }
        if (mouthOffset != null) {
            this.clientMouthOffset = mouthOffset;
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EATING_PARTICLES_EVENT) {
            ItemStack heldItem = this.getItemBySlot(EquipmentSlot.MAINHAND);
            // cap the number of pending particles
            if (!heldItem.isEmpty() && this.pendingClientEatingParticles < 2 * EATING_PARTICLES_SPAWNED) {
                this.pendingClientEatingParticles += EATING_PARTICLES_SPAWNED;
                this.pendingClientEatingParticleItem = heldItem.copy();
            }
        } else if (id == EATING_ANIMATION_EVENT) {
            this.clientEatingAnimationStartTicks = this.tickCount;
        } else {
            super.handleEntityEvent(id);
        }
    }

    void spawnPendingClientEatingParticles() {
        if (this.pendingClientEatingParticles <= 0) {
            return;
        }

        ItemStack particleItem = this.pendingClientEatingParticleItem;
        if (particleItem.isEmpty()) {
            this.pendingClientEatingParticles = 0;
            return;
        }

        int particles = this.pendingClientEatingParticles;
        this.pendingClientEatingParticles = 0;
        this.pendingClientEatingParticleItem = ItemStack.EMPTY;
        Vec3 mouthPosition = this.getClientMouthPosition();
        for (int i = 0; i < particles; i++) {
            Vec3 motion = new Vec3(((double) this.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D)
                    .xRot(-this.getXRot() * (float) (Math.PI / 180.0D))
                    .yRot(-this.getYRot() * (float) (Math.PI / 180.0D));
            this.level().addParticle(
                    // note: both stored bursts of particles will be of `particleItem`, even if both items were different.
                    new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, particleItem),
                    mouthPosition.x,
                    mouthPosition.y,
                    mouthPosition.z,
                    motion.x,
                    motion.y + 0.05D,
                    motion.z
            );
        }
    }

    private Vec3 getClientMouthPosition() {
        return this.position().add(this.rotateClientOffset(this.clientMouthOffset, 1.0F));
    }

    private Vec3 rotateClientOffset(Vec3 offset, float partialTick) {
        double bodyRotation = (double) (this.getPreciseBodyRotation(partialTick) * (float) (Math.PI / 180.0D)) + (Math.PI / 2.0D);
        double x = Math.cos(bodyRotation) * offset.z + Math.sin(bodyRotation) * offset.x;
        double z = Math.sin(bodyRotation) * offset.z - Math.cos(bodyRotation) * offset.x;
        return new Vec3(x, offset.y, z);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return this.immuneTo.stream().anyMatch(source::is) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    /*
     * Use this function later when we add proper fox/braixen food and possibly breeding
     */
    public boolean isFutureBreedingFood(ItemStack stack) {
        return stack.is(ItemTags.FOX_FOOD);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
        /*
        Fox fox = EntityType.FOX.create(level);
        if (fox != null) {
            fox.setVariant(this.random.nextBoolean() ? this.getVariant() : ((Fox)otherParent).getVariant());
        }

        return fox;
         */
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        /*
        // declare this at the top of the class:
        // private static final EntityDimensions BABY_DIMENSIONS = EntityType.FOX.getDimensions().scale(0.5F).withEyeHeight(0.2975F);

        // then return dimensions if mob is a baby
        if (this.isBaby()) return BABY_DIMENSIONS;
         */
        return super.getDefaultDimensions(pose);
    }

    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        // temp code to get a braixen to "trust" you when you feed it a berry
        if (this.isFutureBreedingFood(itemstack) && !this.level().isClientSide) {
            this.usePlayerItem(player, hand, itemstack);
            this.addTrustedUUID(player.getUUID());
            this.level().broadcastEntityEvent(this, (byte)18); // 18 = love hearts
            return InteractionResult.SUCCESS;

        }

        return super.mobInteract(player, hand);
    }

    @Override
    protected void usePlayerItem(Player player, net.minecraft.world.InteractionHand hand, ItemStack stack) {
        if (this.isFutureBreedingFood(stack)) {
            this.playSound(this.getEatingSound(stack), 1.0F, 1.0F);
        }

        super.usePlayerItem(player, hand, stack);
    }

    @Override
    public Type getVariant() {
        return Type.byId(this.entityData.get(DATA_TYPE_ID));
    }

    @Override
    public void setVariant(Type variant) {
        this.entityData.set(DATA_TYPE_ID, variant.getId());
    }

    public List<UUID> getTrustedUUIDs() {
        List<UUID> trusted = new ArrayList<>(3);
        trusted.add(this.entityData.get(DATA_TRUSTED_ID_0).orElse(null));
        trusted.add(this.entityData.get(DATA_TRUSTED_ID_1).orElse(null));
        return trusted;
    }

    public void addTrustedUUID(@Nullable UUID uuid) {
        if (this.entityData.get(DATA_TRUSTED_ID_0).isPresent()) {
            this.entityData.set(DATA_TRUSTED_ID_1, Optional.ofNullable(uuid));
        } else {
            this.entityData.set(DATA_TRUSTED_ID_0, Optional.ofNullable(uuid));
        }
    }

    public boolean trusts(UUID uuid) {
        return this.getTrustedUUIDs().contains(uuid);
    }

    private boolean isDefending() {
        return this.getFlag(FLAG_DEFENDING);
    }

    private void setDefending(boolean defending) {
        this.setFlag(FLAG_DEFENDING, defending);
    }

    private void setFlag(int flagId, boolean value) {
        if (value) {
            this.entityData.set(DATA_FLAGS_ID, (byte) (this.entityData.get(DATA_FLAGS_ID) | flagId));
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte) (this.entityData.get(DATA_FLAGS_ID) & ~flagId));
        }
    }

    private boolean getFlag(int flagId) {
        return (this.entityData.get(DATA_FLAGS_ID) & flagId) != 0;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (this.isDefending() && target == null) {
            this.setDefending(false);
        }

        super.setTarget(target);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        ListTag trustedTags = new ListTag();

        for (UUID uuid : this.getTrustedUUIDs()) {
            if (uuid != null) {
                trustedTags.add(NbtUtils.createUUID(uuid));
            }
        }

        compound.put("Trusted", trustedTags);
        compound.putString("Type", this.getVariant().getSerializedName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        for (Tag tag : compound.getList("Trusted", 11)) {
            this.addTrustedUUID(NbtUtils.loadUUID(tag));
        }

        this.setVariant(Type.byName(compound.getString("Type")));
    }

    @Override
    public SoundEvent getEatingSound(ItemStack itemStack) {
        return SoundEvents.FOX_EAT;
    }

    @Override
    public void playAmbientSound() {
        SoundEvent soundEvent = this.getAmbientSound();
        if (soundEvent == SoundEvents.FOX_SCREECH) {
            this.playSound(soundEvent, 2.0F, this.getVoicePitch());
        } else {
            super.playAmbientSound();
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (!this.level().isDay() && this.random.nextFloat() < 0.1F) {
            List<Player> players = this.level().getEntitiesOfClass(
                    Player.class,
                    this.getBoundingBox().inflate(16.0D, 16.0D, 16.0D),
                    net.minecraft.world.entity.EntitySelector.NO_SPECTATORS
            );
            if (players.isEmpty()) {
                return SoundEvents.FOX_SCREECH;
            }
        }

        return SoundEvents.FOX_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.FOX_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public boolean canTakeItem(ItemStack itemStack) {
        EquipmentSlot slot = this.getEquipmentSlotForItem(itemStack);
        return this.getItemBySlot(slot).isEmpty() && slot == EquipmentSlot.MAINHAND && super.canTakeItem(itemStack);
    }

    @Override
    public boolean canHoldItem(ItemStack stack) {
        ItemStack heldItem = this.getItemBySlot(EquipmentSlot.MAINHAND);
        return heldItem.isEmpty() || this.ticksSinceEaten > 0 && stack.has(DataComponents.FOOD) && !heldItem.has(DataComponents.FOOD);
    }

    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack itemStack = itemEntity.getItem();
        if (this.canHoldItem(itemStack)) {
            int count = itemStack.getCount();
            if (count > 1) {
                this.dropItemStack(itemStack.split(count - 1));
            }

            this.spitOutItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
            this.onItemPickup(itemEntity);
            this.setItemSlot(EquipmentSlot.MAINHAND, itemStack.split(1));
            this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            this.take(itemEntity, itemStack.getCount());
            itemEntity.discard();
            this.ticksSinceEaten = 0;
        }
    }

    private void spitOutItem(ItemStack stack) {
        if (!stack.isEmpty() && !this.level().isClientSide) {
            ItemEntity itemEntity = new ItemEntity(
                    this.level(),
                    this.getX() + this.getLookAngle().x,
                    this.getY() + 1.0D,
                    this.getZ() + this.getLookAngle().z,
                    stack
            );
            itemEntity.setPickUpDelay(40);
            itemEntity.setThrower(this);
            this.playSound(SoundEvents.FOX_SPIT, 1.0F, 1.0F);
            this.level().addFreshEntity(itemEntity);
        }
    }

    private void dropItemStack(ItemStack stack) {
        this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), stack));
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        ItemStack heldItem = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!heldItem.isEmpty()) {
            this.spawnAtLocation(heldItem);
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    @Override
    public Vec3 getLeashOffset(float partialTick) {
        if (this.level().isClientSide) {
            return this.clientLeashOffset;
        }
        return this.getLeashOffset();
    }

    @Override
    protected Vec3 getLeashOffset() {
        return DEFAULT_LEASH_OFFSET;
    }

    class BraixenFloatGoal extends FloatGoal {
        BraixenFloatGoal() {
            super(Braixen.this);
        }

        @Override
        public boolean canUse() {
            return Braixen.this.isInWater() && Braixen.this.getFluidHeight(FluidTags.WATER) > 0.25D
                    || Braixen.this.isInLava()
                    || Braixen.this.isInFluidType((fluidType, height) -> Braixen.this.canSwimInFluidType(fluidType) && height > 0.25D);
        }
    }

    class BraixenPanicGoal extends PanicGoal {
        BraixenPanicGoal(double speedModifier) {
            super(Braixen.this, speedModifier);
        }

        @Override
        public boolean shouldPanic() {
            return !Braixen.this.isDefending() && super.shouldPanic();
        }
    }

    class BraixenMeleeAttackGoal extends MeleeAttackGoal {
        BraixenMeleeAttackGoal(double speedModifier, boolean followingTargetEvenIfNotSeen) {
            super(Braixen.this, speedModifier, followingTargetEvenIfNotSeen);
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (this.canPerformAttack(target)) {
                this.resetAttackCooldown();
                this.mob.doHurtTarget(target);
                Braixen.this.playSound(SoundEvents.FOX_BITE, 1.0F, 1.0F);
            }
        }
    }

    class BraixenFollowParentGoal extends FollowParentGoal {
        BraixenFollowParentGoal(Animal animal, double speedModifier) {
            super(animal, speedModifier);
        }

        @Override
        public boolean canUse() {
            return !Braixen.this.isDefending() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Braixen.this.isDefending() && super.canContinueToUse();
        }
    }

    public class BraixenEatBerriesGoal extends MoveToBlockGoal {
        private static final int WAIT_TICKS = 40;
        protected int ticksWaited;

        public BraixenEatBerriesGoal(double speedModifier, int searchRange, int verticalSearchRange) {
            super(Braixen.this, speedModifier, searchRange, verticalSearchRange);
        }

        @Override
        public double acceptedDistance() {
            return 2.0D;
        }

        @Override
        public boolean shouldRecalculatePath() {
            return this.tryTicks % 100 == 0;
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            return Braixen.this.isRipeBerryBlock(level.getBlockState(pos));
        }

        @Override
        public void tick() {
            if (this.isReachedTarget()) {
                if (this.ticksWaited >= WAIT_TICKS) {
                    this.onReachedTarget();
                } else {
                    this.ticksWaited++;
                }
            } else if (Braixen.this.random.nextFloat() < 0.05F) {
                Braixen.this.playSound(SoundEvents.FOX_SNIFF, 1.0F, 1.0F);
            }

            super.tick();
        }

        protected void onReachedTarget() {
            if (net.neoforged.neoforge.event.EventHooks.canEntityGrief(Braixen.this.level(), Braixen.this)) {
                Braixen.this.pickBerries(this.blockPos, Braixen.this.level().getBlockState(this.blockPos));
            }
        }

        @Override
        public void start() {
            this.ticksWaited = 0;
            super.start();
        }
    }

    private boolean isRipeBerryBlock(BlockState state) {
        return state.is(Blocks.SWEET_BERRY_BUSH) && state.getValue(SweetBerryBushBlock.AGE) >= 2 || CaveVines.hasGlowBerries(state);
    }

    private void pickBerries(BlockPos pos, BlockState state) {
        if (state.is(Blocks.SWEET_BERRY_BUSH)) {
            this.pickSweetBerries(pos, state);
        } else if (CaveVines.hasGlowBerries(state)) {
            CaveVines.use(this, state, this.level(), pos);
        }
    }

    private void pickSweetBerries(BlockPos pos, BlockState state) {
        int age = state.getValue(SweetBerryBushBlock.AGE);
        int berryCount = 1 + this.level().random.nextInt(2) + (age == 3 ? 1 : 0);
        ItemStack heldItem = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (heldItem.isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES));
            berryCount--;
        }

        if (berryCount > 0) {
            Block.popResource(this.level(), pos, new ItemStack(Items.SWEET_BERRIES, berryCount));
        }

        this.playSound(SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 1.0F, 1.0F);
        this.level().setBlock(pos, state.setValue(SweetBerryBushBlock.AGE, 1), 2);
        this.level().gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(this));
    }

    class BraixenSearchForItemsGoal extends Goal {
        BraixenSearchForItemsGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!Braixen.this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
                return false;
            } else if (Braixen.this.getTarget() != null || Braixen.this.getLastHurtByMob() != null) {
                return false;
            } else if (Braixen.this.getRandom().nextInt(reducedTickDelay(10)) != 0) {
                return false;
            }

            return !this.findNearbyItems().isEmpty();
        }

        @Override
        public void tick() {
            List<ItemEntity> items = this.findNearbyItems();
            if (Braixen.this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() && !items.isEmpty()) {
                Braixen.this.getNavigation().moveTo(items.getFirst(), 1.2D);
            }
        }

        @Override
        public void start() {
            List<ItemEntity> items = this.findNearbyItems();
            if (!items.isEmpty()) {
                Braixen.this.getNavigation().moveTo(items.getFirst(), 1.2D);
            }
        }

        private List<ItemEntity> findNearbyItems() {
            return Braixen.this.level().getEntitiesOfClass(
                    ItemEntity.class,
                    Braixen.this.getBoundingBox().inflate(8.0D, 8.0D, 8.0D),
                    ALLOWED_ITEMS
            );
        }
    }

    class DefendTrustedTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
        @Nullable
        private LivingEntity trustedLastHurtBy;
        @Nullable
        private LivingEntity trustedLastHurt;
        private int timestamp;

        DefendTrustedTargetGoal(Class<LivingEntity> targetType, boolean mustSee, boolean mustReach, @Nullable Predicate<LivingEntity> predicate) {
            super(Braixen.this, targetType, 10, mustSee, mustReach, predicate);
        }

        @Override
        public boolean canUse() {
            if (this.randomInterval > 0 && this.mob.getRandom().nextInt(this.randomInterval) != 0) {
                return false;
            }

            for (UUID uuid : Braixen.this.getTrustedUUIDs()) {
                if (uuid != null
                        && Braixen.this.level() instanceof ServerLevel serverLevel
                        && serverLevel.getEntity(uuid) instanceof LivingEntity livingEntity) {
                    this.trustedLastHurt = livingEntity;
                    this.trustedLastHurtBy = livingEntity.getLastHurtByMob();
                    int lastHurtByTimestamp = livingEntity.getLastHurtByMobTimestamp();
                    return lastHurtByTimestamp != this.timestamp && this.canAttack(this.trustedLastHurtBy, this.targetConditions);
                }
            }

            return false;
        }

        @Override
        public void start() {
            this.setTarget(this.trustedLastHurtBy);
            this.target = this.trustedLastHurtBy;
            if (this.trustedLastHurt != null) {
                this.timestamp = this.trustedLastHurt.getLastHurtByMobTimestamp();
            }

            Braixen.this.playSound(SoundEvents.FOX_AGGRO, 1.0F, 1.0F);
            Braixen.this.setDefending(true);
            super.start();
        }
    }

    public enum Type implements StringRepresentable {
        YELLOW(0, "yellow"),
        LAVENDER(1, "lavender");

        public static final StringRepresentable.EnumCodec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        private static final IntFunction<Type> BY_ID = ByIdMap.continuous(Type::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

        private final int id;
        private final String name;

        Type(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public int getId() {
            return this.id;
        }

        public static Type byName(String name) {
            return CODEC.byName(name, YELLOW);
        }

        public static Type byId(int id) {
            return BY_ID.apply(id);
        }
    }
}
