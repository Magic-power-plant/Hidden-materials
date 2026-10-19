package com.mpp.hiddenmaterials.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mpp.hiddenmaterials.client.BookOpenHook;

import net.minecraft.item.ItemStack;
import slimeknights.mantle.client.book.data.BookData;

/**
 * 在书被打开之前检查一次缓存是否需要失效，见 {@link BookOpenHook}。
 */
@Mixin(BookData.class)
public abstract class BookDataMixin
{
    @Inject(method = "openGui", at = @At("HEAD"), remap = false)
    private void hiddenmaterials$refreshBeforeOpen(ItemStack stack, CallbackInfo callback)
    {
        BookOpenHook.beforeBookOpen((BookData) (Object) this);
    }
}
