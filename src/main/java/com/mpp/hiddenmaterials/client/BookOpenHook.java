package com.mpp.hiddenmaterials.client;

import java.util.Map;
import java.util.WeakHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import slimeknights.mantle.client.book.data.BookData;

/**
 * 书本缓存的失效处理。
 *
 * <p>Mantle 的书只会在第一次打开时构建一次（{@code BookData.initialized}），章节里的材料页
 * 也就被缓存下来了。如果玩家在之后解锁了阶段，缓存里的书仍然是旧的，所以打开书之前比较一次
 * “规则版本 + 玩家阶段”的签名，变化了就让 Mantle 重新构建（{@code reset()} 之后
 * {@code openGui()} 会自己再 {@code load()} 一次）。
 *
 * <p>签名按书本实例记录：即使同时存在多本 Mantle 书，也不会互相把对方标记成“已是最新”。
 */
@SideOnly(Side.CLIENT)
public final class BookOpenHook
{
    private static final Logger LOG = LogManager.getLogger("hiddenmaterials");

    /** 每本书上一次构建时使用的签名。书本是长生命周期对象，用弱引用避免持有已废弃的书。 */
    private static final Map<BookData, String> LAST_SIGNATURE = new WeakHashMap<BookData, String>();

    private BookOpenHook()
    {
    }

    /** 由 Mixin 在 {@code BookData.openGui} 开头调用。 */
    public static void beforeBookOpen(BookData book)
    {
        if (book == null)
        {
            return;
        }

        String signature;
        try
        {
            signature = MaterialBookVisibility.signature();
            if (signature.equals(LAST_SIGNATURE.get(book)))
            {
                return;
            }
        }
        catch (Exception exception)
        {
            LOG.error("Hidden Materials: could not evaluate the book rebuild signature, keeping the cached book", exception);
            return;
        }

        LAST_SIGNATURE.put(book, signature);
        book.reset();
        LOG.debug("Rebuilding book content for hidden-material stages [{}]", signature);
    }
}
