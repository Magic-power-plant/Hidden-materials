package com.mpp.hiddenmaterials.stage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 材料 -> Game Stages 规则表：整个模组唯一的一份“哪个材料需要哪个阶段”的数据。
 *
 * <p>这个类刻意不引用任何 Minecraft / Tinkers / CraftTweaker 类型，所以客户端与服务端都能安全加载：
 * <ul>
 *     <li>CraftTweaker 脚本（客户端与服务端都会执行）通过它写入规则；</li>
 *     <li>客户端的书本过滤通过它读取规则。</li>
 * </ul>
 *
 * <p>名字匹配规则（见 {@link #find(String)}）：
 * <ol>
 *     <li>先按材料标识精确匹配，例如 {@code iron}、{@code plustic:alumite}；</li>
 *     <li>{@code othermod:iron} 这样的带命名空间名字，也会去匹配 {@code iron}；</li>
 *     <li>反过来，写法 {@code tconstruct:iron} 也能匹配标识为 {@code iron} 的材料。</li>
 * </ol>
 * 比较材料名时忽略大小写，但阶段名与 Game Stages 保持一致（区分大小写）。
 */
public final class MaterialStageRules
{
    /** 省略命名空间时假定的命名空间，用来兼容 {@code tconstruct:iron} 这种写法。 */
    private static final String DEFAULT_NAMESPACE = "tconstruct";

    /** key = 规范化后的材料名（见 {@link #normalizeMaterial(String)}），value = 规则。 */
    private static final Map<String, Rule> RULES = new ConcurrentHashMap<String, Rule>();

    /** 每次规则变化都会 +1，客户端用它判断书的内容是否需要重建。 */
    private static final AtomicInteger REVISION = new AtomicInteger();

    private MaterialStageRules()
    {
    }

    /** 单个材料的阶段要求。不可变对象，可安全跨线程读取。 */
    public static final class Rule
    {
        private final String material;
        private final Set<String> stages;
        private final boolean requireAll;

        Rule(String material, Set<String> stages, boolean requireAll)
        {
            this.material = material;
            this.stages = Collections.unmodifiableSet(stages);
            this.requireAll = requireAll;
        }

        /** 规则里写的材料名（已规范化）。 */
        public String getMaterial()
        {
            return this.material;
        }

        /** 该材料需要的阶段。 */
        public Set<String> getStages()
        {
            return this.stages;
        }

        /** true = 需要全部阶段；false = 任意一个阶段即可。 */
        public boolean isRequireAll()
        {
            return this.requireAll;
        }

        /** 玩家（客户端同步过来的阶段集合）是否满足这条规则。 */
        public boolean isSatisfiedBy(Collection<String> playerStages)
        {
            if (playerStages == null || playerStages.isEmpty())
            {
                return false;
            }

            if (this.requireAll)
            {
                for (String stage : this.stages)
                {
                    if (!playerStages.contains(stage))
                    {
                        return false;
                    }
                }

                return true;
            }

            for (String stage : this.stages)
            {
                if (playerStages.contains(stage))
                {
                    return true;
                }
            }

            return false;
        }

        @Override
        public String toString()
        {
            return this.material + " -> " + (this.requireAll ? "all of " : "any of ") + this.stages;
        }
    }

    /**
     * 写入（或替换）一个材料的规则。同一个材料重复调用时，以最后一次为准。
     *
     * @param material   材料标识，例如 {@code iron} / {@code tconstruct:iron}
     * @param requireAll true = 需要全部阶段，false = 任意一个阶段
     * @param stages     阶段名；为空等同于 {@link #remove(String)}
     */
    public static void set(String material, boolean requireAll, Collection<String> stages)
    {
        String key = normalizeMaterial(material);
        if (key.isEmpty())
        {
            throw new IllegalArgumentException("material name must not be empty");
        }

        Set<String> cleaned = new LinkedHashSet<String>();
        if (stages != null)
        {
            for (String stage : stages)
            {
                if (stage == null)
                {
                    continue;
                }

                String trimmed = stage.trim();
                if (!trimmed.isEmpty())
                {
                    cleaned.add(trimmed);
                }
            }
        }

        if (cleaned.isEmpty())
        {
            remove(material);
            return;
        }

        RULES.put(key, new Rule(key, cleaned, requireAll));
        REVISION.incrementAndGet();
    }

    /**
     * 移除一个材料（及它的等价写法）的规则。
     *
     * @return 是否真的移除了规则
     */
    public static boolean remove(String material)
    {
        String key = normalizeMaterial(material);
        boolean removed = RULES.remove(key) != null;

        // "iron" 与 "tconstruct:iron" 视为同一个材料，两个写法都清掉。
        String alias = key.startsWith(DEFAULT_NAMESPACE + ":")
                ? key.substring(DEFAULT_NAMESPACE.length() + 1)
                : (key.indexOf(':') < 0 ? DEFAULT_NAMESPACE + ":" + key : null);

        if (alias != null && !alias.equals(key) && RULES.remove(alias) != null)
        {
            removed = true;
        }

        if (removed)
        {
            REVISION.incrementAndGet();
        }

        return removed;
    }

    /** 清空所有规则。 */
    public static void clear()
    {
        if (!RULES.isEmpty())
        {
            RULES.clear();
            REVISION.incrementAndGet();
        }
    }

    /** 当前规则数量。 */
    public static int size()
    {
        return RULES.size();
    }

    /** 是否一条规则都没有；没有任何规则时书本过滤完全不做额外工作。 */
    public static boolean isEmpty()
    {
        return RULES.isEmpty();
    }

    /** 规则版本号，每次增删改都会变化。 */
    public static int revision()
    {
        return REVISION.get();
    }

    /** 找出某个材料标识对应的规则，找不到返回 null。 */
    public static Rule find(String materialIdentifier)
    {
        if (materialIdentifier == null || RULES.isEmpty())
        {
            return null;
        }

        String id = normalizeMaterial(materialIdentifier);

        Rule rule = RULES.get(id);
        if (rule != null)
        {
            return rule;
        }

        // othermod:iron -> iron
        int split = id.indexOf(':');
        if (split > 0)
        {
            rule = RULES.get(id.substring(split + 1));
            if (rule != null)
            {
                return rule;
            }

            return null;
        }

        // 材料标识没有命名空间（Tinkers 自带材料都是这种），兼容 "tconstruct:iron" 写法。
        return RULES.get(DEFAULT_NAMESPACE + ":" + id);
    }

    /** 玩家是否能看到这个材料：没有规则 -> 可见；有规则 -> 需要满足阶段要求。 */
    public static boolean isVisible(String materialIdentifier, Collection<String> playerStages)
    {
        Rule rule = find(materialIdentifier);
        return rule == null || rule.isSatisfiedBy(playerStages);
    }

    /** 所有规则的快照，按材料名排序，用于日志与调试输出。 */
    public static List<Rule> all()
    {
        List<Rule> rules = new ArrayList<Rule>(RULES.values());
        Collections.sort(rules, new java.util.Comparator<Rule>()
        {
            @Override
            public int compare(Rule left, Rule right)
            {
                return left.getMaterial().compareTo(right.getMaterial());
            }
        });
        return rules;
    }

    private static String normalizeMaterial(String material)
    {
        return material == null ? "" : material.trim().toLowerCase(Locale.ROOT);
    }
}
