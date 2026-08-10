package dev.karn.karnmining.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.karn.karnmining.gui.KarnMiningConfigScreen;

public final class KarnMiningModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return KarnMiningConfigScreen::new;
    }
}
