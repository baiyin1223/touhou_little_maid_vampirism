package com.github.catbert.tlmv.meal;

import com.github.catbert.tlmv.TLMVMain;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 外部血液食物注册表。从 blood_food_values.json 加载物品ID→blood恢复值的映射，
 * 支持为第三方模组的吸血鬼食物（如 VampiresDelight）精确指定恢复量。
 * <p>
 * JSON 格式: {@code {"values": {"modid:item": bloodValue, ...}}}
 * <p>
 * 使用方式：在 {@link TLMVMain} 构造中调用 {@link #init()}。
 */
public final class ExternalBloodFoodRegistry {

    private static final String RESOURCE_PATH = "/data/touhou_little_maid_vampirism/blood_food_values.json";

    private static Map<ResourceLocation, Integer> bloodFoodMap = Collections.emptyMap();
    private static boolean initialized = false;

    private ExternalBloodFoodRegistry() {
    }

    /**
     * 从 classpath 加载 blood_food_values.json，必须在 mod 初始化阶段调用。
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        try (Reader reader = new InputStreamReader(
                ExternalBloodFoodRegistry.class.getResourceAsStream(RESOURCE_PATH),
                StandardCharsets.UTF_8)) {

            Gson gson = new Gson();
            Type type = new TypeToken<BloodFoodData>() {}.getType();
            BloodFoodData data = gson.fromJson(reader, type);

            Map<ResourceLocation, Integer> map = new HashMap<>();
            if (data != null && data.values != null) {
                for (var entry : data.values.entrySet()) {
                    map.put(new ResourceLocation(entry.getKey()), entry.getValue());
                }
            }
            bloodFoodMap = Collections.unmodifiableMap(map);
            TLMVMain.LOGGER.info("[ExternalBloodFoodRegistry] Loaded {} external blood food entries", bloodFoodMap.size());

        } catch (Exception e) {
            TLMVMain.LOGGER.warn("[ExternalBloodFoodRegistry] Failed to load blood_food_values.json: {}", e.getMessage());
            bloodFoodMap = Collections.emptyMap();
        }
    }

    /**
     * 检查给定物品 ID 是否为已注册的外部血液食物。
     */
    public static boolean isBloodFood(ResourceLocation id) {
        return bloodFoodMap.containsKey(id);
    }

    /**
     * 获取给定物品 ID 的 blood 恢复值。
     *
     * @return blood 恢复值，若未注册则返回 -1
     */
    public static int getBloodValue(ResourceLocation id) {
        return bloodFoodMap.getOrDefault(id, -1);
    }

    /**
     * JSON 顶层结构
     */
    private static class BloodFoodData {
        Map<String, Integer> values;
    }
}
