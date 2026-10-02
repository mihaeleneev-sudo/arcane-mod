package com.arcane.client;

import com.arcane.ArcaneMod;
import com.arcane.Packets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class ArcaneClient implements ClientModInitializer {
    public static final EntityModelLayer SAIR_LAYER =
            new EntityModelLayer(new Identifier(ArcaneMod.MOD_ID, "sair_costume"), "main");
    public static final EntityModelLayer EIRO_LAYER =
            new EntityModelLayer(new Identifier(ArcaneMod.MOD_ID, "eiro_overlay"), "main");
    // варианты для скинов с тонкими руками (Alex)
    public static final EntityModelLayer SAIR_SLIM_LAYER =
            new EntityModelLayer(new Identifier(ArcaneMod.MOD_ID, "sair_costume_slim"), "main");
    public static final EntityModelLayer EIRO_SLIM_LAYER =
            new EntityModelLayer(new Identifier(ArcaneMod.MOD_ID, "eiro_overlay_slim"), "main");

    private static KeyBinding transformKey;
    private static KeyBinding abilityKey;

    @Override
    public void onInitializeClient() {
        // Клавиша превращения (по умолчанию левый Alt, меняется в настройках управления)
        transformKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arcane.transform", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.categories.arcane"));

        // Клавиша способности: бросок блока (по умолчанию R)
        abilityKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arcane.ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.arcane"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (abilityKey.wasPressed()) {
                if (client.player != null) {
                    ClientPlayNetworking.send(Packets.ABILITY, PacketByteBufs.create());
                }
            }
            while (transformKey.wasPressed()) {
                if (client.player != null) {
                    ClientPlayNetworking.send(Packets.TOGGLE, PacketByteBufs.create());
                }
            }
            VoiceHud.tick();
        });

        // Данные от сервера
        ClientPlayNetworking.registerGlobalReceiver(Packets.SYNC, (client, handler, buf, sender) -> {
            UUID id = buf.readUuid();
            int form = buf.readInt();
            client.execute(() -> ClientForms.set(id, form));
        });
        ClientPlayNetworking.registerGlobalReceiver(Packets.VOICE, (client, handler, buf, sender) -> {
            int index = buf.readInt();
            client.execute(() -> VoiceHud.show(index));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientForms.clear());

        // Модели костюма и кожи
        EntityModelLayerRegistry.registerModelLayer(SAIR_LAYER, () -> createSairLayer(false));
        EntityModelLayerRegistry.registerModelLayer(SAIR_SLIM_LAYER, () -> createSairLayer(true));
        EntityModelLayerRegistry.registerModelLayer(EIRO_LAYER, () -> createEiroLayer(false));
        EntityModelLayerRegistry.registerModelLayer(EIRO_SLIM_LAYER, () -> createEiroLayer(true));

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof PlayerEntityRenderer pr) {
                helper.register(new FormFeatureRenderer(pr, context.getModelLoader()));
            }
        });

        HudRenderCallback.EVENT.register(VoiceHud::render);
    }

    /** Костюм Саира: "одежда" чуть больше тела + высокая плетёная шляпа вместо головы. Текстура 128x64. */
    private static TexturedModelData createSairLayer(boolean slim) {
        ModelData data = PlayerEntityModel.getTexturedModelData(new Dilation(0.3f), slim);
        data.getRoot().addChild("hat",
                ModelPartBuilder.create().uv(64, 0)
                        .cuboid(-5.0f, -11.0f, -5.0f, 10.0f, 12.0f, 10.0f, new Dilation(0.0f)),
                ModelTransform.NONE);
        return TexturedModelData.of(data, 128, 64);
    }

    /** Кожа Эйро: тонкий слой почти вплотную к телу. Текстура 64x64. */
    private static TexturedModelData createEiroLayer(boolean slim) {
        ModelData data = PlayerEntityModel.getTexturedModelData(new Dilation(0.08f), slim);
        return TexturedModelData.of(data, 64, 64);
    }
}
