package com.mpp.hiddenmaterials.crafttweaker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mpp.hiddenmaterials.stage.MaterialStageRules;

import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.Material;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * CraftTweaker 接口：{@code mods.hiddenmaterials.MaterialStages}
 *
 * <p>用法（.zs 脚本）：
 * <pre>
 * // 拥有 stage_iron 之后，“材料与您”里才会出现铁的材料页
 * mods.hiddenmaterials.MaterialStages.require("tconstruct:iron", "stage_iron");
 *
 * // 需要同时拥有两个阶段
 * mods.hiddenmaterials.MaterialStages.requireAll("cobalt", ["stage_nether", "stage_smeltery"]);
 *
 * // 任意一个阶段即可
 * mods.hiddenmaterials.MaterialStages.requireAny("manyullyn", "stage_nether", "stage_end");
 *
 * // 取消 / 清空
 * mods.hiddenmaterials.MaterialStages.remove("cobalt");
 * mods.hiddenmaterials.MaterialStages.clear();
 *
 * // 查有哪些材料名可以写（print 只接受字符串，数组需要遍历）
 * for (name in mods.hiddenmaterials.MaterialStages.getAllMaterialNames()) {
 *     print(name);
 * }
 * print(mods.hiddenmaterials.MaterialStages.getRules());
 * </pre>
 *
 * <p>脚本会在客户端与服务端都执行：服务端那份用于数据留存/其它模组读取，客户端那份决定书本内容。
 */
@ZenRegister
@ZenClass("mods.hiddenmaterials.MaterialStages")
@ModOnly("hiddenmaterials")
public class ZenMaterialStages
{
    private static final Logger LOG = LogManager.getLogger("hiddenmaterials");

    private ZenMaterialStages()
    {
    }

    /**
     * 解锁单个阶段后显示该材料。
     *
     * @param material 材料标识，例如 {@code iron}、{@code tconstruct:iron}、{@code plustic:alumite}
     * @param stage    Game Stages 阶段名
     */
    @ZenMethod
    public static void require(String material, String stage)
    {
        requireAll(material, stage);
    }

    /**
     * 需要同时拥有全部阶段，材料才会出现在“材料与您”里。
     */
    @ZenMethod
    public static void requireAll(String material, String... stages)
    {
        set(material, true, stages);
    }

    /**
     * 只要拥有其中任意一个阶段，材料就会出现在“材料与您”里。
     */
    @ZenMethod
    public static void requireAny(String material, String... stages)
    {
        set(material, false, stages);
    }

    /** 移除某个材料的规则。 */
    @ZenMethod
    public static void remove(String material)
    {
        if (MaterialStageRules.remove(material))
        {
            LOG.info("Hidden Materials: removed stage rule for material {}", material);
        }
    }

    /** 清空所有规则。 */
    @ZenMethod
    public static void clear()
    {
        MaterialStageRules.clear();
        LOG.info("Hidden Materials: cleared all material stage rules");
    }

    /**
     * 当前所有材料标识（来自 {@code TinkerRegistry}），方便在脚本里查名字：
     * <pre>
     * for name in mods.hiddenmaterials.MaterialStages.getAllMaterialNames() {
     *     print(name);
     * }
     * </pre>
     * 注意：脚本执行时（INITIALIZATION）材料已经注册完毕。
     */
    @ZenMethod
    public static String[] getAllMaterialNames()
    {
        List<String> names = new ArrayList<String>();
        for (Material material : TinkerRegistry.getAllMaterials())
        {
            names.add(material.getIdentifier());
        }

        Collections.sort(names);
        return names.toArray(new String[names.size()]);
    }

    /**
     * 当前规则的可读快照（每行一条，没有规则时返回 {@code "(no rules)"}）。
     * 返回单个字符串是为了能直接 {@code print(...)}；CraftTweaker 的 print 只接受字符串。
     */
    @ZenMethod
    public static String getRules()
    {
        List<MaterialStageRules.Rule> rules = MaterialStageRules.all();
        if (rules.isEmpty())
        {
            return "(no rules)";
        }

        StringBuilder text = new StringBuilder();
        for (MaterialStageRules.Rule rule : rules)
        {
            if (text.length() > 0)
            {
                text.append('\n');
            }

            text.append(rule.toString());
        }

        return text.toString();
    }

    private static void set(String material, boolean requireAll, String... stages)
    {
        if (material == null || material.trim().isEmpty())
        {
            throw new IllegalArgumentException("material name must not be empty");
        }

        if (stages == null || stages.length == 0 || isAllBlank(stages))
        {
            LOG.warn("Hidden Materials: no stage given for material {}, rule ignored", material);
            return;
        }

        MaterialStageRules.set(material, requireAll, java.util.Arrays.asList(stages));

        if (!materialExists(material))
        {
            LOG.warn("Hidden Materials: no Tinkers material matches '{}' (rule kept, but it will never match). "
                    + "Use mods.hiddenmaterials.MaterialStages.getAllMaterialNames() to list valid names.", material);
        }
        else
        {
            LOG.info("Hidden Materials: material {} is now visible with {} {}",
                    material, requireAll ? "all of" : "any of", java.util.Arrays.toString(stages));
        }
    }

    private static boolean isAllBlank(String[] stages)
    {
        for (String stage : stages)
        {
            if (stage != null && !stage.trim().isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    /** 材料名是否真的存在，匹配规则与 {@link MaterialStageRules#find(String)} 保持一致。 */
    private static boolean materialExists(String name)
    {
        String wanted = name.trim().toLowerCase(Locale.ROOT);
        int split = wanted.indexOf(':');
        String stripped = split > 0 ? wanted.substring(split + 1) : wanted;

        for (Material material : TinkerRegistry.getAllMaterials())
        {
            String id = material.getIdentifier().toLowerCase(Locale.ROOT);
            if (id.equals(wanted))
            {
                return true;
            }

            if (split > 0)
            {
                if (id.equals(stripped))
                {
                    return true;
                }
            }
            else if (id.equals("tconstruct:" + wanted))
            {
                return true;
            }
        }

        return false;
    }
}
