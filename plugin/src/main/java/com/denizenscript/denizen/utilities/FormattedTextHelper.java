package com.denizenscript.denizen.utilities;

import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;

import java.util.ArrayList;
import java.util.List;

public class FormattedTextHelper {

    public static String escape(String input) {
        return input.replace("&", "&amp").replace(";", "&sc").replace("[", "&lb").replace("]", "&rb").replace(String.valueOf(ChatColor.COLOR_CHAR), "&ss");
    }

    public static String unescape(String input) {
        return input.replace("&sc", ";").replace("&lb", "[").replace("&rb", "]").replace("&ss", String.valueOf(ChatColor.COLOR_CHAR)).replace("&amp", "&");
    }

    public static String stringify(BaseComponent[] components) {
        StringBuilder builder = new StringBuilder(128 * components.length);
        for (BaseComponent component : components) {
            builder.append(stringify(component));
        }
        return builder.toString();
    }

    public static boolean boolNotNull(Boolean bool) {
        return bool != null && bool;
    }

    public static String stringify(BaseComponent component) {
        StringBuilder builder = new StringBuilder(128);
        ChatColor color = component.getColorRaw();
        if (color != null) {
            builder.append(color.toString());
        }
        if (boolNotNull(component.isBoldRaw())) {
            builder.append(ChatColor.BOLD.toString());
        }
        if (boolNotNull(component.isItalicRaw())) {
            builder.append(ChatColor.ITALIC.toString());
        }
        if (boolNotNull(component.isStrikethroughRaw())) {
            builder.append(ChatColor.STRIKETHROUGH.toString());
        }
        if (boolNotNull(component.isUnderlinedRaw())) {
            builder.append(ChatColor.UNDERLINE.toString());
        }
        if (boolNotNull(component.isObfuscatedRaw())) {
            builder.append(ChatColor.MAGIC.toString());
        }
        boolean hasInsertion = component.getInsertion() != null;
        if (hasInsertion) {
            builder.append(ChatColor.COLOR_CHAR).append("[insertion=").append(escape(component.getInsertion())).append("]");
        }
        boolean hasHover = component.getHoverEvent() != null;
        if (hasHover) {
            HoverEvent hover = component.getHoverEvent();
            builder.append(ChatColor.COLOR_CHAR).append("[hover=").append(hover.getAction().name()).append(";").append(escape(stringify(hover.getValue()))).append("]");
        }
        boolean hasClick = component.getClickEvent() != null;
        if (hasClick) {
            ClickEvent click = component.getClickEvent();
            builder.append(ChatColor.COLOR_CHAR).append("[click=").append(click.getAction().name()).append(";").append(escape(click.getValue())).append("]");
        }
        if (component instanceof TranslatableComponent) {
            builder.append(ChatColor.COLOR_CHAR).append("[translate=").append(escape(((TranslatableComponent) component).getTranslate()));
            List<BaseComponent> with = ((TranslatableComponent) component).getWith();
            if (with != null) {
                for (BaseComponent withComponent : with) {
                    builder.append(";").append(escape(stringify(withComponent)));
                }
            }
            builder.append("]");
        }
        else if (component instanceof SelectorComponent) {
            builder.append(ChatColor.COLOR_CHAR).append("[selector=").append(escape(((SelectorComponent) component).getSelector())).append("]");
        }
        else if (component instanceof KeybindComponent) {
            builder.append(ChatColor.COLOR_CHAR).append("[keybind=").append(escape(((KeybindComponent) component).getKeybind())).append("]");
        }
        else if (component instanceof ScoreComponent) {
            builder.append(ChatColor.COLOR_CHAR).append("[score=").append(escape(((ScoreComponent) component).getName()))
                    .append(";").append(escape(((ScoreComponent) component).getObjective()))
                    .append(";").append(escape(((ScoreComponent) component).getValue())).append("]");
        }
        else if (component instanceof TextComponent) {
            builder.append(((TextComponent) component).getText());
        }
        List<BaseComponent> after = component.getExtra();
        if (after != null) {
            for (BaseComponent afterComponent : after) {
                builder.append(stringify(afterComponent));
            }
        }
        if (hasClick) {
            builder.append(ChatColor.COLOR_CHAR + "[/click]");
        }
        if (hasHover) {
            builder.append(ChatColor.COLOR_CHAR + "[/hover]");
        }
        if (hasInsertion) {
            builder.append(ChatColor.COLOR_CHAR + "[/insertion]");
        }
        builder.append(RESET);
        String output = builder.toString();
        while (output.contains(RESET + RESET)) {
            output = output.replace(RESET  + RESET, RESET);
        }
        return output;
    }

    public static final String RESET = ChatColor.RESET.toString();

