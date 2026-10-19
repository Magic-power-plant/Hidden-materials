package com.mpp.hiddenmaterials.mixin;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mpp.hiddenmaterials.client.MaterialBookVisibility;

import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.book.sectiontransformer.AbstractMaterialSectionTransformer;
import slimeknights.tconstruct.library.materials.Material;

/**
 * 过滤“材料与您”书本里自动生成的 {@code materials} 章节。
 *
 * <p>{@code transform} 会执行
 * {@code TinkerRegistry.getAllMaterials().stream().filter(!hidden).filter(isValidMaterial)}，
 * 这里把 {@code getAllMaterials()} 的结果换成“当前玩家已经解锁”的材料，
 * 之后的 {@code !isHidden()} 与各子类自己的 {@code isValidMaterial} 判断保持原样。
 */
@Mixin(AbstractMaterialSectionTransformer.class)
public abstract class AbstractMaterialSectionTransformerMixin
{
    @Redirect(
            method = "transform(Lslimeknights/mantle/client/book/data/BookData;Lslimeknights/mantle/client/book/data/SectionData;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lslimeknights/tconstruct/library/TinkerRegistry;getAllMaterials()Ljava/util/Collection;",
                    remap = false),
            remap = false)
    private static Collection<Material> hiddenmaterials$filterByGameStages()
    {
        return MaterialBookVisibility.filter(TinkerRegistry.getAllMaterials());
    }
}
