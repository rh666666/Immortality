package com.adoleiiiiii.immortality;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * 客户端侧入口。
 */
@Mod(value = Immortality.MODID, dist = Dist.CLIENT)
public class ImmortalityClient {

    public ImmortalityClient(ModContainer container) {
        // 注册配置界面 TODO: 待补充配置项后启用
        // container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
