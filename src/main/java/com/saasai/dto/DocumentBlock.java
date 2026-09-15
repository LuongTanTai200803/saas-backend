package com.saasai.dto;

public record DocumentBlock(
        String text,
        BlockType type,
        String leftText,
        String rightText
) {}

