package cn.vove7.common.datamanager.parse.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;

import java.util.Objects;

/**
 * #ActionScope
 * 指令作用域
 *
 * @see Action
 * <p>
 * Created by Vove on 2018/6/18
 */
@Entity()
public class ActionScope {
    @Id
    private
    Long id;
    /**
     * App包名
     */
    private String packageName;
    /**
     * Activity页面
     */
    private String activity = "";
    private Integer hashCode;


    @Keep
    public ActionScope(Long id, String packageName, String activity) {
        this.id = id;
        this.packageName = packageName;
        this.activity = activity;
    }

    public int genHashCode() {
        hashCode = Objects.hash(packageName, activity);
        return hashCode;
    }

    @Keep
    public ActionScope(String packageName, String activity) {
        this.packageName = packageName;
        this.activity = activity;
    }

    @Generated(hash = 1143247331)
    public ActionScope() {
    }

    @Generated(hash = 432106659)
    public ActionScope(Long id, String packageName, String activity,
            Integer hashCode) {
        this.id = id;
        this.packageName = packageName;
        this.activity = activity;
        this.hashCode = hashCode;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getActivity() {
        return this.activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public Integer getHashCode() {
        return hashCode;
    }

    public void setHashCode(Integer hashCode) {
        this.hashCode = hashCode;
    }

    @Override
    public String toString() {
        return "{" + packageName + ", " + activity + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionScope that = (ActionScope) o;
        return packageName.startsWith(that.packageName) &&
                (that.activity == null || Objects.equals(activity, that.activity));
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, activity);
    }
}