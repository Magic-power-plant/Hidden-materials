package com.mpp.hiddenmaterials.mixin;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mpp.hiddenmaterials.client.MaterialBookVisibility;

import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.book.sectiontransformer.BowMaterialSectionTransformer;
import slimeknights.tconstruct.library.materials.Material;

/**
 * 过滤 {@code bowmaterials} 章节（弓/弩材料），规则与 {@code materials} 章节一致。
 */
@Mixin(BowMaterialSectionTransformer.class)
public abstract class BowMaterialSectionTransformerMixin
{
    @Redirect(
            method = "generateContent(Ljava/lang/String;Lslimeknights/mantle/client/book/data/SectionData;)Ljava/util/List;",
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
