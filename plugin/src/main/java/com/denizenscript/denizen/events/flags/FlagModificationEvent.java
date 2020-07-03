package com.denizenscript.denizen.events.flags;

import com.denizenscript.denizen.flags.FlagManager.Flag;
import com.denizenscript.denizencore.objects.ObjectTag;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class FlagModificationEvent extends Event {
  private static final HandlerList HANDLER_LIST = new HandlerList();

  private final ObjectTag tag;
  private final Flag flag;
  private final String value;

  public FlagModificationEvent(ObjectTag tag, Flag flag, String value) {
    this.tag = tag;
    this.flag = flag;
    this.value = value;
  }

  public ObjectTag getTag() {
    return tag;
  }

  public Flag getFlag() {
    return flag;
  }

  public String getValue() {
    return value;
  }

  public static HandlerList getHandlerList() {
    return HANDLER_LIST;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLER_LIST;
  }
}
