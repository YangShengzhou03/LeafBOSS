package com.leafboss.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.common.Result;
import com.leafboss.entity.BossReview;
import com.leafboss.entity.CardKey;
import com.leafboss.entity.Company;
import com.leafboss.entity.ReviewVote;
import com.leafboss.mapper.CompanyMapper;
import com.leafboss.mapper.ReviewVoteMapper;
import com.leafboss.service.BossReviewService;
import com.leafboss.service.CardKeyService;
import com.leafboss.service.CompanyService;
import com.leafboss.service.OperationLogService;
import com.leafboss.utils.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/boss-reviews")
public class PublicBossReviewController {

    @Autowired
    private BossReviewService bossReviewService;

    @Autowired
    private CardKeyService cardKeyService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private ReviewVoteMapper reviewVoteMapper;

    @Autowired
    private CompanyMapper companyMapper;

    @PostMapping
    @Transactional
    public Result<Boolean> createReview(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String cardKey = request.get("card_key");
        String companyName = request.get("company_name");
        String content = request.get("content");

        if (cardKey == null || cardKey.trim().isEmpty()) {
            return Result.error("卡密不能为空");
        }
        if (companyName == null || companyName.trim().isEmpty()) {
            return Result.error("公司名不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            return Result.error("评论内容不能为空");
        }

        QueryWrapper<CardKey> cardKeyQuery = new QueryWrapper<>();
        cardKeyQuery.eq("card_key", cardKey);
        CardKey cardKeyEntity = cardKeyService.getOne(cardKeyQuery);

        if (cardKeyEntity == null) {
            return Result.error("卡密不存在");
        }

        if (!"已使用".equals(cardKeyEntity.getStatus())) {
            return Result.error("该卡密未使用，无法发表评论");
        }

        Company company = companyService.getOrCreateByName(companyName);

        QueryWrapper<BossReview> reviewQuery = new QueryWrapper<>();
        reviewQuery.eq("card_key", cardKey);
        reviewQuery.eq("company_id", company.getId());
        if (bossReviewService.count(reviewQuery) > 0) {
            return Result.error("您已经评论过该公司");
        }

        BossReview review = new BossReview();
        review.setCardKey(cardKey);
        review.setCompanyId(company.getId());
        review.setContent(content);

        boolean saved = bossReviewService.save(review);
        if (saved) {
            companyService.update(new UpdateWrapper<Company>().eq("id", company.getId())
                    .setSql("comment_count = comment_count + 1"));
            operationLogService.logOperation("管理", "用户发表评论", LogUtil.getClientIpAddress(httpRequest));
            return Result.success("评论发布成功", true);
        } else {
            return Result.error("评论发布失败");
        }
    }

    @GetMapping
    public Result<Object> getReviews(
            @RequestParam String company_name,
            @RequestParam(required = false) String card_key,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (company_name == null || company_name.trim().isEmpty()) {
            return Result.error("公司名不能为空");
        }

        // 不存在则建公司并计 1 次浏览，存在则浏览数自增，一条 SQL 完成
        // ponytail: 刷新会重复计数，如需精确统计按 IP+日期去重
        companyMapper.upsertViewCount(company_name);

        Page<BossReview> pageInfo = new Page<>(page, size);
        Page<BossReview> result = bossReviewService.pageByCompanyName(pageInfo, company_name);

        // 标记当前卡密在本页各评论上的投票状态（不带卡密则不标记）
        if (!result.getRecords().isEmpty() && card_key != null && !card_key.trim().isEmpty()) {
            QueryWrapper<ReviewVote> voteQuery = new QueryWrapper<>();
            voteQuery.eq("card_key", card_key)
                    .in("review_id", result.getRecords().stream().map(BossReview::getId).collect(Collectors.toList()));
            Map<Integer, String> myVotes = reviewVoteMapper.selectList(voteQuery).stream()
                    .collect(Collectors.toMap(ReviewVote::getReviewId, ReviewVote::getVote));
            result.getRecords().forEach(r -> r.setMyVote(myVotes.get(r.getId())));
        }

        return Result.success("评论列表查询成功", Map.of(
            "page", result.getCurrent(),
            "size", result.getSize(),
            "total", result.getTotal(),
            "records", result.getRecords()
        ));
    }

    /**
     * 点赞/点踩，按卡密去重（一卡一票，与评论的卡密体系一致）：
     * 未投过则计票，投了相同票则取消，投了另一票则改票
     */
    @PostMapping("/{id}/vote")
    @Transactional
    public Result<Object> vote(@PathVariable Integer id,
                               @RequestBody Map<String, String> request) {
        String vote = request.get("vote");
        if (!"like".equals(vote) && !"dislike".equals(vote)) {
            return Result.error("投票类型只能是 like 或 dislike");
        }

        String cardKey = request.get("card_key");
        if (cardKey == null || cardKey.trim().isEmpty()) {
            return Result.error("卡密不能为空");
        }
        QueryWrapper<CardKey> cardKeyQuery = new QueryWrapper<>();
        cardKeyQuery.eq("card_key", cardKey);
        CardKey cardKeyEntity = cardKeyService.getOne(cardKeyQuery);
        if (cardKeyEntity == null) {
            return Result.error("卡密不存在");
        }
        if (!"已使用".equals(cardKeyEntity.getStatus())) {
            return Result.error("该卡密未使用，无法投票");
        }

        BossReview review = bossReviewService.getById(id);
        if (review == null) {
            return Result.error("评论不存在");
        }

        QueryWrapper<ReviewVote> query = new QueryWrapper<>();
        query.eq("review_id", id).eq("card_key", cardKey);
        ReviewVote existing = reviewVoteMapper.selectOne(query);
        boolean canceling = existing != null && existing.getVote().equals(vote);

        if (existing == null) {
            ReviewVote v = new ReviewVote();
            v.setReviewId(id);
            v.setCardKey(cardKey);
            v.setVote(vote);
            reviewVoteMapper.insert(v);
            bossReviewService.update(new UpdateWrapper<BossReview>().eq("id", id)
                    .setSql(vote + "_count = " + vote + "_count + 1"));
        } else if (canceling) {
            reviewVoteMapper.deleteById(existing.getId());
            bossReviewService.update(new UpdateWrapper<BossReview>().eq("id", id)
                    .gt(vote + "_count", 0)
                    .setSql(vote + "_count = " + vote + "_count - 1"));
        } else {
            String oldVote = existing.getVote();
            existing.setVote(vote);
            reviewVoteMapper.updateById(existing);
            bossReviewService.update(new UpdateWrapper<BossReview>().eq("id", id)
                    .gt(oldVote + "_count", 0)
                    .setSql(oldVote + "_count = " + oldVote + "_count - 1"));
            bossReviewService.update(new UpdateWrapper<BossReview>().eq("id", id)
                    .setSql(vote + "_count = " + vote + "_count + 1"));
        }

        BossReview updated = bossReviewService.getById(id);
        return Result.success("投票成功", Map.of(
            "likeCount", updated.getLikeCount(),
            "dislikeCount", updated.getDislikeCount(),
            "myVote", canceling ? "" : vote
        ));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Boolean> deleteReview(@PathVariable Integer id,
                                        @RequestParam String cardKey,
                                        HttpServletRequest httpRequest) {
        if (cardKey == null || cardKey.trim().isEmpty()) {
            return Result.error("卡密不能为空");
        }

        BossReview review = bossReviewService.getById(id);
        if (review == null) {
            return Result.error("评论不存在");
        }

        if (!cardKey.equals(review.getCardKey())) {
            return Result.error("卡密不匹配，无法删除该评论");
        }

        boolean deleted = bossReviewService.removeById(id);
        if (deleted) {
            companyService.update(new UpdateWrapper<Company>().eq("id", review.getCompanyId())
                    .gt("comment_count", 0)
                    .setSql("comment_count = comment_count - 1"));
            operationLogService.logOperation("管理", "用户删除评论", LogUtil.getClientIpAddress(httpRequest));
            return Result.success("评论删除成功", true);
        } else {
            return Result.error("评论删除失败");
        }
    }
}
