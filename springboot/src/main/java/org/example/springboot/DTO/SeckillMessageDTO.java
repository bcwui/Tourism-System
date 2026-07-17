package org.example.springboot.DTO;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class SeckillMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long activityId;
    private Long userId;
    private Long ticketId;
    private Integer quantity;
    private String visitorName;
    private String visitorPhone;
    private String idCard;
    private LocalDate visitDate;
}
