package com.matchimban.matchimban_api.restaurant.service.serviceImpl;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.restaurant.dto.response.MyReviewsResponse;
import com.matchimban.matchimban_api.restaurant.dto.response.ReviewDetailResponse;
import com.matchimban.matchimban_api.restaurant.dto.view.MyReviewSummary;
import com.matchimban.matchimban_api.restaurant.error.ReviewErrorCode;
import com.matchimban.matchimban_api.restaurant.repository.ReviewRepository;
import com.matchimban.matchimban_api.restaurant.repository.projection.MyReviewRow;
import com.matchimban.matchimban_api.restaurant.service.ReviewReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.matchimban.matchimban_api.global.time.TimeKst.toKstLocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewReadServiceImpl implements ReviewReadService {

    private final ReviewRepository reviewRepository;

    @Override
    public MyReviewsResponse getMyReviews(Long memberId, Long cursor, int size) {
        Pageable pageable = PageRequest.of(0, size + 1);

        List<MyReviewRow> rows = reviewRepository.findMyReviewRows(memberId, cursor, pageable);

        boolean hasNext = rows.size() > size;
        List<MyReviewRow> pageRows = hasNext ? rows.subList(0, size) : rows;

        if (pageRows.isEmpty()) {
            return new MyReviewsResponse(List.of(), null, false);
        }

        Long nextCursor = hasNext ? pageRows.get(pageRows.size() - 1).getReviewId() : null;

        List<MyReviewSummary> items = pageRows.stream()
                .map(r -> new MyReviewSummary(
                        r.getReviewId(),
                        r.getMeetingId(),
                        r.getRating(),
                        r.getContent(),
                        toKstLocalDateTime(r.getCreatedAt()),
                        r.getRestaurantId(),
                        r.getRestaurantName(),
                        r.getRestaurantImageUrl1(),
                        r.getCategoryName(),
                        r.getCategoryEmoji()
                ))
                .toList();

        return new MyReviewsResponse(items, nextCursor, hasNext);
    }

    @Override
    public ReviewDetailResponse getMyReviewDetail(Long memberId, Long reviewId) {

        var row = reviewRepository.findMyReviewDetailRow(reviewId, memberId)
                .orElseThrow(() -> new ApiException(ReviewErrorCode.REVIEW_NOT_FOUND));

        return new ReviewDetailResponse(
                row.getReviewId(),
                row.getMeetingId(),
                row.getRating(),
                row.getContent(),
                toKstLocalDateTime(row.getCreatedAt()),
                toKstLocalDateTime(row.getUpdatedAt()),
                row.getRestaurantId(),
                row.getRestaurantName(),
                row.getRestaurantImageUrl1(),
                row.getCategoryName(),
                row.getCategoryEmoji()
        );
    }
}