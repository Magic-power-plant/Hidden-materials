package com.mpp.hiddenmaterials.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mpp.hiddenmaterials.stage.MaterialStageRules;

import net.darkhax.gamestages.GameStageHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import slimeknights.tconstruct.library.materials.Material;

/**
 * 客户端书本过滤：把当前玩家还没有解锁阶段的材料从生成的材料列表里剔除。
 *
 * <p>“材料与您”（{@code tconstruct:book}）里的材料页是客户端打开书时按
 * {@code TinkerRegistry.getAllMaterials()} 生成的，所以只需要在客户端判断即可；
 * 阶段数据由 Game Stages 同步到客户端，用 {@link GameStageHelper#getPlayerData(EntityPlayer)} 读取。
 */
@SideOnly(Side.CLIENT)
public final class MaterialBookVisibility
{
    private static final Logger LOG = LogManager.getLogger("hiddenmaterials");

    private MaterialBookVisibility()
    {
    }

    /**
     * 过滤材料列表：没有配置规则、或拿不到玩家时原样返回，避免误伤。
     * 万一过滤过程出错也只记录日志并放行全部材料——绝不能因为显示逻辑把书弄崩。
     */
    public static Collection<Material> filter(Collection<Material> materials)
    {
        if (materials == null || materials.isEmpty() || MaterialStageRules.isEmpty())
        {
            return materials;
        }

        try
        {
            Collection<String> stages = currentStages();
            if (stages == null)
            {
                return materials;
            }

            List<Material> visible = new ArrayList<Material>(materials.size());
            for (Material material : materials)
            {
                if (MaterialStageRules.isVisible(material.getIdentifier(), stages))
                {
                    visible.add(material);
                }
                else if (LOG.isDebugEnabled())
                {
                    LOG.debug("Hiding Tinkers material {} from the book for the current player", material.getIdentifier());
                }
            }

            return visible;
        }
        catch (Exception exception)
        {
            LOG.error("Hidden Materials: could not filter Tinkers book materials, leaving them all visible", exception);
            return materials;
        }
    }

    /**
     * 当前书本内容的“签名”：规则版本 + 玩家已解锁阶段。
     * 签名变化说明材料页需要重建，见 {@link BookOpenHook}。
     */
    public static String signature()
    {
        StringBuilder signature = new StringBuilder();
        signature.append(MaterialStageRules.revision()).append('|').append(MaterialStageRules.size());

        Collection<String> stages = currentStages();
        if (stages != null)
        {
            List<String> sorted = new ArrayList<String>(stages);
            Collections.sort(sorted);
            for (String stage : sorted)
            {
                signature.append(',').append(stage);
            }
        }

        return signature.toString();
    }

    /** 当前客户端玩家的阶段集合；拿不到玩家时返回 null。 */
    private static Collection<String> currentStages()
    {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null)
        {
            return null;
        }

        return GameStageHelper.getPlayerData(player).getStages();
    }
}
