package com.saasai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assistants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assistant {

    @Id
    @Column(name = "assistant_id")
    private Integer assistantId;

    @Column(name = "assistant_name", nullable = false)
    private String assistantName;
}