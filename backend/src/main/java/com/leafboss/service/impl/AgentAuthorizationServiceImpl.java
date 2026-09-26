package com.leafboss.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.entity.AgentAuthorization;
import com.leafboss.entity.Product;
import com.leafboss.entity.Specification;
import com.leafboss.entity.User;
import com.leafboss.mapper.AgentAuthorizationMapper;
import com.leafboss.mapper.ProductMapper;
import com.leafboss.mapper.SpecificationMapper;
import com.leafboss.mapper.UserMapper;
import com.leafboss.service.AgentAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgentAuthorizationServiceImpl implements AgentAuthorizationService {

    @Autowired
    private AgentAuthorizationMapper agentAuthorizationMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private SpecificationMapper specificationMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public AgentAuthorization getById(Integer id) {
        return agentAuthorizationMapper.selectById(id);
    }

    @Override
    public AgentAuthorization findActiveAuthorization(String agentId, Integer productId) {
        QueryWrapper<AgentAuthorization> wrapper = new QueryWrapper<>();
        wrapper.eq("agent_id", agentId)
               .eq("product_id", productId)
               .eq("status", 1)
               .and(w -> w.isNull("expires_at").or().gt("expires_at", LocalDateTime.now()))
               .orderByDesc("expires_at")
               .last("LIMIT 1");
        return agentAuthorizationMapper.selectOne(wrapper);
    }

    @Override
    public boolean deductRemaining(String agentId, Integer productId, int count) {
        // 原子扣减，WHERE 条件保证并发下不超扣（count 为 int，无注入风险）
        UpdateWrapper<AgentAuthorization> wrapper = new UpdateWrapper<>();
        wrapper.setSql("remaining_count = remaining_count - " + count)
               .eq("agent_id", agentId)
               .eq("product_id", productId)
               .eq("status", 1)
               .ge("remaining_count", count)
               .and(w -> w.isNull("expires_at").or().gt("expires_at", LocalDateTime.now()));
        return agentAuthorizationMapper.update(null, wrapper) > 0;
    }

    @Override
    public void refundRemaining(String agentId, Integer productId, int count) {
        UpdateWrapper<AgentAuthorization> wrapper = new UpdateWrapper<>();
        wrapper.setSql("remaining_count = remaining_count + " + count)
               .eq("agent_id", agentId)
               .eq("product_id", productId);
        agentAuthorizationMapper.update(null, wrapper);
    }

    @Override
    public List<Map<String, Object>> findAuthorizedProducts(String agentId) {
        QueryWrapper<AgentAuthorization> wrapper = new QueryWrapper<>();
        wrapper.eq("agent_id", agentId)
               .eq("status", 1)
               .and(w -> w.isNull("expires_at").or().gt("expires_at", LocalDateTime.now()));
        List<AgentAuthorization> authorizations = agentAuthorizationMapper.selectList(wrapper);
        if (authorizations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> productIds = authorizations.stream()
                .map(AgentAuthorization::getProductId)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, String> productNameMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            productNameMap = productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));
        }
        // 批量获取商品规格名，按商品分组拼接
        Map<Integer, String> specNameMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            QueryWrapper<Specification> specWrapper = new QueryWrapper<>();
            specWrapper.in("product_id", productIds).orderByAsc("id");
            specNameMap = specificationMapper.selectList(specWrapper).stream()
                    .collect(Collectors.groupingBy(Specification::getProductId,
                            Collectors.mapping(Specification::getName, Collectors.joining(","))));
        }
        // 对每个授权直接返回余额
        final Map<Integer, String> nameMap = productNameMap;
        final Map<Integer, String> specsMap = specNameMap;
        return authorizations.stream().map(auth -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("authId", auth.getId());
            map.put("productId", auth.getProductId());
            map.put("productName", nameMap.getOrDefault(auth.getProductId(), ""));
            map.put("specNames", specsMap.getOrDefault(auth.getProductId(), ""));
            map.put("remaining", auth.getRemainingCount());
            map.put("grantedAt", auth.getGrantedAt());
            map.put("expiresAt", auth.getExpiresAt());
            map.put("status", auth.getStatus());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<Map<String, Object>> listWithDetails(Page<AgentAuthorization> pageParam, String keyword, Integer status) {
        QueryWrapper<AgentAuthorization> wrapper = new QueryWrapper<>();

        if (status != null) {
            wrapper.eq("status", status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 搜索 agent_id 或关联的邮箱/用户名需后置过滤
            wrapper.and(w -> w.like("agent_id", keyword));
        }
        wrapper.orderByDesc("created_at");

        Page<AgentAuthorization> page = agentAuthorizationMapper.selectPage(pageParam, wrapper);
        List<AgentAuthorization> records = page.getRecords();

        Page<Map<String, Object>> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (records.isEmpty()) {
            resultPage.setRecords(new ArrayList<>());
            return resultPage;
        }

        // 批量获取代理商信息
        List<String> agentIds = records.stream()
                .map(AgentAuthorization::getAgentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, User> agentMap = new HashMap<>();
        if (!agentIds.isEmpty()) {
            agentMap = userMapper.selectBatchIds(agentIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        }

        // 批量获取商品信息
        List<Integer> productIds = records.stream()
                .map(AgentAuthorization::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, Product> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            productMap = productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        }

        // 如果 keyword 是邮箱/用户名，按 agent 信息后置过滤
        Map<String, User> finalAgentMap = agentMap;
        List<AgentAuthorization> filtered = records;
        if (keyword != null && !keyword.trim().isEmpty()) {
            filtered = records.stream().filter(auth -> {
                User agent = finalAgentMap.get(auth.getAgentId());
                if (agent != null) {
                    return (agent.getEmail() != null && agent.getEmail().contains(keyword))
                        || (agent.getUsername() != null && agent.getUsername().contains(keyword));
                }
                return auth.getAgentId() != null && auth.getAgentId().contains(keyword);
            }).collect(Collectors.toList());
            resultPage.setTotal(filtered.size());
        }

        Map<Integer, Product> finalProductMap = productMap;
        List<Map<String, Object>> dtoList = filtered.stream().map(auth -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", auth.getId());
            map.put("agentId", auth.getAgentId());
            User agent = finalAgentMap.get(auth.getAgentId());
            map.put("agentName", agent != null ? agent.getUsername() : null);
            map.put("agentEmail", agent != null ? agent.getEmail() : null);
            map.put("productId", auth.getProductId());
            Product product = finalProductMap.get(auth.getProductId());
            map.put("productName", product != null ? product.getName() : null);
            map.put("remainingCount", auth.getRemainingCount());
            map.put("grantedAt", auth.getGrantedAt());
            map.put("expiresAt", auth.getExpiresAt());
            map.put("status", auth.getStatus());
            return map;
        }).collect(Collectors.toList());

        resultPage.setRecords(dtoList);
        return resultPage;
    }

    @Override
    public boolean createAuthorization(AgentAuthorization auth) {
        if (auth.getGrantedAt() == null) {
            auth.setGrantedAt(LocalDateTime.now());
        }
        if (auth.getStatus() == null) {
            auth.setStatus(1);
        }
        auth.setCreatedAt(LocalDateTime.now());
        auth.setUpdatedAt(LocalDateTime.now());
        return agentAuthorizationMapper.insert(auth) > 0;
    }

    @Override
    public boolean updateAuthorization(AgentAuthorization auth) {
        auth.setUpdatedAt(LocalDateTime.now());
        return agentAuthorizationMapper.updateById(auth) > 0;
    }

    @Override
    public boolean revoke(Integer id) {
        AgentAuthorization auth = agentAuthorizationMapper.selectById(id);
        if (auth == null) return false;
        auth.setStatus(0);
        auth.setUpdatedAt(LocalDateTime.now());
        return agentAuthorizationMapper.updateById(auth) > 0;
    }

    @Override
    public boolean restore(Integer id) {
        AgentAuthorization auth = agentAuthorizationMapper.selectById(id);
        if (auth == null) return false;
        auth.setStatus(1);
        auth.setUpdatedAt(LocalDateTime.now());
        return agentAuthorizationMapper.updateById(auth) > 0;
    }

    @Override
    public boolean delete(Integer id) {
        return agentAuthorizationMapper.deleteById(id) > 0;
    }
}
