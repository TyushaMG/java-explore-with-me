package ru.practicum.categories.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CategoryInputDto {

    @NotBlank
    @Size(min = 1, max = 50)
    private String name;
}
