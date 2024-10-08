package com.example.chat.service;


import com.example.chat.dto.ChatMessageDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    public ChatMessageDto saveMessage(ChatMessageDto messageDto) {
        return new ChatMessageDto(
                messageDto.getSender(),
                messageDto.getMessage(),
                messageDto.getChatroomSeq(),
                messageDto.getSenderSeq(),
                messageDto.getTimestamp(),
                messageDto.getDatestamp(),
                messageDto.getUserProfile()
        );
    }

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper; // Jackson ObjectMapper

    private static final String CHAT_ROOM_PREFIX = "chatroom:";
    private static final String ANNOUNCEMENT_PREFIX = "announcement:";

    public void saveMessage(String chatroomSeq, ChatMessageDto messageDto) {
        String key = CHAT_ROOM_PREFIX + chatroomSeq;
        try {
            String messageJson = objectMapper.writeValueAsString(messageDto);
            redisTemplate.opsForList().rightPush(key, messageJson);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<ChatMessageDto> getMessages(String chatroomSeq) {
        String key = CHAT_ROOM_PREFIX + chatroomSeq;
        List<String> messagesJson = redisTemplate.opsForList().range(key, 0, -1);

        if (messagesJson == null) {
            return List.of();
        }

        return messagesJson.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, ChatMessageDto.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(messageDto -> messageDto != null)
                .collect(Collectors.toList());
    }

    // 채팅방의 마지막 메시지를 불러오는 메소드
    public ChatMessageDto getLastMessage(String chatroomSeq) {
        String key = CHAT_ROOM_PREFIX + chatroomSeq;
        String lastMessageJson = redisTemplate.opsForList().index(key, -1);
        System.out.println("last!!!!!"+lastMessageJson);

        if (lastMessageJson == null) {
            return null;
        }

        try {

            return objectMapper.readValue(lastMessageJson, ChatMessageDto.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 공지사항 저장
    public void saveAnnouncement(String chatroomSeq, String announcement) {
        String key = ANNOUNCEMENT_PREFIX + chatroomSeq;
        redisTemplate.opsForValue().set(key, announcement);
    }

    // 공지사항 조회
    public String getAnnouncement(String chatroomSeq) {
        String key = ANNOUNCEMENT_PREFIX + chatroomSeq;
        return redisTemplate.opsForValue().get(key);
    }

}
