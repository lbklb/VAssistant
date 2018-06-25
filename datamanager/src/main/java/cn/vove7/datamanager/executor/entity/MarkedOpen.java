package cn.vove7.datamanager.executor.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Index;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.NotNull;

/**
 * 打开事件
 * Created by Vove on 2018/6/23
 */
@Entity(indexes = {@Index(value = "key")})
public class MarkedOpen {
   public static final String TYPE_APP = "app";//应用 value -> pkg
   public static final String TYPE_SYS_FUN = "sys_fun";//系统功能 value: fun_key

   @Id
   private Long id;
   @NotNull
   private String key;//alias
   private String type;
   /**
    * key的正则
    */
   private String regStr;
   private String value;//标识

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   //AppInfo data;
   @Keep
   public MarkedOpen(Long id, @NotNull String key) {
      this.id = id;
      this.key = key;
   }

   @Keep
   public MarkedOpen() {
   }

   @Keep
   public MarkedOpen(String key, String type, String regStr, String value) {
      this.key = key;
      this.type = type;
      this.regStr = regStr;
      this.value = value;
   }

   @Keep
   public MarkedOpen(Long id, @NotNull String key, String type, String regStr,
                     String value) {
      this.id = id;
      this.key = key;
      this.type = type;
      this.regStr = regStr;
      this.value = value;
   }

   public Long getId() {
      return this.id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public String getKey() {
      return this.key;
   }

   public void setKey(String key) {
      this.key = key;
   }

   public String getRegStr() {
      return this.regStr;
   }

   public void setRegStr(String regStr) {
      this.regStr = regStr;
   }
}
