package com.cavetale.festival.attraction;

import com.cavetale.core.font.DefaultFont;
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
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;

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
        if (!done && want == null) {
            boolean has = false;
            player.sendMessage(textOfChildren(Mytems.VILLAGER_FACE,
                                              booth.format(" Thank you so much for visiting. Take this!")));
            perfect(player);
            if (give != null) {
                session.getCollection().add(give);
                festival.openInventory(player);
            }
            prepareFirstCompletionReward(player); // saves
            return;
        }
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(m -> {
                BookMeta meta = (BookMeta) m;
                final Component page;
                if (done) {
                    page = textOfChildren(Mytems.VILLAGER_FACE,
                                          (want != null
                                           ? booth.format(" Thank you again for your help! ")
                                           : booth.format(" Thank you again for visiting me! ")),
                                          Mytems.HEART,
                                          newline(), newline(),
                                          (textOfChildren(Mytems.ARROW_RIGHT, text(" View Inventory", BLUE))
                                           .hoverEvent(showText(text("View Inventory", BLUE)))
                                           .clickEvent(runCommand("/fest inv"))));
                } else {
                    page = textOfChildren(Mytems.VILLAGER_FACE,
                                          booth.format(" " + dialogue + " "),
                                          newline(), newline(),
                                          booth.format("Do you have an item that could help me?"),
                                          newline(),
                                          (DefaultFont.YES_BUTTON.component
                                           .clickEvent(runCommand("/fest yes " + name))
                                           .hoverEvent(showText(text("Give Item", GREEN)))),
                                          space(),
                                          (DefaultFont.NO_BUTTON.component
                                           .clickEvent(runCommand("/fest no " + name))
                                           .hoverEvent(showText(text("Maybe Later", RED)))),
                                          newline(), newline(),
                                          (textOfChildren(Mytems.ARROW_RIGHT, text(" View Inventory", BLUE))
                                           .hoverEvent(showText(text("View Inventory", BLUE)))
                                           .clickEvent(runCommand("/fest inv"))));
                }
                meta.setAuthor("Cavetale");
                meta.title(text("Festival"));
                meta.pages(List.of(page));
            });
        player.closeInventory();
        player.openBook(book);
    }

    @Override
    public void onClickYes(Player player) {
        if (want == null) return;
        if (!checkPermission(player)) return;
        Session session = festival.sessionOf(player);
        final boolean done = session.isUniqueLocked(this);
        if (done) return;
        if (!session.getCollection().contains(want)) {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            book.editMeta(m -> {
                    BookMeta meta = (BookMeta) m;
                    final Component page;
                    page = textOfChildren(Mytems.VILLAGER_FACE,
                                          booth.format(" Looks like you don't have anything that could help me."
                                                       + " Thanks for trying anyway. "),
                                          Mytems.SMILE,
                                          newline(), newline(),
                                          (textOfChildren(Mytems.ARROW_RIGHT, text(" View Inventory", BLUE))
                                           .hoverEvent(showText(text("View Inventory", BLUE)))
                                           .clickEvent(runCommand("/fest inv"))));
                    meta.setAuthor("Cavetale");
                    meta.title(text("Festival"));
                    meta.pages(List.of(page));
                });
            player.closeInventory();
            player.openBook(book);
            return;
        }
        session.getCollection().remove(want);
        if (give != null) {
            session.getCollection().add(give);
        }
        prepareFirstCompletionReward(player); // saves
        perfect(player);
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(m -> {
                BookMeta meta = (BookMeta) m;
                final Component page;
                page = textOfChildren(Mytems.VILLAGER_FACE,
                                      (give != null
                                       ? booth.format(" Thank you so much, this is perfect! Take this instead.")
                                       : booth.format(" Thank you so much, this is perfect!")),
                                      Mytems.HEART,
                                      newline(), newline(),
                                      (textOfChildren(Mytems.ARROW_RIGHT, text(" View Inventory", BLUE))
                                       .hoverEvent(showText(text("View Inventory", BLUE)))
                                       .clickEvent(runCommand("/fest inv"))));
                meta.setAuthor("Cavetale");
                meta.title(text("Festival"));
                meta.pages(List.of(page));
            });
        player.closeInventory();
        player.openBook(book);
    }

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
