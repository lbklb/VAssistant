package cn.vove7.datamanager.greendao;

import java.util.Map;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import cn.vove7.datamanager.executor.entity.MarkedContact;
import cn.vove7.datamanager.executor.entity.MarkedOpen;
import cn.vove7.datamanager.executor.entity.ServerContact;
import cn.vove7.datamanager.parse.model.Action;
import cn.vove7.datamanager.parse.model.ActionScope;
import cn.vove7.datamanager.parse.model.Param;
import cn.vove7.datamanager.parse.statusmap.MapNode;
import cn.vove7.datamanager.parse.statusmap.Reg;

import cn.vove7.datamanager.greendao.MarkedContactDao;
import cn.vove7.datamanager.greendao.MarkedOpenDao;
import cn.vove7.datamanager.greendao.ServerContactDao;
import cn.vove7.datamanager.greendao.ActionDao;
import cn.vove7.datamanager.greendao.ActionScopeDao;
import cn.vove7.datamanager.greendao.ParamDao;
import cn.vove7.datamanager.greendao.MapNodeDao;
import cn.vove7.datamanager.greendao.RegDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see org.greenrobot.greendao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig markedContactDaoConfig;
    private final DaoConfig markedOpenDaoConfig;
    private final DaoConfig serverContactDaoConfig;
    private final DaoConfig actionDaoConfig;
    private final DaoConfig actionScopeDaoConfig;
    private final DaoConfig paramDaoConfig;
    private final DaoConfig mapNodeDaoConfig;
    private final DaoConfig regDaoConfig;

    private final MarkedContactDao markedContactDao;
    private final MarkedOpenDao markedOpenDao;
    private final ServerContactDao serverContactDao;
    private final ActionDao actionDao;
    private final ActionScopeDao actionScopeDao;
    private final ParamDao paramDao;
    private final MapNodeDao mapNodeDao;
    private final RegDao regDao;

    public DaoSession(Database db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        markedContactDaoConfig = daoConfigMap.get(MarkedContactDao.class).clone();
        markedContactDaoConfig.initIdentityScope(type);

        markedOpenDaoConfig = daoConfigMap.get(MarkedOpenDao.class).clone();
        markedOpenDaoConfig.initIdentityScope(type);

        serverContactDaoConfig = daoConfigMap.get(ServerContactDao.class).clone();
        serverContactDaoConfig.initIdentityScope(type);

        actionDaoConfig = daoConfigMap.get(ActionDao.class).clone();
        actionDaoConfig.initIdentityScope(type);

        actionScopeDaoConfig = daoConfigMap.get(ActionScopeDao.class).clone();
        actionScopeDaoConfig.initIdentityScope(type);

        paramDaoConfig = daoConfigMap.get(ParamDao.class).clone();
        paramDaoConfig.initIdentityScope(type);

        mapNodeDaoConfig = daoConfigMap.get(MapNodeDao.class).clone();
        mapNodeDaoConfig.initIdentityScope(type);

        regDaoConfig = daoConfigMap.get(RegDao.class).clone();
        regDaoConfig.initIdentityScope(type);

        markedContactDao = new MarkedContactDao(markedContactDaoConfig, this);
        markedOpenDao = new MarkedOpenDao(markedOpenDaoConfig, this);
        serverContactDao = new ServerContactDao(serverContactDaoConfig, this);
        actionDao = new ActionDao(actionDaoConfig, this);
        actionScopeDao = new ActionScopeDao(actionScopeDaoConfig, this);
        paramDao = new ParamDao(paramDaoConfig, this);
        mapNodeDao = new MapNodeDao(mapNodeDaoConfig, this);
        regDao = new RegDao(regDaoConfig, this);

        registerDao(MarkedContact.class, markedContactDao);
        registerDao(MarkedOpen.class, markedOpenDao);
        registerDao(ServerContact.class, serverContactDao);
        registerDao(Action.class, actionDao);
        registerDao(ActionScope.class, actionScopeDao);
        registerDao(Param.class, paramDao);
        registerDao(MapNode.class, mapNodeDao);
        registerDao(Reg.class, regDao);
    }
    
    public void clear() {
        markedContactDaoConfig.clearIdentityScope();
        markedOpenDaoConfig.clearIdentityScope();
        serverContactDaoConfig.clearIdentityScope();
        actionDaoConfig.clearIdentityScope();
        actionScopeDaoConfig.clearIdentityScope();
        paramDaoConfig.clearIdentityScope();
        mapNodeDaoConfig.clearIdentityScope();
        regDaoConfig.clearIdentityScope();
    }

    public MarkedContactDao getMarkedContactDao() {
        return markedContactDao;
    }

    public MarkedOpenDao getMarkedOpenDao() {
        return markedOpenDao;
    }

    public ServerContactDao getServerContactDao() {
        return serverContactDao;
    }

    public ActionDao getActionDao() {
        return actionDao;
    }

    public ActionScopeDao getActionScopeDao() {
        return actionScopeDao;
    }

    public ParamDao getParamDao() {
        return paramDao;
    }

    public MapNodeDao getMapNodeDao() {
        return mapNodeDao;
    }

    public RegDao getRegDao() {
        return regDao;
    }

}
