package net.citizensnpcs.nms.v1_16_R3.entity;

import org.bukkit.Bukkit;
import net.minecraft.server.v1_16_R3.EntityMinecartAbstract;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftLlama;
import org.bukkit.entity.Llama;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.event.NPCEnderTeleportEvent;
import net.citizensnpcs.api.event.NPCPushEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_16_R3.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.trait.HorseModifiers;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.Util;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.DamageSource;
import net.minecraft.server.v1_16_R3.DataWatcherObject;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.EntityBoat;
import net.minecraft.server.v1_16_R3.EntityLlama;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.SoundEffect;
import net.minecraft.server.v1_16_R3.Vec3D;
import net.minecraft.server.v1_16_R3.World;

public class LlamaController extends MobEntityController {
    public LlamaController() {
        super(EntityLlamaNPC.class);
    }

    @Override
    public Llama getBukkitEntity() {
        return (Llama) super.getBukkitEntity();
    }

    @Override
    public void spawn(Location at, NPC npc) {
        npc.getOrAddTrait(HorseModifiers.class);
        super.spawn(at, npc);
    }

    public static class EntityLlamaNPC extends EntityLlama implements NPCHolder {
        boolean calledNMSHeight = false;
        private final CitizensNPC npc;

        public EntityLlamaNPC(EntityTypes<? extends EntityLlama> types, World world) {
            this(types, world, null);
        }

        public EntityLlamaNPC(EntityTypes<? extends EntityLlama> types, World world, NPC npc) {
            super(types, world);
            this.npc = (CitizensNPC) npc;
            if (npc != null) {
                NMSImpl.clearGoals(npc, goalSelector, targetSelector);
                ((Llama) getBukkitEntity()).setDomestication(((Llama) getBukkitEntity()).getMaxDomestication());
            }
        }

        @Override
        public void a(DataWatcherObject<?> datawatcherobject) {
            if (npc != null && !calledNMSHeight) {
                calledNMSHeight = true;
                NMSImpl.checkAndUpdateHeight(this, datawatcherobject);
                calledNMSHeight = false;
            }

            super.a(datawatcherobject);
        }

        @Override
        protected void a(double d0, boolean flag, IBlockData block, BlockPosition blockposition) {
            if (npc == null || !npc.isFlyable()) {
                super.a(d0, flag, block, blockposition);
            }
        }

        @Override
        public boolean b(float f, float f1) {
            if (npc == null || !npc.isFlyable()) {
                return super.b(f, f1);
            }
            return false;
        }

        @Override
        public void checkDespawn() {
            if (npc == null) {
                super.checkDespawn();
            }
        }

        @Override
        public void collide(net.minecraft.server.v1_16_R3.Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.collide(entity);
            if (npc != null) {
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
            }
        }

        @Override
        public boolean d(NBTTagCompound save) {
            return npc == null ? super.d(save) : false;
        }

        @Override
        public void g(Vec3D vec3d) {
            if (npc == null || !npc.isFlyable()) {
                super.g(vec3d);
            } else {
                NMSImpl.flyingMoveLogic(this, vec3d);
            }
        }

        @Override
        public void enderTeleportTo(double d0, double d1, double d2) {
            if (npc == null) {
                super.enderTeleportTo(d0, d1, d2);
                return;
            }
            NPCEnderTeleportEvent event = new NPCEnderTeleportEvent(npc);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                super.enderTeleportTo(d0, d1, d2);
            }
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(super.getBukkitEntity() instanceof NPCHolder)) {
                NMSImpl.setBukkitEntity(this, new LlamaNPC(this));
            }
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        protected SoundEffect getSoundAmbient() {
            return NMSImpl.getSoundEffect(npc, super.getSoundAmbient(), NPC.AMBIENT_SOUND_METADATA);
        }

        @Override
        protected SoundEffect getSoundDeath() {
            return NMSImpl.getSoundEffect(npc, super.getSoundDeath(), NPC.DEATH_SOUND_METADATA);
        }

        @Override
        protected SoundEffect getSoundHurt(DamageSource damagesource) {
            return NMSImpl.getSoundEffect(npc, super.getSoundHurt(damagesource), NPC.HURT_SOUND_METADATA);
        }

        @Override
        public void i(double x, double y, double z) {
            if (npc == null) {
                super.i(x, y, z);
                return;
            }
            if (NPCPushEvent.getHandlerList().getRegisteredListeners().length == 0) {
                if (!npc.data().get(NPC.DEFAULT_PROTECTED_METADATA, true))
                    super.i(x, y, z);
                return;
            }
            Vector vector = new Vector(x, y, z);
            NPCPushEvent event = Util.callPushEvent(npc, vector);
            if (!event.isCancelled()) {
                vector = event.getCollisionVector();
                super.i(vector.getX(), vector.getY(), vector.getZ());
            }
            // when another entity collides, this method is called to push the
            // NPC so we prevent it from doing anything if the event is
            // cancelled.
        }

        @Override
        public boolean isClimbing() {
            if (npc == null || !npc.isFlyable()) {
                return super.isClimbing();
            } else {
                return false;
            }
        }

        @Override
        public boolean isLeashed() {
            if (npc == null)
                return super.isLeashed();
            boolean protectedDefault = npc.data().get(NPC.DEFAULT_PROTECTED_METADATA, true);
            if (!protectedDefault || !npc.data().get(NPC.LEASH_PROTECTED_METADATA, protectedDefault))
                return super.isLeashed();
            if (super.isLeashed()) {
                unleash(true, false); // clearLeash with client update
            }
            return false; // shouldLeash
        }

        @Override
        public void mobTick() {
            if (npc == null) {
                super.mobTick();
            } else {
                NMSImpl.updateMinecraftAIState(npc, this);
                if (npc.useMinecraftAI()) {
                    super.mobTick();
                }
                NMS.setStepHeight(getBukkitEntity(), 1);
                npc.update();
            }
        }

        @Override
        protected boolean n(Entity entity) {
            if (npc != null && (entity instanceof EntityBoat || entity instanceof EntityMinecartAbstract)) {
                return !npc.data().get(NPC.DEFAULT_PROTECTED_METADATA, true);
            }
            return super.n(entity);
        }
    }

    public static class LlamaNPC extends CraftLlama implements NPCHolder {
        private final CitizensNPC npc;

        public LlamaNPC(EntityLlamaNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
            this.npc = entity.npc;
        }

        @Override
        public NPC getNPC() {
            return npc;
        }
    }
}