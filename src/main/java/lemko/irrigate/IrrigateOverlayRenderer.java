package lemko.irrigate;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Matrix4f;

public class IrrigateOverlayRenderer {

    private static final float OVERLAY_Y_OFFSET = 0.003f;

    private static final float HYD_R = 0.3f;
    private static final float HYD_G = 0.6f;
    private static final float HYD_B = 1.0f;
    private static final float HYD_A = 0.3f;

    private static final float DRY_R = 1.0f;
    private static final float DRY_G = 0.2f;
    private static final float DRY_B = 0.2f;
    private static final float DRY_A = 0.3f;

    private static final RenderPipeline IRRIGATE_PIPELINE = RenderPipeline.builder(
                    RenderPipelines.POSITION_COLOR_SNIPPET
            )
            .withLocation(Identifier.of("irrigate", "irrigate_overlay"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .build();

    private static final RenderLayer IRRIGATE_LAYER = RenderLayer.of(
            "irrigate_overlay",
            1536,
            false,
            true,
            IRRIGATE_PIPELINE,
            RenderLayer.MultiPhaseParameters.builder().build(false)
    );

    public static void register() {
        WorldRenderEvents.LAST.register(context -> {
            if (!IrrigateClient.overlayEnabled) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null) return;

            World world = client.world;
            BlockPos playerPos = client.player.getBlockPos();

            MatrixStack matrices = context.matrixStack();
            if (matrices == null) return;

            var camera = context.camera().getPos();

            matrices.push();
            matrices.translate(-camera.x, -camera.y, -camera.z);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            int radius = 24;
            boolean anyVertices = false;

            for (BlockPos pos : BlockPos.iterate(
                    playerPos.add(-radius, -4, -radius),
                    playerPos.add(radius, 4, radius)
            )) {
                if (!world.getBlockState(pos).isOf(Blocks.FARMLAND)) continue;

                boolean canIrrigate = hasNearbyWater(world, pos);
                float r = canIrrigate ? HYD_R : DRY_R;
                float g = canIrrigate ? HYD_G : DRY_G;
                float b = canIrrigate ? HYD_B : DRY_B;
                float a = canIrrigate ? HYD_A : DRY_A;

                float x0 = pos.getX();
                float x1 = x0 + 1.0f;
                float y  = pos.getY() + 1.0f + OVERLAY_Y_OFFSET;
                float z0 = pos.getZ();
                float z1 = z0 + 1.0f;

                buffer.vertex(matrix, x0, y, z0).color(r, g, b, a);
                buffer.vertex(matrix, x0, y, z1).color(r, g, b, a);
                buffer.vertex(matrix, x1, y, z1).color(r, g, b, a);
                buffer.vertex(matrix, x1, y, z0).color(r, g, b, a);
                anyVertices = true;
            }

            if (anyVertices) {
                IRRIGATE_LAYER.draw(buffer.end());
            }

            matrices.pop();
        });
    }

    private static boolean hasNearbyWater(World world, BlockPos farmlandPos) {
        for (BlockPos check : BlockPos.iterate(
                farmlandPos.add(-4, 0, -4),
                farmlandPos.add(4, 1, 4)
        )) {
            if (world.getFluidState(check).getFluid() == Fluids.WATER ||
                    world.getFluidState(check).getFluid() == Fluids.FLOWING_WATER) {
                return true;
            }
        }
        return false;
    }
}