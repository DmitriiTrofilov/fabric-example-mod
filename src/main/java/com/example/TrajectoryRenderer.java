package com.dmitrofnet.trajectory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

public class TrajectoryRenderer {

    public static void render(MatrixStack matrices, float tickDelta, Camera camera) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;

        if (player == null || player.getEntityWorld() == null) return;

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            stack = player.getOffHandStack();
        }

        if (!isProjectile(stack)) return;

        // Get the BufferBuilder for lines
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getLines());

        // Physics variables
        float speed = 0f;
        float gravity = 0.05f;
        float drag = 0.99f;

        Item item = stack.getItem();
        if (item instanceof BowItem) {
            float pull = (float) (stack.getMaxUseTime(player) - player.getItemUseTimeLeft()) / 20.0F;
            pull = (pull * pull + pull * 2.0F) / 3.0F;
            if (pull > 1.0F) pull = 1.0F;
            
            // If not using item (holding charged), assume full power
            if (player.getItemUseTimeLeft() == 0) pull = 1.0F;
            else if (pull < 0.1F) return; // Not pulled enough

            speed = pull * 3.0F;
        } else if (item instanceof CrossbowItem) {
            if (!CrossbowItem.isCharged(stack)) return;
            speed = 3.15F;
        } else if (item instanceof EnderPearlItem || item instanceof SnowballItem || item instanceof EggItem) {
            speed = 1.5F;
            gravity = 0.03F;
        } else if (item instanceof TridentItem) {
            speed = 2.5F;
        }

        // Calculate Start
        Vec3d startPos = player.getCameraPosVec(tickDelta);
        float pitch = player.getPitch(tickDelta);
        float yaw = player.getYaw(tickDelta);

        // Vector Math
        float f = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        float g = -MathHelper.sin(pitch * 0.017453292F);
        float h = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        Vec3d motion = new Vec3d(f, g, h).normalize().multiply(speed);

        Vec3d pos = startPos;
        Vec3d camPos = camera.getPos();
        
        // Use the MatrixStack passed from WorldRenderer to transform vertices
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        // Simulation Loop
        for (int i = 0; i < 500; i++) {
            Vec3d prevPos = pos;

            // Physics Update
            pos = pos.add(motion);
            motion = motion.multiply(drag);
            motion = motion.add(0, -gravity, 0);

            // Collision Check
            RaycastContext context = new RaycastContext(
                prevPos, pos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
            );
            
            // Note: getEntityWorld() is the new method name in 1.21.10
            HitResult hit = player.getEntityWorld().raycast(context);
            boolean didHit = hit.getType() != HitResult.Type.MISS;

            if (didHit) {
                pos = hit.getPos();
            }

            // Render Line Segment
            // Shift coordinates relative to camera for rendering
            float x1 = (float) (prevPos.x - camPos.x);
            float y1 = (float) (prevPos.y - camPos.y);
            float z1 = (float) (prevPos.z - camPos.z);
            float x2 = (float) (pos.x - camPos.x);
            float y2 = (float) (pos.y - camPos.y);
            float z2 = (float) (pos.z - camPos.z);

            // Color: Green normally, Red on impact
            float r = didHit ? 1.0f : 0.0f;
            float g = didHit ? 0.0f : 1.0f;

            buffer.vertex(positionMatrix, x1, y1, z1)
                  .color(r, g, 0.0f, 1.0f)
                  .normal(1, 0, 0)
                  .next();

            buffer.vertex(positionMatrix, x2, y2, z2)
                  .color(r, g, 0.0f, 1.0f)
                  .normal(1, 0, 0)
                  .next();

            if (didHit) break;
        }
    }

    private static boolean isProjectile(ItemStack stack) {
        return stack.getItem() instanceof BowItem ||
               stack.getItem() instanceof CrossbowItem ||
               stack.getItem() instanceof EnderPearlItem ||
               stack.getItem() instanceof SnowballItem ||
               stack.getItem() instanceof EggItem ||
               stack.getItem() instanceof TridentItem;
    }
}