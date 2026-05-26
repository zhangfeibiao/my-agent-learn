package com.example.agentlearn.skill;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SkillSelector {
    public List<AgentSkill> select(String userQuery, List<AgentSkill> skills, boolean usesRag) {
        String normalizedQuery = normalize(userQuery);
        Set<AgentSkill> selected = new LinkedHashSet<>();

        for (AgentSkill skill : skills) {
            if (usesRag && "markdown-qa".equals(skill.name())) {
                selected.add(skill);
                continue;
            }
            if (score(normalizedQuery, skill) > 0) {
                selected.add(skill);
            }
        }

        return new ArrayList<>(selected);
    }

    private int score(String normalizedQuery, AgentSkill skill) {
        int score = 0;
        String name = normalize(skill.name());
        String description = normalize(skill.description());

        if (normalizedQuery.contains(name)) {
            score += 3;
        }
        for (String token : tokens(name + " " + description)) {
            if (token.length() <= 1) {
                continue;
            }
            if (normalizedQuery.contains(token) || description.contains(token) && containsAnyQueryToken(normalizedQuery, description)) {
                score++;
            }
        }
        return score;
    }

    private boolean containsAnyQueryToken(String normalizedQuery, String description) {
        for (String queryToken : tokens(normalizedQuery)) {
            if (queryToken.length() > 1 && description.contains(queryToken)) {
                return true;
            }
        }
        return false;
    }

    private List<String> tokens(String text) {
        String[] split = normalize(text).split("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHan}]+");
        List<String> tokens = new ArrayList<>();
        for (String token : split) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
}
