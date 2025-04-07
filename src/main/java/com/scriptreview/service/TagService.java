package com.scriptreview.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TagService {
    // Pattern pour capturer les emails complets après @
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");

    public List<String> extractMentions(String content) {
        List<String> mentions = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            mentions.add(matcher.group(1)); // Capture l'email complet sans le @
        }
        return mentions;
    }

    public List<String> extractHashtags(String content) {
        List<String> hashtags = new ArrayList<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        while (matcher.find()) {
            hashtags.add(matcher.group(1));
        }
        return hashtags;
    }
} 