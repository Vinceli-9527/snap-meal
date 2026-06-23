package com.snapmeal.common;
import java.util.Map;
public final class Db {
    private Db(){}
    public static Object get(Map<String,Object> row,String key){Object value=row.get(key);if(value==null)value=row.get(key.toUpperCase());if(value==null)value=row.get(key.toLowerCase());return value;}
    public static long longValue(Map<String,Object> row,String key){return ((Number)get(row,key)).longValue();}
    public static int intValue(Map<String,Object> row,String key){return ((Number)get(row,key)).intValue();}
}
