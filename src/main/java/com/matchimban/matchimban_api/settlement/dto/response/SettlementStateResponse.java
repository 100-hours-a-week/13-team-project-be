package com.matchimban.matchimban_api.settlement.dto.response;

import com.matchimban.matchimban_api.settlement.enums.SettlementNextAction;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SettlementStateResponse {
    private Long settlementId;
    private SettlementStatus settlementStatus;
    private String myRole;
    private SettlementNextAction nextAction;
    private String message;
}