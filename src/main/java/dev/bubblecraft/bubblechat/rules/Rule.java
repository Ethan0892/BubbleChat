package dev.bubblecraft.bubblechat.rules;

import java.util.List;
import java.util.regex.Pattern;

public record Rule(
        String id,
        boolean enabled,
        RuleScope scope,
        Pattern pattern,
        List<RuleAction> actions
) {
}