    public static BaseComponent[] parse(String str) {
        char[] chars = str.toCharArray();
        List<BaseComponent> outputList = new ArrayList<>();
        int started = 0;
        //TextComponent lastText = new TextComponent();
        //outputList.add(lastText);
        TextComponent nextText = new TextComponent();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == ChatColor.COLOR_CHAR && i + 1 < chars.length) {
                char code = chars[i + 1];
                if (code == '[') {
                    int endBracket = str.indexOf(']', i + 2);
                    if (endBracket == -1) {
                        continue;
                    }
                    String innards = str.substring(i + 2, endBracket);
                    List<String> innardParts = CoreUtilities.split(innards, ';');
                    List<String> innardBase = CoreUtilities.split(innardParts.get(0), '=', 2);
                    innardParts.remove(0);
                    String innardType = CoreUtilities.toLowerCase(innardBase.get(0));
                    if (innardBase.size() == 2) {
                        nextText.setText(nextText.getText() + str.substring(started, i));
                        outputList.add(nextText);
                        TextComponent lastText = nextText;
                        nextText = new TextComponent(lastText);
                        nextText.setText("");
                        if (innardType.equals("score") && innardParts.size() == 2) {
                            ScoreComponent component = new ScoreComponent(unescape(innardBase.get(1)), unescape(innardParts.get(0)), unescape(innardParts.get(1)));
                            lastText.addExtra(component);
                        }
                        else if (innardType.equals("keybind")) {
                            KeybindComponent component = new KeybindComponent();
                            component.setKeybind(unescape(innardBase.get(1)));
                            lastText.addExtra(component);
                        }
                        else if (innardType.equals("selector")) {
                            SelectorComponent component = new SelectorComponent(unescape(innardBase.get(1)));
                            lastText.addExtra(component);
                        }
                        else if (innardType.equals("translate")) {
                            TranslatableComponent component = new TranslatableComponent();
                            component.setTranslate(unescape(innardBase.get(1)));
                            for (String extra : innardParts) {
                                for (BaseComponent subComponent : parse(unescape(extra))) {
                                    component.addWith(subComponent);
                                }
                            }
                            lastText.addExtra(component);
                        }
                        else if (innardType.equals("click") && innardParts.size() == 1) {
                            int endIndex = str.indexOf(ChatColor.COLOR_CHAR + "[/click]", i);
                            int backupEndIndex = str.indexOf(ChatColor.COLOR_CHAR + "[click=", i + 5);
                            if (backupEndIndex > 0 && backupEndIndex < endIndex) {
                                endIndex = backupEndIndex;
                            }
                            if (endIndex == -1) {
                                continue;
                            }
                            TextComponent clickableText = new TextComponent();
                            clickableText.setClickEvent(new ClickEvent(ClickEvent.Action.valueOf(innardBase.get(1).toUpperCase()), unescape(innardParts.get(0))));
                            for (BaseComponent subComponent : parse(str.substring(endBracket + 1, endIndex))) {
                                clickableText.addExtra(subComponent);
                            }
                            lastText.addExtra(clickableText);
                            endBracket = endIndex + "&[/click".length();
                        }
                        else if (innardType.equals("hover")) {
                            int endIndex = str.indexOf(ChatColor.COLOR_CHAR + "[/hover]", i);
                            int backupEndIndex = str.indexOf(ChatColor.COLOR_CHAR + "[hover=", i + 5);
                            if (backupEndIndex > 0 && backupEndIndex < endIndex) {
                                endIndex = backupEndIndex;
                            }
                            if (endIndex == -1) {
                                continue;
                            }
                            TextComponent hoverableText = new TextComponent();
                            hoverableText.setHoverEvent(new HoverEvent(HoverEvent.Action.valueOf(innardBase.get(1).toUpperCase()), parse(unescape(innardParts.get(0)))));
                            for (BaseComponent subComponent : parse(str.substring(endBracket + 1, endIndex))) {
                                hoverableText.addExtra(subComponent);
                            }
                            lastText.addExtra(hoverableText);
                            endBracket = endIndex + "&[/hover".length();
                        }
                        else if (innardType.equals("insertion")) {
                            int endIndex = str.indexOf(ChatColor.COLOR_CHAR + "[/insertion]", i);
                            int backupEndIndex = str.indexOf(ChatColor.COLOR_CHAR + "[insertion=", i + 5);
                            if (backupEndIndex > 0 && backupEndIndex < endIndex) {
                                endIndex = backupEndIndex;
                            }
                            if (endIndex == -1) {
                                continue;
                            }
                            TextComponent insertableText = new TextComponent();
                            insertableText.setInsertion(unescape(innardBase.get(1)));
                            for (BaseComponent subComponent : parse(str.substring(endBracket + 1, endIndex))) {
                                insertableText.addExtra(subComponent);
                            }
                            lastText.addExtra(insertableText);
                            endBracket = endIndex + "&[/insertion".length();
                        }
                    }
                    i = endBracket;
                    started = endBracket + 1;
                    continue;
                }
                else if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f') || (code >= 'A' && code <= 'F')) {
                    nextText.setText(nextText.getText() + str.substring(started, i));
                    outputList.add(nextText);
                    //lastText = nextText;
                    nextText = new TextComponent();
                    nextText.setColor(ChatColor.getByChar(code));
                }
                else if ((code >= 'k' && code <= 'o') || (code >= 'K' && code <= 'O')) {
                    if (code == 'k' || code == 'K') {
                        nextText.setObfuscated(true);
                    }
                    else if (code == 'l' || code == 'L') {
                        nextText.setBold(true);
                    }
                    else if (code == 'm' || code == 'M') {
                        nextText.setStrikethrough(true);
                    }
                    else if (code == 'n' || code == 'N') {
                        nextText.setUnderlined(true);
                    }
                    else if (code == 'o' || code == 'O') {
                        nextText.setItalic(true);
                    }
                }
                else if (code == 'r' || code == 'R') {
                    nextText.setText(nextText.getText() + str.substring(started, i));
                    outputList.add(nextText);
                    //lastText = nextText;
                    nextText = new TextComponent();
                }
                else {
                    continue;
                }
                i++;
                started = i + 1;
             }
        }
        nextText.setText(nextText.getText() + str.substring(started));
        if (!nextText.getText().isEmpty()) {
            outputList.add(nextText);
        }
        return outputList.toArray(new BaseComponent[0]);
    }
}
