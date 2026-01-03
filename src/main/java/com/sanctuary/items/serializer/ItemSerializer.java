package com.sanctuary.items.serializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sanctuary.items.model.RpgItemData;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * ItemStack과 RpgItemData 간의 변환을 담당하는 직렬화 클래스입니다.
 * PDC(PersistentDataContainer)에 JSON 형태로 데이터를 저장합니다.
 */
public class ItemSerializer {

    private static final String DATA_KEY = "sanctuary_data";

    private final NamespacedKey dataKey;
    private final Gson gson;

    public ItemSerializer(Plugin plugin) {
        this.dataKey = new NamespacedKey(plugin, DATA_KEY);
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
    }

    /**
     * RpgItemData를 ItemStack의 PDC에 저장합니다.
     * 
     * @param item ItemStack
     * @param data 저장할 RpgItemData
     * @return 수정된 ItemStack
     */
    public ItemStack write(ItemStack item, RpgItemData data) {
        if (item == null || data == null)
            return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String json = gson.toJson(data);
        pdc.set(dataKey, PersistentDataType.STRING, json);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * ItemStack의 PDC에서 RpgItemData를 읽습니다.
     * 
     * @param item ItemStack
     * @return RpgItemData 또는 null
     */
    public RpgItemData read(ItemStack item) {
        if (item == null)
            return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String json = pdc.get(dataKey, PersistentDataType.STRING);

        if (json == null || json.isBlank())
            return null;

        try {
            return gson.fromJson(json, RpgItemData.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * ItemStack의 PDC에서 RpgItemData를 Optional로 읽습니다.
     */
    public Optional<RpgItemData> readOptional(ItemStack item) {
        return Optional.ofNullable(read(item));
    }

    /**
     * ItemStack에 Sanctuary 데이터가 있는지 확인합니다.
     */
    public boolean hasSanctuaryData(ItemStack item) {
        if (item == null)
            return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;

        return meta.getPersistentDataContainer().has(dataKey, PersistentDataType.STRING);
    }

    /**
     * ItemStack에서 Sanctuary 데이터를 제거합니다.
     */
    public ItemStack remove(ItemStack item) {
        if (item == null)
            return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        meta.getPersistentDataContainer().remove(dataKey);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * RpgItemData를 JSON 문자열로 변환합니다.
     */
    public String toJson(RpgItemData data) {
        return gson.toJson(data);
    }

    /**
     * JSON 문자열을 RpgItemData로 변환합니다.
     */
    public RpgItemData fromJson(String json) {
        return gson.fromJson(json, RpgItemData.class);
    }

    public NamespacedKey getDataKey() {
        return dataKey;
    }
}
