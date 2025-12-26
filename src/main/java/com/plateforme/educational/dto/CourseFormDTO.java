package com.plateforme.educational.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CourseFormDTO {
    private Long id;
    private String title;
    private String description;
    private String content;
    private String videoLinks;
}
