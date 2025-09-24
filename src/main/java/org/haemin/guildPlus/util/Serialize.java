package org.haemin.guildPlus.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.Base64;

public class Serialize {

    public static String serialize(Inventory inv) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BukkitObjectOutputStream data = new BukkitObjectOutputStream(out);
            data.writeInt(inv.getSize());
            for (int i=0;i<inv.getSize();i++) data.writeObject(inv.getItem(i));
            data.close();
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            return "";
        }
    }

    public static void deserializeInto(Inventory target, String base64) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
            BukkitObjectInputStream data = new BukkitObjectInputStream(in);
            int size = data.readInt();
            for (int i=0;i<size && i<target.getSize();i++) {
                ItemStack it = (ItemStack) data.readObject();
                target.setItem(i, it);
            }
            data.close();
        } catch (Exception ignored) {}
    }

    public static void copyContents(Inventory from, Inventory to) {
        for (int i=0;i<Math.min(from.getSize(), to.getSize()); i++) {
            to.setItem(i, from.getItem(i));
        }
    }

    // BukkitObject* 클래스
    private static class BukkitObjectOutputStream extends ObjectOutputStream {
        public BukkitObjectOutputStream(OutputStream out) throws IOException { super(out); enableReplaceObject(true); }
    }
    private static class BukkitObjectInputStream extends ObjectInputStream {
        public BukkitObjectInputStream(InputStream in) throws IOException { super(in); }
    }
}
