package cn.vove7.common.datamanager

import cn.vove7.common.R
import cn.vove7.common.app.GlobalLog
import cn.vove7.common.datamanager.executor.entity.MarkedData
import cn.vove7.common.datamanager.greendao.*
import cn.vove7.common.datamanager.parse.DataFrom
import cn.vove7.common.datamanager.parse.model.ActionScope
import cn.vove7.common.datamanager.parse.statusmap.ActionNode
import cn.vove7.vtp.log.Vog
import java.util.*

/**
 * # DaoHelper
 *
 * @author 17719247306
 * 2018/8/25
 */

typealias OnUpdate = (Int) -> Unit

object DaoHelper {
    fun deleteActionNodesInTX(ids: Array<Long>): Boolean {

        return try {
            DAO.daoSession.runInTx {
                ids.forEach {
                    deleteActionNode(it)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     *
     * @param node ActionNode
     * @return Int
     */
    fun insertNewActionNodeInTx(node: ActionNode): Int {
        try {
            DAO.daoSession.runInTx {
                insertNewActionNode(node)
            }
        } catch (e: Exception) {
            GlobalLog.err("${e.message} code:dh48")
            return R.string.text_an_err_happened
        }
        return R.string.text_have_done
    }


    fun deleteActionNodeInTX(nodeId: Long?): Boolean {
        if (nodeId == null) return false;
        return try {
            DAO.daoSession.runInTx {
                deleteActionNode(nodeId)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 删除指定ActionNode
     * @param nodeId Long
     * @return Boolean
     */
    @Throws(Exception::class)
    fun deleteActionNode(nodeId: Long) {
        // 删除记录 Action Reg --ActionScope-- 判断follow 保留scope
        val ancNode = DAO.daoSession.actionNodeDao.queryBuilder().where(ActionNodeDao.Properties.Id.eq(nodeId)).unique()
        val delFollows = LinkedList<ActionNode>()
        delFollows.add(ancNode)

        while (delFollows.isNotEmpty()) {
            val p = delFollows.poll()
            //添加follows至队列
            delFollows.addAll(DAO.daoSession.actionNodeDao.queryBuilder()
                    .where(ActionNodeDao.Properties.ParentId.eq(p.id)).list())
            //Action
            val a = p.actionId
            if (a != null && a > -0L)
                DAO.daoSession.actionDao.deleteByKey(a)

            //desc
            if (p.descId != null) {
                DAO.daoSession.actionDescDao.deleteByKey(p.descId)
            }

            //Regs
            DAO.daoSession.regDao.deleteInTx(
                    DAO.daoSession.regDao.queryBuilder()
                            .where(RegDao.Properties.NodeId.eq(p.id))
                            .list()
            )
            DAO.daoSession.actionNodeDao.delete(p)
        }
    }

    /**
     * 更新全局命令
     * 删除 scopeType is Global and not from user
     * @param nodes List<ActionNode>
     */
    fun updateGlobalInst(nodes: List<ActionNode>): Boolean {
        //
        return updateActionNodeByType(nodes, ActionNode.NODE_SCOPE_GLOBAL)
    }

    /**
     * 更新InApp命令
     * 删除 scopeType is Global and not from user
     * @param nodes List<ActionNode>
     */
    fun updateInAppInst(nodes: List<ActionNode>): Boolean {
        return updateActionNodeByType(nodes, ActionNode.NODE_SCOPE_IN_APP)
    }

    private fun updateActionNodeByType(nodes: List<ActionNode>, type: Int): Boolean {
        //
        val actionNodeDao = DAO.daoSession.actionNodeDao
        val olds = actionNodeDao.queryBuilder()
                .where(ActionNodeDao.Properties.From.notEq(DataFrom.FROM_USER),
                        ActionNodeDao.Properties.ActionScopeType.eq(type)).list()
        return try {
            DAO.daoSession.runInTx {
                olds.forEach {
                    //删除旧服务器数据
                    //del old global
                    deleteActionNode(it.id)
                }
                nodes.forEach {
                    if (it.isDelete) {
                        Vog.d(this, "updateActionNodeByType ---> 已删除 ${it.actionTitle}")
                        return@forEach
                    }

                    val oldNode = getActionNodeByTag(it.tagId)//存在？
                    if (oldNode == null || it.versionCode > oldNode.versionCode) {//更新
                        //判断属于用户
                        if (it.belongSelf()) {//set from -> user
                            it.from = DataFrom.FROM_USER
                            Vog.d(this, "updateActionNodeByType ---> 属于用户: " + it.actionTitle)
                        }
                        insertNewActionNode(it)
                    } else Vog.d(this, "updateGlobalInst ---> ${it.actionTitle} 存在")
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

    }

    fun getActionNodeByTag(tagId: String): ActionNode? {
        return DAO.daoSession.actionNodeDao.queryBuilder()
                .where(ActionNodeDao.Properties.TagId.eq(tagId))
                .unique()
    }


    /**
     *
     * @param newNode ActionNode
     * @return Long? newId
     */
    fun insertNewActionNode(newNode: ActionNode): Long? {
        newNode.id = null
        var newScopeId: Long = -1
        if (newNode.actionScope != null) {
            val scope = newNode.actionScope
            newScopeId = getActionScopeId(scope)
        }

        if (newScopeId != -1L)
            newNode.setScopeId(newScopeId)

        val desc = newNode.desc
        if (desc != null) {
            DAO.daoSession.actionDescDao.insert(desc)
            newNode.descId = desc.id
        }

        val action = newNode.action
        if (action != null) {
            DAO.daoSession.actionDao.insert(action)
            Vog.d(this, "save sid: $newScopeId")
            newNode.setActionId(action.id)
        }


        DAO.daoSession.actionNodeDao.insert(newNode)
        Vog.d(this, "save nodeId: ${newNode.id}")

        newNode.regs.forEach {
            it.id = null
            it.nodeId = newNode.id
            DAO.daoSession.regDao.insert(it)
        }

        //childs
        if (newNode.follows != null)
            for (ci in newNode.follows) {//递归 深度
                ci.parentId = newNode.id
                insertNewActionNode(ci)
            }
        return newNode.id
    }

    /**
     * 更新Marked数据
     * 删除来自服务器,保留用户数据
     * TODO tagid 防止删除重叠数据
     * @param types Array<String>
     * @param datas List<MarkedData>
     * @return Boolean
     */
    fun updateMarkedData(types: Array<String>, datas: List<MarkedData>): Boolean {
        val markedDao = DAO.daoSession.markedDataDao
        val l = markedDao.queryBuilder().where(
                MarkedDataDao.Properties.Type.`in`(*types),
                markedDao.queryBuilder().or(
                        MarkedDataDao.Properties.From.isNull,
                        MarkedDataDao.Properties.From.notEq(DataFrom.FROM_USER)
                )).list()
        val userList = markedDao.queryBuilder().where(
                MarkedDataDao.Properties.Type.`in`(*types),
                MarkedDataDao.Properties.From.eq(DataFrom.FROM_USER)
        ).list().toHashSet()
        return try {
            markedDao.deleteInTx(l)
            datas.forEach {
                if (!userList.contains(it)) {
                    if (it.belongUser(false)) {
                        Vog.d(this, "updateMarkedData ---> 标记为用户:" + it.key)
                        it.from = DataFrom.FROM_USER
                    }
                    markedDao.insert(it)
                } else {
                    Vog.d(this, "updateMarkedData ---> 重复:" + it.key)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            GlobalLog.err(e.message + "code: dh238")
            false
        }
    }

    /**
     * 更新广告数据
     * 删除来自服务器,保留用户数据
     *
     * @param types Array<String>
     * @param datas List<AppAdInfo>
     * @return Boolean
     */
    fun updateAppAdInfo(datas: List<AppAdInfo>): Boolean {
        val appAdInfoDao = DAO.daoSession.appAdInfoDao
        return try {
            val delList = appAdInfoDao.queryBuilder()
                    .where(AppAdInfoDao.Properties.From.notEq(DataFrom.FROM_USER)).list()
            appAdInfoDao.deleteInTx(delList)

            val userList = appAdInfoDao.queryBuilder().where(
                    AppAdInfoDao.Properties.From.eq(DataFrom.FROM_USER)
            ).list().toHashSet()

            datas.forEach {
                if (!userList.contains(it)) {
                    if (it.belongUser(false)) {
                        Vog.d(this, "updateMarkedData ---> 标记为用户:" + it.descTitle)
                        it.from = DataFrom.FROM_USER
                    }
                    appAdInfoDao.insertInTx(it)
                    Vog.d(this, "updateAppAdInfo ---> ${it.descTitle} ${it.id}")
                } else {
                    Vog.d(this, "updateMarkedData ---> 重复:" + it.descTitle)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 根据pkg ac 查找记录id
     * 若不存在 插入返回
     * @param pkg String
     * @param ac String?
     */
    fun getActionScopeId(scope: ActionScope): Long {
        val sCode = scope.genHashCode()
        val s = DAO.daoSession.actionScopeDao.queryBuilder()
                .where(ActionScopeDao.Properties.HashCode.eq(sCode))
                .unique()
        return if (s == null) {
            DAO.daoSession.actionScopeDao.insert(scope)
            scope.id
        } else s.id
    }

    fun getInsetSettingsByName(name: String): InstSettings? {
        return DAO.daoSession.instSettingsDao.queryBuilder()
                .where(InstSettingsDao.Properties.Name.eq(name)).unique()
    }
}