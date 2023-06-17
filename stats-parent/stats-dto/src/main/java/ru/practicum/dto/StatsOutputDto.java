package ru.practicum.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatsOutputDto {

    private String app;
    private String uri;
    private Integer hits;

}
