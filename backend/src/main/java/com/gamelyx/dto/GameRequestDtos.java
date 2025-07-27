package com.gamelyx.dto;

/**
 * DTOs para requests - RECORDS ideales por inmutabilidad
 */
public class GameRequestDtos {

    /**
     * Para: PUT /game/{identifier}/my-review → Request Body - RECORD
     */
    public record UpdateMyGameRequest(
            String status,    // WISHLIST, PLAYING, COMPLETED
            Integer rating,   // 1-10
            String reviewText // Texto de review
    ) {}
}
