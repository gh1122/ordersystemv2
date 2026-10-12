package com.gh.ordersystem.service.impl;

import com.gh.ordersystem.model.vo.MenuVo;
import com.gh.ordersystem.repository.MenuMapper;
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

@Service
public class MenuServiceImpl implements MenuService {
  @Autowired
  private MenuMapper menuMapper;

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
