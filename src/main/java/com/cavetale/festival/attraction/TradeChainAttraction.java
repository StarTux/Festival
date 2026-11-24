package com.cavetale.festival.attraction;

import com.cavetale.core.font.DefaultFont;
import com.cavetale.festival.session.Session;
import com.cavetale.mytems.Mytems;
import java.time.Duration;
import java.util.List;
import lombok.Getter;
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
public final class TradeChainAttraction extends Attraction {
    @Getter private SaveTag saveTag;
    protected static final Duration GAME_TIME = Duration.ofSeconds(60 * 3);
    protected long secondsLeft;
    @Setter protected String want;
    @Setter protected String give;
    @Setter protected String dialogue = "";

    protected TradeChainAttraction(final AttractionConfiguration config) {
        super(config);
        this.stringKeys.add("want");
        this.stringKeys.add("give");
        this.stringKeys.add("dialogue");
        if (config.getSettings().get("want") instanceof String value) want = value;
        if (config.getSettings().get("give") instanceof String value) give = value;
        if (config.getSettings().get("dialogue") instanceof String value) dialogue = value;
    }

    @Override
    protected void loadSaveTag() {
        saveTag = readSaveTag(SaveTag.class, SaveTag::new);
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
        Session session = plugin.getSessions().of(player);
        final boolean done = session.isUniqueLocked(this);
        if (!done && want == null) {
            boolean has = false;
            player.sendMessage(textOfChildren(Mytems.VILLAGER_FACE,
                                              theme.format(" Thank you so much for visiting. Take this!")));
            perfect(player);
            if (give != null) {
                session.getCollection().add(give);
                plugin.showCollection(player);
            }
            prepareReward(player, true); // saves
            return;
        }
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(m -> {
                BookMeta meta = (BookMeta) m;
                final Component page;
                if (done) {
                    page = textOfChildren(Mytems.VILLAGER_FACE,
                                          (want != null
                                           ? theme.format(" Thank you again for your help! ")
                                           : theme.format(" Thank you again for visiting me! ")),
                                          Mytems.HEART,
                                          newline(), newline(),
                                          (textOfChildren(Mytems.ARROW_RIGHT, text(" View Inventory", BLUE))
                                           .hoverEvent(showText(text("View Inventory", BLUE)))
                                           .clickEvent(runCommand("/fest inv"))));
                } else {
                    page = textOfChildren(Mytems.VILLAGER_FACE,
                                          theme.format(" " + dialogue + " "),
                                          newline(), newline(),
                                          theme.format("Do you have an item that could help me?"),
                                          newline(),
                                          (DefaultFont.YES_BUTTON.component
                                           .clickEvent(runCommand("/fest yes"))
                                           .hoverEvent(showText(text("Give Item", GREEN)))),
                                          space(),
                                          (DefaultFont.NO_BUTTON.component
                                           .clickEvent(runCommand("/fest no"))
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
        Session session = plugin.getSessions().of(player);
        final boolean done = session.isUniqueLocked(this);
        if (done) return;
        if (!session.getCollection().contains(want)) {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            book.editMeta(m -> {
                    BookMeta meta = (BookMeta) m;
                    final Component page;
                    page = textOfChildren(Mytems.VILLAGER_FACE,
                                          theme.format(" Looks like you don't have anything that could help me."
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
        prepareReward(player, true); // saves
        perfect(player);
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(m -> {
                BookMeta meta = (BookMeta) m;
                final Component page;
                page = textOfChildren(Mytems.VILLAGER_FACE,
                                      (give != null
                                       ? theme.format(" Thank you so much, this is perfect! Take this instead.")
                                       : theme.format(" Thank you so much, this is perfect!")),
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
    public boolean isParticipant(final Player player) {
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
