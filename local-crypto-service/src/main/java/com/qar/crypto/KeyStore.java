package com.qar.crypto;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KeyStore {

    private final Map<String, SecretKey> keyCache;
    private final Map<String, String> persistentStore;

    public KeyStore() {
        this.keyCache = new ConcurrentHashMap<>();
        this.persistentStore = new HashMap<>();
    }

    public void storeKey(String wrappedKey, SecretKey key) {
        keyCache.put(wrappedKey, key);
        
        String keyBase64 = Base64.getEncoder().encodeToString(key.getEncoded());
        persistentStore.put(wrappedKey, keyBase64);
        
        System.out.println("[密钥存储] 已存储密钥: " + wrappedKey);
    }

    public SecretKey getKey(String wrappedKey) {
        System.out.println("[密钥存储] 查找密钥: " + wrappedKey);
        System.out.println("[密钥存储] 当前存储的密钥数量: " + persistentStore.size());
        System.out.println("[密钥存储] 存储的密钥列表: " + persistentStore.keySet());
        
        SecretKey key = keyCache.get(wrappedKey);
        
        if (key == null) {
            String keyBase64 = persistentStore.get(wrappedKey);
            System.out.println("[密钥存储] 从持久化存储查找: " + (keyBase64 != null ? "找到" : "未找到"));
            if (keyBase64 != null) {
                byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
                key = new SecretKeySpec(keyBytes, "AES");
                keyCache.put(wrappedKey, key);
                System.out.println("[密钥存储] 从持久化存储加载密钥: " + wrappedKey);
            }
        } else {
            System.out.println("[密钥存储] 从缓存获取密钥: " + wrappedKey);
        }
        
        return key;
    }

    public void removeKey(String wrappedKey) {
        keyCache.remove(wrappedKey);
        persistentStore.remove(wrappedKey);
        System.out.println("[密钥存储] 已移除密钥: " + wrappedKey);
    }

    public void clearAll() {
        keyCache.clear();
        persistentStore.clear();
        System.out.println("[密钥存储] 已清除所有密钥");
    }

    public boolean hasKey(String wrappedKey) {
        return keyCache.containsKey(wrappedKey) || persistentStore.containsKey(wrappedKey);
    }

    public int getKeyCount() {
        return persistentStore.size();
    }

    public List<String> getStoredKeys() {
        return new ArrayList<>(persistentStore.keySet());
    }
}
