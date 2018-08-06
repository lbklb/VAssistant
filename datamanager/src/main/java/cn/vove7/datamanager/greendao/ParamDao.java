package cn.vove7.datamanager.greendao;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

import cn.vove7.datamanager.parse.model.Param;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "PARAM".
*/
public class ParamDao extends AbstractDao<Param, Long> {

    public static final String TABLENAME = "PARAM";

    /**
     * Properties of entity Param.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property Desc = new Property(1, String.class, "desc", false, "DESC");
        public final static Property AskText = new Property(2, String.class, "askText", false, "ASK_TEXT");
    }


    public ParamDao(DaoConfig config) {
        super(config);
    }
    
    public ParamDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"PARAM\" (" + //
                "\"_id\" INTEGER PRIMARY KEY ," + // 0: id
                "\"DESC\" TEXT," + // 1: desc
                "\"ASK_TEXT\" TEXT);"); // 2: askText
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"PARAM\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, Param entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        String desc = entity.getDesc();
        if (desc != null) {
            stmt.bindString(2, desc);
        }
 
        String askText = entity.getAskText();
        if (askText != null) {
            stmt.bindString(3, askText);
        }
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, Param entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        String desc = entity.getDesc();
        if (desc != null) {
            stmt.bindString(2, desc);
        }
 
        String askText = entity.getAskText();
        if (askText != null) {
            stmt.bindString(3, askText);
        }
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public Param readEntity(Cursor cursor, int offset) {
        Param entity = new Param( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1), // desc
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2) // askText
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, Param entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setDesc(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setAskText(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(Param entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(Param entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(Param entity) {
        return entity.getId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}