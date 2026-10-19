# Hidden Materials

用 **CraftTweaker** 脚本 + **Game Stages** 阶段，控制 1.12.2 里 Tinkers' Antique（匠魂）书本
`tconstruct:book`（“材料与您” / Materials and You）中材料页的显示：

> 玩家解锁了指定阶段，书里才会出现对应材料；没解锁的玩家在书里完全看不到它。

```zenscript
// 只有拥有 stage_iron 的玩家，才能在“材料与您”里看到铁
mods.hiddenmaterials.MaterialStages.require("tconstruct:iron", "stage_iron");
```

---

## 1. 它到底改了什么

“材料与您”里 `materials`（材料）和 `bowmaterials`（弓弩材料）两个章节的内容**不在 json 里**，
而是打开书时由匠魂动态生成的：

| 章节 | 生成者 | 数据来源 |
| --- | --- | --- |
| `materials` | `MaterialSectionTransformer` → `AbstractMaterialSectionTransformer#transform` | `TinkerRegistry.getAllMaterials()` |
| `bowmaterials` | `BowMaterialSectionTransformer#generateContent` | `TinkerRegistry.getAllMaterials()` |

本模组用 Mixin 把这两处的 `TinkerRegistry.getAllMaterials()` 换成“当前玩家已解锁”的材料集合，
之后再走匠魂原有的 `!material.isHidden()` / `isValidMaterial()` 过滤，所以：

* 未解锁的材料**不会**生成页面、也不会出现在材料图标列表里（不是把页面藏起来，而是根本不出现）；
* 匠魂自己的隐藏材料逻辑、书的其他章节、页码顺序都不受影响。

另外，Mantle 的书只在**第一次打开时构建一次**并缓存。为了让“刚解锁阶段”的玩家马上看到变化，
模组在每次打开书之前比较一次签名（规则版本 + 玩家已解锁阶段），签名变了就 `BookData.reset()`，
让书用当前阶段重新构建——所以你解锁阶段后**重新打开书**即可看到新材料，不需要重启游戏。

## 2. 安装 / 依赖

| 依赖 | 版本（开发/构建时使用） |
| --- | --- |
| Minecraft Forge | 1.12.2 - 14.23.5.2847 |
| [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixin-booter) | 11.18+ |
| [CraftTweaker](https://www.curseforge.com/minecraft/mc-mods/crafttweaker) | 1.12-4.1.20.716+ |
| [Game Stages](https://www.curseforge.com/minecraft/mc-mods/game-stages) | 2.0.123+ |
| [Tinkers' Antique](https://www.curseforge.com/minecraft/mc-mods/tinkers-antique) | 1.12.2-2.13.0.209+ |
| Mantle（匠魂的前置） | 1.12-1.3.3.55+ |

（[Bookshelf](https://www.curseforge.com/minecraft/mc-mods/bookshelf) 是 Game Stages 自己要求的前置，
打包时不需要额外声明，但开发环境要能跑起来就得有它——`build.gradle` 里已经带上了。）

## 3. 脚本 API

ZenScript 类：`mods.hiddenmaterials.MaterialStages`

| 方法 | 说明 |
| --- | --- |
| `require(material, stage)` | 解锁 `stage` 后才显示该材料（等价于只写一个阶段的 `requireAll`） |
| `requireAll(material, stages...)` | 需要**同时**拥有全部阶段 |
| `requireAny(material, stages...)` | 拥有**任意一个**阶段即可 |
| `remove(material)` | 删除该材料的规则 |
| `clear()` | 删除全部规则 |
| `getAllMaterialNames()` | 返回所有可用材料标识（`String[]`），用于查名字 |
| `getRules()` | 返回当前规则的可读快照（`String`，每行一条），用于调试 |

同一个材料重复配置时**以最后一次为准**；不传阶段（空数组）等同于 `remove`。

> CraftTweaker 的 `print` 只接受字符串，所以 `print(getRules())` 可以直接用；
> `getAllMaterialNames()` 返回的是数组，要遍历打印：
> `for name in mods.hiddenmaterials.MaterialStages.getAllMaterialNames() { print(name); }`

### 材料名怎么写

* `iron`、`wood`、`cobalt`、`manyullyn` —— 匠魂自带材料；
* `tconstruct:iron` —— 带命名空间的等价写法（也会匹配标识为 `iron` 的材料）；
* `othermod:alumite` —— 其它模组注册的材料用它们自己的标识；
* 名字不区分大小写。写错名字会在日志里给出警告，可以用下面的片段查全部合法名字：

```zenscript
for name in mods.hiddenmaterials.MaterialStages.getAllMaterialNames() {
    print(name);
}
```

### 阶段名

阶段名**区分大小写**，并直接交给 Game Stages 判断（`/gamestage add <玩家> <阶段>`）。
一个材料可以写多个阶段：

```zenscript
// 需要同时拥有 stage_smeltery 与 stage_nether
mods.hiddenmaterials.MaterialStages.requireAll("cobalt", ["stage_smeltery", "stage_nether"]);

// 两个阶段里任意一个都可以
mods.hiddenmaterials.MaterialStages.requireAny("manyullyn", "stage_nether", "stage_end");
```

完整示例见 [`docs/example.zs`](docs/example.zs)。

## 4. 常用做法

```zenscript
// 一次性把匠魂的基础材料全部藏起来，按进度慢慢解锁
val early = ["wood", "stone", "flint", "bone", "cactus", "obsidian"];
for material in early {
    mods.hiddenmaterials.MaterialStages.require(material, "stage_tinkers_start");
}

// 后期材料
mods.hiddenmaterials.MaterialStages.requireAll("manyullyn", ["stage_nether", "stage_smeltery"]);

// 调试：看看规则和材料名
print(mods.hiddenmaterials.MaterialStages.getRules());
```

改完脚本后用 `/ct reload` 重新加载脚本；规则变化后书的缓存签名会变化，
下次打开书就会重建内容（已经开着的书需要关掉再打开）。

## 5. 已知边界

* **只影响书本显示**。材料本身仍然可以被铸造/使用，JEI 里的材料分类、匠魂的其它 GUI
  不受影响。要让玩家真正用不了材料，需要另外配合配方/合成限制。
* 过滤发生在客户端（书的渲染与构建都在客户端），阶段数据由 Game Stages 同步到客户端；
  脚本在客户端也会执行，规则无需额外同步。
* 玩家在**书开着的时候**解锁阶段，需要关掉再打开书才会刷新。
* 与匠魂自带的高级要求（Mantle 的 advancement 条件）互不干扰。

## 6. 从源码构建

```bash
# 需要 JDK 25 运行 Gradle（RFG 2.0.3 的插件要求），编译目标仍是 Java 8
JAVA_HOME=/path/to/jdk-25 ./gradlew build
```

产物：`build/libs/hiddenmaterials-1.0.jar`。
