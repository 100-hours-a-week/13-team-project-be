package com.matchimban.matchimban_api.vote.service;

import com.matchimban.matchimban_api.restaurant.entity.Restaurant;
import com.matchimban.matchimban_api.restaurant.repository.ReviewRepository;
import com.matchimban.matchimban_api.vote.ai.dto.AiRecommendationResponse;
import com.matchimban.matchimban_api.vote.entity.MeetingRestaurantCandidate;
import com.matchimban.matchimban_api.vote.entity.Vote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface VoteCandidateAsyncService {

    void generateCandidates(Long meetingId, Long round1VoteId, Long round2VoteId);
}
