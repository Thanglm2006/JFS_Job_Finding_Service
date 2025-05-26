package com.example.JFS_Job_Finding_Service.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class userInChat {
    private long id;
    private String name;
    private String avatar;
    private String status= "offline";
    public userInChat(long id, String name, String avatar) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
    }
}
