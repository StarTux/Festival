package com.cavetale.festival.attraction;

import com.cavetale.festival.session.Session;
import com.cavetale.mytems.Mytems;
import java.time.Duration;
import java.util.List;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;

/**
 * Deliver an item, receive an item.  Items are in the session's collection.
 */
public final class TradeChainAttraction extends Attraction<TradeChainAttraction.SaveTag> {
    protected static final Duration GAME_TIME = Duration.ofSeconds(60 * 3);
    protected long secondsLeft;
    @Setter protected String want;
    @Setter protected String give;
    @Setter protected String dialogue = "";

    protected TradeChainAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        this.stringKeys.add("want");
        this.stringKeys.add("give");
    }

    @Override
    public void onEnable() {
        if (want == null) debugLine("No want");
        if (give == null) debugLine("No give");
        if (dialogue.isEmpty()) debugLine("No dialogue");
    }

    @Override
    public void clickMainVillager(Player player) {
        if (!checkPermission(player)) return;
        if (checkPrizeWaiting(player)) return;
        Session session = festival.sessionOf(player);
        final boolean done = session.isUniqueLocked(this);
        if (!done) {
            boolean has = false;
            if (want == null) {
                player.sendMessage(textOfChildren(Mytems.VILLAGER_FACE,
                                                  booth.format(" Thank you so much for visiting. Take this!")));
                has = true;
            }
            if (session.getCollection().contains(want)) {
                player.sendMessage(textOfChildren(Mytems.VILLAGER_FACE,
                                                  (give != null
                                                   ? booth.format(" Thank you so much, this is perfect! Take this instead.")
                                                   : booth.format(" Thank you so much, this is perfect!"))));
                session.getCollection().remove(want);
                has = true;
            }
            if (has) {
                perfect(player);
                if (give != null) {
                    session.getCollection().add(give);
                    festival.openInventory(player);
                }
                prepareFirstCompletionReward(player); // saves
                return;
            }
        }
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(m -> {
                BookMeta meta = (BookMeta) m;
                final Component page;
                if (done) {
                    page = textOfChildren(Mytems.VILLAGER_FACE,
                                          (want != null
                                           ? text(" Thank you again for your help! ")
                                           : text(" Thank you again for visiting me! ")),
                                          Mytems.HEART);
                } else {
                    page = textOfChildren(Mytems.VILLAGER_FACE,
                                          text(" " + dialogue + " "));
                }
                meta.setAuthor("Cavetale");
                meta.title(text("Festival"));
                meta.pages(List.of(page));
            });
        player.openBook(book);
    }

    @Override
    public void onClickYes(Player player) { }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public void start(Player player) { }

    @Override
    public void stop() { }

    @Override
    public void onTick() { }

    static final class SaveTag extends Attraction.SaveTag { }
}
