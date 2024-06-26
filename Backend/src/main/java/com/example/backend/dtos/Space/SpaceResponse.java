package com.example.backend.dtos.Space;

import com.example.backend.dtos.User.UserResponse;
import com.example.backend.enums.Availibility;
import com.example.backend.enums.SpaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SpaceResponse {
    private String spaceId;
    private String spaceName;
    private String spaceLocation;
    private String spaceDescription;
    private double spaceSize;
    private double spacePrice;
    private SpaceType spaceType;
    private UserResponse owner;
    private Availibility availability;
    private Date dateAdded;
    private Date dateUpdated;
    public Serializable getId() {
        return this.spaceId;
    }
}
