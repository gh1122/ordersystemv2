package com.gh.ordersystem.service.impl;

import com.gh.ordersystem.model.vo.MenuVo;
import com.gh.ordersystem.repository.MenuMapper;
import com.gh.ordersystem.repository.RoleMenuMapper;
import com.gh.ordersystem.repository.UserRoleMapper;
import com.gh.ordersystem.service.MenuService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gh.ordersystem.model.vo.BaseVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gh.ordersystem.model.entity.Menu;
import com.gh.ordersystem.model.entity.RoleMenu;
import com.gh.ordersystem.model.entity.UserRole;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MenuServiceImpl implements MenuService {
  @Autowired
  private MenuMapper menuMapper;
  @Autowired
  private UserRoleMapper userRoleMapper;
  @Autowired
  private RoleMenuMapper roleMenuMapper;

  /**
   * 获取完整菜单树（不做权限过滤）
   *
   * 用途：管理员的「菜单管理」页面，需要看到系统全部菜单（包括未分配给任何角色的）。
   * 注意区分：第 3 步还会写一个 getUserMenuTree（按用户权限过滤），给登录后的侧边栏用。
   *
   * 前端类比：
   * 完整树 ≈ 路由配置文件里的全量 routes 数组
   * 用户菜单 ≈ 根据 meta.roles 过滤后动态注册的那部分路由
   */
  @Override
  public BaseVo<List<MenuVo>> getMenuTree() {
    // 1. 查询全部菜单，按 sort 升序排列
    // 前端类比：接口返回列表前先 sort((a, b) => a.sort - b.sort)
    // 不排序的话数据库返回顺序不保证，树里菜单顺序会乱
    QueryWrapper<Menu> wrapper = new QueryWrapper<>();
    wrapper.orderByAsc("sort");
    List<Menu> menus = menuMapper.selectList(wrapper);

    // 2. 平铺列表 → 树形结构（核心转换在 buildTree 里）
    return BaseVo.success(buildTree(menus));
  }

  /**
   * 获取导航菜单（按用户角色权限过滤）
   *
   * 查询链路（三次单表查询，全程零手写 SQL）：
   * userId → t_user_role → roleIds
   * → t_role_menu → menuIds
   * → t_menu where id in (menuIds) → buildTree
   *
   * 前端类比：routes.filter(route => route.meta.roles.includes(myRole))
   * 区别在于"我的角色有哪些菜单"要查三张表才知道。
   *
   * userId 从哪来？Controller 从 request.getAttribute("userId") 取——
   * 那是 JwtInterceptor 解析 Token 后挂上去的（还附带过 Redis 登录态校验）。
   * Service 不碰 HTTP 概念，只管"给 userId，还菜单树"。
   */
  @Override
  public BaseVo<List<MenuVo>> getNavMenu(Integer userId) {
    // 0. userId 为空说明请求没经过正常鉴权，直接拒绝
    if (userId == null) {
      return BaseVo.error(400, "用户ID不能为空");
    }

    // 1. 查 t_user_role → roleIds
    // SELECT * FROM t_user_role WHERE user_id = ?
    QueryWrapper<UserRole> userRoleWrapper = new QueryWrapper<>();
    userRoleWrapper.eq("user_id", userId);
    List<UserRole> userRoles = userRoleMapper.selectList(userRoleWrapper);
    log.info("userRoles: {}", userRoles);
    // log.info("userRoles: {}", userRoles); // userRoles[UserRole(id=1, userId=1,
    // roleId=1)]
    List<Integer> roleIds = userRoles.stream()
        .map(UserRole::getRoleId)
        .collect(Collectors.toList());
    log.info("roleIds: {}", roleIds);
    // ⚠️ 防坑点 1：roleIds 为空必须提前返回
    // 没分配任何角色的用户（新注册的）roleIds 是空列表，
    // 空列表进 wrapper.in() 会生成非法 SQL：WHERE role_id IN () → 直接报错
    // 前端类比：ids.map(...) 之前先判 if (!ids.length) return []
    if (roleIds.isEmpty()) {
      return BaseVo.success(new ArrayList<>());
    }

    // 2. 查 t_role_menu → menuIds
    // SELECT * FROM t_role_menu WHERE role_id IN (1,2,...)
    // ⚠️ 防坑点 2：多角色的菜单会重叠，必须去重
    // 比如管理员和审计员都能看订单管理，menuIds 里就出现两个相同的 id
    // （去重不影响正确性，但 IN (1,2,3,3) 既浪费也不干净）
    // 前端类比：[...new Set(ids)]
    QueryWrapper<RoleMenu> roleMenuWrapper = new QueryWrapper<>();
    roleMenuWrapper.in("role_id", roleIds);
    List<RoleMenu> roleMenus = roleMenuMapper.selectList(roleMenuWrapper);
    log.info("roleMenus: {}", roleMenus);

    List<Integer> menuIds = roleMenus.stream()
        .map(RoleMenu::getMenuId)
        .distinct()
        .collect(Collectors.toList());

    // ⚠️ 防坑点 1 同款：menuIds 也可能为空（角色存在但一个菜单都没配）
    if (menuIds.isEmpty()) {
      return BaseVo.success(new ArrayList<>());
    }

    // 3. 查 t_menu → buildTree
    // SELECT * FROM t_menu WHERE id IN (1,2,...) ORDER BY sort ASC
    // 不排序的话树内菜单顺序不保证
    QueryWrapper<Menu> menuWrapper = new QueryWrapper<>();
    menuWrapper.in("id", menuIds).orderByAsc("sort");

    List<Menu> menus = menuMapper.selectList(menuWrapper);

    // 树组装逻辑零新增——第 2 步抽好的 buildTree 直接复用
    return BaseVo.success(buildTree(menus));
  }

  /**
   * 平铺列表 → 树形结构（Map 索引版，前端 buildTreeByMap 的直译）
   *
   * 输入（数据库查出来的，每行只有 parent_id 指向父节点，是"散装"的）：
   * [{ id:1, parentId:0 }, { id:2, parentId:1 }, { id:3, parentId:2 }, ...]
   *
   * 输出（前端能直接渲染侧边栏的嵌套结构）：
   * [{ id:1, parentId:0, children: [
   * { id:2, parentId:1, children: [
   * { id:3, parentId:2, children: [] }]}]}]
   *
   * 前端原版（同构对照）：
   * function buildTreeByMap(list) {
   * const map = new Map();
   * const roots = [];
   * for (const item of list) {
   * map.set(item.id, { ...item, children: [] }); // ← 对应下面第一步
   * }
   * for (const item of list) {
   * const parent = map.get(item.parentId);
   * if (parent) parent.children.push(node); // ← 对应下面第二步
   * else roots.push(node);
   * }
   * return roots;
   * }
   *
   * 为什么是 O(n)？
   * 两次遍历各扫一遍数组，Map 的 get/put 都是 O(1)。
   * 对比递归写法（每层 filter 全量扫描）是 O(n²)。
   *
   * 为什么用 Map<id, node> 而不是 groupingBy（Map<parentId, 孩子列表>）？
   * 两者同为 O(n)，方向相反：这里是"孩子找爸爸"，groupingBy 是"爸爸找孩子"。
   * 选 Map 版的两个理由：
   * 1. 和前端写法完全同构，看代码零成本；
   * 2. 边界行为更稳：孤儿节点（parentId 指向不存在的菜单，比如父菜单被误删）
   * 会兜底渲染成根节点，而不是像 groupingBy 版那样既挂不进谁也不是根、
   * 静默消失——"配了菜单却看不到还没报错"是生产环境很烦的 bug。
   */
  private List<MenuVo> buildTree(List<Menu> menus) {
    // 第一步：Entity → VO，并顺手复制一份平铺的 VO 列表
    // （和 UserService 里的 toUserVo 一个套路：Entity 含敏感/内部字段，不直接出门）
    List<MenuVo> voList = menus.stream().map(this::toMenuVo).collect(Collectors.toList());

    // 第二步：全部节点存入 map，预创建 children
    // 等价于前端的 map.set(item.id, { ...item, children: [] })
    // 注意：children 必须先创建——下一步挂载时要往里 add，
    // 如果是 null 会直接 NullPointerException
    Map<Integer, MenuVo> map = new HashMap<>();
    for (MenuVo vo : voList) {
      vo.setChildren(new ArrayList<>());
      map.put(vo.getId(), vo);
    }

    // 第三步：遍历，挂载子节点到父节点；找不到父节点 = 根节点
    // 等价于前端的 map.get(item.parentId)，这里用普通 for 循环更直观
    List<MenuVo> roots = new ArrayList<>();
    for (MenuVo vo : voList) {
      MenuVo parent = map.get(vo.getParentId());
      if (parent != null) {
        parent.getChildren().add(vo);
      } else {
        // parentId 为 0（map 里没有 id=0 的节点），或者指向了不存在的父菜单（孤儿兜底）
        roots.add(vo);
      }
    }
    return roots;
  }

  /**
   * Menu（数据库实体）→ MenuVo（返回给前端的视图对象），逐字段手工映射
   *
   * 为什么要转？前端类比：接口返回数据要脱敏/裁剪——
   * Entity 是数据库表在 Java 里的投影，字段可能含敏感信息或前端用不到的字段，
   * 直接返回会泄露内部结构；VO 是"给前端的合同"，字段稳定可控。
   *
   * 为什么手写而不是用 BeanUtils.copyProperties？
   * 手写虽然啰嗦，但编译期就能检查：字段改名时这里立刻报错；
   * 反射拷贝（copyProperties）字段名拼错只会静默跳过，运行期才发现。
   * 等字段多了、项目大了，可以再引入 MapStruct 这类编译期生成映射代码的工具。
   */
  private MenuVo toMenuVo(Menu menu) {
    MenuVo vo = new MenuVo();
    vo.setId(menu.getId());
    vo.setMenuName(menu.getMenuName());
    vo.setPath(menu.getPath());
    vo.setIcon(menu.getIcon());
    vo.setParentId(menu.getParentId());
    vo.setSort(menu.getSort());
    vo.setType(menu.getType());
    vo.setPermission(menu.getPermission());
    vo.setCreateTime(menu.getCreateTime());
    // 注意：children 不在这里 set，统一由 buildTree 的「挂载」步骤负责
    return vo;
  }
}
