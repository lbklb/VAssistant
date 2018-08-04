package cn.vove7.datamanager.greendao;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

import cn.vove7.datamanager.parse.model.Action;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "ACTION".
*/
public class ActionDao extends AbstractDao<Action, Long> {

    public static final String TABLENAME = "ACTION";

    /**
     * Properties of entity Action.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property Priority = new Property(1, int.class, "priority", false, "PRIORITY");
        public final static Property NodeId = new Property(2, long.class, "nodeId", false, "NODE_ID");
        public final static Property ActionScript = new Property(3, String.class, "actionScript", false, "ACTION_SCRIPT");
    }


    public ActionDao(DaoConfig config) {
        super(config);
    }
    
    public ActionDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"ACTION\" (" + //
                "\"_id\" INTEGER PRIMARY KEY ," + // 0: id
                "\"PRIORITY\" INTEGER NOT NULL ," + // 1: priority
                "\"NODE_ID\" INTEGER NOT NULL ," + // 2: nodeId
                "\"ACTION_SCRIPT\" TEXT);"); // 3: actionScript
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"ACTION\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, Action entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindLong(2, entity.getPriority());
        stmt.bindLong(3, entity.getNodeId());
 
        String actionScript = entity.getActionScript();
        if (actionScript != null) {
            stmt.bindString(4, actionScript);
        }
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, Action entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindLong(2, entity.getPriority());
        stmt.bindLong(3, entity.getNodeId());
 
        String actionScript = entity.getActionScript();
        if (actionScript != null) {
            stmt.bindString(4, actionScript);
        }
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public Action readEntity(Cursor cursor, int offset) {
        Action entity = new Action( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.getInt(offset + 1), // priority
            cursor.getLong(offset + 2), // nodeId
            cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3) // actionScript
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, Action entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setPriority(cursor.getInt(offset + 1));
        entity.setNodeId(cursor.getLong(offset + 2));
        entity.setActionScript(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(Action entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(Action entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(Action entity) {
        return entity.getId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}
