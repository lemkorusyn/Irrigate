package lemko.irrigate;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class IrrigateClient implements ClientModInitializer {

    public static boolean overlayEnabled = false;

    @Override
    public void onInitializeClient() {
        IrrigateKeybinds.register();
        IrrigateOverlayRenderer.register();

        // Check keybind every tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (IrrigateKeybinds.toggleOverlay.wasPressed()) {
                overlayEnabled = !overlayEnabled;
            }
        });
    }
}
