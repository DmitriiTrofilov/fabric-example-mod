package com.dmitrofnet.trajectory.mixin;

import com.dmitrofnet.trajectory.TrajectoryRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(
        method = "render", 
        at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/client/render/WorldRenderer;renderWorldBorder(Lnet/minecraft/client/render/Camera;)V", 
            shift = At.Shift.BEFORE
        )
    )
    private void onRender(
        RenderTickCounter tickCounter, 
        boolean renderBlockOutline, 
        Camera camera, 
        GameRenderer gameRenderer, 
        LightmapTextureManager lightmapTextureManager, 
        Matrix4f positionMatrix, 
        Matrix4f projectionMatrix, 
        CallbackInfo ci
    ) {
        // Create a MatrixStack wrapping the position matrix provided by the renderer
        // In 1.21, we often need to manually construct the stack or use the matrix directly.
        // For simplicity in this specific hook, we can create a fresh stack and set it.
        // However, VertexConsumer accepts the Matrix4f directly in TrajectoryRenderer.
        
        // We create a temporary stack just to pass to our renderer structure
        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.multiplyPositionMatrix(positionMatrix);
        
        // Use the proper method to get delta time from RenderTickCounter
        float tickDelta = tickCounter.getTickDelta(true);

        TrajectoryRenderer.render(matrices, tickDelta, camera);
        
        matrices.pop();
    }
}